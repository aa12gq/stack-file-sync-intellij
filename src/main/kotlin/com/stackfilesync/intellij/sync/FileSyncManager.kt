package com.stackfilesync.intellij.sync

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import com.stackfilesync.intellij.model.Repository
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import com.stackfilesync.intellij.utils.FilePatternMatcher
import com.stackfilesync.intellij.utils.CommandExecutor
import com.stackfilesync.intellij.service.SyncHistoryService
import com.stackfilesync.intellij.exception.SyncException
import com.stackfilesync.intellij.utils.NotificationUtils
import com.stackfilesync.intellij.backup.BackupManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.CheckBoxList
import com.intellij.ui.components.JBScrollPane
import javax.swing.JComponent
import java.awt.Dimension
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import java.io.BufferedReader
import java.io.InputStreamReader
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ToolWindow
import com.stackfilesync.intellij.logs.LogsViewProvider
import com.stackfilesync.intellij.logs.LogService
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JPanel
import java.awt.BorderLayout
import javax.swing.JButton
import com.stackfilesync.intellij.service.SyncStateService
import java.util.stream.Collectors
import kotlin.io.path.name
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.JComboBox as ComboBox
import javax.swing.BorderFactory
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import com.stackfilesync.intellij.settings.SyncSettingsState
import javax.swing.DefaultComboBoxModel
import java.awt.GridLayout
import com.stackfilesync.intellij.window.RepositoriesPanel

