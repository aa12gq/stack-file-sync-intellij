# Stack File Sync - IntelliJ IDEA Plugin

<p align="center">
  <img src="src/main/resources/icons/logo.svg" width="80" height="80" alt="Plugin Logo">
</p>

Stack File Sync 是一个强大的 IntelliJ IDEA 插件，用于在多个项目之间同步文件和代码片段。它提供了一个简单而灵活的方式来管理和同步您的代码资产。

![alt text](<./docs/images/demonstration.gif>)

## 功能特点

- 🔄 实时文件同步
- 🕒 可配置的自动同步间隔
- 📁 多仓库支持
- 🔒 安全的文件备份
- ⚡ 高性能文件传输
- 🛠 可自定义的同步规则
- 📊 详细的同步日志

## 安装

### 从 JetBrains Marketplace 安装

1. 在 IntelliJ IDEA 中打开插件市场
2. 搜索 "Stack File Sync"
3. 点击安装按钮

![Installation](docs/images/installation.png)

### 手动安装

1. 下载最新的插件发布包 (.zip)
2. 在 IntelliJ IDEA 中选择 `Settings/Preferences -> Plugins -> ⚙️ -> Install Plugin from Disk`
3. 选择下载的插件包进行安装

## 快速开始

1. 安装插件后，通过 `Tools -> Stack File Sync -> Configure` 打开配置面板
2. 添加新的同步仓库
3. 配置同步规则和自动同步选项
4. 点击工具栏的同步按钮开始同步

![Quick Start](docs/images/quick-start.png)

## 配置说明

### 仓库配置

- **名称**: 仓库的唯一标识符
- **本地路径**: 需要同步的本地文件夹路径
- **远程路径**: 远程同步目标路径
- **同步规则**: 文件包含/排除规则
- **自动同步**: 启用/禁用自动同步功能

### 自动同步设置

- **启用状态**: 开启或关闭自动同步
- **同步间隔**: 设置自动同步的时间间隔（秒）
- **同步模式**: 选择单向或双向同步

## 使用示例

### 手动同步

1. 点击工具栏同步按钮
2. 选择要同步的仓库
3. 等待同步完成

### 自动同步

1. 在仓库设置中启用自动同步
2. 设置同步间隔
3. 插件将按照设定的间隔自动执行同步

## 性能优化

- 使用增量同步减少传输数据量
- 多线程并行处理提升同步速度
- 智能文件缓存机制
- 低内存占用设计

## 常见问题

**Q: 如何解决同步冲突？**
A: 插件会自动检测冲突并提供解决方案，您可以选择保留本地版本或使用远程版本。

**Q: 支持哪些同步模式？**
A: 目前支持单向同步模式。

## 贡献指南

我们欢迎社区贡献！如果您想为项目做出贡献：

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

## 版本历史

- v1.0.0 (2025-02-06)
  - 初始版本发布
  - 基本同步功能
  - 自动同步支持

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

- 项目主页：[GitHub](https://github.com/aa12gq/stack-file-sync-intellij)
- 问题反馈：[Issue Tracker](https://github.com/aa12gq/stack-file-sync-intellij/issues)
- 邮件联系：[aa12gq@gmail.com](mailto:aa12gq@gmail.com)

## 致谢

感谢所有为项目做出贡献的开发者！

---

<p align="center">Made with ❤️ by Stack File Sync Team</p>
</rewritten_file>
