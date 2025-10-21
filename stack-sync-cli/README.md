# Stack Sync CLI

[English](README.md) | [简体中文](README_CN.md)

🚀 A powerful command-line tool for synchronizing Git repositories across development teams, inspired by your IntelliJ plugin.

## Features

- 🔄 **Interactive Repository Selection** - Beautiful terminal UI
- 📦 **Multi-Repository Management** - Manage multiple repositories from one config
- 👀 **File Watching** - Optional auto-sync mode with file change detection
- ⚡ **Fast & Lightweight** - Single binary, no runtime dependencies
- 🎨 **Colorful Output** - Clear status indicators and progress feedback
- 🔒 **SSH & HTTPS Support** - Works with both authentication methods
- 💾 **Auto Backup** - Optional backup before sync operations

## Installation

### 🚀 Quick Install (Recommended)

```bash
curl -fsSL https://raw.githubusercontent.com/stackfilesync/stack-sync-cli/main/scripts/install.sh | bash
```

### 📦 Package Managers

#### macOS

```bash
# Homebrew (if you have a custom tap)
brew tap YOUR_USERNAME/stack-sync
brew install stack-sync

# Or if submitted to Homebrew Core
brew install stack-sync
```

#### Windows

```bash
# Scoop
scoop bucket add YOUR_BUCKET_NAME
scoop install stack-sync

# Chocolatey
choco install stack-sync
```

#### Linux

```bash
# Snap
sudo snap install stack-sync

# Arch Linux (AUR)
yay -S stack-sync-cli
# or
paru -S stack-sync-cli
```

### 🔧 Manual Installation

#### Using Go Install

```bash
go install github.com/stackfilesync/stack-sync-cli/cmd/stack-sync@latest
```

#### From Source

```bash
# Clone the repository
git clone https://github.com/stackfilesync/stack-sync-cli.git
cd stack-sync-cli

# Build the binary
go build -o stack-sync ./cmd/stack-sync

# Move to PATH (optional)
sudo mv stack-sync /usr/local/bin/
```

#### Download Binaries

