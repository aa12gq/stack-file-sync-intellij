# Stack Sync CLI v1.1.5 Release Notes

## 🚀 新功能

### 跨平台构建支持

- **新增跨平台构建脚本** (`scripts/build.sh`)
  - 支持自动构建所有主流平台：macOS (Intel/ARM), Linux (Intel/ARM), Windows
  - 自动创建压缩包和发布文件
  - 支持交叉编译，无需目标平台环境

### macOS 通用二进制文件

- **新增通用二进制文件支持**
  - 单个二进制文件同时支持 Intel 和 ARM64 架构
  - 显著简化 macOS 用户安装体验
  - 减少下载大小和安装复杂度

## 📦 构建产物

### 二进制文件

- `stack-sync-darwin-universal` - macOS 通用二进制文件 (Intel + ARM64)
- `stack-sync-darwin-amd64` - macOS Intel 专用
- `stack-sync-darwin-arm64` - macOS ARM64 专用
- `stack-sync-linux-amd64` - Linux Intel 64 位
- `stack-sync-linux-arm64` - Linux ARM64
- `stack-sync-windows-amd64.exe` - Windows 64 位

### 压缩包

所有二进制文件都包含对应的 `.tar.gz` 压缩包，方便分发和安装。

## 🛠️ 开发者改进

### 构建流程优化

- 统一的构建脚本，支持本地和 CI 环境
- 自动版本管理和标签创建
- 集成压缩包创建和校验

### Homebrew 支持

- 更新了 Homebrew Formula 以使用通用二进制文件
- 简化了 macOS 用户的安装体验
- 支持 `brew install aa12gq/stack-sync/stack-sync` 安装

## 📋 安装方式

### 方式一：直接下载

```bash
# 下载对应平台的二进制文件
curl -L https://github.com/aa12gq/stack-file-sync-intellij/releases/download/v1.1.5/stack-sync-darwin-universal.tar.gz | tar -xz
sudo mv stack-sync-darwin-universal /usr/local/bin/stack-sync
```

### 方式二：Homebrew (推荐)

```bash
brew install aa12gq/stack-sync/stack-sync
```

### 方式三：从源码构建

```bash
git clone https://github.com/aa12gq/stack-file-sync-intellij.git
cd stack-file-sync-intellij/stack-sync-cli
./scripts/build.sh
```

## 🔧 技术细节

### 通用二进制文件

- 使用 `lipo` 工具合并 Intel 和 ARM64 架构
- 文件大小：~35MB (包含两个架构)
- 运行时自动选择对应架构执行

### 构建环境要求

- Go 1.21+
- macOS 开发环境 (用于交叉编译)
- 支持 CGO 的 Go 环境

## 🐛 修复和改进

- 改进了构建脚本的错误处理
- 优化了二进制文件大小
- 增强了跨平台兼容性
- 改进了文档和安装说明

## 📈 性能优化

- 通用二进制文件减少了架构检测开销
- 优化了启动时间
- 改进了内存使用效率

---

**发布日期**: 2024 年 10 月 24 日  
**版本**: v1.1.5  
**兼容性**: macOS 10.15+, Linux, Windows 10+
