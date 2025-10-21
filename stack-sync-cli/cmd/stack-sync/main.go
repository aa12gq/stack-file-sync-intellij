package main

import (
	"fmt"
	"os"
	"strconv"
	"strings"

	"github.com/stackfilesync/stack-sync-cli/internal/config"
	"github.com/stackfilesync/stack-sync-cli/internal/i18n"
	"github.com/stackfilesync/stack-sync-cli/internal/sync"
	"github.com/stackfilesync/stack-sync-cli/internal/ui"
	"github.com/stackfilesync/stack-sync-cli/pkg/models"
)

// Version will be set during build via ldflags
var Version = "1.1.4"

// Global I18n instance
var globalI18n *i18n.I18n

func main() {
	// Initialize I18n
	globalI18n = i18n.New()

	// Check if first argument is a Chinese command to determine language
	if len(os.Args) > 1 {
		// Remove Chinese command support - commands should always be in English
		// Only set language based on config or environment
	}

	// Load config to get language setting (if not already set)
	if globalI18n.GetLanguage() == i18n.English {
		cfg, err := config.Load()
		if err == nil {
			// Set language from config
			globalI18n.SetLanguage(i18n.ParseLanguage(cfg.Settings.Language))
		}
	}

	// Set I18n for UI
	ui.SetI18n(globalI18n)

	if len(os.Args) < 2 {
		// Default behavior: show interactive selector
		runInteractive()
		return
	}

	command := os.Args[1]

	// Commands should always be in English - no translation needed

	switch command {
	case "init":
		initConfig()
	case "sync":
		syncCommand()
	case "list", "ls":
		listCommand()
	case "add":
		addCommand()
	case "remove", "rm":
		removeCommand()
	case "status":
		statusCommand()
	case "watch":
		watchCommand()
	case "help", "-h", "--help":
		printHelp()
	case "version", "-v", "--version":
		printVersion()
	default:
		ui.PrintError(globalI18n.T(i18n.MsgUnknownCommand, os.Args[1]))
		printHelp()
		os.Exit(1)
	}
}

// runInteractive shows the interactive repository selector
func runInteractive() {
	cfg, err := config.Load()
	if err != nil {
		ui.PrintError(globalI18n.T(i18n.MsgFailedToLoadConfig, err))
		os.Exit(1)
	}

	if len(cfg.Repositories) == 0 {
		ui.PrintWarning(globalI18n.T(i18n.MsgNoRepositories))
		ui.PrintInfo("Run 'stack-sync add' to add a repository")
		os.Exit(0)
	}

	manager := sync.NewManager(cfg, globalI18n)

	// Update repository statuses
	ui.PrintInfo("Checking repository statuses...")
	if err := manager.UpdateAllStatuses(); err != nil {
		ui.PrintWarning("Failed to update some repository statuses")
	}

	// Show interactive selector
	repo, err := ui.SelectRepository(cfg.Repositories)
	if err != nil {
		ui.PrintError("Selection cancelled")
		os.Exit(0)
	}

	// Sync the selected repository
	ui.PrintInfo(globalI18n.T(i18n.MsgSyncing, repo.Name))
	if err := manager.SyncRepository(repo); err != nil {
		ui.PrintError(globalI18n.T(i18n.MsgSyncFailed, err))
		os.Exit(1)
	}

	ui.PrintSuccess(globalI18n.T(i18n.MsgSuccessfullySynced, repo.Name))
}

// initConfig initializes the configuration
func initConfig() {
	configPath := config.GetConfigPath()

	// Check if config already exists
	if _, err := os.Stat(configPath); err == nil {
		if !ui.ConfirmAction("Config file already exists. Overwrite?") {
			ui.PrintInfo("Cancelled")
			return
		}
	}

	cfg := config.DefaultConfig()
	if err := config.Save(cfg); err != nil {
		ui.PrintError("Failed to save config: %v", err)
		os.Exit(1)
	}

	ui.PrintSuccess("Configuration initialized at: %s", configPath)
	ui.PrintInfo("Edit the config file to add repositories")
}