Visit [GitHub Releases](https://github.com/stackfilesync/stack-sync-cli/releases) to download pre-built binaries for your platform.

## Quick Start

1. **Initialize Configuration**

```bash
stack-sync init
```

This creates a config file at `~/.stack-sync/config.yml`

2. **Add a Repository**

```bash
stack-sync add
```

Follow the interactive prompts to add your first repository.

3. **Sync Repositories**

```bash
# Interactive mode - select from a list
stack-sync

# Sync specific repository
stack-sync sync my-repo

# Sync all repositories
stack-sync sync
```

## Usage

### Interactive Mode (Default)

Simply run `stack-sync` to see an interactive list:

```
➜ stack-sync
Use the arrow keys to navigate: ↓ ↑ → ← and / toggles search
? Select a repository to sync:
  ✅ my-backend (git@github.com:user/backend.git) [Up to date]
  🔧 frontend-app (git@github.com:user/frontend.git) [Modified]
  📦 mobile-app (git@github.com:user/mobile.git) [Not cloned]
↓ 🔄 config-repo (git@github.com:user/config.git) [Syncing...]

--------- Repository Details ----------
Name:         my-backend
URL:          git@github.com:user/backend.git
Local Path:   /Users/aa12/projects/backend
Status:       Up to date
Watch Mode:   Enabled
Last Sync:    2025-10-21 08:30:15
Files:        125 tracked, 0 modified
```

### Commands

```bash
# Show interactive repository selector
stack-sync

# Initialize configuration file
stack-sync init

# Add a new repository (interactive)
stack-sync add

# List all repositories
stack-sync list

# Sync specific repository
stack-sync sync my-repo

# Sync all repositories
stack-sync sync

# Show repository status
stack-sync status my-repo

# Remove repository from config
stack-sync remove my-repo

# Start file watcher (auto-sync on file changes)
stack-sync watch

# Show help
stack-sync help

# Show version
stack-sync version
```

## Configuration

Config file location: `~/.stack-sync/config.yml`

### Example Configuration

```yaml
server:
  url: "wss://sync-server.example.com"
  api_key: "your-api-key"

settings:
  # Watch mode disabled by default
  watch_mode: false
  auto_sync_interval: 30
  backup_enabled: true
  backup_dir: "~/.stack-sync/backups"
  show_icons: true
  color_output: true

repositories:
  - name: "my-backend"
    url: "git@github.com:user/backend.git"
    local_path: "/Users/aa12/projects/backend"

    # Enable watch mode for this specific repository
    watch_mode: true

    sync_patterns:
      - "src/**/*.go"
      - "pkg/**/*.go"
      - "*.md"
    exclude:
      - "*.log"
      - "vendor/"
      - "node_modules/"

  - name: "frontend-app"
    url: "https://github.com/user/frontend.git"
    local_path: "/Users/aa12/projects/frontend"
    watch_mode: false # No auto-sync for this repo

    sync_patterns:
      - "src/**/*.ts"
      - "src/**/*.tsx"
    exclude:
      - "dist/"
      - "build/"
```

## Watch Mode

Watch mode monitors file changes and automatically syncs repositories.

### Global Watch Mode

Enable in config:

```yaml
settings:
  watch_mode: true # Enable for all repositories
```

### Per-Repository Watch Mode

```yaml
repositories:
  - name: "my-repo"
    watch_mode: true # Enable only for this repo
```

### Start Watcher

```bash
stack-sync watch
```

The watcher will:

- Monitor all repositories with `watch_mode: true`
- Debounce file changes (2 second delay)
- Auto-sync when files matching patterns change
- Ignore temporary files and .git directory

## Status Icons

- ✅ **Up to date** - Repository is synced
- 🔄 **Syncing** - Sync in progress
- 🔧 **Modified** - Local changes detected
- ⚠️ **Conflict** - Merge conflicts detected
- 📦 **Not cloned** - Repository not cloned yet
- ❌ **Error** - Sync error occurred

## Examples

### Daily Workflow

```bash
# Morning: sync all repos
stack-sync sync

# Add new project
stack-sync add
# Name: new-api
# URL: git@github.com:company/new-api.git
# Path: /Users/me/projects/new-api
# Watch mode: yes

# Check status
stack-sync list

# Work on files...

# Enable auto-sync for active projects
stack-sync watch

# End of day: check what changed
stack-sync status
```

### Team Collaboration

1. Share config file with team:

```bash
# Export your config
cp ~/.stack-sync/config.yml ./team-repos.yml

# Team members import it
cp team-repos.yml ~/.stack-sync/config.yml
```

2. Everyone syncs the same repositories:

```bash
stack-sync sync  # Clone/update all team repos
```

## Troubleshooting

### SSH Authentication Issues

Ensure your SSH key is added:

```bash
ssh-add ~/.ssh/id_rsa
```

### Permission Denied

Check repository permissions and SSH keys:

```bash
ssh -T git@github.com
```

### Watch Mode Not Working

Verify watch mode is enabled:

```bash
stack-sync list  # Check for ● indicator
```

## Development

### Build from Source

```bash
git clone https://github.com/stackfilesync/stack-sync-cli.git
cd stack-sync-cli

# Install dependencies
go mod download

# Build
go build -o stack-sync ./cmd/stack-sync

# Run tests
go test ./...
```

### Project Structure

```
stack-sync-cli/
├── cmd/stack-sync/        # CLI entry point
├── internal/
│   ├── config/           # Configuration management
│   ├── git/              # Git operations
│   ├── sync/             # Sync manager & watcher
│   └── ui/               # Terminal UI
├── pkg/models/           # Data models
└── scripts/              # Installation scripts
```

## Roadmap

- [ ] WebSocket server integration
- [ ] Cross-machine sync notifications
- [ ] Conflict resolution UI
- [ ] Sync history and rollback
- [ ] Plugin system
- [ ] Cloud backup integration
- [ ] Team collaboration features

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

MIT License - see LICENSE file for details

## Related Projects

- [Stack File Sync IntelliJ Plugin](https://github.com/stackfilesync/stack-file-sync-intellij) - The original IntelliJ IDEA plugin
- [Stack File Sync VSCode](https://github.com/stackfilesync/stack-file-sync-vscode) - VSCode extension

## Support

- 📖 [Documentation](https://docs.stackfilesync.com)
- 🐛 [Issue Tracker](https://github.com/stackfilesync/stack-sync-cli/issues)
- 💬 [Discussions](https://github.com/stackfilesync/stack-sync-cli/discussions)

---

Made with ❤️ by the Stack File Sync team
