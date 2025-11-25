package main

import (
	"bufio"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"strings"

	"github.com/spf13/cobra"
)

var nodeCmd = &cobra.Command{
	Use:   "node",
	Short: "Prepare and manage nodes",
	Long:  "Commands for preparing nodes for Galley and managing node configuration",
}

var (
	flagNodePrepareSkipOSUpdate bool
)

var nodePrepareCmd = &cobra.Command{
	Use:   "prepare",
	Short: "Prepare this node for Galley (OS updates, k0s installation)",
	Long: `Prepares this node to become part of a Galley cluster by:
  - Updating the OS and configuring automatic security updates
  - Installing k0s
  - Creating and configuring k0s
  - Rebooting if necessary

After preparation, use 'galley controller join <token>' to connect to your cluster.`,
	RunE: runNodePrepare,
}

func init() {
	nodePrepareCmd.Flags().BoolVar(&flagNodePrepareSkipOSUpdate, "skip-os-update", false, "Skip OS update step")
	nodeCmd.AddCommand(nodePrepareCmd)
}

func runNodePrepare(cmd *cobra.Command, args []string) error {
	logAction("Starting node preparation", nil)

	// Load progress to check what's already done
	progress, err := loadProgress()
	if err != nil {
		return fmt.Errorf("failed to load progress: %w", err)
	}

	// Show resume message if resuming
	if len(progress.CompletedSteps) > 0 {
		fmt.Println("\n" + strings.Repeat("=", 70))
		fmt.Println("Resuming node preparation...")
		fmt.Println("Already completed:")
		// Show completed steps in order
		orderedSteps := []string{stepOSUpdate, stepSSHConfig, stepK0sInstall, stepK0sConfig, stepServerHardening}
		for _, step := range orderedSteps {
			if progress.CompletedSteps[step] {
				fmt.Printf("  ✓ %s\n", step)
			}
		}
		fmt.Println(strings.Repeat("=", 70))
	}

	// Only update OS if not skipped and not already done
	if !flagNodePrepareSkipOSUpdate && !progress.isComplete(stepOSUpdate) {
		if err := promptOSUpdate(progress); err != nil {
			return err
		}
	}

	// Configure SSH security after OS update
	if !progress.isComplete(stepSSHConfig) {
		if err := configureSSHSecurity(); err != nil {
			return fmt.Errorf("failed to configure SSH security: %w", err)
		}
		if err := progress.markComplete(stepSSHConfig); err != nil {
			return fmt.Errorf("failed to mark SSH config complete: %w", err)
		}
	}

	if flagDryRun {
		fmt.Println("[dry-run] Would prepare node (install k0s, create config)")
		return nil
	}

	// Ensure k0s is installed
	if !progress.isComplete(stepK0sInstall) {
		if err := ensureK0sInstalled(); err != nil {
			return fmt.Errorf("failed to ensure k0s is installed: %w", err)
		}
		if err := progress.markComplete(stepK0sInstall); err != nil {
			return fmt.Errorf("failed to mark k0s install complete: %w", err)
		}
	}

	// Create k0s config
	if !progress.isComplete(stepK0sConfig) {
		if err := createK0sConfig(); err != nil {
			return fmt.Errorf("failed to create k0s config: %w", err)
		}

		// Prompt to edit config
		if err := promptEditK0sConfig(); err != nil {
			return fmt.Errorf("failed to edit k0s config: %w", err)
		}

		if err := progress.markComplete(stepK0sConfig); err != nil {
			return fmt.Errorf("failed to mark k0s config complete: %w", err)
		}
	}

	// Final security configuration
	if !progress.isComplete(stepServerHardening) {
		if err := performServerHardening(); err != nil {
			return fmt.Errorf("failed to perform final security configuration: %w", err)
		}
		if err := progress.markComplete(stepServerHardening); err != nil {
			return fmt.Errorf("failed to mark final security configuration complete: %w", err)
		}
	}

	// Get k0s version to show user
	version, err := getK0sVersion()
	if err != nil {
		version = "unknown"
	}

	fmt.Println("\n" + strings.Repeat("=", 70))
	fmt.Println("✓ Node preparation completed successfully!")
	fmt.Println(strings.Repeat("=", 70))
	fmt.Printf("k0s version: %s\n", strings.TrimSpace(version))
	fmt.Println("\nNext steps:")
	fmt.Println("  1. Open Galley in your browser to get a join token for the next step:")
	fmt.Printf("     %s/vessel/engine/node/controller\n\n", getClientURL())
	fmt.Println("  2. Run the next step like: galley controller join <token>")
	fmt.Println(strings.Repeat("=", 70))

	logAction("Node preparation completed", map[string]string{
		"status": "success",
	})

	fmt.Printf("\n💡 View all actions taken: galley logs\n\n")

	// Ask for reboot at the end if needed
	if err := promptRebootAtEnd(progress); err != nil {
		return err
	}

	return nil
}

func configureSSHSecurity() error {
	fmt.Println("\n" + strings.Repeat("=", 70))
	fmt.Println("SSH Security Configuration")
	fmt.Println(strings.Repeat("=", 70))

	// Check if SSH is installed and prompt to install if missing
	if err := promptInstallSSH(); err != nil {
		return err
	}

	// Check again if SSH is installed after potential installation
	if _, err := os.Stat("/etc/ssh/sshd_config"); os.IsNotExist(err) {
		fmt.Println("SSH server not installed, skipping SSH security configuration.")
		return nil
	}

	// Disable root login
	if err := promptDisableSSHRootLogin(); err != nil {
		return err
	}

	// Check password authentication
	if err := checkPasswordAuthentication(); err != nil {
		return err
	}

	return nil
}

