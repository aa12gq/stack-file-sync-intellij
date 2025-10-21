# Stack Sync CLI

[English](README.md) | [简体中文](README_CN.md)

🚀 一个强大的命令行工具，用于在开发团队之间同步 Git 仓库，灵感来自你的 IntelliJ 插件。

## 特性

- 🔄 **交互式仓库选择** - 美观终端界面
- 📦 **多仓库管理** - 从一个配置文件管理多个仓库
- 👀 **文件监控** - 可选的自动同步模式，支持文件变化检测
- ⚡ **快速轻量** - 单一二进制文件，无运行时依赖
- 🎨 **彩色输出** - 清晰的状态指示器和进度反馈
- 🔒 **SSH & HTTPS 支持** - 支持两种认证方式
- 💾 **自动备份** - 同步前可选备份

## 安装

### 从源码构建

```bash
# 克隆仓库
git clone https://github.com/stackfilesync/stack-sync-cli.git
cd stack-sync-cli

# 构建二进制文件
go build -o stack-sync ./cmd/stack-sync

# 移动到 PATH（可选）
sudo mv stack-sync /usr/local/bin/
```

### 使用安装脚本

```bash
curl -fsSL https://raw.githubusercontent.com/stackfilesync/stack-sync-cli/main/scripts/install.sh | bash
```

### 使用 Go Install

```bash
go install github.com/stackfilesync/stack-sync-cli/cmd/stack-sync@latest
```

## 快速开始

1. **初始化配置**

```bash
stack-sync init
```

这会在 `~/.stack-sync/config.yml` 创建配置文件

2. **添加仓库**

```bash
stack-sync add
```

按照交互式提示添加你的第一个仓库。

3. **同步仓库**

```bash
# 交互模式 - 从列表中选择
stack-sync

# 同步指定仓库
stack-sync sync my-repo

# 同步所有仓库
stack-sync sync
```

## 使用方法

### 交互模式（默认）

直接运行 `stack-sync` 查看交互式列表：

```
➜ stack-sync
使用方向键导航: ↓ ↑ → ← 和 / 切换搜索
? 选择要同步的仓库:
  ✅ my-backend (git@github.com:user/backend.git) [已是最新]
  🔧 frontend-app (git@github.com:user/frontend.git) [已修改]
  📦 mobile-app (git@github.com:user/mobile.git) [未克隆]
↓ 🔄 config-repo (git@github.com:user/config.git) [同步中...]

--------- 仓库详情 ----------
名称:         my-backend
URL:          git@github.com:user/backend.git
本地路径:     /Users/aa12/projects/backend
状态:         已是最新
监控模式:     已启用
上次同步:     2025-10-21 08:30:15
文件:         125 个已跟踪, 0 个已修改
```

### 命令

```bash
# 显示交互式仓库选择器
stack-sync

# 初始化配置文件
stack-sync init

# 添加新仓库（交互式）
stack-sync add

# 列出所有仓库
stack-sync list

# 同步指定仓库
stack-sync sync my-repo

# 同步所有仓库
stack-sync sync

# 显示仓库状态
stack-sync status my-repo

# 从配置中移除仓库
stack-sync remove my-repo

# 启动文件监控器（文件变化时自动同步）
stack-sync watch

# 显示帮助
stack-sync help

# 显示版本
stack-sync version
```

## 配置

配置文件位置：`~/.stack-sync/config.yml`

### 配置示例

```yaml
server:
  url: "wss://sync-server.example.com"
  api_key: "your-api-key"

settings:
  # 监控模式默认关闭
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

    # 为这个特定仓库启用监控模式
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
    watch_mode: false  # 此仓库不自动同步

    sync_patterns:
      - "src/**/*.ts"
      - "src/**/*.tsx"
    exclude:
      - "dist/"
      - "build/"
```

## 监控模式

监控模式会监视文件变化并自动同步仓库。

### 全局监控模式

在配置中启用：

```yaml
settings:
  watch_mode: true  # 为所有仓库启用
```

### 单仓库监控模式

```yaml
repositories:
  - name: "my-repo"
    watch_mode: true  # 仅为此仓库启用
```

### 启动监控器

```bash
stack-sync watch
```

监控器将会：
- 监视所有 `watch_mode: true` 的仓库
- 防抖动文件变化（2 秒延迟）
- 当匹配模式的文件变化时自动同步
- 忽略临时文件和 .git 目录

## 状态图标

- ✅ **已是最新** - 仓库已同步
- 🔄 **同步中** - 同步进行中
- 🔧 **已修改** - 检测到本地更改
- ⚠️ **冲突** - 检测到合并冲突
- 📦 **未克隆** - 仓库尚未克隆
- ❌ **错误** - 发生同步错误

## 使用示例

### 日常工作流

```bash
# 早上：同步所有仓库
stack-sync sync

# 添加新项目
stack-sync add
# 名称: new-api
# URL: git@github.com:company/new-api.git
# 路径: /Users/me/projects/new-api
# 监控模式: yes

# 查看状态
stack-sync list

# 处理文件...

# 为活跃项目启用自动同步
stack-sync watch

# 下班：检查变化
stack-sync status
```

### 团队协作

1. 与团队分享配置文件：

```bash
# 导出你的配置
cp ~/.stack-sync/config.yml ./team-repos.yml

# 团队成员导入
cp team-repos.yml ~/.stack-sync/config.yml
```

2. 所有人同步相同的仓库：

```bash
stack-sync sync  # 克隆/更新所有团队仓库
```

## 故障排除

### SSH 认证问题

确保已添加 SSH 密钥：

```bash
ssh-add ~/.ssh/id_rsa
```

### 权限被拒绝

检查仓库权限和 SSH 密钥：

```bash
ssh -T git@github.com
```

### 监控模式不工作

验证监控模式已启用：

```bash
stack-sync list  # 查找 ● 指示器
```

## 开发

### 从源码构建

```bash
git clone https://github.com/stackfilesync/stack-sync-cli.git
cd stack-sync-cli

# 安装依赖
go mod download

# 构建
go build -o stack-sync ./cmd/stack-sync

# 运行测试
go test ./...
```

### 项目结构

```
stack-sync-cli/
├── cmd/stack-sync/        # CLI 入口
├── internal/
│   ├── config/           # 配置管理
│   ├── git/              # Git 操作
│   ├── sync/             # 同步管理器和监控器
│   └── ui/               # 终端 UI
├── pkg/models/           # 数据模型
└── scripts/              # 安装脚本
```

## 路线图

- [ ] WebSocket 服务器集成
- [ ] 跨机器同步通知
- [ ] 冲突解决 UI
- [ ] 同步历史和回滚
- [ ] 插件系统
- [ ] 云备份集成
- [ ] 团队协作功能

## 贡献

欢迎贡献！请：

1. Fork 仓库
2. 创建功能分支
3. 进行更改
4. 添加测试
5. 提交 Pull Request

## 许可证

MIT 许可证 - 详见 LICENSE 文件

## 相关项目

- [Stack File Sync IntelliJ 插件](https://github.com/stackfilesync/stack-file-sync-intellij) - 原始 IntelliJ IDEA 插件
- [Stack File Sync VSCode](https://github.com/stackfilesync/stack-file-sync-vscode) - VSCode 扩展

## 支持

- 📖 [文档](https://docs.stackfilesync.com)
- 🐛 [问题跟踪](https://github.com/stackfilesync/stack-sync-cli/issues)
- 💬 [讨论区](https://github.com/stackfilesync/stack-sync-cli/discussions)

---

由 Stack File Sync 团队用 ❤️ 制作
