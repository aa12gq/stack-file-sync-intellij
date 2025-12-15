package sync

import (
	"bufio"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"time"
	"unicode"

	"github.com/eiannone/keyboard"
	"github.com/stackfilesync/stack-sync-cli/internal/config"
	"github.com/stackfilesync/stack-sync-cli/internal/git"
	"github.com/stackfilesync/stack-sync-cli/internal/i18n"
	"github.com/stackfilesync/stack-sync-cli/pkg/models"
)

// Manager handles repository synchronization (matches IntelliJ plugin)
type Manager struct {
	config *config.Config
	i18n   *i18n.I18n
}

// NewManager creates a new sync manager
func NewManager(cfg *config.Config, i18nInstance *i18n.I18n) *Manager {
	return &Manager{
		config: cfg,
		i18n:   i18nInstance,
	}
}

// diffEntry represents a file diff entry for preview mode
type diffEntry struct {
	Path   string
	Status string // A, M, D
}

// recordSyncHistory records sync history and displays change summary
func (m *Manager) recordSyncHistory(repo *models.Repository, fileChanges []models.FileChange, startTime time.Time, success bool, errMsg string) {
	// Calculate statistics
	addedCount := 0
	modifiedCount := 0
	deletedCount := 0
	
	for _, change := range fileChanges {
		switch change.ChangeType {
		case models.ChangeTypeAdded:
			addedCount++
		case models.ChangeTypeModified:
			modifiedCount++
		case models.ChangeTypeDeleted:
			deletedCount++
		}
	}

	// Display change summary
	fmt.Println("\n" + strings.Repeat("=", 60))
	fmt.Println("📊 同步变更摘要 (Sync Change Summary)")
	fmt.Println(strings.Repeat("=", 60))
	fmt.Printf("总文件数: %d\n", len(fileChanges))
	fmt.Printf("  ✅ 新增: %d 个文件\n", addedCount)
	fmt.Printf("  🔄 修改: %d 个文件\n", modifiedCount)
	fmt.Printf("  ❌ 删除: %d 个文件\n", deletedCount)
	fmt.Println(strings.Repeat("-", 60))

	// Show detailed file changes
	if len(fileChanges) > 0 {
		fmt.Println("\n详细变更列表 (Detailed Changes):")
		for _, change := range fileChanges {
			var icon string
			switch change.ChangeType {
			case models.ChangeTypeAdded:
				icon = "✅"
			case models.ChangeTypeModified:
				icon = "🔄"
			case models.ChangeTypeDeleted:
				icon = "❌"
			}
			fmt.Printf("  %s %s\n", icon, change.Path)
		}
	}
	fmt.Println(strings.Repeat("=", 60) + "\n")

	// Create history entry
	duration := time.Since(startTime).Milliseconds()
	history := models.SyncHistory{
		Repository:    repo.Name,
		Branch:        repo.Branch,
		Timestamp:     time.Now(),
		Success:       success,
		Error:         errMsg,
		FileChanges:   fileChanges,
		TotalFiles:    len(fileChanges),
		AddedCount:    addedCount,
		ModifiedCount: modifiedCount,
		DeletedCount:  deletedCount,
		Duration:      duration,
	}

	// Save history
	if err := AddHistory(history); err != nil {
		fmt.Printf("Warning: failed to save sync history: %v\n", err)
	}
}

// SyncRepository synchronizes a repository (following IntelliJ plugin logic)
// 1. Clone remote repository to temp directory
// 2. Checkout specified branch
// 3. Scan files and show interactive selection
// 4. Copy selected files from sourceDirectory to targetDirectory
// 5. Apply file patterns filtering
// 6. Execute post-sync commands
func (m *Manager) SyncRepository(repo *models.Repository) error {
	repo.Status = models.StatusSyncing

	// Create temp directory for cloning
	tempDir, err := os.MkdirTemp("", "stack-sync-*")
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to create temp directory: %w", err)
	}
	defer os.RemoveAll(tempDir)

	// Clone repository to temp directory
	fmt.Printf("Cloning %s @ %s to temp directory...\n", repo.URL, repo.Branch)
	ops, err := git.Clone(repo, tempDir)
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to clone repository: %w", err)
	}

	// Checkout specified branch
	if repo.Branch != "" && repo.Branch != "main" && repo.Branch != "master" {
		fmt.Printf("Checking out branch: %s\n", repo.Branch)
		if err := ops.CheckoutBranch(repo.Branch); err != nil {
			repo.Status = models.StatusError
			return fmt.Errorf("failed to checkout branch %s: %w", repo.Branch, err)
		}
	}

	// Backup if enabled
	if repo.BackupConfig != nil && repo.BackupConfig.Enabled && m.directoryExists(repo.TargetDirectory) {
		if err := m.BackupRepository(repo); err != nil {
			fmt.Printf("Warning: backup failed: %v\n", err)
		}
	}

	// Determine source path
	sourcePath := tempDir
	if repo.SourceDirectory != "" {
		sourcePath = filepath.Join(tempDir, repo.SourceDirectory)
	}

	// Check if source directory exists
	if _, err := os.Stat(sourcePath); os.IsNotExist(err) {
		repo.Status = models.StatusError
		return fmt.Errorf("source directory does not exist: %s", repo.SourceDirectory)
	}

	// Ensure target directory exists
	if err := os.MkdirAll(repo.TargetDirectory, 0755); err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to create target directory: %w", err)
	}

	// Scan files and show interactive selection
	fmt.Printf("Scanning files in %s...\n", sourcePath)
	availableFiles, err := m.scanFiles(sourcePath, repo.FilePatterns, repo.ExcludePatterns)
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to scan files: %w", err)
	}

	if len(availableFiles) == 0 {
		fmt.Printf("No files found matching patterns: %v\n", repo.FilePatterns)
		repo.Status = models.StatusUpToDate
		return nil
	}

	// Show interactive file selection
	fmt.Printf("Found %d files to sync:\n", len(availableFiles))
	selectedFiles, err := m.selectFilesToSync(availableFiles, sourcePath)
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("file selection cancelled: %w", err)
	}

	if len(selectedFiles) == 0 {
		fmt.Printf("No files selected for sync\n")
		repo.Status = models.StatusUpToDate
		return nil
	}

	// Copy selected files from source to target
	fmt.Printf("Syncing %d selected files from %s to %s...\n", len(selectedFiles), sourcePath, repo.TargetDirectory)
	startTime := time.Now()
	copiedFiles, fileChanges, err := m.copySelectedFiles(sourcePath, repo.TargetDirectory, selectedFiles)
	if err != nil {
		repo.Status = models.StatusError
		// Record failed sync
		m.recordSyncHistory(repo, []models.FileChange{}, startTime, false, err.Error())
		return fmt.Errorf("failed to copy files: %w", err)
	}

	fmt.Printf("Synced %d files\n", copiedFiles)

	// Update status
	repo.Status = models.StatusUpToDate
	now := time.Now()
	repo.LastSync = &now
	repo.FilesTracked = copiedFiles

	// Record sync history
	m.recordSyncHistory(repo, fileChanges, startTime, true, "")

	// Execute post-sync commands
	if len(repo.PostSyncCommands) > 0 {
		if err := m.executePostSyncCommands(repo); err != nil {
			fmt.Printf("Warning: post-sync command failed: %v\n", err)
		}
	}

	return nil
}

