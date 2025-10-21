# Quick Start Guide

[English](QUICKSTART.md) | [简体中文](QUICKSTART_CN.md)

## Installation

### Option 1: Build from Source

```bash
cd stack-sync-cli
make build
sudo make install
```

### Option 2: Use Go Install

```bash
go install github.com/stackfilesync/stack-sync-cli/cmd/stack-sync@latest
```

## First Time Setup

### 1. Initialize Configuration

```bash
stack-sync init
```

This creates `~/.stack-sync/config.yml`

### 2. Add Your First Repository

```bash
stack-sync add
```

You'll be prompted for:
- **Repository name**: e.g., `my-backend`
- **Repository URL**: e.g., `git@github.com:user/backend.git`
- **Local path**: e.g., `/Users/me/projects/backend`
- **Watch mode**: `y` or `n`

### 3. Start Syncing

#### Interactive Mode (Recommended)

```bash
stack-sync
```

Use arrow keys to select a repository, then press Enter to sync.

#### Sync Specific Repository

```bash
stack-sync sync my-backend
```

#### Sync All Repositories

```bash
stack-sync sync
```

## Common Workflows

### Morning: Sync All Projects

```bash
# Pull latest changes from all repos
stack-sync sync
```

### Add a New Project

```bash
stack-sync add
# Follow the prompts
```

### Check Repository Status

```bash
# List all repositories with status
stack-sync list

# Detailed status of one repo
stack-sync status my-backend
```

### Enable Auto-Sync for Active Project

Edit `~/.stack-sync/config.yml`:

```yaml
repositories:
  - name: "my-backend"
    watch_mode: true  # Enable auto-sync
```

Then start the watcher:

```bash
stack-sync watch
```

Now any file changes in `my-backend` will trigger auto-sync!

### Remove a Repository

```bash
stack-sync remove my-backend
```

Note: This only removes from config, local files remain.

## Configuration Tips

### Example Config Structure

```yaml
settings:
  watch_mode: false        # Global default
  backup_enabled: true
  backup_dir: "~/.stack-sync/backups"

repositories:
  # Active development - auto-sync enabled
  - name: "current-project"
    url: "git@github.com:me/project.git"
    local_path: "/Users/me/projects/current"
    watch_mode: true       # Override global setting
    sync_patterns:
      - "src/**/*.ts"
      - "package.json"
    exclude:
      - "node_modules/"
      - "dist/"

  # Reference project - manual sync only
  - name: "reference-lib"
    url: "https://github.com/team/lib.git"
    local_path: "/Users/me/projects/lib"
    watch_mode: false      # No auto-sync
```

### Sync Patterns

Include only specific files:

```yaml
sync_patterns:
  - "src/**/*.go"     # All Go files in src/
  - "pkg/**/*.go"     # All Go files in pkg/
  - "*.md"            # All markdown files
  - "go.mod"          # Specific file
```

Exclude unwanted files:

```yaml
exclude:
  - "vendor/"         # Directories
  - "*.log"           # File patterns
  - ".DS_Store"       # Specific files
  - "node_modules/"
```

## Status Icons Reference

| Icon | Status | What to Do |
|------|--------|------------|
| ✅ | Up to date | Nothing needed |
| 🔧 | Modified | You have local changes |
| 📦 | Not cloned | Will be cloned on sync |
| 🔄 | Syncing | Wait for completion |
| ⚠️ | Conflict | Resolve conflicts manually |
| ❌ | Error | Check error message |

## Keyboard Shortcuts

### Interactive Mode

- `↑/↓` - Navigate repositories
- `/` - Toggle search
- `Enter` - Select and sync
- `Ctrl+C` - Cancel

## Troubleshooting

### "Repository not found"

Make sure the repository is added:

```bash
stack-sync list
```

If not there, add it:

```bash
stack-sync add
```

### SSH Authentication Failed

1. Check your SSH key:

```bash
ssh -T git@github.com
```

2. Add your SSH key if needed:

```bash
ssh-add ~/.ssh/id_rsa
```

### Watch Mode Not Working

1. Verify watch mode is enabled:

```bash
stack-sync list
# Look for ● indicator
```

2. Check config file:

```bash
cat ~/.stack-sync/config.yml
```

3. Start watcher:

```bash
stack-sync watch
```

### Config File Location

Default: `~/.stack-sync/config.yml`

To view:

```bash
cat ~/.stack-sync/config.yml
```

To edit:

```bash
nano ~/.stack-sync/config.yml
# or
vim ~/.stack-sync/config.yml
```

## Pro Tips

### 1. Team Collaboration

Share your config with the team:

```bash
# Export
cp ~/.stack-sync/config.yml ./team-repos.yml

# Team members import
cp team-repos.yml ~/.stack-sync/config.yml
stack-sync sync  # Clone all team repos
```

### 2. Backup Before Big Changes

Enable backups in config:

```yaml
settings:
  backup_enabled: true
  backup_dir: "~/.stack-sync/backups"
```

### 3. Selective Sync

Only sync specific file types for large repos:

```yaml
sync_patterns:
  - "**/*.md"      # Only documentation
  - "**/*.go"      # Only Go code
```

### 4. Integration with Git Hooks

Add to `.git/hooks/post-commit`:

```bash
#!/bin/bash
stack-sync sync my-repo
```

### 5. Cron Job Auto-Sync

Add to crontab:

```bash
# Sync every hour
0 * * * * /usr/local/bin/stack-sync sync

# Sync every morning at 9 AM
0 9 * * * /usr/local/bin/stack-sync sync
```

## Next Steps

- Read the full [README.md](README.md)
- Check the [example config](.stack-sync.example.yml)
- Review [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) for architecture details

## Get Help

```bash
stack-sync help
```

Happy syncing! 🚀
