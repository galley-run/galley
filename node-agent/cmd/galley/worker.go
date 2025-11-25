package main

import (
	"context"
	"fmt"
	"log"
	"os/exec"
	"strings"

	"github.com/spf13/cobra"
)

var workerCmd = &cobra.Command{
	Use:   "worker",
	Short: "Setup and manage the worker node",
}

var (
	flagInviteExpiry string
)

var workerInviteCmd = &cobra.Command{
	Use:   "invite",
	Short: "Create join token for workers to join your cluster",
	Long: `Creates join token so you can let a new node your your Galley cluster as a worker.

Prerequisites:
  - Node must be prepared first with 'galley node prepare'
  - Node should be running k0s and either configured as a controller or worker

This command will:
  - Generate worker join token with an expiry of 60 minutes`,
	Args: cobra.ExactArgs(0),
	RunE: func(cobraCmd *cobra.Command, args []string) error {
		config, err := loadConfig()
		if err != nil {
			return fmt.Errorf("failed to load Galley config: %w", err)
		}

		workerToken, err := generateWorkerToken(flagInviteExpiry)
		if err != nil {
			log.Printf("Warning: Failed to generate worker token: %v", err)
			log.Println("You can manually create a token later with: k0s token create --role worker")
		} else {
			displayWorkerJoinInstructions(config.VesselEngineId, workerToken)
		}

		return nil
	},
}

var workerJoinCmd = &cobra.Command{
	Use:   "join <token>",
	Short: "Join this node to your cluster as a controller (requires prepared node)",
	Long: `Connects this prepared node to your Galley cluster as a controller.

Prerequisites:
  - Node must be prepared first with 'galley node prepare'
  - Token from Galley web interface

This command will:
  - Fetch node configuration from Galley platform
  - Install k0s as a controller
  - Start the k0s service
  - Generate worker join tokens`,
	Args: cobra.ExactArgs(1),
	RunE: func(cobraCmd *cobra.Command, args []string) error {
		token := args[0]

		vesselEngineId, joinToken, err := decodeWorkerToken(token)
		if err != nil {
			return err
		}

		logAction("Starting worker join", map[string]string{
			"vessel_engine_id": vesselEngineId,
			"join_token":       joinToken,
		})

		platformURL := getPlatformURL()

		config, err := loadConfig()
		if err != nil {
			return fmt.Errorf("failed to load Galley config: %w", err)
		}
		if err := setConfigValue(config, "vessel_engine_id", vesselEngineId); err != nil {
			return fmt.Errorf("failed to save vessel_engine_id %w in Galley config", err)
		}
		logAction("VesselEngineId saved in Galley config", map[string]string{
			"vessel_engine_id": vesselEngineId,
		})
		if err := setConfigValue(config, "node_type", "worker"); err != nil {
			return fmt.Errorf("failed to save node_type %w in Galley config", err)
		}
		logAction("NodeType saved in Galley config", map[string]string{
			"node_type": "worker",
		})

		if flagDryRun {
			fmt.Printf("[dry-run] worker join %s, platform=%s\n", token, platformURL)
			return nil
		}

		// Verify k0s is installed (should be from node prepare)
		if _, err := exec.LookPath("k0s"); err != nil {
			return fmt.Errorf("k0s is not installed. Please run 'galley node prepare' first")
		}

		log.Printf("Joining cluster as: worker")

		// Install k0s controller
		if err := installK0sWorker(joinToken); err != nil {
			return fmt.Errorf("failed to install k0s controller: %w", err)
		}

		// Start k0s service
		if err := startK0sService(); err != nil {
			return fmt.Errorf("failed to start k0s service: %w", err)
		}

		fmt.Println("\n" + strings.Repeat("=", 60))
		fmt.Println("✓ Worker joined cluster successfully!")
		fmt.Println("Node type: worker")
		fmt.Println(strings.Repeat("=", 60))

		logAction("Controller join completed", map[string]string{
			"node_type": "worker",
			"status":    "success",
		})

		//if err := markGalleyNodeReady(platformURL, vesselEngineId, token); err != nil {
		//	return fmt.Errorf("failed to mark node as ready in Galley: %w", err)
		//}

		//fmt.Println("\n" + strings.Repeat("=", 60))
		//fmt.Println("✓ Controller marked as ready in Galley")
		//fmt.Println("\n" + strings.Repeat("=", 60))

		//logAction("Controller marking ready in Galley completed", map[string]string{
		//	"status": "success",
		//})

		// Generate worker token and display join instructions
		//workerToken, err := generateWorkerToken(flagInviteExpiry)
		//if err != nil {
		//	log.Printf("Warning: Failed to generate worker token: %v", err)
		//	log.Println("You can manually create a token later with: k0s token create --role worker")
		//} else {
		//	displayWorkerJoinInstructions(vesselEngineId, workerToken)
		//}

		// TODO: INSTALL/APPLY GALLEY NODE AGENT ON WORKER

		ctx := context.Background()

		//if err := runCommandWithContext(ctx, "k0s", "kubectl", "-n", "galley", "rollout", "restart", "deploy/galley-agent"); err != nil {
		//	return fmt.Errorf("failed to enable k0s service: %w", err)
		//}
		//kubectl --kubeconfig ~/Documents/kube-config-multipass.yaml -n galley rollout restart deploy/galley-agent
		//kubectl --kubeconfig ~/Documents/kube-config-multipass.yaml -n galley describe pod -l app=galley-agent | sed -n '/Events/,$p'

		fmt.Printf("\n💡 View all actions taken by Galley CLI: galley logs\n")
		fmt.Printf("   Log file location: %s\n\n", getLogPath())

		return nil
	},
}

func init() {
	workerCmd.AddCommand(workerInviteCmd)
	workerCmd.AddCommand(workerJoinCmd)
	workerInviteCmd.Flags().StringVar(&flagInviteExpiry, "expiry", "1h", "K0s token expiry time, e.g. 10m or 1h")
}
