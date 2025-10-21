package config

import (
	"fmt"
	"os"
	"path/filepath"

	"github.com/stackfilesync/stack-sync-cli/pkg/models"
	"gopkg.in/yaml.v3"
)

// ServerConfig represents the sync server configuration
type ServerConfig struct {
	URL    string `yaml:"url"`
	APIKey string `yaml:"api_key"`
}

// Settings represents global settings
type Settings struct {
	BackupEnabled bool   `yaml:"backup_enabled"`
	BackupDir     string `yaml:"backup_dir"`
	ShowIcons     bool   `yaml:"show_icons"`
	ColorOutput   bool   `yaml:"color_output"`
	Language      string `yaml:"language"` // Language setting: "en-US" or "zh-CN"
}

// Config represents the complete configuration
type Config struct {
	Server       ServerConfig        `yaml:"server"`
	Settings     Settings            `yaml:"settings"`
	Repositories []models.Repository `yaml:"repositories"`
}

// DefaultConfig returns a new config with default values
func DefaultConfig() *Config {
	homeDir, _ := os.UserHomeDir()
	return &Config{
		Server: ServerConfig{
			URL:    "wss://sync-server.example.com",
			APIKey: "",
		},
		Settings: Settings{
			BackupEnabled: true,
			BackupDir:     filepath.Join(homeDir, ".stack-sync", "backups"),
			ShowIcons:     true,
			ColorOutput:   true,
			Language:      "en-US", // Default to English
		},
		Repositories: []models.Repository{},
	}
}

// GetConfigPath returns the path to the config file
func GetConfigPath() string {
	homeDir, err := os.UserHomeDir()
	if err != nil {
		return ".stack-sync/config.yml"
	}
	return filepath.Join(homeDir, ".stack-sync", "config.yml")
}

// Load reads and parses the configuration file
func Load() (*Config, error) {
	configPath := GetConfigPath()

	// Check if config exists
	if _, err := os.Stat(configPath); os.IsNotExist(err) {
		// Return default config if not exists
		return DefaultConfig(), nil
	}

	data, err := os.ReadFile(configPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read config file: %w", err)
	}

	var config Config
	if err := yaml.Unmarshal(data, &config); err != nil {
		return nil, fmt.Errorf("failed to parse config file: %w", err)
	}

	return &config, nil
}

// Save writes the configuration to file
func Save(config *Config) error {
	configPath := GetConfigPath()

	// Create directory if not exists
	configDir := filepath.Dir(configPath)
	if err := os.MkdirAll(configDir, 0755); err != nil {
		return fmt.Errorf("failed to create config directory: %w", err)
	}

	data, err := yaml.Marshal(config)
	if err != nil {
		return fmt.Errorf("failed to marshal config: %w", err)
	}

	if err := os.WriteFile(configPath, data, 0644); err != nil {
		return fmt.Errorf("failed to write config file: %w", err)
	}

	return nil
}

// AddRepository adds a new repository to the config
func (c *Config) AddRepository(repo models.Repository) error {
	// Check for duplicate names
	for _, r := range c.Repositories {
		if r.Name == repo.Name {
			return fmt.Errorf("repository with name '%s' already exists", repo.Name)
		}
	}

	c.Repositories = append(c.Repositories, repo)
	return Save(c)
}

// RemoveRepository removes a repository by name
func (c *Config) RemoveRepository(name string) error {
	for i, repo := range c.Repositories {
		if repo.Name == name {
			c.Repositories = append(c.Repositories[:i], c.Repositories[i+1:]...)
			return Save(c)
		}
	}
	return fmt.Errorf("repository '%s' not found", name)
}

// GetRepository returns a repository by name
func (c *Config) GetRepository(name string) (*models.Repository, error) {
	for i := range c.Repositories {
		if c.Repositories[i].Name == name {
			return &c.Repositories[i], nil
		}
	}
	return nil, fmt.Errorf("repository '%s' not found", name)
}