// syncCommand syncs a specific repository or all
func syncCommand() {
	cfg, err := config.Load()
	if err != nil {
		ui.PrintError("Failed to load config: %v", err)
		os.Exit(1)
	}

	manager := sync.NewManager(cfg, globalI18n)

	// Parse command line arguments
	var repoName, filterKeyword, numberSelection string
	args := os.Args[2:] // Skip "stack-sync" and "sync"

	for i := 0; i < len(args); i++ {
		arg := args[i]
		if arg == "-f" && i+1 < len(args) {
			filterKeyword = args[i+1]
			i++ // Skip the next argument as it's the filter value
		} else if arg == "-n" && i+1 < len(args) {
			numberSelection = args[i+1]
			i++ // Skip the next argument as it's the number selection value
		} else if !strings.HasPrefix(arg, "-") {
			// Repository name (not a flag)
			repoName = arg
		}
	}

	// If repository name provided, sync that one
	if repoName != "" {
		repo, err := cfg.GetRepository(repoName)
		if err != nil {
			ui.PrintError("Repository not found: %s", repoName)
			os.Exit(1)
		}

		ui.PrintInfo(globalI18n.T(i18n.MsgSyncing, repo.Name))

		// Use appropriate sync method based on parameters
		if numberSelection != "" {
			// Use number selection method
			if err := manager.SyncRepositoryWithNumberSelection(repo, filterKeyword, numberSelection); err != nil {
				ui.PrintError("Sync failed: %v", err)
				os.Exit(1)
			}
		} else {
			// Use filter method or regular sync
			if err := manager.SyncRepositoryWithFilter(repo, filterKeyword); err != nil {
				ui.PrintError("Sync failed: %v", err)
				os.Exit(1)
			}
		}

		ui.PrintSuccess("Successfully synced %s", repo.Name)
		return
	}

	// Sync all repositories
	ui.PrintInfo(globalI18n.T(i18n.MsgSyncing, "all repositories"))
	failed := 0
	for i := range cfg.Repositories {
		repo := &cfg.Repositories[i]
		ui.PrintInfo(globalI18n.T(i18n.MsgSyncing, repo.Name))

		if err := manager.SyncRepository(repo); err != nil {
			ui.PrintError("Failed to sync %s: %v", repo.Name, err)
			failed++
		} else {
			ui.PrintSuccess("Synced %s", repo.Name)
		}
	}

	if failed > 0 {
		ui.PrintWarning("%d repositories failed to sync", failed)
		os.Exit(1)
	}

	ui.PrintSuccess("All repositories synced successfully")
}

// listCommand lists all repositories
func listCommand() {
	cfg, err := config.Load()
	if err != nil {
		ui.PrintError("Failed to load config: %v", err)
		os.Exit(1)
	}

	manager := sync.NewManager(cfg, globalI18n)
	manager.UpdateAllStatuses()

	ui.PrintRepositoryList(cfg.Repositories)
}

