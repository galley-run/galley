package main

import (
	"bufio"
	"context"
	"encoding/json"
	"fmt"
	"log"
	"os"
	"os/exec"
	"strings"
	"time"
)

type packageManager struct {
	name     string
	binaries []string
	commands [][]string
}

var packageManagers = []packageManager{
	{
		name:     "apt (Debian/Ubuntu)",
		binaries: []string{"apt-get"},
		commands: [][]string{
			{"apt-get", "update"},
			{"apt-get", "install", "-y", "unattended-upgrades"},
			{"apt-get", "upgrade", "-y"},
			{"apt-get", "dist-upgrade", "-y"},
			{"apt-get", "autoremove", "-y"},
			{"apt-get", "autoclean"},
		},
	},
	{
		name:     "dnf (Fedora/RHEL 8+)",
		binaries: []string{"dnf"},
		commands: [][]string{
			{"dnf", "install", "-y", "dnf-automatic"},
			{"dnf", "upgrade", "-y", "--refresh", "--security"},
		},
	},
	{
		name:     "yum (RHEL/CentOS 7)",
		binaries: []string{"yum"},
		commands: [][]string{
			{"yum", "install", "-y", "yum-cron"},
			{"yum", "update", "-y", "--security"},
		},
	},
	{
		name:     "zypper (openSUSE/SLES)",
		binaries: []string{"zypper"},
		commands: [][]string{
			{"zypper", "refresh"},
			{"zypper", "update", "-y", "--auto-agree-with-licenses"},
			{"zypper", "patch", "-y", "--category", "security"},
		},
	},
	{
		name:     "pacman (Arch Linux)",
		binaries: []string{"pacman"},
		commands: [][]string{
			{"pacman", "-Syu", "--noconfirm"},
		},
	},
	{
		name:     "apk (Alpine Linux)",
		binaries: []string{"apk"},
		commands: [][]string{
			{"apk", "update"},
			{"apk", "upgrade", "--available"},
		},
	},
}

func promptOSUpdate(progress *PrepareProgress) error {
	fmt.Println("\n" + strings.Repeat("=", 60))
	fmt.Println("OS Update")
	fmt.Println(strings.Repeat("=", 60))
	fmt.Println("It's recommended to update the OS before installing k0s.")
	fmt.Print("\nDo you want to update the OS now? [Y/n]: ")

	reader := bufio.NewReader(os.Stdin)
	response, err := reader.ReadString('\n')
	if err != nil {
		return fmt.Errorf("failed to read input: %w", err)
	}

	response = strings.TrimSpace(strings.ToLower(response))

	// Default to "yes" if user just presses Enter
	if response == "" || response == "y" || response == "yes" {
		if err := updateOS(progress, reader); err != nil {
			return err
		}
		// Mark step complete and set needs reboot flag
		progress.NeedsReboot = true
		return progress.markComplete(stepOSUpdate)
	}

	fmt.Println("\nOS update skipped.")
	return progress.markComplete(stepOSUpdate)
}

func updateOS(progress *PrepareProgress, reader *bufio.Reader) error {
	ctx := context.Background()

	for _, pm := range packageManagers {
		if isAvailable(pm.binaries) {
			fmt.Printf("Detected %s package manager\n", pm.name)

			if err := runCommandsWithContext(ctx, pm.commands); err != nil {
				return err
			}

			// Prompt for dist-upgrade on apt-based systems
			if pm.name == "apt (Debian/Ubuntu)" {
				if err := promptDistUpgrade(ctx, reader); err != nil {
					return err
				}
			}

			// Enable automatic security updates based on package manager
			var autoUpdateErr error
			switch pm.name {
			case "apt (Debian/Ubuntu)":
				autoUpdateErr = enableUnattendedUpgrades()
			case "dnf (Fedora/RHEL 8+)":
				autoUpdateErr = enableDnfAutomatic()
			case "yum (RHEL/CentOS 7)":
				autoUpdateErr = enableYumCron()
			}

			if autoUpdateErr != nil {
				fmt.Printf("⚠️  Warning: Failed to configure automatic security updates: %v\n", autoUpdateErr)
			} else if pm.name == "apt (Debian/Ubuntu)" || pm.name == "dnf (Fedora/RHEL 8+)" || pm.name == "yum (RHEL/CentOS 7)" {
				fmt.Println("✓ Automatic security updates enabled")
			}

			fmt.Println("✓ OS update completed")

			return nil
		}
	}

	return fmt.Errorf("unsupported package manager or distribution")
}

