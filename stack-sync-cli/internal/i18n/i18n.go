package i18n

import (
	"fmt"
	"strings"
)

// MessageKey represents a message key for internationalization
type MessageKey string

// Message keys
const (
	// Commands
	MsgCommandInit    MessageKey = "command.init"
	MsgCommandSync    MessageKey = "command.sync"
	MsgCommandList    MessageKey = "command.list"
	MsgCommandAdd     MessageKey = "command.add"
	MsgCommandRemove  MessageKey = "command.remove"
	MsgCommandStatus  MessageKey = "command.status"
	MsgCommandWatch   MessageKey = "command.watch"
	MsgCommandHelp    MessageKey = "command.help"
	MsgCommandVersion MessageKey = "command.version"

	// UI Messages
	MsgSelectRepository    MessageKey = "ui.select_repository"
	MsgNoRepositories      MessageKey = "ui.no_repositories"
	MsgAddRepositoryPrompt MessageKey = "ui.add_repository_prompt"
	MsgRepositoryName      MessageKey = "ui.repository_name"
	MsgRepositoryURL       MessageKey = "ui.repository_url"
	MsgBranch              MessageKey = "ui.branch"
	MsgSourceDirectory     MessageKey = "ui.source_directory"
	MsgTargetDirectory     MessageKey = "ui.target_directory"
	MsgFilePatterns        MessageKey = "ui.file_patterns"
	MsgExcludePatterns     MessageKey = "ui.exclude_patterns"
	MsgEnableAutoSync      MessageKey = "ui.enable_auto_sync"
	MsgEnableWatchMode     MessageKey = "ui.enable_watch_mode"
	MsgAddPostSyncCommands MessageKey = "ui.add_post_sync_commands"
	MsgCommandDirectory    MessageKey = "ui.command_directory"
	MsgCommandToRun        MessageKey = "ui.command_to_run"
	MsgExecutionOrder      MessageKey = "ui.execution_order"
	MsgAddAnotherCommand   MessageKey = "ui.add_another_command"
	MsgSyncRepositoryNow   MessageKey = "ui.sync_repository_now"

	// Status Messages
	MsgSyncing            MessageKey = "status.syncing"
	MsgSuccessfullySynced MessageKey = "status.successfully_synced"
	MsgSyncFailed         MessageKey = "status.sync_failed"
	MsgRepositoryAdded    MessageKey = "status.repository_added"
	MsgRepositoryRemoved  MessageKey = "status.repository_removed"
	MsgConfigurationSaved MessageKey = "status.configuration_saved"

	// Error Messages
	MsgFailedToLoadConfig MessageKey = "error.failed_to_load_config"
	MsgRepositoryNotFound MessageKey = "error.repository_not_found"
	MsgFailedToAddRepo    MessageKey = "error.failed_to_add_repo"
	MsgFailedToRemoveRepo MessageKey = "error.failed_to_remove_repo"
	MsgUnknownCommand     MessageKey = "error.unknown_command"

	// Help Messages
	MsgHelpUsage    MessageKey = "help.usage"
	MsgHelpCommands MessageKey = "help.commands"
	MsgHelpOptions  MessageKey = "help.options"
	MsgHelpExamples MessageKey = "help.examples"

	// Repository List Messages
	MsgRepositoryInfo     MessageKey = "repo.info"
	MsgRepositorySource   MessageKey = "repo.source"
	MsgRepositoryTarget   MessageKey = "repo.target"
	MsgRepositoryPatterns MessageKey = "repo.patterns"
	MsgRepositoryExclude  MessageKey = "repo.exclude"
	MsgWatchModeEnabled   MessageKey = "repo.watch_enabled"
	MsgAutoSyncInterval   MessageKey = "repo.auto_sync_interval"
	MsgPostSyncCommands   MessageKey = "repo.post_sync_commands"

	// Status Display Messages
	MsgStatusDisplay   MessageKey = "status.display"
	MsgStatusSeparator MessageKey = "status.separator"
	MsgConfiguration   MessageKey = "status.configuration"
	MsgSyncNow         MessageKey = "status.sync_now"

	// Additional Status Messages
	MsgSelectionCancelled     MessageKey = "status.selection_cancelled"
	MsgConfigSaved            MessageKey = "status.config_saved"
	MsgAllReposSynced         MessageKey = "status.all_repos_synced"
	MsgRepoSynced             MessageKey = "status.repo_synced"
	MsgInputCancelled         MessageKey = "status.input_cancelled"
	MsgRepoAdded              MessageKey = "status.repo_added"
	MsgRepoRemoved            MessageKey = "status.repo_removed"
	MsgRepoSyncedSuccessfully MessageKey = "status.repo_synced_successfully"
	MsgWatcherStarted         MessageKey = "status.watcher_started"

	// Additional Error Messages
	MsgFailedToSaveConfig    MessageKey = "error.failed_to_save_config"
	MsgFailedToSyncRepo      MessageKey = "error.failed_to_sync_repo"
	MsgFailedToCreateWatcher MessageKey = "error.failed_to_create_watcher"
	MsgFailedToStartWatcher  MessageKey = "error.failed_to_start_watcher"
	MsgUsageRemove           MessageKey = "error.usage_remove"

	// File Selection Messages
	MsgAvailableFilesToSync MessageKey = "file_selection.available_files"
	MsgSelectionModes       MessageKey = "file_selection.selection_modes"
	MsgChooseSelectionMode  MessageKey = "file_selection.choose_mode"
	MsgSelectFilesToSync    MessageKey = "file_selection.select_files"
	MsgControls             MessageKey = "file_selection.controls"
	MsgKeyboardMode         MessageKey = "file_selection.keyboard_mode"
	MsgNumberMode           MessageKey = "file_selection.number_mode"
	MsgSelectAllFiles       MessageKey = "file_selection.select_all"
	MsgFilterByKeyword      MessageKey = "file_selection.filter_keyword"
	MsgCancel               MessageKey = "file_selection.cancel"
	MsgSelectedFiles        MessageKey = "file_selection.selected_files"
)