// SyncRepositoryWithFilter synchronizes a repository with a pre-filter keyword
func (m *Manager) SyncRepositoryWithFilter(repo *models.Repository, filterKeyword string) error {
	repo.Status = models.StatusSyncing

	// Create temp directory for cloning
	tempDir, err := os.MkdirTemp("", "stack-sync-*")
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to create temp directory: %w", err)
	}
	defer os.RemoveAll(tempDir)

	// Clone repository to temp directory
	fmt.Printf("Cloning %s @ %s to temp directory...\n", repo.URL, repo.Branch)
	ops, err := git.Clone(repo, tempDir)
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to clone repository: %w", err)
	}

	// Checkout specified branch
	if repo.Branch != "" && repo.Branch != "main" && repo.Branch != "master" {
		fmt.Printf("Checking out branch: %s\n", repo.Branch)
		if err := ops.CheckoutBranch(repo.Branch); err != nil {
			repo.Status = models.StatusError
			return fmt.Errorf("failed to checkout branch %s: %w", repo.Branch, err)
		}
	}

	// Backup if enabled
	if repo.BackupConfig != nil && repo.BackupConfig.Enabled && m.directoryExists(repo.TargetDirectory) {
		if err := m.BackupRepository(repo); err != nil {
			fmt.Printf("Warning: backup failed: %v\n", err)
		}
	}

	// Determine source path
	sourcePath := tempDir
	if repo.SourceDirectory != "" {
		sourcePath = filepath.Join(tempDir, repo.SourceDirectory)
	}

	// Check if source directory exists
	if _, err := os.Stat(sourcePath); os.IsNotExist(err) {
		repo.Status = models.StatusError
		return fmt.Errorf("source directory does not exist: %s", repo.SourceDirectory)
	}

	// Ensure target directory exists
	if err := os.MkdirAll(repo.TargetDirectory, 0755); err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to create target directory: %w", err)
	}

	// Scan files and show interactive selection
	fmt.Printf("Scanning files in %s...\n", sourcePath)
	availableFiles, err := m.scanFiles(sourcePath, repo.FilePatterns, repo.ExcludePatterns)
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to scan files: %w", err)
	}

	if len(availableFiles) == 0 {
		fmt.Printf("No files found matching patterns: %v\n", repo.FilePatterns)
		repo.Status = models.StatusUpToDate
		return nil
	}

	// Show interactive file selection
	fmt.Printf("Found %d files to sync:\n", len(availableFiles))

	// Apply pre-filter if provided, but still allow full interactive selection
	if filterKeyword != "" {
		filteredFiles := m.preFilterFilesByKeyword(availableFiles, filterKeyword)
		if len(filteredFiles) == 0 {
			fmt.Printf("No files found matching filter keyword: %s\n", filterKeyword)
			repo.Status = models.StatusUpToDate
			return nil
		}
		fmt.Printf("Pre-filtered to %d files matching '%s'\n", len(filteredFiles), filterKeyword)
		fmt.Println("You can still use interactive selection modes (keyboard, number, etc.)")
		availableFiles = filteredFiles
	}

	selectedFiles, err := m.selectFilesToSync(availableFiles, sourcePath)
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("file selection cancelled: %w", err)
	}

	if len(selectedFiles) == 0 {
		fmt.Printf("No files selected for sync\n")
		repo.Status = models.StatusUpToDate
		return nil
	}

	// Copy selected files from source to target
	fmt.Printf("Syncing %d selected files from %s to %s...\n", len(selectedFiles), sourcePath, repo.TargetDirectory)
	startTime := time.Now()
	copiedFiles, fileChanges, err := m.copySelectedFiles(sourcePath, repo.TargetDirectory, selectedFiles)
	if err != nil {
		repo.Status = models.StatusError
		// Record failed sync
		m.recordSyncHistory(repo, []models.FileChange{}, startTime, false, err.Error())
		return fmt.Errorf("failed to copy files: %w", err)
	}

	fmt.Printf("Synced %d files\n", copiedFiles)

	// Update status
	repo.Status = models.StatusUpToDate
	now := time.Now()
	repo.LastSync = &now
	repo.FilesTracked = copiedFiles

	// Record sync history
	m.recordSyncHistory(repo, fileChanges, startTime, true, "")

	// Execute post-sync commands
	if len(repo.PostSyncCommands) > 0 {
		if err := m.executePostSyncCommands(repo); err != nil {
			fmt.Printf("Warning: post-sync command failed: %v\n", err)
		}
	}

	return nil
}

