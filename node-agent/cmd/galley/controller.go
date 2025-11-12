package main

import (
  "fmt"

  "github.com/spf13/cobra"
)

var controllerCmd = &cobra.Command{
  Use:   "controller",
  Short: "Setup and manage the controller node",
}

var controllerInstallCmd = &cobra.Command{
  Use:   "install <node-id>",
  Short: "Install K0s and configures this node as a controller node",
  Args:  cobra.ExactArgs(1),
  RunE: func(cmd *cobra.Command, args []string) error {
    nodeID := args[0]

    checkCommands("k0s")
    if flagDryRun {
      fmt.Printf("[dry-run] controller install %s, platform=%s", nodeID, flagPlatformURL)
      return nil
    }

    role, err := platformDesiredRole(flagPlatformURL, nodeID, "")
    if err != nil {
      return fmt.Errorf("platformDesiredRole %w", err)
    }
  },
}

func init() {
  controllerCmd.AddCommand(controllerInstallCmd /*controllerInviteCmd, controllerJoinCmd*/)
  //controllerInviteCmd.Flags().StringVar(&flagInviteExpiry, "expiry", "10m", "Token vervaltijd, bijv. 10m of 1h")
}
