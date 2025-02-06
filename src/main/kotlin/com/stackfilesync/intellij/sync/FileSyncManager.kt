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
        // 克隆仓库
        indicator.text = "克隆仓库..."
        val gitDir = tempDir.resolve(repository.name)
        cloneRepository(repository, gitDir)

        // 切换分支
        indicator.text = "切换分支..."
        checkoutBranch(repository, gitDir)

        // 同步文件
        indicator.text = "同步文件..."
        syncFiles(repository, gitDir)
    }

    private fun cloneRepository(repository: Repository, gitDir: Path) {
        try {
            val handler = GitLineHandler(
                project,
                gitDir.toFile(),
                GitCommand.CLONE
            )
            handler.addParameters(
                "--depth", "1",
                "--filter=blob:none",
                "--no-checkout",
                repository.url,
                gitDir.toString()
            )
            
            git.runCommand(handler).throwOnError()
        } catch (e: Exception) {
            throw SyncException.GitException(
                "克隆仓库失败: ${repository.url}",
                e
            )
        }
    }

    private fun checkoutBranch(repository: Repository, gitDir: Path) {
        try {
            val handler = GitLineHandler(
                project,
                gitDir.toFile(),
                GitCommand.CHECKOUT
            )
            handler.addParameters(repository.branch)
            
            git.runCommand(handler).throwOnError()
        } catch (e: Exception) {
            throw SyncException.GitException(
                "切换分支失败: ${repository.branch}",
                e
            )
        }
    }

    private fun syncFiles(repository: Repository, sourcePath: Path) {
        try {
            val targetDir = getTargetDirectory(repository)
            val sourceDir = sourcePath.resolve(repository.sourceDirectory)
            
            // 获取要同步的文件
            val files = findFilesToSync(sourceDir, repository)
            indicator.isIndeterminate = false
            indicator.fraction = 0.0
            
            // 备份现有文件
            indicator.text = "备份文件..."
            val existingFiles = files.map { file ->
                val relativePath = sourceDir.relativize(file)
                targetDir.resolve(relativePath)
            }
            backupManager.backupFiles(repository, existingFiles)
            
            // 同步文件
            files.forEachIndexed { index, file ->
                val relativePath = sourceDir.relativize(file)
                val targetFile = targetDir.resolve(relativePath)
                
                // 创建目标目录
                targetFile.parent.toFile().mkdirs()
                
                // 复制文件
                Files.copy(file, targetFile, StandardCopyOption.REPLACE_EXISTING)
                
                syncedFiles.add(relativePath.toString())
                indicator.fraction = (index + 1).toDouble() / files.size
                indicator.text = "同步文件: $relativePath"
            }
            
            // 清理旧备份
            val maxBackups = repository.backupConfig?.maxBackups ?: 10
            backupManager.cleanOldBackups(maxBackups)
            
        } catch (e: Exception) {
            throw SyncException.FileException("同步文件失败", e)
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
} 