// SyncRepositoryWithNumberSelection synchronizes a repository with pre-selected file numbers
func (m *Manager) SyncRepositoryWithNumberSelection(repo *models.Repository, filterKeyword, numberSelection string) error {
	repo.Status = models.StatusSyncing

	// Create temp directory for cloning
	tempDir, err := os.MkdirTemp("", "stack-sync-*")
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to create temp directory: %w", err)
	}
	defer os.RemoveAll(tempDir)

	// Clone repository to temp directory
	fmt.Printf("Cloning %s @ %s to temp directory...\n", repo.URL, repo.Branch)
	ops, err := git.Clone(repo, tempDir)
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to clone repository: %w", err)
	}

	// Checkout specified branch
	if repo.Branch != "" && repo.Branch != "main" && repo.Branch != "master" {
		fmt.Printf("Checking out branch: %s\n", repo.Branch)
		if err := ops.CheckoutBranch(repo.Branch); err != nil {
			repo.Status = models.StatusError
			return fmt.Errorf("failed to checkout branch %s: %w", repo.Branch, err)
		}
	}

	// Backup if enabled
	if repo.BackupConfig != nil && repo.BackupConfig.Enabled && m.directoryExists(repo.TargetDirectory) {
		if err := m.BackupRepository(repo); err != nil {
			fmt.Printf("Warning: backup failed: %v\n", err)
		}
	}

	// Determine source path
	sourcePath := tempDir
	if repo.SourceDirectory != "" {
		sourcePath = filepath.Join(tempDir, repo.SourceDirectory)
	}

	// Check if source directory exists
	if _, err := os.Stat(sourcePath); os.IsNotExist(err) {
		repo.Status = models.StatusError
		return fmt.Errorf("source directory does not exist: %s", repo.SourceDirectory)
	}

	// Ensure target directory exists
	if err := os.MkdirAll(repo.TargetDirectory, 0755); err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to create target directory: %w", err)
	}

	// Scan files
	fmt.Printf("Scanning files in %s...\n", sourcePath)
	availableFiles, err := m.scanFiles(sourcePath, repo.FilePatterns, repo.ExcludePatterns)
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to scan files: %w", err)
	}

	if len(availableFiles) == 0 {
		fmt.Printf("No files found matching patterns: %v\n", repo.FilePatterns)
		repo.Status = models.StatusUpToDate
		return nil
	}

	// Apply pre-filter if provided
	if filterKeyword != "" {
		filteredFiles := m.preFilterFilesByKeyword(availableFiles, filterKeyword)
		if len(filteredFiles) == 0 {
			fmt.Printf("No files found matching filter keyword: %s\n", filterKeyword)
			repo.Status = models.StatusUpToDate
			return nil
		}
		fmt.Printf("Pre-filtered to %d files matching '%s'\n", len(filteredFiles), filterKeyword)
		availableFiles = filteredFiles
	}

	// Show files and auto-select using number selection
	fmt.Printf("Found %d files to sync:\n", len(availableFiles))
	selectedFiles, err := m.selectFilesWithNumberSelection(availableFiles, sourcePath, numberSelection)
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("file selection failed: %w", err)
	}

	if len(selectedFiles) == 0 {
		fmt.Printf("No files selected for sync\n")
		repo.Status = models.StatusUpToDate
		return nil
	}

	// Copy selected files from source to target
	fmt.Printf("Syncing %d selected files from %s to %s...\n", len(selectedFiles), sourcePath, repo.TargetDirectory)
	startTime := time.Now()
	copiedFiles, fileChanges, err := m.copySelectedFiles(sourcePath, repo.TargetDirectory, selectedFiles)
	if err != nil {
		repo.Status = models.StatusError
		// Record failed sync
		m.recordSyncHistory(repo, []models.FileChange{}, startTime, false, err.Error())
		return fmt.Errorf("failed to copy files: %w", err)
	}

	fmt.Printf("Synced %d files\n", copiedFiles)

	// Update status
	repo.Status = models.StatusUpToDate
	now := time.Now()
	repo.LastSync = &now
	repo.FilesTracked = copiedFiles

	// Record sync history
	m.recordSyncHistory(repo, fileChanges, startTime, true, "")

	// Execute post-sync commands
	if len(repo.PostSyncCommands) > 0 {
		if err := m.executePostSyncCommands(repo); err != nil {
			fmt.Printf("Warning: post-sync command failed: %v\n", err)
		}
	}

	return nil
}

// selectFilesWithNumberSelection shows files and automatically selects using number selection
func (m *Manager) selectFilesWithNumberSelection(availableFiles []string, sourcePath string, numberSelection string) ([]string, error) {
	if len(availableFiles) == 0 {
		return []string{}, nil
	}

	fmt.Printf("\n📁 %s\n", m.i18n.T(i18n.MsgAvailableFilesToSync))
	fmt.Println("────────────────────────────────────────")

	// Show all files
	for i, file := range availableFiles {
		fmt.Printf("  %d. %s\n", i+1, file)
	}

	fmt.Printf("\n🔢 Auto-selecting files using: %s\n", numberSelection)

	// Parse the number selection
	selectedFiles, err := m.parseFileSelection(numberSelection, availableFiles)
	if err != nil {
		return nil, fmt.Errorf("invalid number selection '%s': %w", numberSelection, err)
	}

	fmt.Printf("Selected %d files:\n", len(selectedFiles))
	for _, file := range selectedFiles {
		fmt.Printf("  ✓ %s\n", file)
	}

	return selectedFiles, nil
}

// preFilterFilesByKeyword filters files by keyword (for command line -f option)
func (m *Manager) preFilterFilesByKeyword(availableFiles []string, keyword string) []string {
	var filtered []string
	keyword = strings.ToLower(keyword)

	for _, file := range availableFiles {
		if strings.Contains(strings.ToLower(file), keyword) {
			filtered = append(filtered, file)
		}
	}

	return filtered
}

// copyFiles copies files from source to target with pattern filtering
func (m *Manager) copyFiles(src, dst string, patterns, excludes []string) (int, error) {
	copiedCount := 0

	err := filepath.Walk(src, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		// Skip .git directory
		if info.IsDir() && info.Name() == ".git" {
			return filepath.SkipDir
		}

		// Get relative path
		relPath, err := filepath.Rel(src, path)
		if err != nil {
			return err
		}

		// Skip root directory
		if relPath == "." {
			return nil
		}

		// Check if file should be excluded
		if m.shouldExclude(relPath, excludes) {
			if info.IsDir() {
				return filepath.SkipDir
			}
			return nil
		}

		// Check if file matches patterns (only for files, not directories)
		if !info.IsDir() && !m.matchesPatterns(relPath, patterns) {
			return nil
		}

		// Calculate destination path
		destPath := filepath.Join(dst, relPath)

		if info.IsDir() {
			// Create directory
			return os.MkdirAll(destPath, info.Mode())
		}

		// Copy file
		if err := m.copyFile(path, destPath); err != nil {
			return fmt.Errorf("failed to copy %s: %w", relPath, err)
		}

		copiedCount++
		return nil
	})

	return copiedCount, err
}

// copyFile copies a single file
func (m *Manager) copyFile(src, dst string) error {
	// Ensure destination directory exists
	if err := os.MkdirAll(filepath.Dir(dst), 0755); err != nil {
		return err
	}

	// Open source file
	srcFile, err := os.Open(src)
	if err != nil {
		return err
	}
	defer srcFile.Close()

	// Create destination file
	dstFile, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer dstFile.Close()

	// Copy contents
	if _, err := io.Copy(dstFile, srcFile); err != nil {
		return err
	}

	// Copy permissions
	srcInfo, err := os.Stat(src)
	if err != nil {
		return err
	}
	return os.Chmod(dst, srcInfo.Mode())
}

