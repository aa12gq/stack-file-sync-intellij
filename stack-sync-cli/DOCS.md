# 📚 文档索引

## 语言 / Languages

- 🇺🇸 [English Documentation](#english-documentation)
- 🇨🇳 [中文文档](#中文文档)

---

## English Documentation

### Getting Started
- **[README.md](README.md)** - Complete documentation with features, installation, usage, and examples
- **[QUICKSTART.md](QUICKSTART.md)** - Quick start guide for beginners
- **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - Technical summary and architecture overview

### Configuration
- **[.stack-sync.example.yml](.stack-sync.example.yml)** - Example configuration file with detailed comments

### Development
- **[LICENSE](LICENSE)** - MIT License
- **[Makefile](Makefile)** - Build automation commands

---

## 中文文档

### 入门指南
- **[README_CN.md](README_CN.md)** - 完整文档，包含功能、安装、使用和示例
- **[QUICKSTART_CN.md](QUICKSTART_CN.md)** - 初学者快速入门指南
- **[PROJECT_SUMMARY.md](PROJECT_SUMMARY.md)** - 技术总结和架构概览（英文）

### 配置
- **[.stack-sync.example.yml](.stack-sync.example.yml)** - 配置文件示例，带详细注释

### 开发
- **[LICENSE](LICENSE)** - MIT 许可证
- **[Makefile](Makefile)** - 构建自动化命令

---

## 文档导航 / Navigation

### 基础使用 / Basic Usage

| 文档 | 说明 | Language |
|------|------|----------|
| [README.md](README.md) | Main documentation | English |
| [README_CN.md](README_CN.md) | 主要文档 | 中文 |
| [QUICKSTART.md](QUICKSTART.md) | Quick start guide | English |
| [QUICKSTART_CN.md](QUICKSTART_CN.md) | 快速开始指南 | 中文 |

### 技术文档 / Technical Docs

| 文档 | 说明 | Language |
|------|------|----------|
| [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) | Project architecture & summary | English |
| [.stack-sync.example.yml](.stack-sync.example.yml) | Configuration example | English |

### 开发文档 / Development

| 文档 | 说明 |
|------|------|
| [Makefile](Makefile) | Build commands |
| [scripts/install.sh](scripts/install.sh) | Installation script |
| [scripts/build.sh](scripts/build.sh) | Build script |

---

## 快速链接 / Quick Links

### 安装 / Installation
```bash
# From source
make build && sudo make install

# Using Go
go install github.com/stackfilesync/stack-sync-cli/cmd/stack-sync@latest
```

### 快速开始 / Quick Start
```bash
# Initialize
stack-sync init

# Add repository
stack-sync add

# Start syncing
stack-sync
```

### 获取帮助 / Get Help
```bash
stack-sync help
```

---

## 项目结构 / Project Structure

```
stack-sync-cli/
├── 📄 README.md                    # English documentation
├── 📄 README_CN.md                 # 中文文档
├── 📄 QUICKSTART.md                # English quick start
├── 📄 QUICKSTART_CN.md             # 中文快速开始
├── 📄 PROJECT_SUMMARY.md           # Technical summary
├── 📄 DOCS.md                      # This file (文档索引)
├── 📄 .stack-sync.example.yml      # Configuration example
├── 📄 LICENSE                      # MIT License
├── 📄 Makefile                     # Build automation
│
├── 📁 cmd/stack-sync/              # CLI entry point
│   └── main.go
│
├── 📁 internal/
│   ├── config/                     # Configuration management
│   ├── git/                        # Git operations
│   ├── sync/                       # Sync manager & watcher
│   └── ui/                         # Terminal UI
│
├── 📁 pkg/models/                  # Data models
│
└── 📁 scripts/                     # Installation scripts
    ├── install.sh
    └── build.sh
```

---

## 贡献 / Contributing

欢迎贡献！我们接受中英文的 Issue 和 Pull Request。

Contributions are welcome! We accept issues and pull requests in both English and Chinese.

---

**Made with ❤️ by Stack File Sync Team**
