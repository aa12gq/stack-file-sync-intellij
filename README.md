# Stack File Sync - IntelliJ IDEA Plugin

<p align="center">
  <img src="src/main/resources/icons/logo.svg" width="80" height="80" alt="Plugin Logo">
</p>

Stack File Sync is a powerful IntelliJ IDEA plugin designed for synchronizing files and code snippets between multiple projects. It provides a simple and flexible way to manage and sync your code assets.

![Demonstration](./docs/images/demonstration.gif)

## Key Features

- 🔄 Real-time file synchronization
- 🕒 Configurable auto-sync intervals
- 📁 Multi-repository support
- 🔒 Secure file backup
- ⚡ High-performance file transfer
- 🛠 Customizable sync rules
- 📊 Detailed sync logs

## Installation

### From JetBrains Marketplace

1. Open the plugin marketplace in IntelliJ IDEA
2. Search for "Stack File Sync"
3. Click the Install button

### Manual Installation

1. Download the latest plugin release package (.zip)
2. In IntelliJ IDEA, go to `Settings/Preferences -> Plugins -> ⚙️ -> Install Plugin from Disk`
3. Select the downloaded plugin package to install

## Quick Start

1. After installation, open the configuration panel via `Tools -> Stack File Sync -> Configure`
2. Add a new sync repository
3. Configure sync rules and auto-sync options
4. Click the sync button in the toolbar to start syncing

## Configuration

### Repository Settings

- **Name**: Unique identifier for the repository
- **Local Path**: Local folder path to sync
- **Remote Path**: Remote sync target path
- **Sync Rules**: File include/exclude patterns
- **Auto Sync**: Enable/disable auto sync feature

### Auto Sync Settings

- **Enable Status**: Turn auto sync on/off
- **Sync Interval**: Set auto sync time interval (seconds)
- **Sync Mode**: Choose one-way or two-way sync

## Usage Examples

### Manual Sync

1. Click the sync button in the toolbar
2. Select the repository to sync
3. Wait for the sync to complete

### Auto Sync

1. Enable auto sync in repository settings
2. Set the sync interval
3. The plugin will automatically sync according to the set interval

## Performance Optimization

- Uses incremental sync to reduce data transfer
- Multi-threaded parallel processing for better sync speed
- Smart file caching mechanism
- Low memory footprint design

## FAQ

**Q: How to resolve sync conflicts?**
A: The plugin automatically detects conflicts and provides resolution options. You can choose to keep the local version or use the remote version.

**Q: What sync modes are supported?**
A: Currently supports one-way sync mode.

## Contributing

We welcome community contributions! If you'd like to contribute:

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Version History

- v1.0.0 (2024-02-06)
  - Initial release
  - Basic sync functionality
  - Auto sync support

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details

## Contact

- Project Homepage: [GitHub](https://github.com/aa12gq/stack-file-sync-intellij)
- Issue Tracker: [Issue Tracker](https://github.com/aa12gq/stack-file-sync-intellij/issues)
- Email: [aa12gq@gmail.com](mailto:aa12gq@gmail.com)

## Acknowledgments

Thanks to all the developers who have contributed to this project!

---

<p align="center">Made with ❤️ by Stack File Sync Team</p>
