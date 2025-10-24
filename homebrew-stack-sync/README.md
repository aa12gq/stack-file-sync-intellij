# Homebrew Stack Sync CLI

This is a Homebrew tap for [Stack Sync CLI](https://github.com/aa12gq/stack-file-sync-intellij).

## Installation

```bash
brew install aa12gq/stack-sync-cli/stack-sync
```

## What is Stack Sync?

Stack Sync is an interactive file synchronization tool for development teams. It helps you sync files from remote repositories to your local development environment with an intuitive terminal interface.

### Features

- 🎯 Interactive terminal UI with file selection
- 📁 Multi-repository management support
- 👀 File watching and auto-sync capabilities
- 🖥️ Cross-platform support (macOS, Linux, Windows)
- 🔧 Post-sync command execution
- 🔐 Support for SSH and HTTPS authentication

### Quick Start

1. Initialize configuration:

   ```bash
   stack-sync init
   ```

2. Add a repository:

   ```bash
   stack-sync add
   ```

3. Sync files:
   ```bash
   stack-sync sync <repository-name>
   ```

For more information, visit the [main repository](https://github.com/aa12gq/stack-file-sync-intellij).
