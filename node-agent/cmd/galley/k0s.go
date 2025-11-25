package main

import (
  "context"
  "encoding/base64"
  "fmt"
  "os"
  "os/exec"
  "runtime"
  "strings"
)

const (
  k0sInstallScript = "https://get.k0s.sh"
  k0sBinaryPath    = "/usr/local/bin/k0s"
  k0sConfigDir     = "/etc/k0s"
  k0sConfigFile    = "/etc/k0s/k0s.yaml"
  downloadBaseURL  = "https://get.galley.run"
)

// ensureK0sInstalled checks if k0s is installed and installs it if not
func ensureK0sInstalled() error {
  fmt.Println("\n" + strings.Repeat("=", 70))
  fmt.Println("k0s Installation")
  fmt.Println(strings.Repeat("=", 70))

  // Check if k0s is already installed
  if _, err := exec.LookPath("k0s"); err == nil {
    fmt.Println("✓ k0s is already installed")
    return nil
  }

  fmt.Println("k0s not found, installing...")
  logAction("Installing k0s", nil)

  // Only support Linux for k0s installation
  if runtime.GOOS != "linux" {
    return fmt.Errorf("k0s installation is only supported on Linux, current OS: %s", runtime.GOOS)
  }

  // Download and run k0s install script
  fmt.Println("Installing k0s...")
  ctx := context.Background()
  installCmd := fmt.Sprintf("curl -sSLf %s | sudo sh", k0sInstallScript)
  if err := runCommandWithContext(ctx, "sh", "-c", installCmd); err != nil {
    return fmt.Errorf("failed to install k0s: %w", err)
  }

  // Verify installation
  if _, err := exec.LookPath("k0s"); err != nil {
    return fmt.Errorf("k0s installation completed but binary not found in PATH")
  }

  fmt.Println("✓ k0s installed successfully")
  return nil
}

// getK0sVersion returns the installed k0s version
func getK0sVersion() (string, error) {
  cmd := exec.Command("k0s", "version")
  output, err := cmd.Output()
  if err != nil {
    return "", fmt.Errorf("failed to get k0s version: %w", err)
  }
  return string(output), nil
}

// createK0sConfig creates the default k0s configuration file
func createK0sConfig() error {
  // Check if config already exists
  if _, err := os.Stat(k0sConfigFile); err == nil {
    fmt.Printf("✓ k0s configuration already exists at %s\n", k0sConfigFile)
    return nil
  }

  fmt.Println("Creating k0s configuration...")

  // Create k0s config directory
  if err := os.MkdirAll(k0sConfigDir, 0755); err != nil {
    return fmt.Errorf("failed to create k0s config directory: %w", err)
  }

  // Generate default config
  execCmd := exec.Command("k0s", "config", "create")
  output, err := execCmd.Output()
  if err != nil {
    return fmt.Errorf("failed to generate k0s config: %w", err)
  }

  if len(output) == 0 {
    return fmt.Errorf("k0s config create returned empty output")
  }

  // Write config to file
  if err := os.WriteFile(k0sConfigFile, output, 0644); err != nil {
    return fmt.Errorf("failed to write k0s config: %w", err)
  }

  fmt.Printf("✓ k0s configuration created at %s\n", k0sConfigFile)
  return nil
}

// promptEditK0sConfig asks the user if they want to edit the k0s config
func promptEditK0sConfig() error {
  // Verify config file exists
  if _, err := os.Stat(k0sConfigFile); os.IsNotExist(err) {
    return fmt.Errorf("config file does not exist: %s", k0sConfigFile)
  }

  fmt.Print("\nWould you like to view and/or edit the k0s configuration file? [y/N]: ")

  var response string
  fmt.Scanln(&response)
  response = strings.TrimSpace(strings.ToLower(response))

  if response != "y" && response != "yes" {
    return nil
  }

  // Find available editor
  editors := []string{"nano", "vi", "vim"}
  var editor string
  for _, e := range editors {
    if _, err := exec.LookPath(e); err == nil {
      editor = e
      break
    }
  }

  if editor == "" {
    return fmt.Errorf("no text editor found (tried: %s)", strings.Join(editors, ", "))
  }

  fmt.Printf("Opening configuration with %s...\n", editor)

  // Open editor - editors need interactive terminal, so use exec directly
  execCmd := exec.Command(editor, k0sConfigFile)
  execCmd.Stdin = os.Stdin
  execCmd.Stdout = os.Stdout
  execCmd.Stderr = os.Stderr

  if err := execCmd.Run(); err != nil {
    return fmt.Errorf("failed to edit configuration: %w", err)
  }

  fmt.Println("✓ Configuration saved")
  return nil
}