// matchesPatterns checks if a file path matches any of the patterns
func (m *Manager) matchesPatterns(path string, patterns []string) bool {
	if len(patterns) == 0 || (len(patterns) == 1 && patterns[0] == "*") {
		return true
	}

	for _, pattern := range patterns {
		// Handle glob patterns
		if matched, _ := filepath.Match(pattern, filepath.Base(path)); matched {
			return true
		}
		// Handle directory patterns
		if strings.Contains(pattern, "/") {
			if matched, _ := filepath.Match(pattern, path); matched {
				return true
			}
		}
		// Handle extension patterns
		if strings.HasPrefix(pattern, "*.") {
			ext := filepath.Ext(path)
			if ext == pattern[1:] {
				return true
			}
		}
	}

	return false
}

// shouldExclude checks if a file should be excluded
func (m *Manager) shouldExclude(path string, excludes []string) bool {
	for _, exclude := range excludes {
		if matched, _ := filepath.Match(exclude, filepath.Base(path)); matched {
			return true
		}
		if strings.Contains(exclude, "/") {
			if matched, _ := filepath.Match(exclude, path); matched {
				return true
			}
		}
	}
	return false
}

// executePostSyncCommands executes post-sync commands in order
func (m *Manager) executePostSyncCommands(repo *models.Repository) error {
	// Sort commands by order
	commands := make([]models.PostSyncCommand, len(repo.PostSyncCommands))
	copy(commands, repo.PostSyncCommands)
	sort.Slice(commands, func(i, j int) bool {
		return commands[i].Order < commands[j].Order
	})

	fmt.Printf("Executing %d post-sync commands...\n", len(commands))

	for i, cmd := range commands {
		fmt.Printf("[%d/%d] Running: cd %s && %s\n", i+1, len(commands), cmd.Directory, cmd.Command)

		// Create command - use shell to properly handle complex commands
		execCmd := exec.Command("sh", "-c", cmd.Command)
		execCmd.Dir = cmd.Directory
		execCmd.Stdout = os.Stdout
		execCmd.Stderr = os.Stderr

		// Execute command
		if err := execCmd.Run(); err != nil {
			return fmt.Errorf("command failed: %s: %w", cmd.Command, err)
		}

		fmt.Printf("[%d/%d] Completed: cd %s && %s\n", i+1, len(commands), cmd.Directory, cmd.Command)
	}

	return nil
}

// BackupRepository creates a backup of the target directory
func (m *Manager) BackupRepository(repo *models.Repository) error {
	if !m.config.Settings.BackupEnabled {
		return nil
	}

	// Create backup directory
	backupDir := filepath.Join(
		m.config.Settings.BackupDir,
		repo.Name,
		time.Now().Format("20060102-150405"),
	)

	if err := os.MkdirAll(backupDir, 0755); err != nil {
		return fmt.Errorf("failed to create backup directory: %w", err)
	}

	fmt.Printf("Creating backup: %s\n", backupDir)

	// Copy target directory to backup
	_, err := m.copyFiles(repo.TargetDirectory, backupDir, []string{"*"}, []string{})
	return err
}

// UpdateRepositoryStatus updates the status of a repository
func (m *Manager) UpdateRepositoryStatus(repo *models.Repository) error {
	// Check if target directory exists
	if _, err := os.Stat(repo.TargetDirectory); os.IsNotExist(err) {
		repo.Status = models.StatusNotCloned
		return nil
	}

	// For now, assume up to date if target exists
	// TODO: Compare with remote to detect changes
	repo.Status = models.StatusUpToDate

	// Count files in target directory
	fileCount := 0
	filepath.Walk(repo.TargetDirectory, func(path string, info os.FileInfo, err error) error {
		if err == nil && !info.IsDir() {
			fileCount++
		}
		return nil
	})
	repo.FilesTracked = fileCount

	return nil
}

// UpdateAllStatuses updates the status of all repositories
func (m *Manager) UpdateAllStatuses() error {
	for i := range m.config.Repositories {
		if err := m.UpdateRepositoryStatus(&m.config.Repositories[i]); err != nil {
			fmt.Printf("Warning: failed to update status for %s: %v\n",
				m.config.Repositories[i].Name, err)
		}
	}
	return nil
}

// GetRepositoryInfo returns detailed info about a repository
func (m *Manager) GetRepositoryInfo(repo *models.Repository) (map[string]interface{}, error) {
	info := map[string]interface{}{
		"name":             repo.Name,
		"url":              repo.URL,
		"branch":           repo.Branch,
		"source_directory": repo.SourceDirectory,
		"target_directory": repo.TargetDirectory,
		"status":           repo.GetStatusText(),
		"file_patterns":    repo.FilePatterns,
		"exclude_patterns": repo.ExcludePatterns,
		"last_sync":        repo.LastSync,
		"files_tracked":    repo.FilesTracked,
	}

	if repo.AutoSync != nil && repo.AutoSync.Enabled {
		info["auto_sync"] = fmt.Sprintf("Every %d seconds", repo.AutoSync.Interval)
	}

	if len(repo.PostSyncCommands) > 0 {
		info["post_sync_commands"] = len(repo.PostSyncCommands)
	}

	return info, nil
}

// directoryExists checks if a directory exists
func (m *Manager) directoryExists(path string) bool {
	info, err := os.Stat(path)
	if err != nil {
		return false
	}
	return info.IsDir()
}

// scanFiles scans the source directory and returns all files matching the patterns
func (m *Manager) scanFiles(sourcePath string, patterns, excludes []string) ([]string, error) {
	var files []string

	err := filepath.Walk(sourcePath, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		// Skip .git directory
		if info.IsDir() && info.Name() == ".git" {
			return filepath.SkipDir
		}

		// Get relative path
		relPath, err := filepath.Rel(sourcePath, path)
		if err != nil {
			return err
		}

		// Skip root directory
		if relPath == "." {
			return nil
		}

		// Check if file should be excluded
		if m.shouldExclude(relPath, excludes) {
			if info.IsDir() {
				return filepath.SkipDir
			}
			return nil
		}

		// Check if file matches patterns (only for files, not directories)
		if !info.IsDir() && m.matchesPatterns(relPath, patterns) {
			files = append(files, relPath)
		}

		return nil
	})

	return files, err
}

