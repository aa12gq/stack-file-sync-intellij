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

class FileSyncManager(
    private val project: Project,
    private val indicator: ProgressIndicator
) {
    private val git = Git.getInstance()
    private val tempDir = Files.createTempDirectory("stack-file-sync")
    private var syncedFiles = mutableListOf<String>()
    private val backupManager = BackupManager(project)

    fun sync(repository: Repository) {
        val startTime = System.currentTimeMillis()
        var success = false
        var error: String? = null
        
        try {
            validateConfig(repository)
            
            indicator.text = "准备同步..."
            indicator.isIndeterminate = true
            
            syncFromGit(repository)
            
            // 执行后处理命令
            executePostSyncCommands(repository)
            
            success = true
            NotificationUtils.showInfo(
                project,
                "同步成功",
                "已成功同步 ${syncedFiles.size} 个文件"
            )
            
        } catch (e: SyncException) {
            error = e.message
            NotificationUtils.showError(project, "同步失败", e.message ?: "未知错误")
            throw e
        } catch (e: Exception) {
            error = e.message
            NotificationUtils.showError(
                project,
                "同步失败",
                "发生未知错误: ${e.message}"
            )
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
            throw SyncException.ConfigException("源目录不能为空")
        }
        if (repository.targetDirectory.isBlank()) {
            throw SyncException.ConfigException("目标目录不能为空")
        }
    }

    private fun syncFromGit(repository: Repository) {
        try {
            val gitDir = tempDir.resolve(repository.name)
            
            // 克隆仓库
            cloneRepository(repository, gitDir)
            
            // 同步文件
            syncFiles(repository, gitDir)
            
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is SyncException -> e.message
                else -> "同步失败: ${e.message}"
            }
            
            NotificationUtils.showError(
                project,
                "同步失败",
                errorMessage ?: "未知错误"
            )
            
            throw e
        }
    }

    private fun cloneRepository(repository: Repository, gitDir: Path) {
        try {
            indicator.text = "正在克隆仓库: ${repository.url}..."
            
            // 先删除目标目录如果存在的话
            if (Files.exists(gitDir)) {
                gitDir.toFile().deleteRecursively()
            }
            
            // 创建父目录
            Files.createDirectories(gitDir.parent)
            
            val handler = GitLineHandler(
                project,
                gitDir.parent.toFile(), // 使用父目录作为工作目录
                GitCommand.CLONE
            )
            
            // 参照VSCode插件的克隆参数，移除重复的"clone"命令
            handler.addParameters(
                "--quiet",  // 减少输出
                "--single-branch",  // 只克隆单个分支
                "--branch", repository.branch,  // 指定分支
                "--depth=1",  // 浅克隆
                repository.url,
                gitDir.toString()
            )
            
            val result = git.runCommand(handler)
            
            if (result.exitCode != 0) {
                val errorMessage = """
                    同步失败: 克隆仓库失败
                    仓库: ${repository.url}
                    分支: ${repository.branch}
                    错误信息: ${result.errorOutput}
                """.trimIndent()
                
                NotificationUtils.showError(
                    project,
                    "同步失败",
                    errorMessage
                )
                
                throw SyncException.GitException(errorMessage)
            }
            
        } catch (e: Exception) {
            val errorMessage = """
                同步失败: 克隆仓库失败
                仓库: ${repository.url}
                分支: ${repository.branch}
                错误信息: ${e.message}
            """.trimIndent()
            
            NotificationUtils.showError(
                project,
                "同步失败",
                errorMessage
            )
            
            throw SyncException.GitException(errorMessage, e)
        }
    }

    private fun checkoutBranch(repository: Repository, gitDir: Path) {
        try {
            // 添加日志输出
            indicator.text = "正在切换到分支: ${repository.branch}..."
            
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
                
                // 显示错误通知
                NotificationUtils.showError(
                    project,
                    "切换分支失败",
                    errorMessage
                )
                
                throw SyncException.GitException(errorMessage)
            }
            
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
            
            // 显示错误通知
            NotificationUtils.showError(
                project,
                "切换分支失败",
                errorMessage
            )
            
            throw SyncException.GitException(errorMessage, e)
        }
    }

    private fun syncFiles(repository: Repository, sourcePath: Path) {
        val startTime = System.currentTimeMillis()
        try {
            val targetDir = getTargetDirectory(repository)
            val sourceDir = sourcePath.resolve(repository.sourceDirectory)
            
            // 检查源目录是否存在
            if (!Files.exists(sourceDir)) {
                val errorMessage = """
                    未找到源目录
                    目录: ${sourceDir}
                    请检查:
                    1. 源目录路径是否正确
                    2. 仓库是否包含该目录
                """.trimIndent()
                NotificationUtils.showError(project, "同步失败", errorMessage)
                throw SyncException.FileException(errorMessage)
            }
            
            // 获取要同步的文件
            indicator.text = "正在扫描文件..."
            val files = findFilesToSync(sourceDir, repository)
            
            if (files.isEmpty()) {
                val errorMessage = """
                    未找到可同步的文件
                    请检查:
                    1. 文件匹配模式是否正确: ${repository.filePatterns ?: listOf("**/*.proto")}
                    2. 源目录是否包含匹配的文件: ${repository.sourceDirectory}
                    3. 文件是否被排除规则过滤: ${repository.excludePatterns}
                """.trimIndent()
                NotificationUtils.showError(project, "同步失败", errorMessage)
                throw SyncException.FileException(errorMessage)
            }

            // 让用户选择要同步的文件
            val selectedFiles = chooseFilesToSync(files, sourceDir)
            if (selectedFiles.isEmpty()) {
                return // 用户取消了选择
            }
            
            indicator.isIndeterminate = false
            indicator.fraction = 0.0
            
            // 备份现有文件
            indicator.text = "正在备份文件..."
            backupSelectedFiles(repository, selectedFiles, targetDir)
            
            // 同步选中的文件
            var successCount = 0
            selectedFiles.forEachIndexed { index, file ->
                try {
                    val relativePath = sourceDir.relativize(file)
                    val targetFile = targetDir.resolve(relativePath)
                    
                    // 创建目标目录
                    targetFile.parent.toFile().mkdirs()
                    
                    // 复制文件
                    Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                    
                    syncedFiles.add(relativePath.toString())
                    successCount++
                    
                    // 更新进度
                    indicator.fraction = (index + 1).toDouble() / selectedFiles.size
                    indicator.text = "正在同步: $relativePath (${index + 1}/${selectedFiles.size})"
                    
                } catch (e: Exception) {
                    val errorMessage = "同步文件失败: ${file}, 原因: ${e.message}"
                    NotificationUtils.showError(project, "同步失败", errorMessage)
                    throw SyncException.FileException(errorMessage, e)
                }
            }
            
            // 显示成功通知
            val duration = System.currentTimeMillis() - startTime
            NotificationUtils.showInfo(
                project,
                "同步成功",
                """
                已同步 $successCount 个文件
                耗时: ${duration / 1000.0} 秒
                """.trimIndent()
            )
            
        } catch (e: Exception) {
            val errorMessage = "同步失败: ${e.message}"
            NotificationUtils.showError(project, "同步失败", errorMessage)
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
                
                init {
                    title = "选择要同步的文件"
                    init()
                    
                    // 添加文件列表
                    availableFiles.forEach { file ->
                        val relativePath = sourceDir.relativize(file).toString()
                        checkBoxList.addItem(relativePath, relativePath, true)  // 默认全选
                    }
                }
                
                override fun createCenterPanel(): JComponent {
                    val scrollPane = JBScrollPane(checkBoxList)
                    scrollPane.preferredSize = Dimension(400, 300)
                    return scrollPane
                }
                
                fun getSelectedFiles(): List<Path> {
                    return checkBoxList.selectedIndices.map { index ->
                        availableFiles[index]
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
            NotificationUtils.showError(project, "备份失败", errorMessage)
            throw SyncException.FileException(errorMessage, e)
        }
    }

    private fun findFilesToSync(sourceDir: Path, repository: Repository): List<Path> {
        return FilePatternMatcher.findMatchingFiles(
            sourceDir,
            repository.filePatterns,
            repository.excludePatterns
        )
    }

    private fun executePostSyncCommands(repository: Repository) {
        repository.postSyncCommands.forEach { command ->
            try {
                indicator.text = "执行命令: ${command.command}"
                
                val projectDir = project.basePath?.let { File(it) }
                    ?: throw RuntimeException("无法获取项目目录")
                    
                val workingDir = if (command.directory.startsWith("/")) {
                    File(command.directory)
                } else {
                    File(projectDir, command.directory)
                }
                
                if (!workingDir.exists()) {
                    throw RuntimeException("命令执行目录不存在: ${workingDir.absolutePath}")
                }
                
                val result = CommandExecutor.execute(command.command, workingDir).get()
                
                if (result.exitCode != 0) {
                    throw RuntimeException(
                        """
                        命令执行失败:
                        Exit Code: ${result.exitCode}
                        Error: ${result.error}
                        Output: ${result.output}
                        """.trimIndent()
                    )
                }
                
                // 记录命令输出
                if (result.output.isNotBlank()) {
                    indicator.text = "命令输出:\n${result.output}"
                }
                
            } catch (e: Exception) {
                throw SyncException.CommandException(
                    "执行命令失败: ${command.command}",
                    e
                )
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
} 