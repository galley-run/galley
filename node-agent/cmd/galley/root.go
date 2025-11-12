package main

import (
	"github.com/spf13/cobra"
)

var (
	flagPlatformURL string
	flagDryRun      bool
)

var rootCmd = &cobra.Command{
	Use:   "galley",
	Short: "Galley Node Agent",
	Long:  "The Galley Node Agent allows you to manage Galley nodes and setup your Kubernetes cluster.",
}

func init() {
	rootCmd.PersistentFlags().StringVar(&flagPlatformURL, "platform-url", "api.galley.run", "Use if you need a different Platform API url")
	rootCmd.PersistentFlags().BoolVar(&flagDryRun, "dry-run", false, "Show all steps we will take during a command, without executing them.")

	rootCmd.AddCommand(controllerCmd)
}