// Language represents a supported language
type Language string

const (
	English Language = "en-US"
	Chinese Language = "zh-CN"
)

// Messages contains all localized messages
type Messages map[MessageKey]string

// I18n handles internationalization
type I18n struct {
	currentLanguage Language
	messages        map[Language]Messages
}

// New creates a new I18n instance
func New() *I18n {
	i18n := &I18n{
		currentLanguage: English,
		messages:        make(map[Language]Messages),
	}

	// Initialize messages
	i18n.initMessages()
	return i18n
}

// SetLanguage sets the current language
func (i *I18n) SetLanguage(lang Language) {
	i.currentLanguage = lang
}

// GetLanguage returns the current language
func (i *I18n) GetLanguage() Language {
	return i.currentLanguage
}

// T translates a message key to the current language
func (i *I18n) T(key MessageKey, args ...interface{}) string {
	messages, exists := i.messages[i.currentLanguage]
	if !exists {
		// Fallback to English
		messages = i.messages[English]
	}

	message, exists := messages[key]
	if !exists {
		// Fallback to English
		if englishMessages, exists := i.messages[English]; exists {
			message = englishMessages[key]
		}
		if message == "" {
			return string(key) // Return key if no translation found
		}
	}

	if len(args) > 0 {
		return fmt.Sprintf(message, args...)
	}
	return message
}