// addCommand adds a new repository
func addCommand() {
	cfg, err := config.Load()
	if err != nil {
		ui.PrintError("Failed to load config: %v", err)
		os.Exit(1)
	}

	ui.PrintInfo("Add a new repository (following IntelliJ plugin model)")
	fmt.Println()

	// Prompt for repository details
	name, err := ui.PromptInput("Repository name", "")
	if err != nil {
		ui.PrintError("Input cancelled")
		os.Exit(0)
	}

	url, err := ui.PromptInput("Repository URL", "")
	if err != nil {
		ui.PrintError("Input cancelled")
		os.Exit(0)
	}

	branch, err := ui.PromptInput("Branch", "main")
	if err != nil {
		ui.PrintError("Input cancelled")
		os.Exit(0)
	}

	sourceDir, err := ui.PromptInput("Source directory (in remote repo, empty for root)", "")
	if err != nil {
		ui.PrintError("Input cancelled")
		os.Exit(0)
	}

	targetDir, err := ui.PromptInput("Target directory (local project path)", "")
	if err != nil {
		ui.PrintError("Input cancelled")
		os.Exit(0)
	}

	// Optional: File patterns
	ui.PrintInfo("File patterns to sync (e.g., *.proto, *.go, src/**/*.js)")
	ui.PrintInfo("Use * to sync all files, or comma-separated patterns")
	patternsInput, err := ui.PromptInput("Patterns", "*")
	if err != nil {
		patternsInput = "*"
	}
	filePatterns := []string{patternsInput}
	if patternsInput != "*" {
		filePatterns = strings.Split(patternsInput, ",")
		for i := range filePatterns {
			filePatterns[i] = strings.TrimSpace(filePatterns[i])
		}
	}

	// Optional: Exclude patterns
	ui.PrintInfo("Files to exclude (e.g., *.log, node_modules/, .git/)")
	excludeInput, err := ui.PromptInput("Exclude patterns (comma-separated, optional)", "")
	if err != nil {
		excludeInput = ""
	}
	var excludePatterns []string
	if excludeInput != "" {
		excludePatterns = strings.Split(excludeInput, ",")
		for i := range excludePatterns {
			excludePatterns[i] = strings.TrimSpace(excludePatterns[i])
		}
	}

	// Authentication configuration
	var repoType, username, password string

	// Detect repo type from URL
	if strings.HasPrefix(url, "git@") || strings.HasPrefix(url, "ssh://") {
		repoType = "SSH"
		ui.PrintInfo("SSH URL detected - using SSH key authentication")
	} else if strings.HasPrefix(url, "https://") || strings.HasPrefix(url, "http://") {
		repoType = "HTTPS"
		ui.PrintInfo("HTTPS URL detected")

		// Ask for credentials
		if ui.ConfirmAction("Does this repository require authentication?") {
			ui.PrintInfo("For private repositories, enter your credentials")
			ui.PrintInfo("You can use a Personal Access Token as password")

			username, err = ui.PromptInput("Username (or 'git' for token auth)", "")
			if err != nil {
				username = ""
			}

			password, err = ui.PromptInput("Password or Personal Access Token", "")
			if err != nil {
				password = ""
			}
		}
	} else {
		repoType = "SSH" // Default to SSH for unknown formats
	}

	// Auto sync configuration
	enableAutoSync := ui.ConfirmAction("Enable auto-sync?")
	var autoSync *models.AutoSyncConfig
	if enableAutoSync {
		intervalStr, _ := ui.PromptInput("Auto-sync interval (seconds)", "300")
		interval := 300
		if i, err := strconv.Atoi(intervalStr); err == nil {
			interval = i
		}
		autoSync = &models.AutoSyncConfig{
			Enabled:  true,
			Interval: interval,
		}
	}

	// Watch mode configuration
	enableWatchMode := ui.ConfirmAction("Enable watch mode (auto-sync on file changes)?")

	// Post-sync commands
	var postSyncCommands []models.PostSyncCommand
	ui.PrintInfo("Post-sync commands run AFTER files are synced (e.g., build, compile)")
	if ui.ConfirmAction("Add post-sync commands?") {
		for {
			cmdDir, err := ui.PromptInput("Command directory", targetDir)
			if err != nil {
				break
			}
			ui.PrintInfo("Example: protoc --dart_out=grpc:. -I. *.proto")
			cmd, err := ui.PromptInput("Command to run", "")
			if err != nil {
				break
			}
			orderStr, _ := ui.PromptInput("Execution order", fmt.Sprintf("%d", len(postSyncCommands)))
			order := len(postSyncCommands)
			if o, err := strconv.Atoi(orderStr); err == nil {
				order = o
			}

			postSyncCommands = append(postSyncCommands, models.PostSyncCommand{
				Directory: cmdDir,
				Command:   cmd,
				Order:     order,
			})

			if !ui.ConfirmAction("Add another command?") {
				break
			}
		}
	}

	// Create repository
	repo := models.Repository{
		Name:             name,
		URL:              url,
		Branch:           branch,
		SourceDirectory:  sourceDir,
		TargetDirectory:  targetDir,
		LocalPath:        targetDir, // LocalPath is same as TargetDirectory
		FilePatterns:     filePatterns,
		ExcludePatterns:  excludePatterns,
		SyncPatterns:     filePatterns,    // SyncPatterns is same as FilePatterns
		Exclude:          excludePatterns, // Exclude is same as ExcludePatterns
		WatchMode:        enableWatchMode,
		AutoSync:         autoSync,
		BackupConfig:     &models.BackupConfig{Enabled: true, MaxBackups: 10},
		PostSyncCommands: postSyncCommands,
		RepoType:         repoType,
		Username:         username,
		Password:         password,
	}

	if err := cfg.AddRepository(repo); err != nil {
		ui.PrintError("Failed to add repository: %v", err)
		os.Exit(1)
	}

	ui.PrintSuccess("Repository added: %s", name)
	fmt.Println()
	ui.PrintInfo(globalI18n.T(i18n.MsgConfiguration))
	fmt.Printf("  %s: %s\n", globalI18n.T(i18n.MsgRepositoryName), repo.Name)
	fmt.Printf("  %s: %s @ %s\n", globalI18n.T(i18n.MsgRepositoryURL), repo.URL, repo.Branch)
	fmt.Printf("  %s: %s\n", globalI18n.T(i18n.MsgRepositorySource), repo.SourceDirectory)
	fmt.Printf("  %s: %s\n", globalI18n.T(i18n.MsgRepositoryTarget), repo.TargetDirectory)
	fmt.Printf("  %s: %v\n", globalI18n.T(i18n.MsgRepositoryPatterns), repo.FilePatterns)
	if len(repo.ExcludePatterns) > 0 {
		fmt.Printf("  %s: %v\n", globalI18n.T(i18n.MsgRepositoryExclude), repo.ExcludePatterns)
	}
	if repo.WatchMode {
		fmt.Printf("  %s\n", globalI18n.T(i18n.MsgWatchModeEnabled))
	}
	if repo.AutoSync != nil && repo.AutoSync.Enabled {
		fmt.Printf("  %s\n", globalI18n.T(i18n.MsgAutoSyncInterval, repo.AutoSync.Interval))
	}
	if len(repo.PostSyncCommands) > 0 {
		fmt.Printf("  %s\n", globalI18n.T(i18n.MsgPostSyncCommands, len(repo.PostSyncCommands)))
	}
	fmt.Println()

	// Ask if user wants to sync now
	if ui.ConfirmAction(globalI18n.T(i18n.MsgSyncNow)) {
		manager := sync.NewManager(cfg, globalI18n)
		repoPtr, _ := cfg.GetRepository(name)

		ui.PrintInfo("Syncing %s...", name)
		if err := manager.SyncRepository(repoPtr); err != nil {
			ui.PrintError("Sync failed: %v", err)
			os.Exit(1)
		}

		ui.PrintSuccess("Repository synced successfully")
	}
}