// selectFilesToSync shows an interactive file selection interface
func (m *Manager) selectFilesToSync(availableFiles []string, sourcePath string) ([]string, error) {
	if len(availableFiles) == 0 {
		return []string{}, nil
	}

	fmt.Printf("\n📁 %s\n", m.i18n.T(i18n.MsgAvailableFilesToSync))
	fmt.Println("────────────────────────────────────────")

	// Show all files - no limit for better user experience
	for i, file := range availableFiles {
		fmt.Printf("  %d. %s\n", i+1, file)
	}

	fmt.Printf("\n%s\n", m.i18n.T(i18n.MsgSelectionModes))
	fmt.Printf("  %s\n", m.i18n.T(i18n.MsgKeyboardMode))
	fmt.Printf("  %s\n", m.i18n.T(i18n.MsgNumberMode))
	fmt.Printf("  %s\n", m.i18n.T(i18n.MsgSelectAllFiles))
	fmt.Printf("  %s\n", m.i18n.T(i18n.MsgFilterByKeyword))
	fmt.Printf("  %s\n", m.i18n.T(i18n.MsgCancel))
	fmt.Println("  [Enter] - Select all files")

	fmt.Printf("\n%s ", m.i18n.T(i18n.MsgChooseSelectionMode))
	var input string
	fmt.Scanln(&input)

	// Handle empty input (select all)
	if input == "" {
		return availableFiles, nil
	}

	// Handle selection mode
	switch strings.ToLower(input) {
	case "k":
		// Keyboard mode
		return m.selectFilesWithKeyboard(availableFiles)
	case "n":
		// Number mode - show number input options
		return m.selectFilesWithNumbers(availableFiles)
	case "a":
		// Select all files
		return availableFiles, nil
	case "c":
		// Cancel (select none)
		return []string{}, nil
	case "f":
		// Filter by keyword
		return m.filterFilesByKeyword(availableFiles)
	default:
		// Try to parse as number input for backward compatibility
		if selectedFiles, err := m.parseFileSelection(input, availableFiles); err == nil {
			return selectedFiles, nil
		}
		// Invalid input, select all by default
		fmt.Printf("Invalid input '%s', selecting all files by default\n", input)
		return availableFiles, nil
	}
}

// selectFilesWithNumbers handles number-based file selection
func (m *Manager) selectFilesWithNumbers(availableFiles []string) ([]string, error) {
	fmt.Println("\nNumber selection options:")
	fmt.Println("  [1-9] - Select specific file by number")
	fmt.Println("  [1,3,5] - Select multiple files (comma-separated)")
	fmt.Println("  [1-5] - Select range of files")
	fmt.Println("  [a] - Select all files")
	fmt.Println("  [n] - Select none (cancel)")
	fmt.Println("  [Enter] - Select all files")

	fmt.Print("\nEnter your choice: ")
	var input string
	fmt.Scanln(&input)

	// Handle empty input (select all)
	if input == "" {
		return availableFiles, nil
	}

	// Handle numeric input (single file, multiple files, or range)
	if selectedFiles, err := m.parseFileSelection(input, availableFiles); err == nil {
		return selectedFiles, nil
	}

	// Handle letter commands
	switch strings.ToLower(input) {
	case "a":
		// Select all files
		return availableFiles, nil
	case "n":
		// Select none
		return []string{}, nil
	default:
		// Invalid input, select all by default
		fmt.Printf("Invalid input '%s', selecting all files by default\n", input)
		return availableFiles, nil
	}
}

// parseFileSelection parses user input for file selection
// Supports: single number (1), multiple numbers (1,3,5), ranges (1-5)
func (m *Manager) parseFileSelection(input string, availableFiles []string) ([]string, error) {
	input = strings.TrimSpace(input)

	// Check if input contains only numbers, commas, dashes, and spaces
	for _, char := range input {
		if !unicode.IsDigit(char) && char != ',' && char != '-' && char != ' ' {
			return nil, fmt.Errorf("invalid input format")
		}
	}

	var selectedFiles []string
	var selectedIndices []int

	// Split by comma to handle multiple selections
	parts := strings.Split(input, ",")

	for _, part := range parts {
		part = strings.TrimSpace(part)

		// Handle range (e.g., "1-5")
		if strings.Contains(part, "-") {
			rangeParts := strings.Split(part, "-")
			if len(rangeParts) != 2 {
				return nil, fmt.Errorf("invalid range format")
			}

			start, err1 := strconv.Atoi(strings.TrimSpace(rangeParts[0]))
			end, err2 := strconv.Atoi(strings.TrimSpace(rangeParts[1]))

			if err1 != nil || err2 != nil {
				return nil, fmt.Errorf("invalid range numbers")
			}

			if start < 1 || end > len(availableFiles) || start > end {
				return nil, fmt.Errorf("range out of bounds")
			}

			// Add range to selected indices
			for i := start; i <= end; i++ {
				selectedIndices = append(selectedIndices, i-1) // Convert to 0-based index
			}
		} else {
			// Handle single number
			num, err := strconv.Atoi(part)
			if err != nil {
				return nil, fmt.Errorf("invalid number: %s", part)
			}

			if num < 1 || num > len(availableFiles) {
				return nil, fmt.Errorf("number %d out of bounds (1-%d)", num, len(availableFiles))
			}

			selectedIndices = append(selectedIndices, num-1) // Convert to 0-based index
		}
	}

	// Remove duplicates and sort
	uniqueIndices := make(map[int]bool)
	for _, idx := range selectedIndices {
		uniqueIndices[idx] = true
	}

	var sortedIndices []int
	for idx := range uniqueIndices {
		sortedIndices = append(sortedIndices, idx)
	}
	sort.Ints(sortedIndices)

	// Convert indices to file paths
	for _, idx := range sortedIndices {
		selectedFiles = append(selectedFiles, availableFiles[idx])
	}

	return selectedFiles, nil
}

