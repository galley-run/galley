package main

import (
	"bufio"
	"fmt"
	"io"
	"net/http"
	"os"
	"os/exec"
	"strings"
	"time"

	"github.com/spf13/cobra"
)

var (
	// Version can be set at build time using -ldflags
	Version = "dev"

	flagPlatformURL     string
	flagClientURL       string
	flagDryRun          bool
	flagSkipUpdateCheck bool
)

var rootCmd = &cobra.Command{
	Use:               "galley",
	Short:             "Galley Node Agent",
	Long:              "The Galley Node Agent allows you to manage Galley nodes and setup your Kubernetes cluster.",
	Version:           Version,
	PersistentPreRunE: checkForUpdates,
}

func init() {
	rootCmd.PersistentFlags().StringVar(&flagPlatformURL, "platform-url", "", "Use if you need a different Platform API url (overrides config)")
	rootCmd.PersistentFlags().StringVar(&flagClientURL, "client-url", "", "Use if you need a different Client API url (overrides config)")
	rootCmd.PersistentFlags().BoolVar(&flagDryRun, "dry-run", false, "Show all steps we will take during a command, without executing them.")
	rootCmd.PersistentFlags().BoolVar(&flagSkipUpdateCheck, "skip-update-check", false, "Skip checking for updates before running commands.")

	rootCmd.AddCommand(nodeCmd)
	rootCmd.AddCommand(controllerCmd)
	rootCmd.AddCommand(workerCmd)
	rootCmd.AddCommand(updateCmd)
	rootCmd.AddCommand(configCmd)
	rootCmd.AddCommand(logsCmd)
}

// getPlatformURL returns the platform URL from flag, config, or default
func getPlatformURL() string {
	if flagPlatformURL != "" {
		return flagPlatformURL
	}

	config, err := loadConfig()
	if err == nil && config.PlatformURL != "" {
		return config.PlatformURL
	}

	return "api.galley.run"
}

// getClientURL returns the client URL from flag, config, or default
func getClientURL() string {
	if flagClientURL != "" {
		return flagClientURL
	}

	config, err := loadConfig()
	if err == nil && config.ClientURL != "" {
		return config.ClientURL
	}

	return "https://cloud.galley.run"
}

// getDownloadBase returns the download base URL from config or default
func getDownloadBase() string {
	config, err := loadConfig()
	if err == nil && config.DownloadBase != "" {
		return config.DownloadBase
	}
	return "https://get.galley.run"
}

// checkForUpdates checks if a newer version is available and prompts the user to update
func checkForUpdates(cmd *cobra.Command, args []string) error {
	if os.Geteuid() != 0 {
		fmt.Println("This command needs sudo, retrying the command with sudo:")
		// Re-exec with sudo
		execArgs := append([]string{os.Args[0]}, os.Args[1:]...)
		c := exec.Command("sudo", execArgs...)
		c.Stdin = os.Stdin
		c.Stdout = os.Stdout
		c.Stderr = os.Stderr
		err := c.Run()
		if err != nil {
			fmt.Println("Failed to run with sudo:", err)
		}
		os.Exit(0)
	}

	// Skip update check for certain commands and flags
	if flagSkipUpdateCheck || cmd.Name() == "update" || cmd.Name() == "version" || cmd.Name() == "help" {
		return nil
	}

	// Skip if version is "dev" (development build)
	if Version == "dev" {
		return nil
	}

	// Get latest version from server with timeout
	downloadBase := getDownloadBase()
	url := fmt.Sprintf("%s/latest", downloadBase)

	client := &http.Client{
		Timeout: 2 * time.Second, // Quick timeout to avoid blocking
	}

	resp, err := client.Get(url)
	if err != nil {
		// Silently skip update check if server is unreachable
		return nil
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		// Silently skip if endpoint doesn't exist yet
		return nil
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil
	}

	responseText := strings.TrimSpace(string(body))

	// Parse versions from response
	// Expected format:
	// Current version:     24.04
	// Available LTS:       26.04 LTS
	// Available non-LTS:   26.10
	var ltsVersion, nonLtsVersion string
	lines := strings.Split(responseText, "\n")
	for _, line := range lines {
		lowerLine := strings.ToLower(line)
		if strings.Contains(lowerLine, "available") {
			parts := strings.Split(line, ":")
			if len(parts) >= 2 {
				// Get the version (after the colon)
				versionPart := strings.TrimSpace(parts[1])

				// Check if the version value has "LTS" suffix (actual marker)
				hasLtsSuffix := strings.HasSuffix(strings.ToUpper(versionPart), "LTS")

				// Remove "LTS" suffix for clean version number
				cleanVersion := strings.TrimSuffix(versionPart, "LTS")
				cleanVersion = strings.TrimSuffix(cleanVersion, "lts")
				cleanVersion = strings.TrimSpace(cleanVersion)

				// Prefer the actual LTS marker in the version over the label
				// This handles cases like "Available non-LTS: 26.04 LTS"
				if hasLtsSuffix {
					ltsVersion = cleanVersion
				} else {
					nonLtsVersion = cleanVersion
				}
			}
		}
	}

	// Normalize versions for comparison (remove 'v' prefix if present)
	currentVersion := strings.TrimPrefix(Version, "v")
	normalizedLtsVersion := strings.TrimPrefix(ltsVersion, "v")
	normalizedNonLtsVersion := strings.TrimPrefix(nonLtsVersion, "v")

	reader := bufio.NewReader(os.Stdin)

	// First, check if LTS version is available and different from current
	if ltsVersion != "" && normalizedLtsVersion != currentVersion {
		fmt.Printf("\n⚠️  A new LTS version of galley is available: v%s (current: v%s)\n", normalizedLtsVersion, currentVersion)
		fmt.Printf("Would you like to update to the LTS version now? [Y/n] ")

		response, err := reader.ReadString('\n')
		if err != nil {
			return nil
		}

		response = strings.ToLower(strings.TrimSpace(response))

		// Default to "yes" if empty response
		if response == "" || response == "y" || response == "yes" {
			fmt.Println("\nRunning update...")
			flagUpdateVersion = ltsVersion
			updateCmd.Run(cmd, args)
			fmt.Println("\nUpdate complete. Please run your command again.")
			os.Exit(0)
		}
	}

	// If user declined LTS or no LTS available, check for non-LTS
	if nonLtsVersion != "" && normalizedNonLtsVersion != currentVersion && normalizedNonLtsVersion != normalizedLtsVersion {
		fmt.Printf("\n⚠️  A newer non-LTS version is also available: v%s\n", normalizedNonLtsVersion)
		fmt.Printf("Would you like to update to the non-LTS version? [Y/n] ")

		response, err := reader.ReadString('\n')
		if err != nil {
			return nil
		}

		response = strings.ToLower(strings.TrimSpace(response))

		// Default to "yes" if empty response
		if response == "" || response == "y" || response == "yes" {
			fmt.Println("\nRunning update...")
			flagUpdateVersion = nonLtsVersion
			updateCmd.Run(cmd, args)
			fmt.Println("\nUpdate complete. Please run your command again.")
			os.Exit(0)
		}
	}

	// User declined all updates or already on latest
	if ltsVersion != "" || nonLtsVersion != "" {
		fmt.Println("Continuing without update...")
	}
	return nil
}
