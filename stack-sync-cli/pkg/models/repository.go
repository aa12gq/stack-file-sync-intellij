package models

import "time"

// SyncStatus represents the current status of a repository
type SyncStatus string

const (
	StatusSyncing    SyncStatus = "syncing"
	StatusUpToDate   SyncStatus = "up-to-date"
	StatusConflict   SyncStatus = "conflict"
	StatusNotCloned  SyncStatus = "not-cloned"
	StatusModified   SyncStatus = "modified"
	StatusError      SyncStatus = "error"
)

// Repository represents a Git repository configuration (matches IntelliJ plugin)
type Repository struct {
	Name              string            `yaml:"name"`
	URL               string            `yaml:"url"`
	Branch            string            `yaml:"branch"`
	SourceDirectory   string            `yaml:"source_directory"`    // 远程仓库中的源目录
	TargetDirectory   string            `yaml:"target_directory"`    // 本地项目的目标目录
	LocalPath         string            `yaml:"local_path"`          // 本地仓库路径 (同 TargetDirectory)
	FilePatterns      []string          `yaml:"file_patterns"`
	ExcludePatterns   []string          `yaml:"exclude_patterns"`
	SyncPatterns      []string          `yaml:"sync_patterns"`       // 同步文件模式 (同 FilePatterns)
	Exclude           []string          `yaml:"exclude"`             // 排除模式 (同 ExcludePatterns)
	WatchMode         bool              `yaml:"watch_mode"`          // 是否启用文件监控
	AutoSync          *AutoSyncConfig   `yaml:"auto_sync,omitempty"`
	BackupConfig      *BackupConfig     `yaml:"backup_config,omitempty"`
	PostSyncCommands  []PostSyncCommand `yaml:"post_sync_commands,omitempty"`
	RepoType          string            `yaml:"repo_type"`           // SSH or HTTPS
	Username          string            `yaml:"username,omitempty"`
	Password          string            `yaml:"password,omitempty"`

	// Runtime status (not saved to config)
	Status       SyncStatus  `yaml:"-"`
	LastSync     *time.Time  `yaml:"-"`
	FilesTracked int         `yaml:"-"`
	FilesModified int        `yaml:"-"`
}

// AutoSyncConfig represents auto-sync settings
type AutoSyncConfig struct {
	Enabled  bool `yaml:"enabled"`
	Interval int  `yaml:"interval"` // seconds
}

// BackupConfig represents backup settings
type BackupConfig struct {
	Enabled    bool `yaml:"enabled"`
	MaxBackups int  `yaml:"max_backups"`
}

// PostSyncCommand represents a command to run after sync
type PostSyncCommand struct {
	Directory string `yaml:"directory"` // 在哪个目录下运行
	Command   string `yaml:"command"`   // 运行什么命令
	Order     int    `yaml:"order"`     // 执行顺序
}

// GetIcon returns the emoji icon based on repository status
func (r *Repository) GetIcon() string {
	switch r.Status {
	case StatusSyncing:
		return "🔄"
	case StatusUpToDate:
		return "✅"
	case StatusConflict:
		return "⚠️"
	case StatusNotCloned:
		return "📦"
	case StatusModified:
		return "🔧"
	case StatusError:
		return "❌"
	default:
		return "📁"
	}
}

// GetStatusText returns human-readable status text
func (r *Repository) GetStatusText() string {
	switch r.Status {
	case StatusSyncing:
		return "Syncing..."
	case StatusUpToDate:
		return "Up to date"
	case StatusConflict:
		return "Conflicts detected"
	case StatusNotCloned:
		return "Not cloned"
	case StatusModified:
		return "Modified"
	case StatusError:
		return "Error"
	default:
		return "Unknown"
	}
}

// GetDisplayName returns formatted name for display
func (r *Repository) GetDisplayName() string {
	return r.GetIcon() + " " + r.Name + " (" + r.URL + " @ " + r.Branch + ")"
}