class FileSyncManager(
    private val project: Project,
    private val indicator: ProgressIndicator
) {
    private val git = Git.getInstance()
    private val tempDir = Files.createTempDirectory("stack-file-sync")
    private var syncedFiles = mutableListOf<String>()
    private val backupManager = BackupManager(project)
    private val logService = LogService.getInstance(project)

    /**
     * 同步仓库文件
     * @param repository 要同步的仓库
     * @param showFileSelection 是否显示文件选择对话框
     * @param isAutoSync 是否是自动同步
     * @param moduleFilter 模块过滤器
     */
    fun sync(
        repository: Repository, 
        showFileSelection: Boolean = true, 
        isAutoSync: Boolean = false,
        moduleFilter: String? = null
    ) {
        val startTime = System.currentTimeMillis()
        var success = false
        var error: String? = null
        
        try {
            validateConfig(repository)
            
            // 激活主窗口
            invokeLater {
                val toolWindow = ToolWindowManager.getInstance(project)
                    .getToolWindow("Stack File Sync")
                toolWindow?.show()
            }
            
            logService.clear()
            logService.appendLog("开始${if (isAutoSync) "自动" else "手动"}同步仓库: ${repository.name}")
            
            // 如果是自动同步模式，显示下次执行时间
            if (isAutoSync && repository.autoSync?.enabled == true) {
                val interval = repository.autoSync?.interval ?: 300
                val nextTime = System.currentTimeMillis() + (interval * 1000)
                val nextTimeStr = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date(nextTime))
                logService.appendLog("仓库 [${repository.name}] 下次自动同步时间: $nextTimeStr\n")
            }
            
            indicator.text = "准备同步..."
            indicator.isIndeterminate = true
            
            // 同步文件
            // 自动同步时强制使用全量同步模式
            val actualShowFileSelection = if (isAutoSync) false else showFileSelection
            val syncResult = syncFromGit(repository, actualShowFileSelection, moduleFilter)
            // 如果同步被取消或没有选择文件，直接返回
            if (!syncResult) {
                logService.appendLog("\n同步已取消")
                return
            }
            
            // 在同步成功后执行后处理命令
            if (!repository.postSyncCommands.isNullOrEmpty()) {
                logService.appendLog("\n开始执行后处理命令...")
                indicator.text = "执行后处理命令..."
                executePostSyncCommands(repository)
                logService.appendLog("所有命令执行完成")
            }
            
            success = true
            logService.appendLog("\n✅ 同步完成")
            
            // 在 EDT 线程上显示通知
            invokeLater {
                NotificationUtils.showInfo(
                    project,
                    "同步成功",
                    "已成功同步 ${syncedFiles.size} 个文件"
                )
            }
            
        } catch (e: SyncException) {
            error = e.message
            logService.appendLog("\n❌ 同步失败: ${e.message}")
            invokeLater {
                NotificationUtils.showError(project, "同步失败", e.message ?: "未知错误")
            }
            throw e
        } catch (e: Exception) {
            error = e.message
            logService.appendLog("\n❌ 同步失败: ${e.message}")
            invokeLater {
                NotificationUtils.showError(
                    project,
                    "同步失败",
                    "发生未知错误: ${e.message}"
                )
            }
            throw SyncException.GitException("同步失败", e)
        } finally {
            // 清理临时目录
            tempDir.toFile().deleteRecursively()
            
            // 记录同步历史
            SyncHistoryService.getInstance().addHistoryItem(
                SyncHistoryService.HistoryItem(
                    timestamp = startTime,
                    repository = repository.name,
                    branch = repository.branch,
                    success = success,
                    error = error,
                    fileCount = syncedFiles.size,
                    duration = System.currentTimeMillis() - startTime,
                    syncType = if (repository.autoSync?.enabled == true) "auto" else "manual"
                )
            )
            
            // 最重要的：始终确保同步状态被设置为完成
            invokeLater {
                SyncStateService.getInstance(project).setSyncFinished()
            }
        }
    }

    private fun validateConfig(repository: Repository) {
        if (repository.name.isBlank()) {
            throw SyncException.ConfigException("仓库名称不能为空")
        }
        
        if (repository.url.isBlank()) {
            throw SyncException.ConfigException("Git仓库URL不能为空")
        }
        if (repository.branch.isBlank()) {
            throw SyncException.ConfigException("Git分支不能为空")
        }
        
        if (repository.sourceDirectory.isBlank()) {
            throw SyncException.ConfigException("远程目录不能为空")
        }
        if (repository.targetDirectory.isBlank()) {
            throw SyncException.ConfigException("目标目录不能为空")
        }
    }

    private fun syncFromGit(
        repository: Repository, 
        showFileSelection: Boolean = true,
        moduleFilter: String? = null
    ): Boolean {
        try {
            val gitDir = tempDir.resolve(repository.name)
            
            // 克隆仓库
            cloneRepository(repository, gitDir)
            
            // 同步文件
            logService.appendLog("开始同步文件...")
            val syncSuccess = syncFiles(repository, gitDir, showFileSelection, moduleFilter)
            
            if (syncSuccess) {
                logService.appendLog("文件同步完成")
                return true
            }
            
            return false
            
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is SyncException -> e.message
                else -> "同步失败: ${e.message}"
            }
            
            logService.appendLog("❌ $errorMessage")
            invokeLater {
                NotificationUtils.showError(
                    project,
                    "同步失败",
                    errorMessage ?: "未知错误"
                )
            }
            
            return false
        }
    }

    private fun cloneRepository(repository: Repository, gitDir: Path) {
        try {
            indicator.text = "正在克隆仓库: ${repository.url} (分支: ${repository.branch})..."
            logService.appendLog("克隆仓库: ${repository.url}")
            logService.appendLog("指定分支: ${repository.branch}")
            
            // 先删除目标目录如果存在的话
            if (Files.exists(gitDir)) {
                gitDir.toFile().deleteRecursively()
            }
            
            // 创建父目录
            Files.createDirectories(gitDir.parent)
            
            val handler = GitLineHandler(
                project,
                gitDir.parent.toFile(),
                GitCommand.CLONE
            )
            
            // 克隆参数
            handler.addParameters(
                "--quiet",
                "--single-branch",
                "--branch", repository.branch,
                "--depth=1"
            )
            
            // 如果是 HTTPS 类型且提供了凭证
            var askPassScript: File? = null
            if (repository.repoType == "HTTPS" && !repository.username.isNullOrBlank() && !repository.password.isNullOrBlank()) {
                // 创建临时脚本
                askPassScript = createTemporaryAskPassScript(repository.username!!, repository.password!!)
                
                // 在 IntelliJ 的 Git4Idea API 中可能没有直接设置环境变量的方法
                // 另一种方式是修改 URL 来包含凭证 (不太安全但可行)
                val urlWithCredentials = repository.url.replace(
                    "https://", 
                    "https://${repository.username}:${repository.password}@"
                )
                handler.addParameters(urlWithCredentials, gitDir.toString())
                
                logService.appendLog("使用 HTTPS 凭证认证")
            } else {
                // 使用原始 URL
                handler.addParameters(repository.url, gitDir.toString())
            }
            
            // 命令日志 - 隐藏密码
            val safeUrl = if (repository.repoType == "HTTPS" && !repository.username.isNullOrBlank()) {
                repository.url.replace("://", "://${repository.username}:****@")
            } else {
                repository.url
            }
            logService.appendLog("执行命令: git clone --quiet --single-branch --branch ${repository.branch} --depth=1 $safeUrl ${gitDir}")
            
            // 如果有 askPassScript，尝试通过 ProcessBuilder 执行
            if (askPassScript != null) {
                // 使用 ProcessBuilder 执行带环境变量的命令
                val pb = ProcessBuilder(
                    "git", "clone", "--quiet", "--single-branch", 
                    "--branch", repository.branch, "--depth=1",
                    repository.url, gitDir.toString()
                )
                pb.directory(gitDir.parent.toFile())
                pb.environment()["GIT_ASKPASS"] = askPassScript.absolutePath
                
                val process = pb.start()
                val processExitCode = process.waitFor()
                
                // 如果进程返回非零，则读取错误并抛出异常
                if (processExitCode != 0) {
                    val processErrorOutput = process.errorStream.bufferedReader().readText()
                    val errorMessage = """
                        同步失败: 克隆仓库失败
                        仓库: ${repository.url}
                        分支: ${repository.branch}
                        错误信息: $processErrorOutput
                        
                        请检查:
                        1. 仓库地址是否正确
                        2. 分支名称是否正确
                        3. 是否有权限访问该仓库和分支
                        4. 网络连接是否正常
                    """.trimIndent()
                    
                    logService.appendLog("❌ $errorMessage")
                    throw SyncException.GitException(errorMessage)
                }
            } else {
                // 使用标准的 git.runCommand
                val gitResult = git.runCommand(handler)
                
                // 检查 git 结果
                if (gitResult.exitCode != 0) {
                    val errorMessage = """
                        同步失败: 克隆仓库失败
                        仓库: ${repository.url}
                        分支: ${repository.branch}
                        错误信息: ${gitResult.errorOutput}
                        
                        请检查:
                        1. 仓库地址是否正确
                        2. 分支名称是否正确
                        3. 是否有权限访问该仓库和分支
                        4. 网络连接是否正常
                    """.trimIndent()
                    
                    logService.appendLog("❌ $errorMessage")
                    throw SyncException.GitException(errorMessage)
                }
            }
            
            // 在克隆成功后添加额外日志
            if (Files.exists(gitDir)) {
                logService.appendLog("仓库克隆成功，检查目录结构：")
                
                // 列出根目录内容
                Files.list(gitDir).use { stream ->
                    stream.forEach { path ->
                        val relativePath = gitDir.relativize(path)
                        val fileType = if (Files.isDirectory(path)) "目录" else "文件"
                        logService.appendLog("- $fileType: $relativePath")
                    }
                }
                
                // 检查源目录
                val sourceDir = gitDir.resolve(repository.sourceDirectory)
                if (Files.exists(sourceDir)) {
                    logService.appendLog("远程目录 '${repository.sourceDirectory}' 存在")
                    
                    // 如果源目录是非根目录，列出其内容
                    if (repository.sourceDirectory.isNotBlank()) {
                        Files.list(sourceDir).use { stream ->
                            stream.forEach { path ->
                                val relativePath = sourceDir.relativize(path)
                                val fileType = if (Files.isDirectory(path)) "目录" else "文件"
                                logService.appendLog("  - $fileType: $relativePath")
                            }
                        }
                    }
                } else {
                    logService.appendLog("警告：远程目录 '${repository.sourceDirectory}' 不存在")
                }
            }
            
            // 验证当前分支
            val branchHandler = GitLineHandler(
                project,
                gitDir.toFile(),
                GitCommand.REV_PARSE
            )
            branchHandler.addParameters("--abbrev-ref", "HEAD")
            
            val branchResult = git.runCommand(branchHandler)
            val currentBranch = branchResult.output.joinToString("").trim()
            
            if (currentBranch != repository.branch) {
                logService.appendLog("⚠️ 当前分支 ($currentBranch) 与配置的分支 (${repository.branch}) 不一致")
                logService.appendLog("正在切换到指定分支...")
                
                // 切换到指定分支
                checkoutBranch(repository, gitDir)
            } else {
                logService.appendLog("✅ 已在指定分支: ${repository.branch}")
            }
            
        } catch (e: Exception) {
            val errorMessage = """
                同步失败: 克隆仓库失败
                仓库: ${repository.url}
                分支: ${repository.branch}
                错误信息: ${e.message}
                
                请检查:
                1. 仓库地址是否正确
                2. 分支名称是否正确
                3. 是否有权限访问该仓库和分支
                4. 网络连接是否正常
            """.trimIndent()
            
            logService.appendLog("❌ $errorMessage")
            throw SyncException.GitException(errorMessage, e)
        }
    }

    private fun checkoutBranch(repository: Repository, gitDir: Path) {
        try {
            // 添加日志输出
            indicator.text = "正在切换到分支: ${repository.branch}..."
            
            // 如果是 HTTPS 类型且提供了凭证，使用 ProcessBuilder
            if (repository.repoType == "HTTPS" && !repository.username.isNullOrBlank() && !repository.password.isNullOrBlank()) {
                val askPassScript = createTemporaryAskPassScript(repository.username!!, repository.password!!)
                tempCredentialFiles.add(askPassScript)
                
                // 使用 ProcessBuilder 来执行命令并设置环境变量
                val pb = ProcessBuilder(
                    "git", "checkout", repository.branch
                )
                pb.directory(gitDir.toFile())
                pb.environment()["GIT_ASKPASS"] = askPassScript.absolutePath
                pb.redirectErrorStream(true)
                
                val process = pb.start()
                val exitCode = process.waitFor()
                
                if (exitCode != 0) {
                    val output = process.inputStream.bufferedReader().readText()
                    val errorMessage = """
                        Git操作失败: 切换分支失败: ${repository.branch}
                        错误信息: $output
                        
                        请检查:
                        1. 分支名称是否正确
                        2. 分支是否存在
                        3. 是否有权限访问该分支
                        4. 远程仓库连接是否正常
                    """.trimIndent()
                    
                    logService.appendLog("❌ $errorMessage")
                    throw SyncException.GitException(errorMessage)
                }
            } else {
                // 使用 GitLineHandler
                val handler = GitLineHandler(
                    project,
                    gitDir.toFile(),
                    GitCommand.CHECKOUT
                )
                handler.addParameters(repository.branch)
                
                // 执行Git命令
                val result = git.runCommand(handler)
                
                // 检查命令执行结果
                if (result.exitCode != 0) {
                    // 获取详细错误信息
                    val errorOutput = result.errorOutput
                    val errorMessage = """
                        Git操作失败: 切换分支失败: ${repository.branch}
                        错误信息: ${errorOutput}
                        
                        请检查:
                        1. 分支名称是否正确
                        2. 分支是否存在
                        3. 是否有权限访问该分支
                        4. 远程仓库连接是否正常
                    """.trimIndent()
                    
                    logService.appendLog("❌ $errorMessage")
                    throw SyncException.GitException(errorMessage)
                }
            }
            
            logService.appendLog("✅ 成功切换到分支: ${repository.branch}")
        } catch (e: Exception) {
            val errorMessage = """
                Git操作失败: 切换分支失败: ${repository.branch}
                错误信息: ${e.message}
                
                请检查:
                1. 分支名称是否正确
                2. 分支是否存在
                3. 是否有权限访问该分支
                4. 远程仓库连接是否正常
            """.trimIndent()
            
            logService.appendLog("❌ $errorMessage")
            throw SyncException.GitException(errorMessage, e)
        }
    }

    private fun syncFiles(
        repository: Repository, 
        sourcePath: Path, 
        showFileSelection: Boolean = true,
        moduleFilter: String? = null
    ): Boolean {
        try {
            val targetDir = getTargetDirectory(repository)
            val sourceDir = sourcePath.resolve(repository.sourceDirectory)
            
            // 检查远程目录是否存在
            if (!Files.exists(sourceDir)) {
                val errorMessage = """
                    未找到远程目录
                    目录: ${sourceDir}
                    请检查:
                    1. 远程目录路径是否正确
                    2. 仓库是否包含该目录
                """.trimIndent()
                logService.appendLog("❌ $errorMessage")
                throw SyncException.FileException(errorMessage)
            }
            
            // 获取要同步的文件
            indicator.text = "正在扫描文件..."
            val allFiles = findFilesToSync(sourceDir, repository)
            
            if (allFiles.isEmpty()) {
                logService.appendLog("没有找到需要同步的文件")
                return false
            }

            // 增加模块过滤逻辑
            val filteredFiles = if (moduleFilter != null) {
                logService.appendLog("应用模块过滤: $moduleFilter")
                allFiles.filter { file ->
                    val relativePath = sourceDir.relativize(file).toString()
                    val matches = relativePath.contains(moduleFilter, ignoreCase = true)
                    if (matches) {
                        logService.appendLog("匹配文件: $relativePath")
                    }
                    matches
                }.also {
                    logService.appendLog("过滤后剩余 ${it.size} 个文件")
                }
            } else {
                allFiles
            }
            
            logService.appendLog("找到 ${filteredFiles.size} 个文件可以同步")

            // 根据同步模式决定是否显示文件选择对话框
            val files = if (showFileSelection) {
                chooseFilesToSync(filteredFiles, sourceDir)
            } else {
                filteredFiles
            }
            
            if (files.isEmpty()) {
                logService.appendLog("未选择任何文件，同步已取消")
                return false
            }

            logService.appendLog("已选择 ${files.size} 个文件进行同步")

            // 同步所有选中的文件
            indicator.isIndeterminate = false
            indicator.fraction = 0.0
            
            // 备份现有文件
            logService.appendLog("正在备份文件...")
            backupSelectedFiles(repository, files, targetDir)
            
            // 同步文件
            files.forEachIndexed { index, file ->
                try {
                    val relativePath = sourceDir.relativize(file)
                    val targetFile = targetDir.resolve(relativePath)
                    
                    // 创建目标目录
                    targetFile.parent.toFile().mkdirs()
                    
                    // 复制文件
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                    
                    syncedFiles.add(relativePath.toString())
                    
                    // 更新进度
                    indicator.fraction = (index + 1).toDouble() / files.size
                    indicator.text = "正在同步: $relativePath (${index + 1}/${files.size})"
                    logService.appendLog("同步文件: $relativePath")
                    
                } catch (e: Exception) {
                    val errorMessage = "同步文件失败: ${file}, 原因: ${e.message}"
                    logService.appendLog("❌ $errorMessage")
                    throw SyncException.FileException(errorMessage, e)
                }
            }
            
            logService.appendLog("✅ 成功同步 ${files.size} 个文件")
            return true
            
        } catch (e: Exception) {
            val errorMessage = "同步失败: ${e.message}"
            logService.appendLog("❌ $errorMessage")
            throw SyncException.FileException(errorMessage, e)
        }
    }

    // 让用户选择要同步的文件
    private fun chooseFilesToSync(availableFiles: List<Path>, sourceDir: Path): List<Path> {
        var selectedFiles = emptyList<Path>()
        
        // 在 EDT 线程上执行对话框操作
        ApplicationManager.getApplication().invokeAndWait {
            val dialog = object : DialogWrapper(project, true) {
                private val checkBoxList = CheckBoxList<String>()
                private val selectAllCheckBox = JCheckBox("全选").apply {
                    isSelected = true
                }
                
                // 添加搜索框
                private val searchField = JTextField().apply {
                    preferredSize = Dimension(300, 30)
                }
                
                // 添加搜索按钮
                private val searchButton = JButton("过滤").apply {
                    addActionListener {
                        filterItems(searchField.text.trim())
                    }
                }
                
                // 文件路径列表（用于过滤）
                private val allFilePaths = availableFiles.map { sourceDir.relativize(it).toString() }
                
                init {
                    title = "选择要同步的文件"
                    init()
                    
                    // 添加文件列表
                    allFilePaths.forEach { path ->
                        checkBoxList.addItem(path, path, true)  // 默认全选
                    }

                    // 设置全选复选框的动作
                    selectAllCheckBox.addActionListener {
                        val selected = selectAllCheckBox.isSelected
                        for (i in 0 until checkBoxList.itemsCount) {
                            val item = checkBoxList.getItemAt(i)
                            if (item is String) {
                                checkBoxList.setItemSelected(item, selected)
                            }
                        }
                        checkBoxList.repaint()
                    }

                    // 设置复选框列表的点击处理
                    checkBoxList.setCheckBoxListListener { index, value ->
                        // 更新全选状态
                        updateSelectAllState()
                    }
                    
                    // 添加回车键监听器到搜索框
                    searchField.addKeyListener(object : KeyAdapter() {
                        override fun keyPressed(e: KeyEvent) {
                            if (e.keyCode == KeyEvent.VK_ENTER) {
                                filterItems(searchField.text.trim())
                            }
                        }
                    })
                }
                
                // 添加过滤功能
                private fun filterItems(filterText: String) {
                    // 如果过滤文本为空，恢复所有项
                    if (filterText.isEmpty()) {
                        checkBoxList.clear()
                        allFilePaths.forEach { path ->
                            checkBoxList.addItem(path, path, selectAllCheckBox.isSelected)
                        }
                        return
                    }
                    
                    // 使用过滤文本过滤路径
                    val filteredPaths = allFilePaths.filter { path ->
                        path.toLowerCase().contains(filterText.toLowerCase()) ||
                                matchGlobPattern(path, filterText)
                    }
                    
                    // 更新列表
                    checkBoxList.clear()
                    filteredPaths.forEach { path ->
                        checkBoxList.addItem(path, path, selectAllCheckBox.isSelected)
                    }
                    
                    // 如果过滤后有文件，保存过滤条件
                    if (filteredPaths.isNotEmpty() && filterText.isNotEmpty()) {
                        try {
                            // 尝试保存到最近选择
                            SyncSettingsState.getInstance().addRecentFileSelection(filterText)
                        } catch (e: Exception) {
                            // 忽略可能的异常
                        }
                    }
                }
                
                // 简单的通配符匹配
                private fun matchGlobPattern(path: String, pattern: String): Boolean {
                    if (!pattern.contains("*")) return false
                    
                    val regex = pattern
                        .replace(".", "\\.")
                        .replace("*", ".*")
                        .let { ".*$it.*" }
                        .toRegex(RegexOption.IGNORE_CASE)
                    
                    return regex.matches(path)
                }
                
                private fun updateSelectAllState() {
                    var allSelected = true
                    for (i in 0 until checkBoxList.itemsCount) {
                        val item = checkBoxList.getItemAt(i)
                        if (item is String && !checkBoxList.isItemSelected(item)) {
                            allSelected = false
                            break
                        }
                    }
                    selectAllCheckBox.isSelected = allSelected
                }
                
                override fun createCenterPanel(): JComponent {
                    val panel = JPanel(BorderLayout())
                    
                    // 创建搜索面板
                    val searchPanel = JPanel(BorderLayout()).apply {
                        border = BorderFactory.createEmptyBorder(0, 0, 10, 0)
                        add(JLabel("搜索/过滤:"), BorderLayout.WEST)
                        add(searchField, BorderLayout.CENTER)
                        add(searchButton, BorderLayout.EAST)
                    }
                    
                    // 创建文件数量面板
                    val countPanel = JPanel(BorderLayout()).apply {
                        add(selectAllCheckBox, BorderLayout.WEST)
                        add(JLabel("共 ${availableFiles.size} 个文件"), BorderLayout.EAST)
                    }
                    
                    // 创建顶部面板（使用BorderLayout而不是GridLayout）
                    val topPanel = JPanel(BorderLayout())
                    topPanel.add(countPanel, BorderLayout.NORTH)
                    topPanel.add(searchPanel, BorderLayout.SOUTH)
                    
                    val scrollPane = JBScrollPane(checkBoxList)
                    scrollPane.preferredSize = Dimension(500, 400)
                    
                    panel.add(topPanel, BorderLayout.NORTH)
                    panel.add(scrollPane, BorderLayout.CENTER)
                    
                    return panel
                }
                
                override fun getPreferredFocusedComponent() = searchField
                
                fun getSelectedFiles(): List<Path> {
                    val selectedPaths = mutableListOf<String>()
                    for (i in 0 until checkBoxList.itemsCount) {
                        val item = checkBoxList.getItemAt(i)
                        if (item is String && checkBoxList.isItemSelected(item)) {
                            selectedPaths.add(item)
                        }
                    }
                    
                    return selectedPaths.mapNotNull { relativePath ->
                        availableFiles.find { sourceDir.relativize(it).toString() == relativePath }
                    }
                }
            }

            if (dialog.showAndGet()) {
                selectedFiles = dialog.getSelectedFiles()
            }
        }
        
        return selectedFiles
    }

    // 备份选中的文件
    private fun backupSelectedFiles(repository: Repository, filesToSync: List<Path>, targetDir: Path) {
        try {
            val existingFiles = filesToSync.map { file ->
                targetDir.resolve(file.fileName)
            }
            backupManager.backupFiles(repository, existingFiles)
        } catch (e: Exception) {
            val errorMessage = "备份文件失败: ${e.message}"
            invokeLater {
                NotificationUtils.showError(project, "备份失败", errorMessage)
            }
            throw SyncException.FileException(errorMessage, e)
        }
    }

    private fun findFilesToSync(sourceDir: Path, repository: Repository): List<Path> {
        logService.appendLog("源目录路径: ${sourceDir}")
        logService.appendLog("文件匹配模式: ${repository.filePatterns.joinToString(", ")}")
        logService.appendLog("排除模式: ${repository.excludePatterns.joinToString(", ")}")
        
        // 检查源目录是否存在
        if (!Files.exists(sourceDir)) {
            logService.appendLog("警告：指定的源目录 '${sourceDir}' 不存在")
            return emptyList()
        }
        
        // 列出源目录中的文件和子目录
        try {
            val dirContents = mutableListOf<Path>()
            Files.list(sourceDir).use { stream ->
                stream.forEach { dirContents.add(it) }
            }
            logService.appendLog("源目录包含 ${dirContents.size} 个文件/目录：${dirContents.map { it.fileName.toString() }.joinToString(", ")}")
        } catch (e: Exception) {
            logService.appendLog("列出源目录内容时出错: ${e.message}")
        }
        
        val matchingFiles = FilePatternMatcher.findMatchingFiles(
            sourceDir,
            repository.filePatterns,
            repository.excludePatterns
        )
        
        logService.appendLog("匹配到 ${matchingFiles.size} 个文件")
        return matchingFiles
    }

    private fun executePostSyncCommands(repository: Repository) {
        val workspaceRoot = project.basePath ?: throw RuntimeException("无法获取项目目录")
        
        // 按order字段排序命令
        val sortedCommands = repository.postSyncCommands.sortedBy { it.order }
        
        sortedCommands.forEachIndexed { index, command ->
            try {
                logService.appendLog("\n执行命令 [${command.order}] ${index + 1}/${sortedCommands.size}")
                
                // 解析命令执行目录
                val cmdDir = if (command.directory.startsWith("/")) {
                    File(command.directory)
                } else {
                    File(workspaceRoot, command.directory)
                }

                // 检查目录是否存在
                if (!cmdDir.exists()) {
                    val errorMessage = "命令执行目录不存在: ${cmdDir.absolutePath}"
                    logService.appendLog("❌ $errorMessage")
                    throw RuntimeException(errorMessage)
                }

                logService.appendLog("目录: ${cmdDir.absolutePath}")
                logService.appendLog("命令: ${command.command}")

                // 获取系统环境变量
                val env = System.getenv().toMutableMap()
                
                // 添加常用的路径和环境变量
                val userHome = System.getProperty("user.home")
                val paths = listOf(
                    "/usr/local/bin",
                    "/opt/homebrew/bin",
                    "$userHome/.cargo/bin",
                    "$userHome/.local/bin",
                    "$userHome/go/bin",
                    "/usr/local/go/bin"
                )
                
                // 合并 PATH
                val currentPath = env["PATH"] ?: ""
                env["PATH"] = (paths + currentPath.split(File.pathSeparator))
                    .distinct()
                    .joinToString(File.pathSeparator)

                // 执行命令，使用 shell 的初始化文件
                val wrappedCommand = """
                    source ~/.zshrc 2>/dev/null || source ~/.bashrc 2>/dev/null
                    source ~/.profile 2>/dev/null
                    ${command.command}
                """.trimIndent()

                // 执行命令
                val process = ProcessBuilder().apply {
                    directory(cmdDir)
                    if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                        command("cmd", "/c", command.command)
                    } else {
                        command("sh", "-c", wrappedCommand)
                    }
                    redirectErrorStream(true)
                    environment().putAll(env)
                }.start()

                // 读取命令输出
                var hasError = false
                process.inputStream.bufferedReader().use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val output = line ?: ""
                        logService.appendLog(output)
                        // 只在遇到真正的错误时才标记为失败
                        if (output.contains("command not found", ignoreCase = true) ||
                            output.contains("permission denied", ignoreCase = true) ||
                            output.contains("no such file or directory", ignoreCase = true) ||
                            output.contains("failed with exit code", ignoreCase = true)) {
                            hasError = true
                        }
                    }
                }

                // 等待命令执行完成
                val exitCode = process.waitFor()
                // 只有在退出码非0且确实发生错误时才认为是失败
                if (exitCode != 0 && hasError) {
                    throw RuntimeException("命令执行失败，退出码: $exitCode")
                }

                logService.appendLog("✅ 命令执行成功")
            } catch (e: Exception) {
                val errorMessage = "执行命令失败: ${command.command}\n错误: ${e.message}"
                logService.appendLog("❌ $errorMessage")
                throw SyncException.CommandException(errorMessage, e)
            }
        }
    }

    private fun getTargetDirectory(repository: Repository): Path {
        val projectDir = project.basePath?.let { Path.of(it) }
            ?: throw RuntimeException("无法获取项目目录")
            
        return if (repository.targetDirectory.startsWith("/")) {
            Path.of(repository.targetDirectory)
        } else {
            projectDir.resolve(repository.targetDirectory)
        }
    }

    // 显示通知也需要在 EDT 上执行
    private fun showNotification(title: String, message: String, isError: Boolean = false) {
        invokeLater {
            if (isError) {
                NotificationUtils.showError(project, title, message)
            } else {
                NotificationUtils.showInfo(project, title, message)
            }
        }
    }

    // 添加以下辅助方法
    private fun createTemporaryAskPassScript(username: String, password: String): File {
        val script = File.createTempFile("git_askpass", ".sh")
        script.setExecutable(true)
        
        // 写入脚本内容
        script.writeText("""
            #!/bin/sh
            if echo "$1" | grep -qi "username"; then
                echo "$username"
            else
                echo "$password"
            fi
        """.trimIndent())
        
        return script
    }

    private val tempCredentialFiles = mutableListOf<File>()

    private fun cleanupTemporaryCredentialFiles() {
        tempCredentialFiles.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // 忽略删除失败的错误
            }
        }
        tempCredentialFiles.clear()
    }
} 