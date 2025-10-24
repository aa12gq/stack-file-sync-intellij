# Homebrew Installation Guide

This guide explains how to install `stack-sync` using Homebrew on macOS.

## Installation

### Method 1: Direct Installation (Recommended)

```bash
# Install directly from our tap
brew install aa12gq/stack-sync-cli/stack-sync
```

### Method 2: Using Our Custom Tap

If you prefer to add the tap first:

```bash
# Add our custom tap
brew tap aa12gq/stack-sync-cli

# Install stack-sync
brew install stack-sync
```

## Verification

After installation, verify that `stack-sync` is working correctly:

```bash
# Check version
stack-sync version

# Initialize configuration
stack-sync init

# List available commands
stack-sync --help
```

## Uninstallation

To remove `stack-sync`:

```bash
# Uninstall the formula
brew uninstall stack-sync

# Remove the tap (optional)
brew untap aa12gq/stack-sync
```

## Troubleshooting

### Xcode Version Issues

If you encounter Xcode version errors during installation, the formula uses pre-compiled binaries, so this shouldn't be an issue. However, if you still see errors:

1. Update Xcode from the App Store
2. Update Command Line Tools:
   ```bash
   sudo rm -rf /Library/Developer/CommandLineTools
   sudo xcode-select --install
   ```

### Homebrew Issues

If you have issues with the tap:

```bash
# Update Homebrew
brew update

# Clean up any cached files
brew cleanup

# Re-tap and install
brew untap aa12gq/stack-sync
brew tap aa12gq/stack-sync
brew install stack-sync
```

## What's Included

The Homebrew formula installs:

- `stack-sync` binary in `/opt/homebrew/bin/` (Apple Silicon) or `/usr/local/bin/` (Intel)
- Pre-compiled binary for macOS (supports both Intel and Apple Silicon)
- No additional dependencies required

## Support

For issues with the Homebrew installation:

1. Check the [main repository issues](https://github.com/aa12gq/stack-file-sync-intellij/issues)
2. Verify your Homebrew installation: `brew doctor`
3. Check formula status: `brew info aa12gq/stack-sync/stack-sync`

## Contributing

To contribute to the Homebrew formula:

1. Fork the [homebrew-stack-sync](https://github.com/aa12gq/homebrew-stack-sync) repository
2. Make your changes to the formula
3. Test the installation locally
4. Submit a pull request

---

For more information about `stack-sync`, visit the [main documentation](./stack-sync-cli/README.md).
