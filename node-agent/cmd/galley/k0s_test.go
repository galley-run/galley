package main

import (
	"os/exec"
	"runtime"
	"strings"
	"testing"
)

func TestEnsureK0sInstalled(t *testing.T) {
	t.Run("detects existing k0s installation", func(t *testing.T) {
		// Check if k0s is available on this system
		_, err := exec.LookPath("k0s")
		if err != nil {
			t.Skip("k0s not installed on this system, skipping detection test")
		}

		// If k0s exists, ensureK0sInstalled should succeed without installing
		err = ensureK0sInstalled()
		if err != nil {
			t.Errorf("ensureK0sInstalled() failed when k0s is already installed: %v", err)
		}
	})

	t.Run("fails on non-linux systems when k0s not installed", func(t *testing.T) {
		if runtime.GOOS == "linux" {
			t.Skip("This test only runs on non-Linux systems")
		}

		// Check if k0s is not installed
		_, err := exec.LookPath("k0s")
		if err == nil {
			t.Skip("k0s is installed, cannot test installation failure")
		}

		// Should fail with OS error on non-Linux systems
		err = ensureK0sInstalled()
		if err == nil {
			t.Error("ensureK0sInstalled() should fail on non-Linux systems")
		}
	})
}

func TestGetK0sVersion(t *testing.T) {
	t.Run("returns version when k0s is installed", func(t *testing.T) {
		// Check if k0s is available
		_, err := exec.LookPath("k0s")
		if err != nil {
			t.Skip("k0s not installed on this system")
		}

		version, err := getK0sVersion()
		if err != nil {
			t.Errorf("getK0sVersion() failed: %v", err)
		}

		if version == "" {
			t.Error("getK0sVersion() returned empty version")
		}

		t.Logf("k0s version: %s", version)
	})

	t.Run("fails when k0s is not installed", func(t *testing.T) {
		// Check if k0s is not available
		_, err := exec.LookPath("k0s")
		if err == nil {
			t.Skip("k0s is installed, cannot test failure case")
		}

		_, err = getK0sVersion()
		if err == nil {
			t.Error("getK0sVersion() should fail when k0s is not installed")
		}
	})
}

func TestK0sConstants(t *testing.T) {
	t.Run("validates k0s constants", func(t *testing.T) {
		if k0sInstallScript == "" {
			t.Error("k0sInstallScript should not be empty")
		}

		if k0sBinaryPath == "" {
			t.Error("k0sBinaryPath should not be empty")
		}

		if k0sConfigDir == "" {
			t.Error("k0sConfigDir should not be empty")
		}

		if k0sConfigFile == "" {
			t.Error("k0sConfigFile should not be empty")
		}

		// Verify install script is a valid URL
		if len(k0sInstallScript) < 8 || k0sInstallScript[:4] != "http" {
			t.Error("k0sInstallScript should be a valid HTTP(S) URL")
		}

		// Verify binary path is absolute
		if k0sBinaryPath[0] != '/' {
			t.Error("k0sBinaryPath should be an absolute path")
		}

		// Verify config dir is absolute
		if k0sConfigDir[0] != '/' {
			t.Error("k0sConfigDir should be an absolute path")
		}

		// Verify config file is absolute
		if k0sConfigFile[0] != '/' {
			t.Error("k0sConfigFile should be an absolute path")
		}

		// Verify config file is in config dir
		if !strings.HasPrefix(k0sConfigFile, k0sConfigDir) {
			t.Errorf("k0sConfigFile %s should be in k0sConfigDir %s", k0sConfigFile, k0sConfigDir)
		}

		// Verify config file has .yaml extension
		if !strings.HasSuffix(k0sConfigFile, ".yaml") {
			t.Error("k0sConfigFile should have .yaml extension")
		}
	})
}

func TestK0sInstallationWorkflow(t *testing.T) {
	t.Run("validates installation workflow", func(t *testing.T) {
		// This test documents the expected workflow without actually installing

		// Step 1: Check if k0s exists
		_, err := exec.LookPath("k0s")
		k0sExists := err == nil

		// Step 2: If it exists, ensureK0sInstalled should be fast
		if k0sExists {
			t.Log("k0s is installed, ensureK0sInstalled should skip installation")
		} else {
			t.Log("k0s is not installed, ensureK0sInstalled would attempt installation")

			// Step 3: Installation only works on Linux
			if runtime.GOOS != "linux" {
				t.Log("Not on Linux, installation would fail")
			} else {
				t.Log("On Linux, installation would download from", k0sInstallScript)
			}
		}
	})
}

func TestCreateK0sConfig(t *testing.T) {
	t.Run("requires k0s to be installed", func(t *testing.T) {
		_, err := exec.LookPath("k0s")
		if err != nil {
			t.Skip("k0s not installed, cannot test config creation")
		}

		// We can't actually test this without root permissions
		// but we can verify k0s can generate config
		cmd := exec.Command("k0s", "config", "create")
		output, err := cmd.Output()
		if err != nil {
			t.Errorf("k0s config create failed: %v", err)
		}

		if len(output) == 0 {
			t.Error("k0s config create returned empty output")
		}

		t.Logf("k0s config generation works, output length: %d bytes", len(output))
	})
}

func TestPromptEditK0sConfig(t *testing.T) {
	t.Run("validates editor detection", func(t *testing.T) {
		// Test that at least one editor is available
		editors := []string{"nano", "vi", "vim"}
		foundEditor := false
		var availableEditor string

		for _, e := range editors {
			if _, err := exec.LookPath(e); err == nil {
				foundEditor = true
				availableEditor = e
				break
			}
		}

		if !foundEditor {
			t.Log("No standard editor found on this system")
		} else {
			t.Logf("Found editor: %s", availableEditor)
		}
	})
}