// filterFilesByKeyword allows user to filter files by keyword
func (m *Manager) filterFilesByKeyword(availableFiles []string) ([]string, error) {
	fmt.Print("Enter keyword to filter files: ")
	var keyword string
	fmt.Scanln(&keyword)

	if keyword == "" {
		return availableFiles, nil
	}

	var filteredFiles []string
	keywordLower := strings.ToLower(keyword)

	for _, file := range availableFiles {
		if strings.Contains(strings.ToLower(file), keywordLower) {
			filteredFiles = append(filteredFiles, file)
		}
	}

	fmt.Printf("Found %d files matching '%s':\n", len(filteredFiles), keyword)
	for i, file := range filteredFiles {
		fmt.Printf("  %d. %s\n", i+1, file)
	}

	if len(filteredFiles) == 0 {
		fmt.Println("No files match the keyword. Returning to main selection.")
		return m.selectFilesToSync(availableFiles, "")
	}

	fmt.Print("Sync these filtered files? [y/N]: ")
	var confirm string
	fmt.Scanln(&confirm)

	if strings.ToLower(confirm) == "y" || strings.ToLower(confirm) == "yes" {
		return filteredFiles, nil
	}

	// Go back to main selection
	return m.selectFilesToSync(availableFiles, "")
}

// copySelectedFiles copies only the selected files from source to target
// Returns the number of files copied and a list of file changes
func (m *Manager) copySelectedFiles(sourcePath, targetPath string, selectedFiles []string) (int, []models.FileChange, error) {
	var fileChanges []models.FileChange

	for _, relPath := range selectedFiles {
		srcPath := filepath.Join(sourcePath, relPath)
		dstPath := filepath.Join(targetPath, relPath)

		// Check if destination file exists to determine change type
		var changeType models.FileChangeType
		dstInfo, err := os.Stat(dstPath)
		if os.IsNotExist(err) {
			changeType = models.ChangeTypeAdded
		} else {
			// File exists, check if it's modified by comparing modification time or size
			srcInfo, err := os.Stat(srcPath)
			if err != nil {
				return len(fileChanges), fileChanges, fmt.Errorf("failed to stat source file %s: %w", relPath, err)
			}
			
			// Compare modification time and size to determine if modified
			if srcInfo.ModTime().After(dstInfo.ModTime()) || srcInfo.Size() != dstInfo.Size() {
				changeType = models.ChangeTypeModified
			} else {
				changeType = models.ChangeTypeModified // Still mark as modified even if same, since we're syncing
			}
		}

		// Create destination directory
		dstDir := filepath.Dir(dstPath)
		if err := os.MkdirAll(dstDir, 0755); err != nil {
			return len(fileChanges), fileChanges, fmt.Errorf("failed to create directory %s: %w", dstDir, err)
		}

		// Get source file size
		srcInfo, err := os.Stat(srcPath)
		if err != nil {
			return len(fileChanges), fileChanges, fmt.Errorf("failed to stat source file %s: %w", relPath, err)
		}

		// Copy file
		if err := m.copyFile(srcPath, dstPath); err != nil {
			return len(fileChanges), fileChanges, fmt.Errorf("failed to copy %s: %w", relPath, err)
		}

		// Record file change
		fileChanges = append(fileChanges, models.FileChange{
			Path:      relPath,
			ChangeType: changeType,
			Size:      srcInfo.Size(),
		})
	}

	return len(fileChanges), fileChanges, nil
}

// applyDiffSelections applies selected diff entries (including deletions) to target
func (m *Manager) applyDiffSelections(sourcePath, targetPath string, entries []diffEntry) ([]models.FileChange, error) {
	var changes []models.FileChange

	for _, entry := range entries {
		srcPath := filepath.Join(sourcePath, entry.Path)
		dstPath := filepath.Join(targetPath, entry.Path)

		switch entry.Status {
		case "D":
			// Delete target file
			info, err := os.Stat(dstPath)
			if err != nil && !os.IsNotExist(err) {
				return changes, fmt.Errorf("failed to stat target file %s: %w", entry.Path, err)
			}
			if err := os.Remove(dstPath); err != nil && !os.IsNotExist(err) {
				return changes, fmt.Errorf("failed to delete %s: %w", entry.Path, err)
			}
			var size int64
			if info != nil {
				size = info.Size()
			}
			changes = append(changes, models.FileChange{
				Path:       entry.Path,
				ChangeType: models.ChangeTypeDeleted,
				Size:       size,
			})
		default:
			// Added or Modified -> copy from source
			srcInfo, err := os.Stat(srcPath)
			if err != nil {
				return changes, fmt.Errorf("failed to stat source file %s: %w", entry.Path, err)
			}

			if err := os.MkdirAll(filepath.Dir(dstPath), 0755); err != nil {
				return changes, fmt.Errorf("failed to create directory %s: %w", filepath.Dir(dstPath), err)
			}

			if err := m.copyFile(srcPath, dstPath); err != nil {
				return changes, fmt.Errorf("failed to copy %s: %w", entry.Path, err)
			}

			changeType := models.ChangeTypeModified
			if entry.Status == "A" {
				changeType = models.ChangeTypeAdded
			}

			changes = append(changes, models.FileChange{
				Path:       entry.Path,
				ChangeType: changeType,
				Size:       srcInfo.Size(),
			})
		}
	}

	return changes, nil
}

// selectFilesWithKeyboard provides an interactive keyboard-based file selection
func (m *Manager) selectFilesWithKeyboard(availableFiles []string) ([]string, error) {
	if len(availableFiles) == 0 {
		return []string{}, nil
	}

	// Initialize keyboard
	if err := keyboard.Open(); err != nil {
		return nil, fmt.Errorf("failed to initialize keyboard: %w", err)
	}
	defer keyboard.Close()

	// Selection state
	selectedFiles := make(map[int]bool) // Track which files are selected
	currentIndex := 0                   // Current highlighted file
	maxIndex := len(availableFiles) - 1

	// Clear screen and show initial state
	m.renderFileSelection(availableFiles, selectedFiles, currentIndex)

	for {
		_, key, err := keyboard.GetKey()
		if err != nil {
			return nil, fmt.Errorf("keyboard input error: %w", err)
		}

		switch key {
		case keyboard.KeyArrowUp:
			if currentIndex > 0 {
				currentIndex--
			} else {
				currentIndex = maxIndex // Wrap to bottom
			}
			m.renderFileSelection(availableFiles, selectedFiles, currentIndex)

		case keyboard.KeyArrowDown:
			if currentIndex < maxIndex {
				currentIndex++
			} else {
				currentIndex = 0 // Wrap to top
			}
			m.renderFileSelection(availableFiles, selectedFiles, currentIndex)

		case keyboard.KeySpace:
			// Toggle selection of current file
			selectedFiles[currentIndex] = !selectedFiles[currentIndex]
			m.renderFileSelection(availableFiles, selectedFiles, currentIndex)

		case keyboard.KeyEnter:
			// Confirm selection
			var result []string
			for i, selected := range selectedFiles {
				if selected {
					result = append(result, availableFiles[i])
				}
			}
			return result, nil

		case keyboard.KeyEsc:
			// Cancel selection
			return []string{}, nil

		case 'a', 'A':
			// Select all
			for i := range availableFiles {
				selectedFiles[i] = true
			}
			m.renderFileSelection(availableFiles, selectedFiles, currentIndex)

		case 'n', 'N':
			// Select none
			selectedFiles = make(map[int]bool)
			m.renderFileSelection(availableFiles, selectedFiles, currentIndex)

		case 'q', 'Q':
			// Quit
			return []string{}, nil
		}
	}
}