func promptInstallSSH() error {
	// Check if SSH is already installed
	if _, err := os.Stat("/etc/ssh/sshd_config"); err == nil {
		return nil // SSH is already installed
	}

	fmt.Println("\n⚠️  SSH server is not installed.")
	fmt.Println("For remote server management, it's recommended to have SSH installed.")
	fmt.Print("\nDo you want to install SSH server? [Y/n]: ")

	reader := bufio.NewReader(os.Stdin)
	response, err := reader.ReadString('\n')
	if err != nil {
		return fmt.Errorf("failed to read input: %w", err)
	}

	response = strings.TrimSpace(strings.ToLower(response))

	// Default to "yes"
	if response == "" || response == "y" || response == "yes" {
		if err := installSSH(); err != nil {
			return fmt.Errorf("failed to install SSH: %w", err)
		}
		fmt.Println("✓ SSH server installed successfully")
	} else {
		fmt.Println("SSH installation skipped.")
	}

	return nil
}

func promptDisableSSHRootLogin() error {
	// Check current root login setting
	rootLoginEnabled, err := isSSHRootLoginEnabled()
	if err != nil {
		return fmt.Errorf("failed to check SSH root login status: %w", err)
	}

	if !rootLoginEnabled {
		fmt.Println("✓ SSH root login is already disabled")
		return nil
	}

	// Before disabling root login, ensure there's a sudo user who can access the system
	if err := checkSudoAccess(); err != nil {
		fmt.Printf("\n⚠️  Warning: %v\n", err)
		fmt.Println("It's recommended to set up a non-root user with sudo access before disabling root login.")
		fmt.Println("Otherwise, you may lose administrative access to this server.")
		fmt.Print("\nDo you still want to disable SSH root login? [y/N]: ")

		reader := bufio.NewReader(os.Stdin)
		response, err := reader.ReadString('\n')
		if err != nil {
			return fmt.Errorf("failed to read input: %w", err)
		}

		response = strings.TrimSpace(strings.ToLower(response))

		// In this case, default to "no" for safety
		if response != "y" && response != "yes" {
			fmt.Println("SSH root login remains enabled.")
			return nil
		}
	} else {
		fmt.Println("\n✓ Verified: Non-root user with sudo access exists")
	}

	fmt.Println("\n⚠️  SSH root login is currently enabled.")
	fmt.Println("For security, it's recommended to disable direct root login via SSH.")
	fmt.Print("\nDo you want to disable SSH root login? [Y/n]: ")

	reader := bufio.NewReader(os.Stdin)
	response, err := reader.ReadString('\n')
	if err != nil {
		return fmt.Errorf("failed to read input: %w", err)
	}

	response = strings.TrimSpace(strings.ToLower(response))

	// Default to "yes"
	if response == "" || response == "y" || response == "yes" {
		if err := disableSSHRootLogin(); err != nil {
			return err
		}
		fmt.Println("✓ SSH root login disabled successfully")
		logAction("Disabled SSH root login", nil)
	} else {
		fmt.Println("SSH root login remains enabled.")
	}

	return nil
}

func isSSHRootLoginEnabled() (bool, error) {
	content, err := os.ReadFile("/etc/ssh/sshd_config")
	if err != nil {
		return false, err
	}

	lines := strings.Split(string(content), "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		// Skip comments
		if strings.HasPrefix(line, "#") {
			continue
		}
		// Check for PermitRootLogin directive
		if strings.HasPrefix(line, "PermitRootLogin") {
			parts := strings.Fields(line)
			if len(parts) >= 2 {
				value := strings.ToLower(parts[1])
				return value == "yes" || value == "prohibit-password" || value == "without-password", nil
			}
		}
	}
	// Default is usually yes in most distributions
	return true, nil
}

func disableSSHRootLogin() error {
	content, err := os.ReadFile("/etc/ssh/sshd_config")
	if err != nil {
		return err
	}

	lines := strings.Split(string(content), "\n")
	modified := false
	found := false

	for i, line := range lines {
		trimmed := strings.TrimSpace(line)
		if strings.HasPrefix(trimmed, "PermitRootLogin") && !strings.HasPrefix(trimmed, "#") {
			lines[i] = "PermitRootLogin no"
			modified = true
			found = true
			break
		}
	}

	// If not found, append it
	if !found {
		lines = append(lines, "", "# Added by Galley", "PermitRootLogin no")
		modified = true
	}

	if modified {
		newContent := strings.Join(lines, "\n")
		if err := os.WriteFile("/etc/ssh/sshd_config", []byte(newContent), 0600); err != nil {
			return err
		}
		logFileWrite("/etc/ssh/sshd_config", "Disabled SSH root login")

		// Validate the config before restarting
		fmt.Println("Validating SSH configuration...")
		validateCmd := exec.Command("sshd", "-t")
		validateCmd.Stdout = os.Stdout
		validateCmd.Stderr = os.Stderr
		if err := validateCmd.Run(); err != nil {
			return fmt.Errorf("SSH configuration validation failed: %w\nPlease check /etc/ssh/sshd_config", err)
		}
		fmt.Println("✓ SSH configuration is valid")

		// Restart SSH service
		if err := restartSSHService(); err != nil {
			return fmt.Errorf("failed to restart SSH service: %w", err)
		}
	}

	return nil
}