func promptDistUpgrade(ctx context.Context, reader *bufio.Reader) error {
	currentVersion := getCurrentOSVersion()

	// Check if do-release-upgrade is available
	if _, err := exec.LookPath("do-release-upgrade"); err != nil {
		return nil
	}

	const cfgPath = "/etc/update-manager/release-upgrades"

	original, err := os.ReadFile(cfgPath)
	if err == nil {
		currentPrompt := getReleasePromptFromConfig(string(original))

		// If the system is not on LTS track, ask if we may switch it to LTS
		if currentPrompt != "" && currentPrompt != "lts" {
			fmt.Println("\n" + strings.Repeat("-", 60))
			fmt.Println("Ubuntu Release Policy")
			fmt.Println(strings.Repeat("-", 60))
			fmt.Printf("Current policy: %s\n", currentPrompt)
			fmt.Println("\nGalley recommends using the LTS release track for servers,")
			fmt.Println("to minimise unexpected changes and keep upgrades predictable.")
			fmt.Print("\nSwitch release policy to LTS-only before checking for upgrades? [Y/n]: ")

			response, readErr := reader.ReadString('\n')
			if readErr != nil {
				return fmt.Errorf("failed to read input: %w", readErr)
			}
			response = strings.TrimSpace(strings.ToLower(response))

			if response == "" || response == "y" || response == "yes" {
				updated := updateReleaseUpgradesPrompt(string(original), "lts")
				if updated != string(original) {
					if err := os.WriteFile(cfgPath, []byte(updated), 0644); err != nil {
						fmt.Printf("⚠️  Warning: failed to update %s: %v\n", cfgPath, err)
						// Continue with existing policy, but we will still only *suggest* LTS upgrades
					} else {
						fmt.Println("✓ Release policy updated to LTS-only.")
					}
				}
			} else {
				fmt.Println("\nKeeping existing release policy. No distribution upgrade will be suggested.")
				return nil
			}
		}
	}

	return suggestLTSUpgrade(ctx, reader, currentVersion)
}

func suggestLTSUpgrade(ctx context.Context, reader *bufio.Reader, currentVersion string) error {
	ltsVersion, hasLTS := getAvailableLTSVersion()
	if !hasLTS || ltsVersion == "" || ltsVersion == currentVersion {
		fmt.Println("\nNo newer Ubuntu LTS release detected or distribution upgrade skipped.")
		return nil
	}

	fmt.Println("\n" + strings.Repeat("-", 60))
	fmt.Println("Distribution Upgrade (LTS)")
	fmt.Println(strings.Repeat("-", 60))
	fmt.Printf("Current version: %s\n", currentVersion)
	fmt.Printf("Available LTS:   %s\n", ltsVersion)
	fmt.Println("\nThis will perform a release upgrade to the latest Ubuntu LTS version.")
	fmt.Print("\nUpgrade to latest LTS? [Y/n]: ")

	response, err := reader.ReadString('\n')
	if err != nil {
		return fmt.Errorf("failed to read input: %w", err)
	}
	response = strings.TrimSpace(strings.ToLower(response))

	if response == "" || response == "y" || response == "yes" {
		fmt.Println("\nPerforming distribution upgrade to latest LTS...")
		if err := runCommandWithContext(
			ctx,
			"do-release-upgrade",
			"-f", "DistUpgradeViewNonInteractive",
			"-y",
		); err != nil {
			return fmt.Errorf("do-release-upgrade to LTS failed: %w", err)
		}
		fmt.Println("✓ Distribution upgrade to LTS completed")
		return nil
	}

	fmt.Println("\nLTS upgrade declined.")
	return nil
}