// renderFileSelection renders the file selection interface with viewport
func (m *Manager) renderFileSelection(availableFiles []string, selectedFiles map[int]bool, currentIndex int) {
	// Clear screen
	fmt.Print("\033[2J\033[H")

	fmt.Printf("📁 %s\n", m.i18n.T(i18n.MsgSelectFilesToSync))
	fmt.Println("─────────────────────────────────────────────────────────────────────────────")

	// Get terminal height (default to 24 if unable to detect)
	terminalHeight := m.getTerminalHeight()

	// Calculate viewport size (leave space for header, footer, and controls)
	maxVisibleItems := terminalHeight - 8 // Reserve space for header, footer, controls

	// Calculate start and end indices for visible items
	startIndex := 0
	endIndex := len(availableFiles)

	if len(availableFiles) > maxVisibleItems {
		// Center the current item in the viewport
		halfViewport := maxVisibleItems / 2
		startIndex = currentIndex - halfViewport
		endIndex = currentIndex + halfViewport + 1

		// Adjust if we're near the beginning or end
		if startIndex < 0 {
			startIndex = 0
			endIndex = maxVisibleItems
		}
		if endIndex > len(availableFiles) {
			endIndex = len(availableFiles)
			startIndex = endIndex - maxVisibleItems
		}
	}

	// Show visible files
	for i := startIndex; i < endIndex; i++ {
		prefix := "  "
		if i == currentIndex {
			prefix = "▶ " // Highlight current item
		}

		status := " "
		if selectedFiles[i] {
			status = "✓" // Show selected items
		}

		fmt.Printf("%s%s %s\n", prefix, status, availableFiles[i])
	}

	// Show scroll indicator if there are more items
	if len(availableFiles) > maxVisibleItems {
		if startIndex > 0 {
			fmt.Println("... ↑ more files above ↑ ...")
		}
		if endIndex < len(availableFiles) {
			fmt.Println("... ↓ more files below ↓ ...")
		}
	}

	fmt.Println("─────────────────────────────────────────────────────────────────────────────")
	fmt.Println(m.i18n.T(i18n.MsgControls))

	// Show selection count
	selectedCount := 0
	for _, selected := range selectedFiles {
		if selected {
			selectedCount++
		}
	}
	fmt.Printf("%s\n", m.i18n.T(i18n.MsgSelectedFiles, selectedCount, len(availableFiles)))
}

// getTerminalHeight returns the terminal height, defaulting to 24 if unable to detect
func (m *Manager) getTerminalHeight() int {
	// Try to get terminal size using system call
	// This is a simple approach that works on most Unix-like systems
	cmd := exec.Command("stty", "size")
	output, err := cmd.Output()
	if err != nil {
		// Fallback to default if stty fails
		return 24
	}

	// Parse output (format: "rows cols")
	parts := strings.Fields(string(output))
	if len(parts) >= 1 {
		if height, err := strconv.Atoi(parts[0]); err == nil && height > 0 {
			return height
		}
	}

	// Fallback to default
	return 24
}

// SyncRepositoryWithDiff provides a visual diff preview before syncing
func (m *Manager) SyncRepositoryWithDiff(repo *models.Repository) error {
	repo.Status = models.StatusSyncing
	startTime := time.Now()

	// Create temp directory for cloning
	tempDir, err := os.MkdirTemp("", "stack-sync-*")
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to create temp directory: %w", err)
	}
	defer os.RemoveAll(tempDir)

	// Clone repository to temp directory
	fmt.Printf("Cloning %s @ %s to temp directory...\n", repo.URL, repo.Branch)
	ops, err := git.Clone(repo, tempDir)
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to clone repository: %w", err)
	}

	// Checkout specified branch
	if repo.Branch != "" && repo.Branch != "main" && repo.Branch != "master" {
		fmt.Printf("Checking out branch: %s\n", repo.Branch)
		if err := ops.CheckoutBranch(repo.Branch); err != nil {
			repo.Status = models.StatusError
			return fmt.Errorf("failed to checkout branch %s: %w", repo.Branch, err)
		}
	}

	// Backup if enabled
	if repo.BackupConfig != nil && repo.BackupConfig.Enabled && m.directoryExists(repo.TargetDirectory) {
		if err := m.BackupRepository(repo); err != nil {
			fmt.Printf("Warning: backup failed: %v\n", err)
		}
	}

	// Determine source path
	sourcePath := tempDir
	if repo.SourceDirectory != "" {
		sourcePath = filepath.Join(tempDir, repo.SourceDirectory)
	}

	// Check if source directory exists
	if _, err := os.Stat(sourcePath); os.IsNotExist(err) {
		repo.Status = models.StatusError
		return fmt.Errorf("source directory does not exist: %s", repo.SourceDirectory)
	}

	// Ensure target directory exists
	if err := os.MkdirAll(repo.TargetDirectory, 0755); err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to create target directory: %w", err)
	}

	// Get diff entries
	entries, err := m.getDiffEntries(sourcePath, repo.TargetDirectory, repo.FilePatterns, repo.ExcludePatterns)
	if err != nil {
		repo.Status = models.StatusError
		return fmt.Errorf("failed to get diff: %w", err)
	}

	if len(entries) == 0 {
		fmt.Println("No changes detected between remote and local. Up to date.")
		repo.Status = models.StatusUpToDate
		return nil
	}

	// Interactive preview & selection
	selectedEntries, cancelled, err := m.previewDiffAndSelect(entries, sourcePath, repo.TargetDirectory)
	if err != nil {
		repo.Status = models.StatusError
		return err
	}
	if cancelled {
		fmt.Println("Diff preview cancelled. No files were synced.")
		repo.Status = models.StatusUpToDate
		return nil
	}
	if len(selectedEntries) == 0 {
		fmt.Println("No files selected after diff preview. Nothing to sync.")
		repo.Status = models.StatusUpToDate
		return nil
	}

	// Apply selections (copy / delete)
	fileChanges, err := m.applyDiffSelections(sourcePath, repo.TargetDirectory, selectedEntries)
	if err != nil {
		repo.Status = models.StatusError
		m.recordSyncHistory(repo, []models.FileChange{}, startTime, false, err.Error())
		return fmt.Errorf("failed to apply selected changes: %w", err)
	}

	// Update status
	repo.Status = models.StatusUpToDate
	now := time.Now()
	repo.LastSync = &now
	repo.FilesTracked = len(fileChanges)

	// Record sync history
	m.recordSyncHistory(repo, fileChanges, startTime, true, "")

	// Execute post-sync commands
	if len(repo.PostSyncCommands) > 0 {
		if err := m.executePostSyncCommands(repo); err != nil {
			fmt.Printf("Warning: post-sync command failed: %v\n", err)
		}
	}

	return nil
}