func checkPasswordAuthentication() error {
	passwordAuthEnabled, err := isPasswordAuthenticationEnabled()
	if err != nil {
		return fmt.Errorf("failed to check password authentication status: %w", err)
	}

	if !passwordAuthEnabled {
		fmt.Println("✓ Password authentication is already disabled")
		return nil
	}

	fmt.Println("\n⚠️  Password authentication is currently enabled.")
	fmt.Println("For enhanced security, it's recommended to use SSH key authentication instead.")
	fmt.Println("\nTo set up SSH key authentication:")
	fmt.Println("  1. On your local machine, generate an SSH key (if you don't have one):")
	fmt.Println("     ssh-keygen -t ed25519 -C \"your_email@example.com\"")
	fmt.Println("  2. Copy your public key to this server:")
	fmt.Println("     ssh-copy-id user@this-server")
	fmt.Println("     OR manually add your public key (~/.ssh/id_ed25519.pub) to:")
	fmt.Println("     ~/.ssh/authorized_keys on this server")

	fmt.Print("\nWould you like to open ~/.ssh/authorized_keys in your default editor to add your SSH key? [y/N]: ")

	reader := bufio.NewReader(os.Stdin)
	response, err := reader.ReadString('\n')
	if err != nil {
		return fmt.Errorf("failed to read input: %w", err)
	}

	response = strings.TrimSpace(strings.ToLower(response))

	if response == "y" || response == "yes" {
		if err := openAuthorizedKeys(); err != nil {
			fmt.Printf("⚠️  Failed to open authorized_keys: %v\n", err)
			fmt.Println("You can manually edit it later at: ~/.ssh/authorized_keys")
		}
	} else {
		fmt.Println("\n💡 Remember to add your SSH key before disabling password authentication!")
	}

	return nil
}

func isPasswordAuthenticationEnabled() (bool, error) {
	content, err := os.ReadFile("/etc/ssh/sshd_config")
	if err != nil {
		return false, err
	}

	lines := strings.Split(string(content), "\n")
	for _, line := range lines {
		line = strings.TrimSpace(line)
		// Skip comments
		if strings.HasPrefix(line, "#") {
			continue
		}
		// Check for PasswordAuthentication directive
		if strings.HasPrefix(line, "PasswordAuthentication") {
			parts := strings.Fields(line)
			if len(parts) >= 2 {
				return strings.ToLower(parts[1]) == "yes", nil
			}
		}
	}
	// Default is usually yes
	return true, nil
}

func openAuthorizedKeys() error {
	homeDir, err := getRealUserHomeDir()
	if err != nil {
		return err
	}

	sshDir := filepath.Join(homeDir, ".ssh")
	authorizedKeysPath := filepath.Join(sshDir, "authorized_keys")

	// Create .ssh directory if it doesn't exist
	if err := os.MkdirAll(sshDir, 0700); err != nil {
		return err
	}

	// Create authorized_keys file if it doesn't exist
	if _, err := os.Stat(authorizedKeysPath); os.IsNotExist(err) {
		if err := os.WriteFile(authorizedKeysPath, []byte(""), 0600); err != nil {
			return err
		}
	}

	// Determine editor
	editor := os.Getenv("EDITOR")
	if editor == "" {
		editor = os.Getenv("VISUAL")
	}
	if editor == "" {
		// Try common editors
		for _, e := range []string{"nano", "vim", "vi"} {
			if _, err := exec.LookPath(e); err == nil {
				editor = e
				break
			}
		}
	}
	if editor == "" {
		return fmt.Errorf("no editor found (set EDITOR environment variable)")
	}

	fmt.Printf("\nOpening %s with %s...\n", authorizedKeysPath, editor)
	fmt.Println("Add your SSH public key (one per line), then save and exit.")
	fmt.Println("Press Enter to continue...")
	bufio.NewReader(os.Stdin).ReadString('\n')

	cmd := exec.Command(editor, authorizedKeysPath)
	cmd.Stdin = os.Stdin
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	if err := cmd.Run(); err != nil {
		return err
	}

	logAction("Opened authorized_keys for editing", map[string]string{
		"path": authorizedKeysPath,
	})

	// After editing authorized_keys, prompt to disable password authentication
	fmt.Println("\n" + strings.Repeat("=", 70))
	fmt.Println("Now that you've added your SSH key, you can disable password authentication")
	fmt.Println("for enhanced security.")
	fmt.Println(strings.Repeat("=", 70))
	fmt.Print("\nDisable SSH password authentication now? [Y/n]: ")

	reader2 := bufio.NewReader(os.Stdin)
	response2, err := reader2.ReadString('\n')
	if err != nil {
		fmt.Printf("⚠️  Failed to read input: %v\n", err)
		fmt.Println("You can disable password authentication later during the final security steps.")
		return nil
	}

	response2 = strings.TrimSpace(strings.ToLower(response2))

	// Default to "yes"
	if response2 == "" || response2 == "y" || response2 == "yes" {
		if err := disablePasswordAuthentication(); err != nil {
			fmt.Printf("⚠️  Failed to disable password authentication: %v\n", err)
			fmt.Println("You can try again later during the final security steps.")
			return nil
		}
		fmt.Println("✓ SSH password authentication disabled successfully")
		logAction("Disabled SSH password authentication", nil)
	} else {
		fmt.Println("💡 You can disable password authentication later during the final security steps.")
	}

	return nil
}

