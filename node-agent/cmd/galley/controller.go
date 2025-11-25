package main

import (
	"encoding/base64"
	"encoding/json"
	"fmt"
	"log"
	"os/exec"
	"strings"

	"github.com/spf13/cobra"
)

var controllerCmd = &cobra.Command{
	Use:   "controller",
	Short: "Setup and manage the controller node",
}

// extractJWTSubject extracts the subject claim from a JWT token
func extractJWTSubject(token string) (string, error) {
	parts := strings.Split(token, ".")
	if len(parts) != 3 {
		return "", fmt.Errorf("invalid JWT token format")
	}

	payload, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return "", fmt.Errorf("failed to decode JWT payload: %w", err)
	}

	var claims struct {
		Sub string `json:"sub"`
	}
	if err := json.Unmarshal(payload, &claims); err != nil {
		return "", fmt.Errorf("failed to parse JWT claims: %w", err)
	}

	return claims.Sub, nil
}

var controllerJoinCmd = &cobra.Command{
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

		vesselEngineNodeId, err := extractJWTSubject(token)
		if err != nil {
			return err
		}

		logAction("Starting controller join", map[string]string{
			"vessel_engine_node_id": vesselEngineNodeId,
		})

		platformURL := getPlatformURL()

		if flagDryRun {
			fmt.Printf("[dry-run] controller join %s, platform=%s\n", token, platformURL)
			return nil
		}

		// Verify k0s is installed (should be from node prepare)
		if _, err := exec.LookPath("k0s"); err != nil {
			return fmt.Errorf("k0s is not installed. Please run 'galley node prepare' first")
		}

		// Fetch node details from platform
		node, err := getGalleyNode(platformURL, token)
		if err != nil {
			return fmt.Errorf("couldn't fetch node details: %w", err)
		}

		nodeType := node.Attributes.NodeType
		vesselEngineId := node.Attributes.VesselEngineID

		config, err := loadConfig()
		if err := setConfigValue(config, "vessel_engine_id", vesselEngineId); err != nil {
			return fmt.Errorf("failed to save vessel_engine_id %w in Galley config", err)
		}
		logAction("VesselEngineId saved in Galley config", map[string]string{
			"vessel_engine_id": vesselEngineId,
		})
		if err := setConfigValue(config, "node_type", nodeType); err != nil {
			return fmt.Errorf("failed to save node_type %w in Galley config", err)
		}
		logAction("NodeType saved in Galley config", map[string]string{
			"node_type": nodeType,
		})

		log.Printf("Joining cluster as: %s", nodeType)

		// Install k0s controller
		if err := installK0sController(nodeType); err != nil {
			return fmt.Errorf("failed to install k0s controller: %w", err)
		}

		// Start k0s service
		if err := startK0sService(); err != nil {
			return fmt.Errorf("failed to start k0s service: %w", err)
		}

		fmt.Println("\n" + strings.Repeat("=", 60))
		fmt.Println("✓ Controller joined cluster successfully!")
		fmt.Printf("Node type: %s\n", nodeType)
		fmt.Println(strings.Repeat("=", 60))

		logAction("Controller join completed", map[string]string{
			"node_type": nodeType,
			"status":    "success",
		})

		if err := markGalleyNodeReady(platformURL, vesselEngineNodeId, token); err != nil {
			return fmt.Errorf("failed to mark node as ready in Galley: %w", err)
		}

		fmt.Println("\n" + strings.Repeat("=", 60))
		fmt.Println("✓ Controller marked as ready in Galley")
		fmt.Println("\n" + strings.Repeat("=", 60))

		logAction("Controller marking ready in Galley completed", map[string]string{
			"status": "success",
		})

		if nodeType == "controller" {
			// Generate worker token and display join instructions
			workerToken, err := generateWorkerToken(flagInviteExpiry)
			if err != nil {
				log.Printf("Warning: Failed to generate worker token: %v", err)
				log.Println("You can manually create a token later with: k0s token create --role worker")
			} else {
				displayWorkerJoinInstructions(vesselEngineId, workerToken)
			}
		}

		fmt.Printf("\n💡 All actions taken by Galley CLI are logged and can be viewed via: galley logs\n")
		fmt.Printf("   Log file location: %s\n\n", getLogPath())

		return nil
	},
}

func init() {
	controllerCmd.AddCommand(controllerJoinCmd)
	controllerJoinCmd.Flags().StringVar(&flagInviteExpiry, "expiry", "1h", "K0s token expiry time, e.g. 10m or 1h")
}