// installK0sController installs k0s as a controller or controller+worker
func installK0sController(nodeType string) error {
  fmt.Println("\nInstalling k0s controller...")
  logAction("Installing k0s controller", map[string]string{
    "node_type": nodeType,
  })

  ctx := context.Background()

  // Determine the role based on node type and build command
  var args []string
  switch strings.ToLower(nodeType) {
  case "controller":
    fmt.Println("Installing as controller-only node")
    args = []string{"install", "controller", "-c", k0sConfigFile}
  case "controller+worker":
    fmt.Println("Installing as controller+worker node")
    args = []string{"install", "controller", "--enable-worker", "--no-taints", "-c", k0sConfigFile}
  //case "worker":
  //	fmt.Println("Installing as worker node")
  //	args = []string{"install", "worker", "-c", k0sConfigFile}
  default:
    return fmt.Errorf("unsupported node type: %s (expected 'controller' or 'controller+worker')", nodeType)
  }

  // Install k0s with the specified role and config
  if err := runCommandWithContext(ctx, "k0s", args...); err != nil {
    return fmt.Errorf("failed to install k0s controller: %w", err)
  }

  fmt.Println("✓ k0s controller installed successfully")
  return nil
}

func installK0sWorker(joinToken string) error {
  fmt.Println("\nInstalling k0s worker...")
  logAction("Installing k0s worker", map[string]string{
    "join_token": joinToken,
  })

  ctx := context.Background()

  var args []string
  args = []string{"worker", joinToken}

  // Install k0s with the specified role and config
  if err := runCommandWithContext(ctx, "k0s", args...); err != nil {
    return fmt.Errorf("failed to install k0s worker: %w", err)
  }

  fmt.Println("✓ k0s worker installed successfully")
  return nil
}

// startK0sService starts the k0s systemd service
func startK0sService() error {
  fmt.Println("\nStarting k0s service...")

  ctx := context.Background()

  // Enable k0s service to start on boot
  if err := runCommandWithContext(ctx, "systemctl", "enable", "k0scontroller"); err != nil {
    return fmt.Errorf("failed to enable k0s service: %w", err)
  }

  // Start k0s service
  if err := runCommandWithContext(ctx, "systemctl", "start", "k0scontroller"); err != nil {
    return fmt.Errorf("failed to start k0s service: %w", err)
  }

  // Wait a moment for service to initialize
  fmt.Println("Waiting for k0s to initialize...")
  if err := runCommandWithContext(ctx, "sleep", "5"); err != nil {
    return fmt.Errorf("failed to wait: %w", err)
  }

  // Check service status
  if err := runCommandWithContext(ctx, "systemctl", "status", "k0scontroller", "--no-pager"); err != nil {
    fmt.Println("Warning: k0s service status check failed, but continuing...")
  }

  fmt.Println("✓ k0s service started successfully")
  return nil
}

func encodeWorkerToken(vesselEngineId, joinToken string) string {
  raw := vesselEngineId + ".worker." + joinToken
  return base64.StdEncoding.EncodeToString([]byte(raw))
}

func decodeWorkerToken(token string) (string, string, error) {
  payload, _ := base64.StdEncoding.DecodeString(token)

  parts := strings.Split(string(payload), ".")

  if len(parts) != 3 {
    return "", "", fmt.Errorf("token is incorrect")
  }

  vesselEngineId := parts[0]
  nodeType := parts[1]
  joinToken := parts[2]

  if nodeType != "worker" {
    return "", "", fmt.Errorf("join token is not a worker token but a: %s", nodeType)
  }

  return vesselEngineId, joinToken, nil
}

// generateWorkerToken creates a worker join token with 1 hour expiry
func generateWorkerToken(expiryTime string) (string, error) {
  fmt.Println("\nGenerating worker join token...")

  // Create token
  cmd := exec.Command("k0s", "token", "create", "--role", "worker", "--expiry="+expiryTime)
  output, err := cmd.Output()
  if err != nil {
    return "", fmt.Errorf("failed to generate worker token: %w", err)
  }

  token := strings.TrimSpace(string(output))
  if token == "" {
    return "", fmt.Errorf("generated token is empty")
  }

  fmt.Println("✓ Worker join token generated")
  return token, nil
}

// displayWorkerJoinInstructions shows instructions for joining a worker node
func displayWorkerJoinInstructions(vesselEngineId, workerToken string) {
  fmt.Println("\n" + strings.Repeat("=", 70))
  fmt.Println("Add Worker Nodes to Your Cluster")
  fmt.Println(strings.Repeat("=", 70))
  fmt.Println("\nTo add worker nodes, SSH into each worker node and run:")
  fmt.Println("\n  # Step 1: Install the Galley CLI")
  fmt.Printf("curl -sSf %s | sudo sh\n", downloadBaseURL)
  fmt.Println("\n  # Step 2: Provision the node with all prerequisites")
  fmt.Println("sudo galley node prepare")
  fmt.Println("\n  # Step 3: Join the cluster")
  fmt.Printf("  sudo galley worker join %s\n", encodeWorkerToken(vesselEngineId, workerToken))
  fmt.Println("\n" + strings.Repeat("=", 70))
  fmt.Println("Note: This token expires in 60 minutes")
  fmt.Println("You can prepare nodes ahead of time, then join them when ready.")
  fmt.Println(strings.Repeat("-", 70) + "\n")
  fmt.Println("If your token expires, run `galley worker invite` on the controller node to generate a new one.")
  fmt.Println(strings.Repeat("=", 70) + "\n")

}