// removeCommand removes a repository
func removeCommand() {
	cfg, err := config.Load()
	if err != nil {
		ui.PrintError("Failed to load config: %v", err)
		os.Exit(1)
	}

	if len(os.Args) < 3 {
		ui.PrintError("Usage: stack-sync remove <repository-name>")
		os.Exit(1)
	}

	repoName := os.Args[2]

	if !ui.ConfirmAction(fmt.Sprintf("Remove repository '%s' from config?", repoName)) {
		ui.PrintInfo("Cancelled")
		return
	}

	if err := cfg.RemoveRepository(repoName); err != nil {
		ui.PrintError("Failed to remove repository: %v", err)
		os.Exit(1)
	}

	ui.PrintSuccess("Repository removed: %s", repoName)
	ui.PrintWarning("Note: Local files were not deleted. Remove manually if needed.")
}

// statusCommand shows detailed status of a repository
func statusCommand() {
	cfg, err := config.Load()
	if err != nil {
		ui.PrintError("Failed to load config: %v", err)
		os.Exit(1)
	}

	if len(os.Args) < 3 {
		// Show status of all repositories
		listCommand()
		return
	}

	repoName := os.Args[2]
	repo, err := cfg.GetRepository(repoName)
	if err != nil {
		ui.PrintError("Repository not found: %s", repoName)
		os.Exit(1)
	}

	manager := sync.NewManager(cfg, globalI18n)
	manager.UpdateRepositoryStatus(repo)

	info, err := manager.GetRepositoryInfo(repo)
	if err != nil {
		ui.PrintError("Failed to get repository info: %v", err)
		os.Exit(1)
	}

	// Print detailed info
	fmt.Println()
	fmt.Println(globalI18n.T(i18n.MsgStatusDisplay))
	fmt.Println(globalI18n.T(i18n.MsgStatusSeparator))
	for key, value := range info {
		fmt.Printf("%-15s: %v\n", key, value)
	}
	fmt.Println()
}

