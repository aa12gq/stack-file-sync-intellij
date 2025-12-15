package sync

import (
	"encoding/json"
	"fmt"
	"os"
	"path/filepath"

	"github.com/google/uuid"
	"github.com/stackfilesync/stack-sync-cli/pkg/models"
)

// GetHistoryPath returns the path to the history file
func GetHistoryPath() string {
	homeDir, err := os.UserHomeDir()
	if err != nil {
		return ".stack-sync/history.json"
	}
	return filepath.Join(homeDir, ".stack-sync", "history.json")
}

// LoadHistory loads sync history from file
func LoadHistory() (*models.SyncHistoryStore, error) {
	historyPath := GetHistoryPath()

	// If file doesn't exist, return empty store
	if _, err := os.Stat(historyPath); os.IsNotExist(err) {
		return &models.SyncHistoryStore{
			Histories: []models.SyncHistory{},
		}, nil
	}

	data, err := os.ReadFile(historyPath)
	if err != nil {
		return nil, fmt.Errorf("failed to read history file: %w", err)
	}

	var store models.SyncHistoryStore
	if err := json.Unmarshal(data, &store); err != nil {
		return nil, fmt.Errorf("failed to parse history file: %w", err)
	}

	return &store, nil
}

// SaveHistory saves sync history to file
func SaveHistory(store *models.SyncHistoryStore) error {
	historyPath := GetHistoryPath()

	// Create directory if not exists
	historyDir := filepath.Dir(historyPath)
	if err := os.MkdirAll(historyDir, 0755); err != nil {
		return fmt.Errorf("failed to create history directory: %w", err)
	}

	data, err := json.MarshalIndent(store, "", "  ")
	if err != nil {
		return fmt.Errorf("failed to marshal history: %w", err)
	}

	if err := os.WriteFile(historyPath, data, 0644); err != nil {
		return fmt.Errorf("failed to write history file: %w", err)
	}

	return nil
}

// AddHistory adds a new sync history entry
func AddHistory(history models.SyncHistory) error {
	store, err := LoadHistory()
	if err != nil {
		return err
	}

	// Generate ID if not set
	if history.ID == "" {
		history.ID = uuid.New().String()
	}

	// Add to beginning of list (most recent first)
	store.Histories = append([]models.SyncHistory{history}, store.Histories...)

	// Keep only last 1000 entries
	if len(store.Histories) > 1000 {
		store.Histories = store.Histories[:1000]
	}

	return SaveHistory(store)
}

// GetHistoryForRepository returns sync history for a specific repository
func GetHistoryForRepository(repoName string, limit int) ([]models.SyncHistory, error) {
	store, err := LoadHistory()
	if err != nil {
		return nil, err
	}

	var result []models.SyncHistory
	count := 0

	for _, history := range store.Histories {
		if history.Repository == repoName {
			result = append(result, history)
			count++
			if limit > 0 && count >= limit {
				break
			}
		}
	}

	return result, nil
}

// GetAllHistory returns all sync history, optionally limited
func GetAllHistory(limit int) ([]models.SyncHistory, error) {
	store, err := LoadHistory()
	if err != nil {
		return nil, err
	}

	if limit > 0 && limit < len(store.Histories) {
		return store.Histories[:limit], nil
	}

	return store.Histories, nil
}