func performServerHardening() error {
	fmt.Println("\n" + strings.Repeat("=", 70))
	fmt.Println("Final Security Configuration")
	fmt.Println(strings.Repeat("=", 70))

	// 1. Install security and monitoring tools
	if err := installSecurityTools(); err != nil {
		return err
	}

	// 2. Enforce strong password policy
	if err := enforcePasswordPolicy(); err != nil {
		return err
	}

	// 3. Lock root user
	if err := promptLockRootUser(); err != nil {
		return err
	}

	// 4. Set default systemd target to multi-user
	if err := setMultiUserTarget(); err != nil {
		return err
	}

	// 5. Disable SSH password authentication
	if err := promptDisablePasswordAuth(); err != nil {
		return err
	}

	// 6. Check for FTP services
	if err := checkFTPServices(); err != nil {
		return err
	}

	// 7. Scan and warn about open ports
	if err := scanOpenPorts(); err != nil {
		return err
	}

	fmt.Println("\n✓ Security configuration checks completed")
	return nil
}

func promptDisablePasswordAuth() error {
	passwordAuthEnabled, err := isPasswordAuthenticationEnabled()
	if err != nil {
		return fmt.Errorf("failed to check password authentication status: %w", err)
	}

	if !passwordAuthEnabled {
		fmt.Println("✓ SSH password authentication is already disabled")
		return nil
	}

	fmt.Println("\n⚠️  SSH password authentication is currently enabled.")
	fmt.Println("For enhanced security, it's strongly recommended to disable password")
	fmt.Println("authentication and use SSH key authentication only.")
	fmt.Print("\nDisable SSH password authentication now? [Y/n]: ")

	reader := bufio.NewReader(os.Stdin)
	response, err := reader.ReadString('\n')
	if err != nil {
		return fmt.Errorf("failed to read input: %w", err)
	}

	response = strings.TrimSpace(strings.ToLower(response))

	// Default to "yes"
	if response == "" || response == "y" || response == "yes" {
		if err := disablePasswordAuthentication(); err != nil {
			return fmt.Errorf("failed to disable password authentication: %w", err)
		}
		fmt.Println("✓ SSH password authentication disabled successfully")
		logAction("Disabled SSH password authentication", nil)
	} else {
		fmt.Println("⚠️  Password authentication remains enabled")
	}

	return nil
}

func disablePasswordAuthentication() error {
	// Read current sshd_config
	content, err := os.ReadFile("/etc/ssh/sshd_config")
	if err != nil {
		return err
	}

	lines := strings.Split(string(content), "\n")
	modified := false
	foundDirective := false

	for i, line := range lines {
		trimmed := strings.TrimSpace(line)
		if strings.HasPrefix(trimmed, "PasswordAuthentication") {
			foundDirective = true
			if !strings.Contains(trimmed, "PasswordAuthentication no") {
				lines[i] = "PasswordAuthentication no"
				modified = true
			}
		}
	}

	// If directive not found, add it
	if !foundDirective {
		lines = append(lines, "", "# Disabled by Galley", "PasswordAuthentication no")
		modified = true
	}

	if modified {
		newContent := strings.Join(lines, "\n")
		if err := os.WriteFile("/etc/ssh/sshd_config", []byte(newContent), 0644); err != nil {
			return err
		}

		// Restart SSH service
		cmd := exec.Command("systemctl", "restart", "sshd")
		if err := cmd.Run(); err != nil {
			// Try alternative service name
			cmd = exec.Command("systemctl", "restart", "ssh")
			if err := cmd.Run(); err != nil {
				return fmt.Errorf("failed to restart SSH service: %w", err)
			}
		}
	}

	return nil
}

func checkFTPServices() error {
	fmt.Println("\nChecking for FTP services...")

	// Check for common FTP services
	ftpServices := []string{"vsftpd", "proftpd", "pure-ftpd"}
	foundServices := []string{}

	for _, service := range ftpServices {
		cmd := exec.Command("systemctl", "is-active", service)
		output, err := cmd.Output()
		if err == nil && strings.TrimSpace(string(output)) == "active" {
			foundServices = append(foundServices, service)
		}
	}

	if len(foundServices) == 0 {
		fmt.Println("✓ No active FTP services found")
		return nil
	}

	fmt.Println("\n⚠️  WARNING: Active FTP services detected:")
	for _, service := range foundServices {
		fmt.Printf("  - %s\n", service)
	}
	fmt.Println("\nFTP is an insecure protocol. Consider using SFTP (SSH File Transfer Protocol) instead.")
	fmt.Print("\nWould you like to disable these FTP services? [y/N]: ")

	reader := bufio.NewReader(os.Stdin)
	response, err := reader.ReadString('\n')
	if err != nil {
		return fmt.Errorf("failed to read input: %w", err)
	}

	response = strings.TrimSpace(strings.ToLower(response))

	if response == "y" || response == "yes" {
		for _, service := range foundServices {
			cmd := exec.Command("systemctl", "stop", service)
			if err := cmd.Run(); err != nil {
				fmt.Printf("⚠️  Failed to stop %s: %v\n", service, err)
				continue
			}
			cmd = exec.Command("systemctl", "disable", service)
			if err := cmd.Run(); err != nil {
				fmt.Printf("⚠️  Failed to disable %s: %v\n", service, err)
				continue
			}
			fmt.Printf("✓ Stopped and disabled %s\n", service)
			logAction("Disabled FTP service", map[string]string{"service": service})
		}
	} else {
		fmt.Println("⚠️  FTP services remain active")
	}

	return nil
}

