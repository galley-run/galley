package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"log"
	"net/http"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"

	"github.com/shirou/gopsutil/v3/disk"
	"github.com/shirou/gopsutil/v4/mem"
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

func markGalleyNodeReady(baseURL, vesselEngineNodeId, token string) error {
	if baseURL == "" {
		return nil
	}

	v, _ := mem.VirtualMemory()
	usage, err := disk.Usage("/")
	if err != nil {
		_ = fmt.Errorf("failed to get disk usage: %w", err)
	}

	osInfo, err := readOSRelease()
	if err != nil {
		_ = fmt.Errorf("failed to read os release: %w", err)
	}

	getCurrentOSVersion()

	url := "https://" + baseURL + "/vessels/engine/node/" + vesselEngineNodeId
	data := map[string]interface{}{
		"provisioningStatus": "ready",
		"cpu":                fmt.Sprintf("%d", runtime.NumCPU()), // number of cores
		"memory":             fmt.Sprintf("%d", v.Total),          // in bytes
		"storage":            fmt.Sprintf("%d", usage.Total),      // in bytes
		"osMetadata": map[string]interface{}{
			"os":          runtime.GOOS,
			"arch":        runtime.GOARCH,
			"distro":      osInfo["NAME"],
			"version":     osInfo["VERSION"],
			"storageUsed": usage.Used, // in bytes
		},
	}
	jsonBody, err := json.Marshal(data)
	if err != nil {
		return fmt.Errorf("failed to marshal request body: %w", err)
	}
	req, err := http.NewRequest("PATCH", url, bytes.NewBuffer(jsonBody))
	if err != nil {
		return fmt.Errorf("failed to create request: %w", err)
	}

	req.Header.Set("Content-Type", "application/vnd.galley-node-agent.v1+json")
	req.Header.Set("Accept", "application/vnd.galley-node-agent.v1+json")
	req.Header.Set("Authorization", "Bearer "+token)
	req.Header.Set("User-Agent", "Galley Node Agent")

	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		return fmt.Errorf("failed to make request: %w", err)
	}
	defer func(Body io.ReadCloser) {
		err := Body.Close()
		if err != nil {
			fmt.Printf("failed to close response body: %v", err)
			return
		}
	}(resp.Body)

	if resp.StatusCode != http.StatusOK {
		body, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("unexpected status code %d: %s", resp.StatusCode, string(body))
	}

	body, err := io.ReadAll(resp.Body)
	if err != nil {
		return fmt.Errorf("failed to read response body: %w", err)
	}

	var nodeResp VesselEngineNodeResponse
	if err := json.Unmarshal(body, &nodeResp); err != nil {
		return fmt.Errorf("failed to parse response: %w", err)
	}

	return nil
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
