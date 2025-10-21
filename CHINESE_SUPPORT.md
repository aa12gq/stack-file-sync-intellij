# Stack Sync CLI 中文支持

Stack Sync CLI 现在完全支持中文界面和命令！

## 功能特性

### 🌏 多语言支持

- **中文界面**：所有提示、消息和帮助信息都支持中文
- **中文命令**：可以使用中文命令进行操作
- **自动切换**：根据配置文件中的语言设置自动切换界面语言

### 📝 中文命令列表

| 中文命令 | 英文命令  | 功能描述             |
| -------- | --------- | -------------------- |
| `初始化` | `init`    | 初始化配置文件       |
| `添加`   | `add`     | 添加新仓库（交互式） |
| `同步`   | `sync`    | 同步仓库或所有仓库   |
| `列表`   | `list`    | 列出所有仓库         |
| `删除`   | `remove`  | 删除仓库             |
| `状态`   | `status`  | 显示仓库状态         |
| `监控`   | `watch`   | 启动文件监控器       |
| `帮助`   | `help`    | 显示帮助信息         |
| `版本`   | `version` | 显示版本信息         |

## 使用方法

### 1. 设置中文界面

在配置文件 `.stack-sync.yml` 中设置：

```yaml
settings:
  language: "zh-CN" # 设置为中文
```

支持的语言代码：

- `zh-CN` 或 `zh` - 中文
- `en-US` 或 `en` - 英文（默认）

### 2. 使用中文命令

设置中文后，可以使用中文命令：

```bash
# 初始化配置
stack-sync 初始化

# 添加仓库
stack-sync 添加

# 同步所有仓库
stack-sync 同步

# 同步指定仓库
stack-sync 同步 my-repo

# 列出仓库
stack-sync 列表

# 查看仓库状态
stack-sync 状态 my-repo

# 删除仓库
stack-sync 删除 my-repo

# 启动监控
stack-sync 监控

# 显示帮助
stack-sync 帮助

# 显示版本
stack-sync 版本
```

### 3. 中文界面示例

当设置为中文时，所有界面都会显示中文：

```
Stack Sync - 开发团队文件同步工具

使用方法:
    stack-sync [命令] [选项]

命令:
    (默认)           显示交互式仓库选择器
    初始化           初始化配置文件
    同步 [仓库] [-f 关键词] 同步仓库或所有仓库
    列表, ls         列出所有仓库
    添加             添加新仓库（交互式）
    删除, rm <仓库>  从配置中删除仓库
    状态 [仓库]      显示仓库状态
    监控             启动文件监控器进行自动同步
    帮助, -h         显示此帮助信息
    版本, -v         显示版本信息
```

## 配置示例

完整的中文配置示例：

```yaml
# Stack Sync CLI 配置文件示例
server:
  url: "wss://sync-server.example.com"
  api_key: "your-api-key"

settings:
  # 语言设置：支持 "en-US" (英文) 或 "zh-CN" (中文)
  language: "zh-CN"

  # 备份设置
  backup_enabled: true
  backup_dir: "~/.stack-sync/backups"

  # 界面设置
  show_icons: true
  color_output: true

repositories:
  - name: "my-backend"
    url: "git@github.com:user/backend.git"
    branch: "main"
    source_directory: "src"
    target_directory: "/Users/aa12/projects/backend/src"
    local_path: "/Users/aa12/projects/backend/src"
    file_patterns:
      - "*.go"
      - "*.mod"
      - "*.sum"
    exclude_patterns:
      - "*.log"
      - "node_modules/"
      - ".git/"
    sync_patterns:
      - "*.go"
      - "*.mod"
      - "*.sum"
    exclude: []
    watch_mode: false
    backup_config:
      enabled: true
      max_backups: 10
    post_sync_commands:
      - directory: "/Users/aa12/projects/backend"
        command: "go mod tidy"
        order: 0
      - directory: "/Users/aa12/projects/backend"
        command: "go build ./..."
        order: 1
    repo_type: "SSH"
    username: ""
    password: ""
```

## 测试中文功能

运行测试脚本验证中文功能：

```bash
./test-chinese.sh
```

## 技术实现

### 国际化系统

- 使用自定义的 i18n 系统
- 支持消息键值对映射
- 自动回退到英文（如果中文翻译不存在）
- 支持参数化消息

### 命令映射

- 中文命令自动映射到英文命令
- 支持命令别名（如 `ls` 对应 `列表`）
- 保持向后兼容性

### 界面本地化

- 所有用户界面消息都支持国际化
- 交互式选择器支持中文
- 错误和成功消息支持中文
- 帮助信息支持中文

## 贡献

欢迎贡献更多语言支持！要添加新语言：

1. 在 `internal/i18n/i18n.go` 中添加新的 `Language` 常量
2. 在 `initMessages()` 函数中添加新语言的消息映射
3. 在 `ParseLanguage()` 函数中添加语言解析逻辑
4. 更新文档和测试

## 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。