// getDiffEntries runs git diff --no-index to collect change list
func (m *Manager) getDiffEntries(sourcePath, targetPath string, patterns, excludes []string) ([]diffEntry, error) {
	cmd := exec.Command("git", "diff", "--no-index", "--name-status", "--", targetPath, sourcePath)
	output, err := cmd.Output()
	if err != nil && !strings.Contains(err.Error(), "exit status 1") { // exit 1 means diff found
		return nil, fmt.Errorf("git diff failed: %w", err)
	}

	var entries []diffEntry
	seen := make(map[string]bool)
	scanner := bufio.NewScanner(strings.NewReader(string(output)))
	reTab := regexp.MustCompile(`\s+`)

	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" {
			continue
		}

		parts := reTab.Split(line, 2)
		if len(parts) < 2 {
			continue
		}

		status := strings.TrimSpace(parts[0])
		pathPart := strings.TrimSpace(parts[1])

		// Normalize path to relative
		relPath := pathPart
		if strings.HasPrefix(pathPart, sourcePath) {
			if rp, err := filepath.Rel(sourcePath, pathPart); err == nil {
				relPath = rp
			}
		} else if strings.HasPrefix(pathPart, targetPath) {
			if rp, err := filepath.Rel(targetPath, pathPart); err == nil {
				relPath = rp
			}
		}

		relPath = filepath.ToSlash(relPath)

		// Filter by patterns/excludes
		if m.shouldExclude(relPath, excludes) {
			continue
		}
		if !m.matchesPatterns(relPath, patterns) {
			continue
		}

		// Normalize status (take first rune)
		if len(status) > 0 {
			status = string(status[0])
		} else {
			status = "M"
		}

		// Deduplicate
		if seen[relPath] {
			continue
		}
		seen[relPath] = true

		entries = append(entries, diffEntry{
			Path:   relPath,
			Status: status,
		})
	}

	return entries, nil
}

// previewDiffAndSelect lets user browse diffs and choose files to sync
func (m *Manager) previewDiffAndSelect(entries []diffEntry, sourcePath, targetPath string) ([]diffEntry, bool, error) {
	selected := make(map[int]bool)
	for i := range entries {
		selected[i] = true // default select all
	}

	reader := bufio.NewScanner(os.Stdin)

	for {
		fmt.Println()
		fmt.Println(strings.Repeat("=", 80))
		fmt.Println("📄  Diff Preview (v <n> 查看diff, t <n> 切换选择, a 全选, n 全不选, c 确认同步, q 取消)")
		fmt.Println(strings.Repeat("-", 80))

		for i, entry := range entries {
			mark := "[ ]"
			if selected[i] {
				mark = "[x]"
			}

			icon := "M"
			switch entry.Status {
			case "A":
				icon = "A"
			case "D":
				icon = "D"
			}

			fmt.Printf("%2d. %s %s %s\n", i+1, mark, icon, entry.Path)
		}

		fmt.Print("\n指令 (v <编号> / t <编号> / a / n / c / q): ")
		if !reader.Scan() {
			return nil, true, fmt.Errorf("failed to read input")
		}
		cmdLine := strings.TrimSpace(reader.Text())
		if cmdLine == "" {
			continue
		}

		parts := strings.Fields(cmdLine)
		cmd := strings.ToLower(parts[0])

		switch cmd {
		case "v":
			if len(parts) < 2 {
				fmt.Println("请输入要查看的编号，例如: v 3")
				continue
			}
			idx, err := strconv.Atoi(parts[1])
			if err != nil || idx < 1 || idx > len(entries) {
				fmt.Println("编号无效")
				continue
			}
			entry := entries[idx-1]
			if err := m.showFileDiff(entry, sourcePath, targetPath); err != nil {
				fmt.Printf("显示 diff 失败: %v\n", err)
			}
		case "t":
			if len(parts) < 2 {
				fmt.Println("请输入要切换的编号，例如: t 3")
				continue
			}
			idx, err := strconv.Atoi(parts[1])
			if err != nil || idx < 1 || idx > len(entries) {
				fmt.Println("编号无效")
				continue
			}
			selected[idx-1] = !selected[idx-1]
		case "a":
			for i := range entries {
				selected[i] = true
			}
		case "n":
			for i := range entries {
				selected[i] = false
			}
		case "c":
			var result []diffEntry
			for i, entry := range entries {
				if selected[i] {
					result = append(result, entry)
				}
			}
			return result, false, nil
		case "q":
			return nil, true, nil
		default:
			fmt.Println("未知指令，请使用 v/t/a/n/c/q")
		}
	}
}

// showFileDiff renders a unified diff for a single file
func (m *Manager) showFileDiff(entry diffEntry, sourcePath, targetPath string) error {
	var left, right string

	switch entry.Status {
	case "A":
		left = "/dev/null"
		right = filepath.Join(sourcePath, entry.Path)
	case "D":
		left = filepath.Join(targetPath, entry.Path)
		right = "/dev/null"
	default:
		left = filepath.Join(targetPath, entry.Path)
		right = filepath.Join(sourcePath, entry.Path)
	}

	cmd := exec.Command("git", "diff", "--no-index", "--color=always", "--", left, right)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	// Ignore exit status 1 which indicates differences
	if err := cmd.Run(); err != nil {
		if exitErr, ok := err.(*exec.ExitError); ok && exitErr.ExitCode() == 1 {
			return nil
		}
		return err
	}

	return nil
}