func getCurrentOSVersion() string {
	// Read /etc/os-release to get current version
	cmd := exec.Command("sh", "-c", "grep VERSION_ID /etc/os-release | cut -d'=' -f2 | tr -d '\"'")
	output, err := cmd.Output()
	if err != nil {
		// Fallback: try lsb_release
		cmd = exec.Command("lsb_release", "-rs")
		output, err = cmd.Output()
		if err != nil {
			return "unknown"
		}
	}
	return strings.TrimSpace(string(output))
}

func getAvailableLTSVersion() (string, bool) {
	// Check if update-manager-core/do-release-upgrade is available
	if _, err := exec.LookPath("do-release-upgrade"); err != nil {
		return "", false
	}

	cmd := exec.Command("do-release-upgrade", "--check-dist-upgrade-only")
	output, err := cmd.CombinedOutput()
	if err != nil {
		return "", false
	}

	outputStr := string(output)
	if strings.Contains(outputStr, "New release") {
		version := extractVersionFromOutput(outputStr)
		if version != "" {
			return version, true
		}
	}

	return "", false
}

func getReleasePromptFromConfig(content string) string {
	lines := strings.Split(content, "\n")
	for _, line := range lines {
		trimmed := strings.TrimSpace(line)
		if strings.HasPrefix(trimmed, "Prompt=") {
			return strings.TrimSpace(strings.TrimPrefix(trimmed, "Prompt="))
		}
	}
	return ""
}

// Changes or adds the Prompt= rule to the desired mode (e.g. "lts").
func updateReleaseUpgradesPrompt(content, prompt string) string {
	lines := strings.Split(content, "\n")
	found := false

	for i, line := range lines {
		trimmed := strings.TrimSpace(line)
		if strings.HasPrefix(trimmed, "Prompt=") {
			lines[i] = "Prompt=" + prompt
			found = true
			break
		}
	}

	if !found {
		lines = append(lines, "Prompt="+prompt)
	}

	return strings.Join(lines, "\n")
}

func extractVersionFromOutput(output string) string {
	// Look for pattern like "New release '24.04' available"
	lines := strings.Split(output, "\n")
	for _, line := range lines {
		if strings.Contains(line, "New release") {
			// Find text between single quotes
			start := strings.Index(line, "'")
			if start != -1 {
				end := strings.Index(line[start+1:], "'")
				if end != -1 {
					return line[start+1 : start+1+end]
				}
			}
		}
	}
	return ""
}

// Progress tracking
type PrepareProgress struct {
	CompletedSteps map[string]bool `json:"completed_steps"`
	NeedsReboot    bool            `json:"needs_reboot"`
}

func loadProgress() (*PrepareProgress, error) {
	data, err := os.ReadFile(galleyProgressFile)
	if err != nil {
		if os.IsNotExist(err) {
			return &PrepareProgress{
				CompletedSteps: make(map[string]bool),
			}, nil
		}
		return nil, err
	}

	var progress PrepareProgress
	if err := json.Unmarshal(data, &progress); err != nil {
		return nil, err
	}

	if progress.CompletedSteps == nil {
		progress.CompletedSteps = make(map[string]bool)
	}

	return &progress, nil
}

func (p *PrepareProgress) save() error {
	if err := os.MkdirAll(galleyStateDir, 0755); err != nil {
		return err
	}

	data, err := json.MarshalIndent(p, "", "  ")
	if err != nil {
		return err
	}

	return os.WriteFile(galleyProgressFile, data, 0644)
}

func (p *PrepareProgress) markComplete(step string) error {
	p.CompletedSteps[step] = true
	return p.save()
}

func (p *PrepareProgress) isComplete(step string) bool {
	return p.CompletedSteps[step]
}

func isResuming() bool {
	progress, err := loadProgress()
	if err != nil {
		return false
	}
	return len(progress.CompletedSteps) > 0
}

func isAvailable(binaries []string) bool {
	for _, binary := range binaries {
		if _, err := exec.LookPath(binary); err == nil {
			return true
		}
	}
	return false
}

func runCommandsWithContext(ctx context.Context, commands [][]string) error {
	for i, cmdArgs := range commands {
		start := time.Now()
		fmt.Printf("[%d/%d] Running: %s %s\n", i+1, len(commands), cmdArgs[0], strings.Join(cmdArgs[1:], " "))

		if err := runCommandWithContext(ctx, cmdArgs[0], cmdArgs[1:]...); err != nil {
			return fmt.Errorf("command %d failed: %w", i+1, err)
		}

		fmt.Printf("[%d/%d] Completed in %v\n", i+1, len(commands), time.Since(start).Round(time.Millisecond))
	}
	return nil
}

