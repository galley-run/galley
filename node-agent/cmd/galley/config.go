package main

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/spf13/cobra"
	"gopkg.in/yaml.v3"
)

type Config struct {
	DownloadBase string `yaml:"download_base"`
	PlatformURL  string `yaml:"platform_url"`
	ClientURL    string `yaml:"client_url"`
}

var configCmd = &cobra.Command{
	Use:   "config",
	Short: "Manage galley configuration",
	Long:  "Manage galley configuration settings stored in ~/.galley/config",
}

var configSetCmd = &cobra.Command{
	Use:   "set <key> <value>",
	Short: "Set a configuration value",
	Long:  "Set a configuration value. Available keys: download_base, platform_url, client_url",
	Args:  cobra.ExactArgs(2),
	RunE:  runConfigSet,
}

var configGetCmd = &cobra.Command{
	Use:   "get <key>",
	Short: "Get a configuration value",
	Long:  "Get a configuration value. Available keys: download_base, platform_url, client_url",
	Args:  cobra.ExactArgs(1),
	RunE:  runConfigGet,
}

var configListCmd = &cobra.Command{
	Use:   "list",
	Short: "List all configuration values",
	RunE:  runConfigList,
}

var configPathCmd = &cobra.Command{
	Use:   "path",
	Short: "Show the path to the configuration file",
	RunE:  runConfigPath,
}

func init() {
	configCmd.AddCommand(configSetCmd)
	configCmd.AddCommand(configGetCmd)
	configCmd.AddCommand(configListCmd)
	configCmd.AddCommand(configPathCmd)
}

func getConfigPath() (string, error) {
	home, err := getRealUserHomeDir()
	if err != nil {
		return "", fmt.Errorf("failed to get home directory: %w", err)
	}
	return filepath.Join(home, ".galley", "config"), nil
}

func loadConfig() (*Config, error) {
	configPath, err := getConfigPath()
	if err != nil {
		return nil, err
	}

	// If config doesn't exist, create it with defaults
	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		defaultConfig := getDefaultConfig()
		if err := saveConfig(defaultConfig); err != nil {
			// If we can't save, just return defaults without error
			return defaultConfig, nil
		}
		return defaultConfig, nil
	}

	data, err := os.ReadFile(configPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read config: %w", err)
	}

	var config Config
	if err := yaml.Unmarshal(data, &config); err != nil {
		return nil, fmt.Errorf("failed to parse config: %w", err)
	}

	return &config, nil
}

func saveConfig(config *Config) error {
	configPath, err := getConfigPath()
	if err != nil {
		return err
	}

	// Create directory if it doesn't exist
	configDir := filepath.Dir(configPath)
	if err := os.MkdirAll(configDir, 0755); err != nil {
		return fmt.Errorf("failed to create config directory: %w", err)
	}

	data, err := yaml.Marshal(config)
	if err != nil {
		return fmt.Errorf("failed to marshal config: %w", err)
	}

	if err := os.WriteFile(configPath, data, 0644); err != nil {
		return fmt.Errorf("failed to write config: %w", err)
	}

	return nil
}

func getDefaultConfig() *Config {
	return &Config{
		DownloadBase: "https://get.galley.run",
		PlatformURL:  "api.galley.run",
		ClientURL:    "https://cloud.galley.run",
	}
}

func runConfigSet(cmd *cobra.Command, args []string) error {
	key := args[0]
	value := args[1]

	config, err := loadConfig()
	if err != nil {
		return err
	}

	switch key {
	case "download_base":
		config.DownloadBase = value
	case "platform_url":
		config.PlatformURL = value
	case "client_url":
		config.ClientURL = value
	default:
		return fmt.Errorf("unknown config key: %s (available: download_base, platform_url, client_url)", key)
	}

	if err := saveConfig(config); err != nil {
		return err
	}

	fmt.Printf("✓ Set %s = %s\n", key, value)
	return nil
}

func runConfigGet(cmd *cobra.Command, args []string) error {
	key := args[0]

	config, err := loadConfig()
	if err != nil {
		return err
	}

	var value string
	switch key {
	case "download_base":
		value = config.DownloadBase
	case "platform_url":
		value = config.PlatformURL
	case "client_url":
		value = config.ClientURL
	default:
		return fmt.Errorf("unknown config key: %s (available: download_base, platform_url, client_url)", key)
	}

	fmt.Println(value)
	return nil
}

func runConfigList(cmd *cobra.Command, args []string) error {
	config, err := loadConfig()
	if err != nil {
		return err
	}

	fmt.Printf("download_base: %s\n", config.DownloadBase)
	fmt.Printf("platform_url: %s\n", config.PlatformURL)
	fmt.Printf("client_url: %s\n", config.ClientURL)
	return nil
}

func runConfigPath(cmd *cobra.Command, args []string) error {
	configPath, err := getConfigPath()
	if err != nil {
		return err
	}
	fmt.Println(configPath)
	return nil
}
