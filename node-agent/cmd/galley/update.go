package main

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"runtime"

	"github.com/spf13/cobra"
)

var (
	flagUpdateVersion string
)

var updateCmd = &cobra.Command{
	Use:   "update",
	Short: "Update galley to the latest version",
	Long:  "Downloads and installs the latest version of galley from the server.",
	RunE:  runUpdate,
}

func init() {
	updateCmd.Flags().StringVar(&flagUpdateVersion, "version", "latest", "Specific version to update to (default: latest)")
}

func runUpdate(cmd *cobra.Command, args []string) error {
	// Check if running as root
	if flagDryRun {
		fmt.Println("[DRY RUN] Would update galley binary")
		return nil
	}

	fmt.Println("Updating galley...")

	// Determine architecture
	arch := getArchitecture()
	if arch == "" {
		return fmt.Errorf("unsupported architecture: %s/%s", runtime.GOOS, runtime.GOARCH)
	}

	// Get download base from config or env
	config, err := loadConfig()
	if err != nil {
		return fmt.Errorf("failed to load config: %w", err)
	}

	downloadBase := os.Getenv("GALLEY_DOWNLOAD_BASE")
	if downloadBase == "" {
		downloadBase = config.DownloadBase
	}

	version := flagUpdateVersion
	if version == "latest" {
		version = "latest"
	}

	url := fmt.Sprintf("%s/bin/%s/galley-%s", downloadBase, version, arch)
	fmt.Printf("Downloading from: %s\n", url)

	// Download new binary
	resp, err := http.Get(url)
	if err != nil {
		return fmt.Errorf("failed to download: %w", err)
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return fmt.Errorf("download failed with status: %d", resp.StatusCode)
	}

	// Get current binary path
	execPath, err := os.Executable()
	if err != nil {
		return fmt.Errorf("failed to get executable path: %w", err)
	}

	// Create temporary file
	tmpFile := execPath + ".new"
	out, err := os.OpenFile(tmpFile, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0755)
	if err != nil {
		return fmt.Errorf("failed to create temporary file: %w", err)
	}
	defer out.Close()

	// Write downloaded content
	_, err = io.Copy(out, resp.Body)
	if err != nil {
		os.Remove(tmpFile)
		return fmt.Errorf("failed to write binary: %w", err)
	}

	out.Close()

	// Replace old binary with new one
	if err := os.Rename(tmpFile, execPath); err != nil {
		os.Remove(tmpFile)
		return fmt.Errorf("failed to replace binary: %w", err)
	}

	fmt.Printf("✓ Successfully updated galley to version %s\n", version)
	return nil
}

func getArchitecture() string {
	switch runtime.GOOS {
	case "linux":
		switch runtime.GOARCH {
		case "amd64":
			return "linux-amd64"
		case "arm64":
			return "linux-arm64"
		case "arm":
			return "linux-armv7"
		}
	}
	return ""
}