func scanOpenPorts() error {
	fmt.Println("\nScanning for open ports...")

	// Use ss command to list listening ports
	cmd := exec.Command("ss", "-tuln")
	output, err := cmd.Output()
	if err != nil {
		// Try netstat as fallback
		cmd = exec.Command("netstat", "-tuln")
		output, err = cmd.Output()
		if err != nil {
			fmt.Println("⚠️  Could not scan ports (ss/netstat not available)")
			return nil
		}
	}

	// Parse output to find listening ports
	lines := strings.Split(string(output), "\n")
	openPorts := make(map[string]bool)

	for _, line := range lines {
		if strings.Contains(line, "LISTEN") || strings.Contains(line, "UNCONN") {
			fields := strings.Fields(line)
			if len(fields) >= 5 {
				// Extract port from address:port format
				localAddr := fields[4]
				if idx := strings.LastIndex(localAddr, ":"); idx != -1 {
					port := localAddr[idx+1:]
					openPorts[port] = true
				}
			}
		}
	}

	// Known safe/expected ports
	expectedPorts := map[string]string{
		"22":    "SSH",
		"53":    "DNS",
		"68":    "DHCP client",
		"6443":  "Kubernetes API",
		"8132":  "konnectivity",
		"9443":  "k0s API",
		"10250": "kubelet",
	}

	unexpectedPorts := []string{}
	for port := range openPorts {
		if _, expected := expectedPorts[port]; !expected && port != "*" && port != "0.0.0.0" {
			unexpectedPorts = append(unexpectedPorts, port)
		}
	}

	if len(unexpectedPorts) > 0 {
		fmt.Println("\n⚠️  WARNING: Unexpected open ports detected:")
		for _, port := range unexpectedPorts {
			fmt.Printf("  - Port %s\n", port)
		}
		fmt.Println("\nReview these ports and ensure they're necessary for your setup.")
		fmt.Println("Consider using a firewall (ufw/iptables) to restrict access.")
	} else {
		fmt.Println("✓ No unexpected open ports detected")
	}

	logAction("Scanned open ports", map[string]string{
		"open_ports_count": fmt.Sprintf("%d", len(openPorts)),
	})

	return nil
}

func checkSudoAccess() error {
	// First check if sudo is installed
	if _, err := exec.LookPath("sudo"); err != nil {
		return fmt.Errorf("sudo is not installed on this system")
	}

	// Get current user
	currentUser := os.Getenv("USER")
	if currentUser == "" || currentUser == "root" {
		// If running as root, check if there are other users with sudo access
		return checkOtherSudoUsers()
	}

	// Check if current user is in sudo/wheel group
	groups, err := exec.Command("groups", currentUser).Output()
	if err != nil {
		return fmt.Errorf("failed to check user groups: %w", err)
	}

	groupList := strings.ToLower(string(groups))
	hasSudoGroup := strings.Contains(groupList, "sudo") || strings.Contains(groupList, "wheel")

	if !hasSudoGroup {
		return fmt.Errorf("current user '%s' is not in sudo/wheel group", currentUser)
	}

	// Test if user can actually use sudo (not just in the group)
	testCmd := exec.Command("sudo", "-n", "true")
	if err := testCmd.Run(); err != nil {
		return fmt.Errorf("current user '%s' cannot execute sudo commands (may need password setup or sudoers configuration)", currentUser)
	}

	return nil
}

func checkOtherSudoUsers() error {
	// Check if there are non-root users in sudo/wheel groups

	// Try sudo group first (Debian/Ubuntu)
	if output, err := exec.Command("getent", "group", "sudo").Output(); err == nil {
		members := parseSudoGroupMembers(string(output))
		if len(members) > 0 {
			return nil // Found users in sudo group
		}
	}

	// Try wheel group (RHEL/CentOS/Fedora)
	if output, err := exec.Command("getent", "group", "wheel").Output(); err == nil {
		members := parseSudoGroupMembers(string(output))
		if len(members) > 0 {
			return nil // Found users in wheel group
		}
	}

	return fmt.Errorf("no non-root users with sudo access found")
}

func parseSudoGroupMembers(groupLine string) []string {
	// Group line format: groupname:x:gid:member1,member2,member3
	parts := strings.Split(strings.TrimSpace(groupLine), ":")
	if len(parts) < 4 {
		return nil
	}

	membersStr := parts[3]
	if membersStr == "" {
		return nil
	}

	members := strings.Split(membersStr, ",")
	// Filter out empty strings and root
	var nonRootMembers []string
	for _, member := range members {
		member = strings.TrimSpace(member)
		if member != "" && member != "root" {
			nonRootMembers = append(nonRootMembers, member)
		}
	}

	return nonRootMembers
}

