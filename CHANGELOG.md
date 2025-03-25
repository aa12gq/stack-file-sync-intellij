# Changelog

## [Unreleased]

### Features

- 添加模块化同步通知系统：
  - 服务发现模块支持跨用户同步通知
  - 实现智能模块过滤功能，根据模块名自动筛选待同步文件
  - 引入仓库选择交互流程，支持手动选择最匹配的仓库
  - 端到端协作流程优化，后端开发人员更新后可直接通知前端
- Added two sync modes:
  - Full Sync: Synchronize all files without selection dialog
  - Selective Sync: Choose specific files to synchronize
- Improved auto-sync behavior:
  - Auto-sync now uses Full Sync mode by default
  - Manual sync supports both Full Sync and Selective Sync modes
  - Auto-sync can be enabled for each repository individually
  - Configurable sync interval (in seconds) for each repository
  - Auto-sync starts immediately after enabling and runs at specified intervals
  - Auto-sync status is preserved across IDE restarts
  - Visual indicators show auto-sync status in repository list

## [1.3.1] - 2025-03-25

### Added

- 模块化同步通知系统
  - 跨用户同步通知分发功能
  - 消息通知接收与实时弹窗提示
  - 基于模块名称的智能文件过滤
  - 仓库智能匹配与手动选择功能
  - 模块化同步流程自动化
  - 端到端协作流程优化

### Improvements

- 文件过滤机制优化，支持模块级别的精准同步
- 通知 UI 交互体验优化
- 同步日志增强，支持模块过滤详情记录
- 用户间通信协议升级，提供更可靠的消息传递

### Technical

- 重构通知分发系统，支持扩展更多通知类型
- 同步工作流优化，减少人工操作步骤

## [1.2.0] - 2025-03-21

### Added

- Support for SSH and HTTPS repository types
  - HTTPS mode with username and password authentication, eliminating repeated credential entry
  - Automatic saving and usage of HTTPS credentials for repository cloning and synchronization
- Enhanced file selection dialog
  - Search/filter functionality with text matching and wildcards
  - Save recently used filter patterns
  - One-click select/deselect all functionality
  - Display of total and selected file counts
- Improved internal sync state management
  - Disable sync buttons during synchronization to prevent duplicate operations
  - Automatic button state restoration after sync completion
  - More detailed error information when synchronization fails
- Repository file scanning optimizations
  - Automatic recursive scanning of files in subdirectories
  - Improved file matching algorithm supporting more complex patterns
  - Detailed synchronization logs including file search paths and results

### Improvements

- Enhanced error handling and user notifications
- Optimized Git authentication process, supporting environment variables and credential embedding
- More detailed synchronization logs clearly showing repository structure and file scanning results
- UI interaction flow optimization, reducing operational steps

### Fixed

- Fixed issues with sync button sometimes showing incorrect state
- Fixed search functionality occasionally not working in file selection dialog
- Fixed subdirectory files sometimes not being matched properly
- Fixed several HTTPS authentication-related stability issues

## [1.1.0] - 2025-02-20

### Added

- P2P file transfer functionality
  - Support peer-to-peer file transfer between IDE instances
  - Custom file receive directory configuration
  - File transfer progress display
  - Batch file transfer support
  - P2P node management
  - Auto-accept file transfer option

## [1.0.1] - 2025-02-15

### Added

- Import/export configuration feature
- Enhanced log display format

### Fixed

- Auto-sync issues
- UI experience improvements

## [1.0.0] - 2025-02-09

### Features

- Support file synchronization from Git repositories to local directories
- Selective file synchronization with checkbox-based file selection
- File filtering patterns to include or exclude specific files
- Automatic synchronization with configurable schedules
- Custom post-sync command execution
- Automatic file backup before synchronization
- Synchronization history tracking
- Detailed synchronization logging

### Improvements

- Enhanced file selection dialog user experience
- Improved checkbox list interaction
- Optimized progress display during synchronization
- Enhanced error handling and notifications

### Bug Fixes

- Fixed checkbox state toggle issues
- Fixed backup path calculation
- Fixed potential sync history loss

### Technical

- Built with Kotlin 1.8.21
- Using Gradle IntelliJ Plugin 1.13.3
- Compatible with IntelliJ IDEA 2022.3 and above
