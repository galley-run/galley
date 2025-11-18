package main

import (
	"github.com/spf13/cobra"
)

var (
	// Version can be set at build time using -ldflags
	Version = "dev"

	flagPlatformURL string
	flagClientURL   string
	flagDryRun      bool
)

var rootCmd = &cobra.Command{
	Use:     "galley",
	Short:   "Galley Node Agent",
	Long:    "The Galley Node Agent allows you to manage Galley nodes and setup your Kubernetes cluster.",
	Version: Version,
}

func init() {
	rootCmd.PersistentFlags().StringVar(&flagPlatformURL, "platform-url", "", "Use if you need a different Platform API url (overrides config)")
	rootCmd.PersistentFlags().StringVar(&flagClientURL, "client-url", "", "Use if you need a different Client API url (overrides config)")
	rootCmd.PersistentFlags().BoolVar(&flagDryRun, "dry-run", false, "Show all steps we will take during a command, without executing them.")

	rootCmd.AddCommand(nodeCmd)
	rootCmd.AddCommand(controllerCmd)
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
