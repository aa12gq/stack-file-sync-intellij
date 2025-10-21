# 快速开始指南

[English](QUICKSTART.md) | [简体中文](QUICKSTART_CN.md)

## 安装

### 方式 1：从源码构建

```bash
cd stack-sync-cli
make build
sudo make install
```

### 方式 2：使用 Go Install

```bash
go install github.com/stackfilesync/stack-sync-cli/cmd/stack-sync@latest
```

## 首次设置

### 1. 初始化配置

```bash
stack-sync init
```

这会创建 `~/.stack-sync/config.yml`

### 2. 添加你的第一个仓库

```bash
stack-sync add
```

你会被提示输入：
- **仓库名称**：例如 `my-backend`
- **仓库 URL**：例如 `git@github.com:user/backend.git`
- **本地路径**：例如 `/Users/me/projects/backend`
- **监控模式**：`y` 或 `n`

### 3. 开始同步

#### 交互模式（推荐）

```bash
stack-sync
```

使用方向键选择一个仓库，然后按回车开始同步。

#### 同步指定仓库

```bash
stack-sync sync my-backend
```

#### 同步所有仓库

```bash
stack-sync sync
```

## 常见工作流

### 早上：同步所有项目

```bash
# 从所有仓库拉取最新更改
stack-sync sync
```

### 添加新项目

```bash
stack-sync add
# 按照提示操作
```

### 检查仓库状态

```bash
# 列出所有仓库及状态
stack-sync list

# 查看单个仓库的详细状态
stack-sync status my-backend
```

### 为活跃项目启用自动同步

编辑 `~/.stack-sync/config.yml`：

```yaml
repositories:
  - name: "my-backend"
    watch_mode: true  # 启用自动同步
```

然后启动监控器：

```bash
stack-sync watch
```

现在 `my-backend` 中的任何文件更改都会触发自动同步！

### 移除仓库

```bash
stack-sync remove my-backend
```

注意：这只会从配置中移除，本地文件保留。

## 配置技巧

### 配置结构示例

```yaml
settings:
  watch_mode: false        # 全局默认值
  backup_enabled: true
  backup_dir: "~/.stack-sync/backups"

repositories:
  # 活跃开发 - 启用自动同步
  - name: "current-project"
    url: "git@github.com:me/project.git"
    local_path: "/Users/me/projects/current"
    watch_mode: true       # 覆盖全局设置
    sync_patterns:
      - "src/**/*.ts"
      - "package.json"
    exclude:
      - "node_modules/"
      - "dist/"

  # 参考项目 - 仅手动同步
  - name: "reference-lib"
    url: "https://github.com/team/lib.git"
    local_path: "/Users/me/projects/lib"
    watch_mode: false      # 不自动同步
```

### 同步模式

仅包含特定文件：

```yaml
sync_patterns:
  - "src/**/*.go"     # src/ 中的所有 Go 文件
  - "pkg/**/*.go"     # pkg/ 中的所有 Go 文件
  - "*.md"            # 所有 markdown 文件
  - "go.mod"          # 特定文件
```

排除不需要的文件：

```yaml
exclude:
  - "vendor/"         # 目录
  - "*.log"           # 文件模式
  - ".DS_Store"       # 特定文件
  - "node_modules/"
```

## 状态图标参考

| 图标 | 状态 | 要做什么 |
|------|------|---------|
| ✅ | 已是最新 | 无需操作 |
| 🔧 | 已修改 | 你有本地更改 |
| 📦 | 未克隆 | 同步时会被克隆 |
| 🔄 | 同步中 | 等待完成 |
| ⚠️ | 冲突 | 手动解决冲突 |
| ❌ | 错误 | 查看错误消息 |

## 键盘快捷键

### 交互模式

- `↑/↓` - 浏览仓库
- `/` - 切换搜索
- `回车` - 选择并同步
- `Ctrl+C` - 取消

## 故障排除

### "仓库未找到"

确保仓库已添加：

```bash
stack-sync list
```

如果不在列表中，添加它：

```bash
stack-sync add
```

### SSH 认证失败

1. 检查你的 SSH 密钥：

```bash
ssh -T git@github.com
```

2. 如果需要，添加 SSH 密钥：

```bash
ssh-add ~/.ssh/id_rsa
```

### 监控模式不工作

1. 验证监控模式已启用：

```bash
stack-sync list
# 查找 ● 指示器
```

2. 检查配置文件：

```bash
cat ~/.stack-sync/config.yml
```

3. 启动监控器：

```bash
stack-sync watch
```

### 配置文件位置

默认：`~/.stack-sync/config.yml`

查看：

```bash
cat ~/.stack-sync/config.yml
```

编辑：

```bash
nano ~/.stack-sync/config.yml
# 或
vim ~/.stack-sync/config.yml
```

## 专业技巧

### 1. 团队协作

与团队分享配置：

```bash
# 导出
cp ~/.stack-sync/config.yml ./team-repos.yml

# 团队成员导入
cp team-repos.yml ~/.stack-sync/config.yml
stack-sync sync  # 克隆所有团队仓库
```

### 2. 大改动前备份

在配置中启用备份：

```yaml
settings:
  backup_enabled: true
  backup_dir: "~/.stack-sync/backups"
```

### 3. 选择性同步

对于大型仓库只同步特定文件类型：

```yaml
sync_patterns:
  - "**/*.md"      # 仅文档
  - "**/*.go"      # 仅 Go 代码
```

### 4. 与 Git Hooks 集成

添加到 `.git/hooks/post-commit`：

```bash
#!/bin/bash
stack-sync sync my-repo
```

### 5. Cron 定时任务自动同步

添加到 crontab：

```bash
# 每小时同步
0 * * * * /usr/local/bin/stack-sync sync

# 每天早上 9 点同步
0 9 * * * /usr/local/bin/stack-sync sync
```

## 下一步

- 阅读完整的 [README_CN.md](README_CN.md)
- 查看[配置示例](.stack-sync.example.yml)
- 查看 [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) 了解架构详情

## 获取帮助

```bash
stack-sync help
```

开心同步！🚀