// initMessages initializes all message translations
func (i *I18n) initMessages() {
	// English messages
	i.messages[English] = Messages{
		// Commands
		MsgCommandInit:    "init",
		MsgCommandSync:    "sync",
		MsgCommandList:    "list",
		MsgCommandAdd:     "add",
		MsgCommandRemove:  "remove",
		MsgCommandStatus:  "status",
		MsgCommandWatch:   "watch",
		MsgCommandHelp:    "help",
		MsgCommandVersion: "version",

		// UI Messages
		MsgSelectRepository:    "Select a repository to sync",
		MsgNoRepositories:      "No repositories configured",
		MsgAddRepositoryPrompt: "Add a new repository (following IntelliJ plugin model)",
		MsgRepositoryName:      "Repository name",
		MsgRepositoryURL:       "Repository URL",
		MsgBranch:              "Branch",
		MsgSourceDirectory:     "Source directory (in remote repo, empty for root)",
		MsgTargetDirectory:     "Target directory (local project path)",
		MsgFilePatterns:        "File patterns to sync (e.g., *.proto, *.go, src/**/*.js)",
		MsgExcludePatterns:     "Files to exclude (e.g., *.log, node_modules/, .git/)",
		MsgEnableAutoSync:      "Enable auto-sync?",
		MsgEnableWatchMode:     "Enable watch mode (auto-sync on file changes)?",
		MsgAddPostSyncCommands: "Add post-sync commands?",
		MsgCommandDirectory:    "Command directory",
		MsgCommandToRun:        "Command to run",
		MsgExecutionOrder:      "Execution order",
		MsgAddAnotherCommand:   "Add another command?",
		MsgSyncRepositoryNow:   "Sync repository now?",

		// Status Messages
		MsgSyncing:            "Syncing %s...",
		MsgSuccessfullySynced: "Successfully synced %s",
		MsgSyncFailed:         "Sync failed: %v",
		MsgRepositoryAdded:    "Repository added: %s",
		MsgRepositoryRemoved:  "Repository removed: %s",
		MsgConfigurationSaved: "Configuration initialized at: %s",

		// Error Messages
		MsgFailedToLoadConfig: "Failed to load config: %v",
		MsgRepositoryNotFound: "Repository not found: %s",
		MsgFailedToAddRepo:    "Failed to add repository: %v",
		MsgFailedToRemoveRepo: "Failed to remove repository: %v",
		MsgUnknownCommand:     "Unknown command: %s",

		// Help Messages
		MsgHelpUsage:    "Stack Sync - File synchronization tool for development teams",
		MsgHelpCommands: "COMMANDS:",
		MsgHelpOptions:  "OPTIONS:",
		MsgHelpExamples: "EXAMPLES:",

		// Repository List Messages
		MsgRepositoryInfo:     "Repository Information:",
		MsgRepositorySource:   "Source",
		MsgRepositoryTarget:   "Target",
		MsgRepositoryPatterns: "Patterns",
		MsgRepositoryExclude:  "Exclude",
		MsgWatchModeEnabled:   "Watch mode: Enabled",
		MsgAutoSyncInterval:   "Auto-sync: Every %d seconds",
		MsgPostSyncCommands:   "Post-sync commands: %d configured",

		// Status Display Messages
		MsgStatusDisplay:   "Repository Information:",
		MsgStatusSeparator: "──────────────────────────────────────",
		MsgConfiguration:   "Configuration:",
		MsgSyncNow:         "Sync repository now?",

		// Additional Status Messages
		MsgSelectionCancelled:     "Selection cancelled",
		MsgConfigSaved:            "Configuration initialized at: %s",
		MsgAllReposSynced:         "All repositories synced successfully",
		MsgRepoSynced:             "Synced %s",
		MsgInputCancelled:         "Input cancelled",
		MsgRepoAdded:              "Repository added: %s",
		MsgRepoRemoved:            "Repository removed: %s",
		MsgRepoSyncedSuccessfully: "Repository synced successfully",
		MsgWatcherStarted:         "File watcher started. Press Ctrl+C to stop.",

		// Additional Error Messages
		MsgFailedToSaveConfig:    "Failed to save config: %v",
		MsgFailedToSyncRepo:      "Failed to sync %s: %v",
		MsgFailedToCreateWatcher: "Failed to create watcher: %v",
		MsgFailedToStartWatcher:  "Failed to start watcher: %v",
		MsgUsageRemove:           "Usage: stack-sync remove <repository-name>",

		// File Selection Messages
		MsgAvailableFilesToSync: "Available files to sync:",
		MsgSelectionModes:       "Selection modes:",
		MsgChooseSelectionMode:  "Choose selection mode:",
		MsgSelectFilesToSync:    "Select files to sync (Use ↑↓ to navigate, Space to select, Enter to confirm)",
		MsgControls:             "Controls: ↑↓ Navigate | Space Toggle | Enter Confirm | Esc Cancel | a All | n None | q Quit",
		MsgKeyboardMode:         "[k] - Keyboard mode (↑↓ navigate, Space select, Enter confirm)",
		MsgNumberMode:           "[n] - Number mode (1,2,3 or 1-3 or 1,3,5)",
		MsgSelectAllFiles:       "[a] - Select all files",
		MsgFilterByKeyword:      "[f] - Filter by keyword",
		MsgCancel:               "[c] - Cancel (select none)",
		MsgSelectedFiles:        "Selected: %d/%d files",
	}

	// Chinese messages
	i.messages[Chinese] = Messages{
		// Commands
		MsgCommandInit:    "初始化",
		MsgCommandSync:    "同步",
		MsgCommandList:    "列表",
		MsgCommandAdd:     "添加",
		MsgCommandRemove:  "删除",
		MsgCommandStatus:  "状态",
		MsgCommandWatch:   "监控",
		MsgCommandHelp:    "帮助",
		MsgCommandVersion: "版本",

		// UI Messages
		MsgSelectRepository:    "选择要同步的仓库",
		MsgNoRepositories:      "未配置任何仓库",
		MsgAddRepositoryPrompt: "添加新仓库（遵循 IntelliJ 插件模式）",
		MsgRepositoryName:      "仓库名称",
		MsgRepositoryURL:       "仓库 URL",
		MsgBranch:              "分支",
		MsgSourceDirectory:     "源目录（远程仓库中的目录，空表示根目录）",
		MsgTargetDirectory:     "目标目录（本地项目路径）",
		MsgFilePatterns:        "要同步的文件模式（例如：*.proto, *.go, src/**/*.js）",
		MsgExcludePatterns:     "要排除的文件（例如：*.log, node_modules/, .git/）",
		MsgEnableAutoSync:      "启用自动同步？",
		MsgEnableWatchMode:     "启用监控模式（文件变化时自动同步）？",
		MsgAddPostSyncCommands: "添加后处理命令？",
		MsgCommandDirectory:    "命令执行目录",
		MsgCommandToRun:        "要执行的命令",
		MsgExecutionOrder:      "执行顺序",
		MsgAddAnotherCommand:   "添加另一个命令？",
		MsgSyncRepositoryNow:   "现在同步仓库？",

		// Status Messages
		MsgSyncing:            "正在同步 %s...",
		MsgSuccessfullySynced: "成功同步 %s",
		MsgSyncFailed:         "同步失败：%v",
		MsgRepositoryAdded:    "仓库已添加：%s",
		MsgRepositoryRemoved:  "仓库已删除：%s",
		MsgConfigurationSaved: "配置已初始化：%s",

		// Error Messages
		MsgFailedToLoadConfig: "加载配置失败：%v",
		MsgRepositoryNotFound: "未找到仓库：%s",
		MsgFailedToAddRepo:    "添加仓库失败：%v",
		MsgFailedToRemoveRepo: "删除仓库失败：%v",
		MsgUnknownCommand:     "未知命令：%s",

		// Help Messages
		MsgHelpUsage:    "Stack Sync - 开发团队文件同步工具",
		MsgHelpCommands: "命令：",
		MsgHelpOptions:  "选项：",
		MsgHelpExamples: "示例：",

		// Repository List Messages
		MsgRepositoryInfo:     "仓库信息：",
		MsgRepositorySource:   "源目录",
		MsgRepositoryTarget:   "目标目录",
		MsgRepositoryPatterns: "文件模式",
		MsgRepositoryExclude:  "排除文件",
		MsgWatchModeEnabled:   "监控模式：已启用",
		MsgAutoSyncInterval:   "自动同步：每 %d 秒",
		MsgPostSyncCommands:   "后处理命令：已配置 %d 个",

		// Status Display Messages
		MsgStatusDisplay:   "仓库信息：",
		MsgStatusSeparator: "──────────────────────────────────────",
		MsgConfiguration:   "配置：",
		MsgSyncNow:         "现在同步仓库？",

		// Additional Status Messages
		MsgSelectionCancelled:     "选择已取消",
		MsgConfigSaved:            "配置已初始化：%s",
		MsgAllReposSynced:         "所有仓库同步成功",
		MsgRepoSynced:             "已同步 %s",
		MsgInputCancelled:         "输入已取消",
		MsgRepoAdded:              "仓库已添加：%s",
		MsgRepoRemoved:            "仓库已删除：%s",
		MsgRepoSyncedSuccessfully: "仓库同步成功",
		MsgWatcherStarted:         "文件监控器已启动。按 Ctrl+C 停止。",

		// Additional Error Messages
		MsgFailedToSaveConfig:    "保存配置失败：%v",
		MsgFailedToSyncRepo:      "同步 %s 失败：%v",
		MsgFailedToCreateWatcher: "创建监控器失败：%v",
		MsgFailedToStartWatcher:  "启动监控器失败：%v",
		MsgUsageRemove:           "用法：stack-sync 删除 <仓库名称>",

		// File Selection Messages
		MsgAvailableFilesToSync: "可同步的文件：",
		MsgSelectionModes:       "选择模式：",
		MsgChooseSelectionMode:  "选择模式：",
		MsgSelectFilesToSync:    "选择要同步的文件（使用 ↑↓ 导航，空格选择，回车确认）",
		MsgControls:             "控制：↑↓ 导航 | 空格 切换 | 回车 确认 | Esc 取消 | a 全选 | n 全不选 | q 退出",
		MsgKeyboardMode:         "[k] - 键盘模式（↑↓ 导航，空格选择，回车确认）",
		MsgNumberMode:           "[n] - 数字模式（1,2,3 或 1-3 或 1,3,5）",
		MsgSelectAllFiles:       "[a] - 选择所有文件",
		MsgFilterByKeyword:      "[f] - 按关键词过滤",
		MsgCancel:               "[c] - 取消（不选择任何文件）",
		MsgSelectedFiles:        "已选择：%d/%d 个文件",
	}
}

// ParseLanguage parses a language string and returns the Language type
func ParseLanguage(lang string) Language {
	lang = strings.ToLower(strings.TrimSpace(lang))
	switch lang {
	case "zh", "zh-cn", "chinese", "中文":
		return Chinese
	case "en", "en-us", "english", "英文":
		return English
	default:
		return English // Default to English
	}
}