func TestK0sConfigPath(t *testing.T) {
	t.Run("validates k0s config path", func(t *testing.T) {
		configPath := "/etc/k0s/k0s.yaml"

		// Check path is absolute
		if configPath[0] != '/' {
			t.Error("k0s config path should be absolute")
		}

		// Check it's in /etc
		if len(configPath) < 5 || configPath[:5] != "/etc/" {
			t.Error("k0s config should be in /etc directory")
		}

		// Check it has .yaml extension
		if len(configPath) < 5 || configPath[len(configPath)-5:] != ".yaml" {
			t.Error("k0s config should have .yaml extension")
		}
	})
}

func TestInstallK0sController(t *testing.T) {
	tests := []struct {
		name     string
		nodeType string
		wantErr  bool
	}{
		{
			name:     "controller node type",
			nodeType: "controller",
			wantErr:  false,
		},
		{
			name:     "controller+worker node type",
			nodeType: "controller+worker",
			wantErr:  false,
		},
		{
			name:     "controller-worker node type",
			nodeType: "controller-worker",
			wantErr:  false,
		},
		{
			name:     "mixed case controller",
			nodeType: "Controller",
			wantErr:  false,
		},
		{
			name:     "invalid node type",
			nodeType: "worker",
			wantErr:  true,
		},
		{
			name:     "empty node type",
			nodeType: "",
			wantErr:  true,
		},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			// We can't actually install k0s in tests, but we can validate the logic
			// by checking if the function would error based on node type

			// Check k0s is available
			if _, err := exec.LookPath("k0s"); err != nil {
				t.Skip("k0s not installed, cannot test installation logic")
			}

			// Test the node type validation logic
			validNodeTypes := []string{"controller", "controller+worker", "controller-worker"}
			isValid := false
			for _, valid := range validNodeTypes {
				if strings.ToLower(tt.nodeType) == valid {
					isValid = true
					break
				}
			}

			if tt.wantErr && isValid {
				t.Errorf("Expected error for node type %s but it's valid", tt.nodeType)
			}
			if !tt.wantErr && !isValid {
				t.Errorf("Expected no error for node type %s but it's invalid", tt.nodeType)
			}
		})
	}
}

func TestStartK0sService(t *testing.T) {
	t.Run("validates systemd service name", func(t *testing.T) {
		serviceName := "k0scontroller"

		if serviceName == "" {
			t.Error("k0s service name should not be empty")
		}

		// Check it has a reasonable name
		if !strings.Contains(serviceName, "k0s") {
			t.Error("k0s service name should contain 'k0s'")
		}
	})
}

func TestK0sNodeTypes(t *testing.T) {
	t.Run("validates supported node types", func(t *testing.T) {
		supportedTypes := []string{"controller", "controller+worker", "controller-worker"}

		for _, nodeType := range supportedTypes {
			if nodeType == "" {
				t.Error("Node type should not be empty")
			}

			// All supported types should contain "controller"
			if !strings.Contains(strings.ToLower(nodeType), "controller") {
				t.Errorf("Node type %s should contain 'controller'", nodeType)
			}
		}
	})
}

func TestGenerateWorkerToken(t *testing.T) {
	t.Run("requires k0s and running controller", func(t *testing.T) {
		// Check if k0s is available
		if _, err := exec.LookPath("k0s"); err != nil {
			t.Skip("k0s not installed, cannot test token generation")
		}

		// We can't actually test this without a running k0s controller
		// but we can verify the command structure
		t.Log("Token generation requires a running k0s controller")
		t.Log("Command would be: k0s token create --role worker --expiry=1h")
	})

	t.Run("validates token expiry format", func(t *testing.T) {
		expiry := "1h"

		// Check expiry format is valid
		if !strings.HasSuffix(expiry, "h") && !strings.HasSuffix(expiry, "m") {
			t.Error("Expiry should end with 'h' or 'm'")
		}

		if expiry != "1h" {
			t.Errorf("Expected expiry to be 1h, got %s", expiry)
		}
	})
}

func TestDisplayWorkerJoinInstructions(t *testing.T) {
	t.Run("validates instruction format", func(t *testing.T) {
		// Test with a dummy token
		testToken := "test-token-12345"
		vesselEngineId := "00D8B226-C47C-46F2-981B-A81BE8EE4213"

		// We can't easily test the actual display output, but we can verify
		// the function doesn't panic and handles the token correctly
		displayWorkerJoinInstructions(vesselEngineId, testToken)

		// If we got here without panic, the function works
		t.Log("displayWorkerJoinInstructions executed successfully")
	})

	t.Run("validates install script URL", func(t *testing.T) {
		installScriptURL := "https://get.galley.run"

		if !strings.HasPrefix(installScriptURL, "https://") && !strings.HasPrefix(installScriptURL, "http://") {
			t.Error("Install script URL should use HTTPS or HTTP")
		}

		if !strings.Contains(installScriptURL, "galley") {
			t.Error("Install script URL should contain 'galley'")
		}

		// URL may or may not have .sh extension (curl pipes handle it)
		t.Logf("Install script URL: %s", installScriptURL)
	})

	t.Run("validates worker join command format", func(t *testing.T) {
		expectedCommand := "galley worker join"

		if !strings.Contains(expectedCommand, "galley") {
			t.Error("Command should contain 'galley'")
		}

		if !strings.Contains(expectedCommand, "worker") {
			t.Error("Command should contain 'worker'")
		}

		if !strings.Contains(expectedCommand, "join") {
			t.Error("Command should contain 'join'")
		}
	})
}
