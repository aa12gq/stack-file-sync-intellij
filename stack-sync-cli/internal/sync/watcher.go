package sync

import (
	"fmt"
	"log"
	"path/filepath"
	"strings"
	"time"

	"github.com/fsnotify/fsnotify"
	"github.com/stackfilesync/stack-sync-cli/pkg/models"
)

// Watcher watches for file changes in repositories
type Watcher struct {
	manager  *Manager
	watcher  *fsnotify.Watcher
	debounce time.Duration
	events   map[string]time.Time // Debounce map
}

// NewWatcher creates a new file watcher
func NewWatcher(manager *Manager) (*Watcher, error) {
	fsWatcher, err := fsnotify.NewWatcher()
	if err != nil {
		return nil, err
	}

	return &Watcher{
		manager:  manager,
		watcher:  fsWatcher,
		debounce: 2 * time.Second, // Wait 2 seconds before syncing
		events:   make(map[string]time.Time),
	}, nil
}

// Start starts watching configured repositories
func (w *Watcher) Start() error {
	// Add repositories with watch mode enabled
	for i := range w.manager.config.Repositories {
		repo := &w.manager.config.Repositories[i]
		if repo.WatchMode {
			if err := w.AddRepository(repo); err != nil {
				log.Printf("Warning: failed to watch %s: %v\n", repo.Name, err)
			}
		}
	}

	// Start event loop
	go w.watchLoop()

	log.Println("File watcher started")
	return nil
}

// AddRepository adds a repository to watch
func (w *Watcher) AddRepository(repo *models.Repository) error {
	if !filepath.IsAbs(repo.LocalPath) {
		return fmt.Errorf("local path must be absolute: %s", repo.LocalPath)
	}

	// Watch the repository directory
	if err := w.watcher.Add(repo.LocalPath); err != nil {
		return fmt.Errorf("failed to watch directory: %w", err)
	}

	log.Printf("Watching repository: %s at %s\n", repo.Name, repo.LocalPath)
	return nil
}

// RemoveRepository removes a repository from watching
func (w *Watcher) RemoveRepository(repo *models.Repository) error {
	return w.watcher.Remove(repo.LocalPath)
}

// Stop stops the watcher
func (w *Watcher) Stop() error {
	return w.watcher.Close()
}

// watchLoop is the main event loop
func (w *Watcher) watchLoop() {
	for {
		select {
		case event, ok := <-w.watcher.Events:
			if !ok {
				return
			}
			w.handleEvent(event)

		case err, ok := <-w.watcher.Errors:
			if !ok {
				return
			}
			log.Printf("Watcher error: %v\n", err)
		}
	}
}

// handleEvent handles a file system event
func (w *Watcher) handleEvent(event fsnotify.Event) {
	// Ignore temporary files and .git directory
	if w.shouldIgnore(event.Name) {
		return
	}

	// Find the repository this file belongs to
	repo := w.findRepository(event.Name)
	if repo == nil {
		return
	}

	// Check if file matches sync patterns
	if !w.matchesPatterns(event.Name, repo) {
		return
	}

	// Debounce: only process if enough time has passed
	now := time.Now()
	if lastEvent, exists := w.events[repo.Name]; exists {
		if now.Sub(lastEvent) < w.debounce {
			return
		}
	}

	w.events[repo.Name] = now

	// Log the event
	log.Printf("File changed in %s: %s (%s)\n", repo.Name, event.Name, event.Op)

	// Trigger sync after debounce period
	go w.debouncedSync(repo)
}

// debouncedSync waits for the debounce period before syncing
func (w *Watcher) debouncedSync(repo *models.Repository) {
	time.Sleep(w.debounce)

	log.Printf("Auto-syncing %s...\n", repo.Name)
	if err := w.manager.SyncRepository(repo); err != nil {
		log.Printf("Failed to sync %s: %v\n", repo.Name, err)
	} else {
		log.Printf("Successfully synced %s\n", repo.Name)
	}
}

// findRepository finds which repository a file path belongs to
func (w *Watcher) findRepository(path string) *models.Repository {
	for i := range w.manager.config.Repositories {
		repo := &w.manager.config.Repositories[i]
		if strings.HasPrefix(path, repo.LocalPath) && repo.WatchMode {
			return repo
		}
	}
	return nil
}

// matchesPatterns checks if a file matches the repository's sync patterns
func (w *Watcher) matchesPatterns(path string, repo *models.Repository) bool {
	// If no patterns specified, match all files
	if len(repo.SyncPatterns) == 0 {
		return true
	}

	// Check exclude patterns first
	for _, exclude := range repo.Exclude {
		if matched, _ := filepath.Match(exclude, filepath.Base(path)); matched {
			return false
		}
	}

	// Check include patterns
	for _, pattern := range repo.SyncPatterns {
		if matched, _ := filepath.Match(pattern, filepath.Base(path)); matched {
			return true
		}
	}

	return false
}

// shouldIgnore checks if a file should be ignored
func (w *Watcher) shouldIgnore(path string) bool {
	base := filepath.Base(path)

	// Ignore hidden files
	if strings.HasPrefix(base, ".") {
		return true
	}

	// Ignore common temporary files
	ignoreSuffixes := []string{
		"~", ".swp", ".tmp", ".bak", ".DS_Store",
	}

	for _, suffix := range ignoreSuffixes {
		if strings.HasSuffix(base, suffix) {
			return true
		}
	}

	// Ignore .git directory
	if strings.Contains(path, "/.git/") {
		return true
	}

	return false
}