func runCommandWithContext(ctx context.Context, name string, args ...string) error {
	cmd := exec.CommandContext(ctx, name, args...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	// Log the command execution
	logCommand(name, args)

	if err := cmd.Run(); err != nil {
		logError(fmt.Sprintf("command: %s %v", name, args), err)
		return fmt.Errorf("%s failed: %w", name, err)
	}

	return nil
}

const (
	galleyStateDir       = "/var/lib/galley"
	galleyProgressFile   = "/var/lib/galley/prepare-progress.json"
	galleyResumeArgsFile = "/var/lib/galley/resume.args"
	galleyResumeWrapper  = "/usr/local/bin/galley-resume"
	galleyResumeUnit     = "galley-resume.service"
)

const (
	stepOSUpdate        = "Server OS is now up to date."
	stepSSHConfig       = "SSH configuration is improved and more secure."
	stepK0sInstall      = "K0s is installed."
	stepK0sConfig       = "K0s configuration is ready and configured."
	stepServerHardening = "Recommended server hardening is applied."
)

func promptRebootAtEnd(progress *PrepareProgress) error {
	if !progress.NeedsReboot {
		return nil
	}

	fmt.Println("\n" + strings.Repeat("=", 60))
	fmt.Println("Reboot Recommended")
	fmt.Println(strings.Repeat("=", 60))
	fmt.Println("OS updates have been installed. A reboot is recommended to apply")
	fmt.Println("all changes and ensure the system is running the latest kernel.")
	fmt.Println(strings.Repeat("=", 60))
	fmt.Print("\nDo you want to reboot now? [Y/n]: ")

	reader := bufio.NewReader(os.Stdin)
	response, err := reader.ReadString('\n')
	if err != nil {
		return fmt.Errorf("failed to read input: %w", err)
	}

	response = strings.TrimSpace(strings.ToLower(response))

	// Default to "yes" if user just presses Enter
	if response == "" || response == "y" || response == "yes" {
		// Clean up progress since we're done
		cleanupProgress()

		fmt.Println("\nRebooting system...")
		fmt.Println("The system will restart in 5 seconds...")

		time.Sleep(5 * time.Second)

		cmd := exec.Command("reboot")
		if err := cmd.Run(); err != nil {
			return fmt.Errorf("failed to reboot: %w", err)
		}

		// Wait indefinitely for the reboot to take effect
		fmt.Println("Waiting for system to reboot...")
		select {} // Block forever
	}

	fmt.Println("\nReboot skipped.")
	fmt.Println("⚠️  IMPORTANT: Please reboot the system later to apply all updates.")
	fmt.Println("You can reboot by running: sudo reboot")

	// Clean up progress file
	cleanupProgress()

	return nil
}

func cleanupProgress() {
	os.Remove(galleyProgressFile)
}

func enableUnattendedUpgrades() error {
	logAction("Configuring automatic security updates (apt/unattended-upgrades)", nil)

	// Configure unattended-upgrades to only install security updates automatically
	config := `APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Download-Upgradeable-Packages "1";
APT::Periodic::AutocleanInterval "7";
APT::Periodic::Unattended-Upgrade "1";
`
	configPath := "/etc/apt/apt.conf.d/20auto-upgrades"
	if err := os.WriteFile(configPath, []byte(config), 0644); err != nil {
		return fmt.Errorf("failed to write auto-upgrades config: %w", err)
	}
	logFileWrite(configPath, "APT automatic updates configuration")

	// Ensure unattended-upgrades is configured for security updates only
	unattendedConfig := `Unattended-Upgrade::Allowed-Origins {
	"${distro_id}:${distro_codename}-security";
	"${distro_id}ESMApps:${distro_codename}-apps-security";
	"${distro_id}ESM:${distro_codename}-infra-security";
};
Unattended-Upgrade::AutoFixInterruptedDpkg "true";
Unattended-Upgrade::MinimalSteps "true";
Unattended-Upgrade::Remove-Unused-Kernel-Packages "true";
Unattended-Upgrade::Remove-Unused-Dependencies "true";
Unattended-Upgrade::Automatic-Reboot "false";
`
	unattendedConfigPath := "/etc/apt/apt.conf.d/50unattended-upgrades"
	if err := os.WriteFile(unattendedConfigPath, []byte(unattendedConfig), 0644); err != nil {
		return fmt.Errorf("failed to write unattended-upgrades config: %w", err)
	}
	logFileWrite(unattendedConfigPath, "Unattended-upgrades security configuration")

	// Enable and start the unattended-upgrades service
	if err := exec.Command("systemctl", "enable", "unattended-upgrades").Run(); err != nil {
		return fmt.Errorf("failed to enable unattended-upgrades service: %w", err)
	}
	logServiceChange("unattended-upgrades", "enabled")

	if err := exec.Command("systemctl", "start", "unattended-upgrades").Run(); err != nil {
		return fmt.Errorf("failed to start unattended-upgrades service: %w", err)
	}
	logServiceChange("unattended-upgrades", "started")

	return nil
}

func enableDnfAutomatic() error {
	logAction("Configuring automatic security updates (dnf/dnf-automatic)", nil)

	// Configure dnf-automatic for security updates only
	config := `[commands]
upgrade_type = security
download_updates = yes
apply_updates = yes

[emitters]
emit_via = stdio

[email]
email_from = root@localhost
email_to = root
email_host = localhost
`
	configPath := "/etc/dnf/automatic.conf"
	if err := os.WriteFile(configPath, []byte(config), 0644); err != nil {
		return fmt.Errorf("failed to write dnf-automatic config: %w", err)
	}
	logFileWrite(configPath, "DNF automatic security updates configuration")

	// Enable and start dnf-automatic timer
	if err := exec.Command("systemctl", "enable", "dnf-automatic.timer").Run(); err != nil {
		return fmt.Errorf("failed to enable dnf-automatic timer: %w", err)
	}
	logServiceChange("dnf-automatic.timer", "enabled")

	if err := exec.Command("systemctl", "start", "dnf-automatic.timer").Run(); err != nil {
		return fmt.Errorf("failed to start dnf-automatic timer: %w", err)
	}
	logServiceChange("dnf-automatic.timer", "started")

	return nil
}

func enableYumCron() error {
	logAction("Configuring automatic security updates (yum/yum-cron)", nil)

	// Configure yum-cron for security updates only
	config := `[commands]
update_cmd = security
update_messages = yes
download_updates = yes
apply_updates = yes

[emitters]
emit_via = stdio

[email]
email_from = root@localhost
email_to = root
email_host = localhost
`
	configPath := "/etc/yum/yum-cron.conf"
	if err := os.WriteFile(configPath, []byte(config), 0644); err != nil {
		return fmt.Errorf("failed to write yum-cron config: %w", err)
	}
	logFileWrite(configPath, "YUM cron security updates configuration")

	// Enable and start yum-cron service
	if err := exec.Command("systemctl", "enable", "yum-cron").Run(); err != nil {
		return fmt.Errorf("failed to enable yum-cron: %w", err)
	}
	logServiceChange("yum-cron", "enabled")

	if err := exec.Command("systemctl", "start", "yum-cron").Run(); err != nil {
		return fmt.Errorf("failed to start yum-cron: %w", err)
	}
	logServiceChange("yum-cron", "started")

	return nil
}

func readOSRelease() (map[string]string, error) {
	file, err := os.Open("/etc/os-release")
	if err != nil {
		return nil, err
	}
	defer func(file *os.File) {
		err := file.Close()
		if err != nil {
			log.Printf("failed to close file: %v", err)
		}
	}(file)

	result := make(map[string]string)
	scanner := bufio.NewScanner(file)

	for scanner.Scan() {
		line := scanner.Text()
		if strings.Contains(line, "=") {
			parts := strings.SplitN(line, "=", 2)
			key := parts[0]
			val := strings.Trim(parts[1], `"`)
			result[key] = val
		}
	}

	return result, scanner.Err()
}