func restartSSHService() error {
	// Try common SSH service names
	services := []string{"sshd", "ssh"}

	for _, service := range services {
		// Check if service exists using systemctl status (returns exit code 4 if not found)
		checkCmd := exec.Command("systemctl", "status", service)
		output, err := checkCmd.CombinedOutput()

		// If service exists (even if inactive), systemctl status won't return "not found"
		if err != nil && strings.Contains(string(output), "not be found") {
			continue // Try next service name
		}

		// Service exists, try to reload first (less disruptive)
		reloadCmd := exec.Command("systemctl", "reload", service)
		reloadCmd.Stdout = os.Stdout
		reloadCmd.Stderr = os.Stderr

		if err := reloadCmd.Run(); err != nil {
			// If reload fails, try restart
			fmt.Printf("⚠️  Reload failed, trying restart...\n")
			restartCmd := exec.Command("systemctl", "restart", service)
			restartCmd.Stdout = os.Stdout
			restartCmd.Stderr = os.Stderr

			if err := restartCmd.Run(); err != nil {
				// Get more details about the failure
				statusCmd := exec.Command("systemctl", "status", service)
				statusOutput, _ := statusCmd.CombinedOutput()
				return fmt.Errorf("failed to restart %s: %w\nStatus output:\n%s", service, err, string(statusOutput))
			}
			logServiceChange(service, "restarted")
		} else {
			logServiceChange(service, "reloaded")
		}
		return nil
	}

	return fmt.Errorf("SSH service not found (tried: %s)", strings.Join(services, ", "))
}

func installSSH() error {
	logAction("Installing SSH server", nil)

	type sshInstaller struct {
		name     string
		binaries []string
		packages []string
		service  string
	}

	installers := []sshInstaller{
		{
			name:     "apt (Debian/Ubuntu)",
			binaries: []string{"apt-get"},
			packages: []string{"openssh-server"},
			service:  "ssh",
		},
		{
			name:     "dnf (Fedora/RHEL 8+)",
			binaries: []string{"dnf"},
			packages: []string{"openssh-server"},
			service:  "sshd",
		},
		{
			name:     "yum (RHEL/CentOS 7)",
			binaries: []string{"yum"},
			packages: []string{"openssh-server"},
			service:  "sshd",
		},
		{
			name:     "zypper (openSUSE/SLES)",
			binaries: []string{"zypper"},
			packages: []string{"openssh"},
			service:  "sshd",
		},
		{
			name:     "pacman (Arch Linux)",
			binaries: []string{"pacman"},
			packages: []string{"openssh"},
			service:  "sshd",
		},
		{
			name:     "apk (Alpine Linux)",
			binaries: []string{"apk"},
			packages: []string{"openssh"},
			service:  "sshd",
		},
	}

	for _, installer := range installers {
		if !isAvailable(installer.binaries) {
			continue
		}

		fmt.Printf("Detected %s package manager\n", installer.name)

		var installCmd *exec.Cmd
		switch installer.name {
		case "apt (Debian/Ubuntu)":
			installCmd = exec.Command("apt-get", "install", "-y", installer.packages[0])
		case "dnf (Fedora/RHEL 8+)":
			installCmd = exec.Command("dnf", "install", "-y", installer.packages[0])
		case "yum (RHEL/CentOS 7)":
			installCmd = exec.Command("yum", "install", "-y", installer.packages[0])
		case "zypper (openSUSE/SLES)":
			installCmd = exec.Command("zypper", "install", "-y", installer.packages[0])
		case "pacman (Arch Linux)":
			installCmd = exec.Command("pacman", "-S", "--noconfirm", installer.packages[0])
		case "apk (Alpine Linux)":
			installCmd = exec.Command("apk", "add", installer.packages[0])
		}

		if installCmd != nil {
			installCmd.Stdout = os.Stdout
			installCmd.Stderr = os.Stderr
			fmt.Printf("Running: %s\n", strings.Join(installCmd.Args, " "))

			if err := installCmd.Run(); err != nil {
				return fmt.Errorf("failed to install SSH: %w", err)
			}

			// Enable and start SSH service
			if err := exec.Command("systemctl", "enable", installer.service).Run(); err != nil {
				return fmt.Errorf("failed to enable SSH service: %w", err)
			}
			logServiceChange(installer.service, "enabled")

			if err := exec.Command("systemctl", "start", installer.service).Run(); err != nil {
				return fmt.Errorf("failed to start SSH service: %w", err)
			}
			logServiceChange(installer.service, "started")

			logAction("SSH server installed and started", map[string]string{
				"package": installer.packages[0],
				"service": installer.service,
			})

			return nil
		}
	}

	return fmt.Errorf("unsupported package manager or distribution")
}

