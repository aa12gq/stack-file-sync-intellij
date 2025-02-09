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

- Built with Kotlin 1.9.24
- Using Gradle IntelliJ Plugin 1.17.2
- Compatible with IntelliJ IDEA 2023.3 and above
