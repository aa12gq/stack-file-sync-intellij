# 测试指南 / Testing Guide

[English](#english-testing-guide) | [简体中文](#中文测试指南)

---

## 中文测试指南

### 准备工作

1. **确保已构建成功**

```bash
cd stack-sync-cli
make build
```

你应该看到：
```
Build complete: build/stack-sync
```

2. **检查二进制文件**

```bash
ls -lh build/stack-sync
./build/stack-sync version
```

应该输出：`Stack Sync CLI v1.0.0`

### 测试步骤

#### 第一步：基础命令测试

```bash
# 1. 测试版本命令
./build/stack-sync version

# 2. 测试帮助命令
./build/stack-sync help

# 预期：应该显示完整的帮助信息，包含所有命令
```

#### 第二步：初始化配置

```bash
# 1. 初始化配置文件
./build/stack-sync init

# 预期：显示 "✓ Configuration initialized at: ~/.stack-sync/config.yml"

# 2. 检查配置文件是否创建
cat ~/.stack-sync/config.yml

# 预期：应该看到默认配置
```

#### 第三步：添加测试仓库

**准备一个测试仓库（推荐用你现有的小项目）**

```bash
# 1. 添加仓库
./build/stack-sync add

# 按提示输入：
# Repository name: test-repo
# Repository URL: git@github.com:yourusername/test-repo.git
# Local path: /tmp/test-repo-sync
# Enable watch mode: n (先测试手动模式)

# 预期：显示 "✓ Repository added: test-repo"
```

#### 第四步：查看仓库列表

```bash
# 列出所有仓库
./build/stack-sync list

# 预期：应该看到表格，显示刚添加的仓库
# 状态应该是 📦 (Not cloned)
```

#### 第五步：同步仓库（克隆）

```bash
# 同步单个仓库
./build/stack-sync sync test-repo

# 预期：
# - 显示克隆进度
# - 克隆完成后显示 "✓ Successfully synced test-repo"

# 验证文件
ls -la /tmp/test-repo-sync

# 应该看到仓库文件
```

#### 第六步：测试状态更新

```bash
# 1. 查看仓库状态
./build/stack-sync status test-repo

# 预期：显示详细信息，包括最后提交等

# 2. 修改一个文件
echo "test" >> /tmp/test-repo-sync/test.txt

# 3. 再次查看状态
./build/stack-sync list

# 预期：状态应该变为 🔧 (Modified)
```

#### 第七步：测试交互式模式

```bash
# 运行交互式选择器
./build/stack-sync

# 预期：
# - 看到美观的仓库列表
# - 可以用上下键选择
# - 按 / 可以搜索
# - 按回车选择仓库
```

#### 第八步：测试多仓库

```bash
# 1. 添加第二个仓库
./build/stack-sync add
# Name: test-repo-2
# URL: https://github.com/yourusername/another-repo.git
# Path: /tmp/test-repo-2
# Watch: n

# 2. 同步所有仓库
./build/stack-sync sync

# 预期：两个仓库都会被同步
```

#### 第九步：测试文件监控模式

```bash
# 1. 编辑配置启用监控
nano ~/.stack-sync/config.yml

# 修改 test-repo 的 watch_mode 为 true:
repositories:
  - name: "test-repo"
    watch_mode: true

# 2. 启动监控（在一个终端）
./build/stack-sync watch

# 预期：显示 "✓ File watcher started. Press Ctrl+C to stop."

# 3. 在另一个终端修改文件
echo "auto sync test" >> /tmp/test-repo-sync/test.txt

# 预期：
# - 监控器检测到文件变化
# - 2秒后自动触发同步
# - 看到同步日志

# 4. 按 Ctrl+C 停止监控
```

#### 第十步：测试移除仓库

```bash
# 移除测试仓库
./build/stack-sync remove test-repo-2

# 预期：
# - 显示确认提示
# - 输入 y 后移除
# - 显示 "✓ Repository removed: test-repo-2"

# 验证
./build/stack-sync list

# 应该只看到 test-repo
```

### 功能测试清单

完成以下清单，确保所有功能正常：

- [ ] ✅ `stack-sync version` - 显示版本
- [ ] ✅ `stack-sync help` - 显示帮助
- [ ] ✅ `stack-sync init` - 创建配置文件
- [ ] ✅ `stack-sync add` - 交互式添加仓库
- [ ] ✅ `stack-sync list` - 显示仓库列表和状态
- [ ] ✅ `stack-sync sync <repo>` - 同步单个仓库
- [ ] ✅ `stack-sync sync` - 同步所有仓库
- [ ] ✅ `stack-sync status <repo>` - 显示仓库详细状态
- [ ] ✅ `stack-sync` (无参数) - 交互式选择器
- [ ] ✅ `stack-sync watch` - 文件监控模式
- [ ] ✅ `stack-sync remove <repo>` - 移除仓库
- [ ] ✅ 状态图标正确显示
- [ ] ✅ 彩色输出正常
- [ ] ✅ SSH 认证工作
- [ ] ✅ HTTPS 认证工作
- [ ] ✅ 文件模式匹配工作
- [ ] ✅ 排除模式工作

### 边界测试

#### 测试错误处理

```bash
# 1. 测试不存在的仓库
./build/stack-sync sync non-existent-repo
# 预期：显示错误 "✗ Repository not found"

# 2. 测试无效的 Git URL
./build/stack-sync add
# 输入无效 URL
# 预期：克隆时显示错误

# 3. 测试无权限的仓库
./build/stack-sync add
# URL: git@github.com:private/no-access.git
# 预期：克隆失败，显示权限错误
```

#### 测试性能

```bash
# 1. 添加多个仓库（5-10个）
# 2. 测试同步所有仓库的速度
time ./build/stack-sync sync

# 3. 测试交互式列表的响应速度
./build/stack-sync
# 应该立即显示，无明显延迟
```

### 清理测试环境

测试完成后清理：

```bash
# 1. 删除配置文件
rm -rf ~/.stack-sync

# 2. 删除测试仓库
rm -rf /tmp/test-repo-sync
rm -rf /tmp/test-repo-2

# 3. 清理构建文件
make clean
```

---

## English Testing Guide

### Preparation

1. **Ensure Build Success**

```bash
cd stack-sync-cli
make build
```

Expected output:
```
Build complete: build/stack-sync
```

2. **Check Binary**

```bash
ls -lh build/stack-sync
./build/stack-sync version
```

Should output: `Stack Sync CLI v1.0.0`

### Testing Steps

#### Step 1: Basic Commands

```bash
# 1. Test version
./build/stack-sync version

# 2. Test help
./build/stack-sync help

# Expected: Full help information with all commands
```

#### Step 2: Initialize Config

```bash
# 1. Initialize
./build/stack-sync init

# Expected: "✓ Configuration initialized at: ~/.stack-sync/config.yml"

# 2. Check config file
cat ~/.stack-sync/config.yml

# Expected: Default configuration
```

#### Step 3: Add Test Repository

```bash
# Add a repository
./build/stack-sync add

# Input prompts:
# Repository name: test-repo
# Repository URL: git@github.com:yourusername/test-repo.git
# Local path: /tmp/test-repo-sync
# Watch mode: n

# Expected: "✓ Repository added: test-repo"
```

#### Step 4: List Repositories

```bash
./build/stack-sync list

# Expected: Table with repository, status should be 📦 (Not cloned)
```

#### Step 5: Sync Repository

```bash
./build/stack-sync sync test-repo

# Expected:
# - Clone progress
# - "✓ Successfully synced test-repo"

# Verify
ls -la /tmp/test-repo-sync
```

#### Step 6: Test Status Updates

```bash
# 1. Check status
./build/stack-sync status test-repo

# 2. Modify a file
echo "test" >> /tmp/test-repo-sync/test.txt

# 3. Check status again
./build/stack-sync list

# Expected: Status changes to 🔧 (Modified)
```

#### Step 7: Interactive Mode

```bash
./build/stack-sync

# Expected:
# - Beautiful repository selector
# - Arrow keys work
# - / toggles search
# - Enter selects repository
```

#### Step 8: Multiple Repositories

```bash
# Add second repo
./build/stack-sync add

# Sync all
./build/stack-sync sync

# Expected: Both repositories synced
```

#### Step 9: Watch Mode

```bash
# 1. Enable watch mode in config
nano ~/.stack-sync/config.yml

# Set watch_mode: true for test-repo

# 2. Start watcher
./build/stack-sync watch

# 3. Modify file in another terminal
echo "auto sync" >> /tmp/test-repo-sync/test.txt

# Expected: Auto-sync triggered after 2 seconds

# 4. Stop with Ctrl+C
```

#### Step 10: Remove Repository

```bash
./build/stack-sync remove test-repo-2

# Expected: Confirmation and removal
```

### Feature Checklist

- [ ] ✅ Version command works
- [ ] ✅ Help command works
- [ ] ✅ Init creates config
- [ ] ✅ Add repository interactive
- [ ] ✅ List shows repositories
- [ ] ✅ Sync single repository
- [ ] ✅ Sync all repositories
- [ ] ✅ Status shows details
- [ ] ✅ Interactive selector works
- [ ] ✅ Watch mode works
- [ ] ✅ Remove repository works
- [ ] ✅ Status icons display correctly
- [ ] ✅ Colors work
- [ ] ✅ SSH auth works
- [ ] ✅ HTTPS auth works

### Cleanup

```bash
rm -rf ~/.stack-sync
rm -rf /tmp/test-repo-sync
make clean
```

---

## 问题排查 / Troubleshooting

### 常见问题

1. **构建失败**
   ```bash
   go mod tidy
   make build
   ```

2. **权限错误**
   ```bash
   chmod +x build/stack-sync
   ```

3. **SSH 认证失败**
   ```bash
   ssh-add ~/.ssh/id_rsa
   ssh -T git@github.com
   ```

4. **配置文件权限**
   ```bash
   chmod 644 ~/.stack-sync/config.yml
   ```

---

## 测试报告模板

测试完成后，记录结果：

```
测试日期：2025-10-21
测试人员：[你的名字]
测试环境：macOS 14 / Go 1.21

✅ 通过的功能：
- 所有基础命令
- 仓库同步
- 交互式UI
- ...

❌ 失败的功能：
- [如果有]

🐛 发现的问题：
- [如果有]

💡 建议改进：
- [如果有]
```

---

**测试愉快！如有问题，请查看 [DOCS.md](DOCS.md) 获取更多帮助。**