func installSecurityTools() error {
	fmt.Println("\nInstalling security and monitoring tools...")

	// Detect package manager
	type installer struct {
		name     string
		binaries []string
		packages []string
	}

	installers := []installer{
		{
			name:     "apt (Debian/Ubuntu)",
			binaries: []string{"apt-get"},
			packages: []string{"fail2ban", "htop"},
		},
		{
			name:     "dnf (Fedora/RHEL 8+)",
			binaries: []string{"dnf"},
			packages: []string{"fail2ban", "htop"},
		},
		{
			name:     "yum (RHEL/CentOS 7)",
			binaries: []string{"yum"},
			packages: []string{"fail2ban", "htop"},
		},
		{
			name:     "zypper (openSUSE/SLES)",
			binaries: []string{"zypper"},
			packages: []string{"fail2ban", "htop"},
		},
		{
			name:     "pacman (Arch Linux)",
			binaries: []string{"pacman"},
			packages: []string{"fail2ban", "htop"},
		},
		{
			name:     "apk (Alpine Linux)",
			binaries: []string{"apk"},
			packages: []string{"fail2ban", "htop"},
		},
	}

	for _, inst := range installers {
		if !isAvailable(inst.binaries) {
			continue
		}

		fmt.Printf("Detected %s package manager\n", inst.name)

		var installCmd *exec.Cmd
		switch inst.name {
		case "apt (Debian/Ubuntu)":
			installCmd = exec.Command("apt-get", "install", "-y", "fail2ban", "htop")
		case "dnf (Fedora/RHEL 8+)":
			installCmd = exec.Command("dnf", "install", "-y", "fail2ban", "htop")
		case "yum (RHEL/CentOS 7)":
			installCmd = exec.Command("yum", "install", "-y", "fail2ban", "htop")
		case "zypper (openSUSE/SLES)":
			installCmd = exec.Command("zypper", "install", "-y", "fail2ban", "htop")
		case "pacman (Arch Linux)":
			installCmd = exec.Command("pacman", "-S", "--noconfirm", "fail2ban", "htop")
		case "apk (Alpine Linux)":
			installCmd = exec.Command("apk", "add", "fail2ban", "htop")
		}

		if installCmd != nil {
			installCmd.Stdout = os.Stdout
			installCmd.Stderr = os.Stderr
			fmt.Printf("Running: %s\n", strings.Join(installCmd.Args, " "))

			if err := installCmd.Run(); err != nil {
				fmt.Printf("⚠️  Failed to install security tools: %v\n", err)
				return err
			}

			// Enable and start fail2ban service
			if err := exec.Command("systemctl", "enable", "fail2ban").Run(); err != nil {
				fmt.Printf("⚠️  Failed to enable fail2ban service: %v\n", err)
			} else {
				logServiceChange("fail2ban", "enabled")
			}

			if err := exec.Command("systemctl", "start", "fail2ban").Run(); err != nil {
				fmt.Printf("⚠️  Failed to start fail2ban service: %v\n", err)
			} else {
				logServiceChange("fail2ban", "started")
			}

			logAction("Installed security tools", map[string]string{
				"packages": "fail2ban, htop",
			})

			fmt.Println("✓ Security tools installed successfully")
			return nil
		}
	}

	return fmt.Errorf("unsupported package manager or distribution")
}

func enforcePasswordPolicy() error {
	fmt.Println("\nEnforcing strong password policy...")

	// Configure /etc/login.defs
	if err := configureLoginDefs(); err != nil {
		fmt.Printf("⚠️  Failed to configure login.defs: %v\n", err)
	}

	// Configure PAM password quality
	if err := configurePAMPasswordQuality(); err != nil {
		fmt.Printf("⚠️  Failed to configure PAM password quality: %v\n", err)
	}

	fmt.Println("✓ Password policy configured")
	logAction("Configured password policy", nil)
	return nil
}

func configureLoginDefs() error {
	loginDefsPath := "/etc/login.defs"

	// Read current login.defs
	content, err := os.ReadFile(loginDefsPath)
	if err != nil {
		return err
	}

	lines := strings.Split(string(content), "\n")
	settings := map[string]string{
		"PASS_MAX_DAYS": "90", // Password expires after 90 days
		"PASS_MIN_DAYS": "1",  // Minimum days before password can be changed
		"PASS_MIN_LEN":  "14", // Minimum password length
		"PASS_WARN_AGE": "7",  // Warning days before password expiration
	}

	modified := false
	foundSettings := make(map[string]bool)

	// Update existing settings
	for i, line := range lines {
		trimmed := strings.TrimSpace(line)
		if strings.HasPrefix(trimmed, "#") || trimmed == "" {
			continue
		}

		for setting, value := range settings {
			if strings.HasPrefix(trimmed, setting) {
				foundSettings[setting] = true
				expectedLine := fmt.Sprintf("%s\t%s", setting, value)
				if lines[i] != expectedLine {
					lines[i] = expectedLine
					modified = true
				}
				break
			}
		}
	}

	// Add missing settings
	for setting, value := range settings {
		if !foundSettings[setting] {
			lines = append(lines, fmt.Sprintf("\n# Added by Galley\n%s\t%s", setting, value))
			modified = true
		}
	}

	if modified {
		newContent := strings.Join(lines, "\n")
		if err := os.WriteFile(loginDefsPath, []byte(newContent), 0644); err != nil {
			return err
		}
		logFileWrite(loginDefsPath, "Configured password expiration and length policies")
		fmt.Println("✓ Updated /etc/login.defs with password policies")
	} else {
		fmt.Println("✓ /etc/login.defs already configured")
	}

	return nil
}

