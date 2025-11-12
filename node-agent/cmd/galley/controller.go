package main

import (
  "fmt"
  "log"

  "github.com/spf13/cobra"
)

var controllerCmd = &cobra.Command{
  Use:   "controller",
  Short: "Setup and manage the controller node",
}

var controllerInstallCmd = &cobra.Command{
  Use:   "install <token>",
  Short: "Install K0s and configures this node as a controller node",
  Args:  cobra.ExactArgs(1),
  RunE: func(cmd *cobra.Command, args []string) error {
    token := args[0]

    checkCommands("k0s")
    if flagDryRun {
      fmt.Printf("[dry-run] controller install %s, platform=%s", token, flagPlatformURL)
      return nil
    }

    node, err := getGalleyNode(flagPlatformURL, token)
    if err != nil {
      return fmt.Errorf("couldn't fetch node details: %w", err)
    }

    var nodeType = node.Attributes.NodeType

    log.Printf("Hallo %s", nodeType)

    return nil
  },
}

func init() {
  controllerCmd.AddCommand(controllerInstallCmd /*controllerInviteCmd, controllerJoinCmd*/)
  //controllerInviteCmd.Flags().StringVar(&flagInviteExpiry, "expiry", "10m", "Token vervaltijd, bijv. 10m of 1h")
}
