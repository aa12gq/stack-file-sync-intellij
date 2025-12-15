package models

import "time"

// FileChangeType represents the type of change for a file
type FileChangeType string

const (
	ChangeTypeAdded   FileChangeType = "added"   // 新增文件
	ChangeTypeModified FileChangeType = "modified" // 修改文件
	ChangeTypeDeleted  FileChangeType = "deleted"  // 删除文件
)

// FileChange represents a single file change
type FileChange struct {
	Path      string        `json:"path"`       // 文件路径（相对路径）
	ChangeType FileChangeType `json:"change_type"` // 变更类型
	Size      int64         `json:"size"`      // 文件大小（字节）
}

// SyncHistory represents a single sync operation history
type SyncHistory struct {
	ID          string       `json:"id"`           // 唯一ID
	Repository  string       `json:"repository"`  // 仓库名称
	Branch      string       `json:"branch"`      // 分支名称
	Timestamp   time.Time    `json:"timestamp"`   // 同步时间
	Success     bool         `json:"success"`     // 是否成功
	Error       string       `json:"error,omitempty"` // 错误信息（如果有）
	FileChanges []FileChange `json:"file_changes"`   // 文件变更列表
	TotalFiles  int          `json:"total_files"`    // 总文件数
	AddedCount  int          `json:"added_count"`    // 新增文件数
	ModifiedCount int        `json:"modified_count"` // 修改文件数
	DeletedCount  int        `json:"deleted_count"`  // 删除文件数
	Duration    int64        `json:"duration"`       // 同步耗时（毫秒）
}

// SyncHistoryStore represents the storage for sync history
type SyncHistoryStore struct {
	Histories []SyncHistory `json:"histories"`
}