// watchCommand starts the file watcher
func watchCommand() {
	cfg, err := config.Load()
	if err != nil {
		ui.PrintError("Failed to load config: %v", err)
		os.Exit(1)
	}

	// Check if any repository has watch mode enabled
	hasWatchRepo := false
	for _, repo := range cfg.Repositories {
		if repo.WatchMode {
			hasWatchRepo = true
			break
		}
	}

	if !hasWatchRepo {
		ui.PrintWarning("No repositories have watch mode enabled")
		ui.PrintInfo("Enable watch mode in the config file or add a repository with watch mode")
		os.Exit(0)
	}

	manager := sync.NewManager(cfg, globalI18n)
	watcher, err := sync.NewWatcher(manager)
	if err != nil {
		ui.PrintError("Failed to create watcher: %v", err)
		os.Exit(1)
	}

	if err := watcher.Start(); err != nil {
		ui.PrintError("Failed to start watcher: %v", err)
		os.Exit(1)
	}

	ui.PrintSuccess("File watcher started. Press Ctrl+C to stop.")

	// Wait for interrupt signal
	select {}
}

// printHelp prints usage information
func printHelp() {
	if globalI18n.GetLanguage() == i18n.Chinese {
		fmt.Print(`
Stack Sync - 开发团队文件同步工具

使用方法:
    stack-sync [命令] [选项]

命令:
    (默认)           显示交互式仓库选择器
    init             初始化配置文件
    sync [仓库] [-f 关键词] [-n 数字] 同步仓库或所有仓库
    list, ls         列出所有仓库
    add              添加新仓库（交互式）
    remove, rm <仓库> 从配置中删除仓库
    status [仓库]    显示仓库状态
    watch            启动文件监控器进行自动同步
    help, -h         显示此帮助信息
    version, -v      显示版本信息

选项:
    -f <关键词>      在选择前按关键词过滤文件
    -n <数字>        直接使用数字选择文件（如：77,93 或 1-5）

示例:
    stack-sync                    # 交互模式
    stack-sync init              # 初始化配置
    stack-sync add               # 添加仓库
    stack-sync sync my-repo      # 同步指定仓库
    stack-sync sync my-repo -f team # 同步仓库，按 'team' 过滤文件
    stack-sync sync my-repo -n 77,93 # 同步仓库，选择第77和93个文件
    stack-sync sync my-repo -f team -n 1-3 # 先过滤再选择前3个文件
    stack-sync sync              # 同步所有仓库
    stack-sync list              # 列出仓库
    stack-sync watch             # 启动自动同步监控器

更多信息，请访问: https://github.com/aa12gq/stackfilesync/stack-sync-cli
`)
	} else {
		fmt.Print(`
Stack Sync - File synchronization tool for development teams

USAGE:
    stack-sync [COMMAND] [OPTIONS]

COMMANDS:
    (default)           Show interactive repository selector
    init               Initialize configuration file
    sync [repo] [-f keyword] [-n numbers] Sync a repository or all repositories
    list, ls           List all repositories
    add                Add a new repository (interactive)
    remove, rm <repo>  Remove a repository from config
    status [repo]      Show repository status
    watch              Start file watcher for auto-sync
    help, -h           Show this help message
    version, -v        Show version information

OPTIONS:
    -f <keyword>       Filter files by keyword before selection
    -n <numbers>       Directly select files by numbers (e.g., 77,93 or 1-5)

EXAMPLES:
    stack-sync                    # Interactive mode
    stack-sync init              # Initialize config
    stack-sync add               # Add a repository
    stack-sync sync my-repo      # Sync specific repository
    stack-sync sync my-repo -f team # Sync repository, filter files by 'team'
    stack-sync sync my-repo -n 77,93 # Sync repository, select files 77 and 93
    stack-sync sync my-repo -f team -n 1-3 # Filter by 'team', then select first 3 files
    stack-sync sync              # Sync all repositories
    stack-sync list              # List repositories
    stack-sync watch             # Start auto-sync watcher

For more information, visit: https://github.com/aa12gq/stackfilesync/stack-sync-cli
`)
	}
}

// printVersion prints version information
func printVersion() {
	fmt.Printf("Stack Sync CLI v%s\n", Version)
}
