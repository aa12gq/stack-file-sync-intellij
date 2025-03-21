# Changelog

## [Unreleased]

### Features

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