func configurePAMPasswordQuality() error {
	// Try common PAM password files
	pamFiles := []string{
		"/etc/pam.d/common-password", // Debian/Ubuntu
		"/etc/pam.d/system-auth",     // RHEL/CentOS
		"/etc/pam.d/password-auth",   // RHEL/CentOS
	}

	var pamFile string
	for _, file := range pamFiles {
		if _, err := os.Stat(file); err == nil {
			pamFile = file
			break
		}
	}

	if pamFile == "" {
		fmt.Println("⚠️  PAM password configuration file not found, skipping PAM setup")
		return nil
	}

	content, err := os.ReadFile(pamFile)
	if err != nil {
		return err
	}

	lines := strings.Split(string(content), "\n")
	modified := false
	found := false

	// Check if pam_pwquality or pam_cracklib is already configured
	for i, line := range lines {
		trimmed := strings.TrimSpace(line)
		if strings.Contains(trimmed, "pam_pwquality.so") || strings.Contains(trimmed, "pam_cracklib.so") {
			found = true
			// Update the line if it doesn't have our settings
			if !strings.Contains(trimmed, "minlen=14") {
				if strings.Contains(trimmed, "pam_pwquality.so") {
					lines[i] = "password    requisite     pam_pwquality.so retry=3 minlen=14 dcredit=-1 ucredit=-1 ocredit=-1 lcredit=-1"
				} else {
					lines[i] = "password    requisite     pam_cracklib.so retry=3 minlen=14 dcredit=-1 ucredit=-1 ocredit=-1 lcredit=-1"
				}
				modified = true
			}
			break
		}
	}

	// If not found, try to add it after the first "password" line
	if !found {
		for i, line := range lines {
			if strings.Contains(line, "password") && !strings.HasPrefix(strings.TrimSpace(line), "#") {
				// Check if pwquality is available, otherwise use cracklib
				module := "pam_pwquality.so"
				if _, err := os.Stat("/lib/security/pam_cracklib.so"); err == nil {
					module = "pam_cracklib.so"
				}
				newLine := fmt.Sprintf("password    requisite     %s retry=3 minlen=14 dcredit=-1 ucredit=-1 ocredit=-1 lcredit=-1", module)
				// Insert after this line
				lines = append(lines[:i+1], append([]string{newLine}, lines[i+1:]...)...)
				modified = true
				break
			}
		}
	}

	if modified {
		newContent := strings.Join(lines, "\n")
		if err := os.WriteFile(pamFile, []byte(newContent), 0644); err != nil {
			return err
		}
		logFileWrite(pamFile, "Configured PAM password quality requirements")
		fmt.Printf("✓ Updated %s with password quality requirements\n", pamFile)
		fmt.Println("  - Minimum length: 14 characters")
		fmt.Println("  - Requires: uppercase, lowercase, digit, and special character")
	} else {
		fmt.Printf("✓ %s already configured\n", pamFile)
	}

	return nil
}

func promptLockRootUser() error {
	// Check if root account is already locked
	cmd := exec.Command("passwd", "-S", "root")
	output, err := cmd.Output()
	if err != nil {
		fmt.Printf("⚠️  Could not check root account status: %v\n", err)
		return nil
	}

	status := string(output)
	// Status format: "root L" (L = locked), "root P" (P = password set)
	if strings.Contains(status, " L ") {
		fmt.Println("✓ Root user account is already locked")
		return nil
	}

	fmt.Println("\n⚠️  Root user account is currently unlocked.")
	fmt.Println("For security, it's recommended to lock the root account and use sudo for")
	fmt.Println("administrative tasks.")
	fmt.Print("\nDo you want to lock the root user account? [Y/n]: ")

	reader := bufio.NewReader(os.Stdin)
	response, err := reader.ReadString('\n')
	if err != nil {
		return fmt.Errorf("failed to read input: %w", err)
	}

	response = strings.TrimSpace(strings.ToLower(response))

	// Default to "yes"
	if response == "" || response == "y" || response == "yes" {
		// Verify there's a sudo user before locking root
		if err := checkSudoAccess(); err != nil {
			fmt.Printf("\n⚠️  Warning: %v\n", err)
			fmt.Println("It's recommended to set up a non-root user with sudo access before locking root.")
			fmt.Print("\nDo you still want to lock the root account? [y/N]: ")

			reader2 := bufio.NewReader(os.Stdin)
			response2, err := reader2.ReadString('\n')
			if err != nil {
				return fmt.Errorf("failed to read input: %w", err)
			}

			response2 = strings.TrimSpace(strings.ToLower(response2))
			if response2 != "y" && response2 != "yes" {
				fmt.Println("Root account remains unlocked.")
				return nil
			}
		}

		// Lock the root account
		lockCmd := exec.Command("passwd", "-l", "root")
		if err := lockCmd.Run(); err != nil {
			return fmt.Errorf("failed to lock root account: %w", err)
		}

		fmt.Println("✓ Root user account locked successfully")
		logAction("Locked root user account", nil)
	} else {
		fmt.Println("Root account remains unlocked.")
	}

	return nil
}

func setMultiUserTarget() error {
	fmt.Println("\nSetting default systemd target to multi-user...")

	// Check current default target
	cmd := exec.Command("systemctl", "get-default")
	output, err := cmd.Output()
	if err != nil {
		fmt.Printf("⚠️  Could not check current default target: %v\n", err)
		return nil
	}

	currentTarget := strings.TrimSpace(string(output))
	if currentTarget == "multi-user.target" {
		fmt.Println("✓ Default target is already set to multi-user.target")
		return nil
	}

	fmt.Printf("Current default target: %s\n", currentTarget)
	fmt.Println("Setting to multi-user.target (console mode, no GUI)...")

	// Set default target to multi-user
	setCmd := exec.Command("systemctl", "set-default", "multi-user.target")
	setCmd.Stdout = os.Stdout
	setCmd.Stderr = os.Stderr

	if err := setCmd.Run(); err != nil {
		return fmt.Errorf("failed to set default target: %w", err)
	}

	fmt.Println("✓ Default target set to multi-user.target")
	logAction("Set systemd default target to multi-user", map[string]string{
		"previous_target": currentTarget,
	})

	return nil
}
