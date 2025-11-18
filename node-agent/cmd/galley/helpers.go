package main

import (
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
)

func getGalleyNode(baseURL, token string) (*DataResource[VesselEngineNodeAttributes], error) {
	if baseURL == "" {
		return nil, nil
	}

	url := "https://" + baseURL + "/vessels/engine/node"
	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return nil, fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/vnd.galley-node-agent.v1+json")
	req.Header.Set("Accept", "application/vnd.galley-node-agent.v1+json")
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("User-Agent", "Galley Node Agent")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("failed to make request: %w", err)
	}
	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			log.Printf("failed to close response body: %v", err)
			return
		}
	}(resp.Body)

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return nil, fmt.Errorf("unexpected status code %d: %s", resp.StatusCode, string(body))
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return nil, fmt.Errorf("failed to read response body: %w", err)
	}

	var nodeResp VesselEngineNodeResponse
	if err := json.Unmarshal(body, &nodeResp); err != nil {
		return nil, fmt.Errorf("failed to parse response: %w", err)
	}

	return &nodeResp.Data, nil
}

func checkCommands(names ...string) {
	for _, name := range names {
		if _, err := exec.LookPath(name); err != nil {
			_ = fmt.Errorf("warning: command not found in PATH: %s\n", name)
		}
	}
}

// getRealUserHomeDir returns the home directory of the actual user,
// even when running under sudo. It checks SUDO_USER and SUDO_HOME
// environment variables to determine the original user's home.
func getRealUserHomeDir() (string, error) {
	// When running under sudo, use the actual user's home directory
	if sudoUser := os.Getenv("SUDO_USER"); sudoUser != "" {
		// Try SUDO_HOME first (if set)
		if sudoHome := os.Getenv("SUDO_HOME"); sudoHome != "" {
			return sudoHome, nil
		}
		// Construct home path for the sudo user
		if sudoUser == "root" {
			return "/root", nil
		}
		return filepath.Join("/home", sudoUser), nil
	}
	// Fallback to regular user home directory
	return os.UserHomeDir()
}
