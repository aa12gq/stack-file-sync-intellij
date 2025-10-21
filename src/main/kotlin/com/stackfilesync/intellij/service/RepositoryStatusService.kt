package com.stackfilesync.intellij.service

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.model.RepositoryStatus
import com.stackfilesync.intellij.model.RepositoryStatusInfo
import com.stackfilesync.intellij.logs.LogService
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * 仓库状态监控服务
 * 负责实时检查和更新仓库的同步状态
 */
@Service(Service.Level.PROJECT)
class RepositoryStatusService(private val project: Project) : Disposable {

    companion object {
        private const val STATUS_CHECK_INTERVAL = 30L // 秒
        private const val INITIAL_DELAY = 5L // 秒
    }

    private val statusMap = ConcurrentHashMap<String, RepositoryStatusInfo>()
    private val syncingRepositories = ConcurrentHashMap.newKeySet<String>()
    private val executor = Executors.newScheduledThreadPool(2)
    private var scheduledFuture: ScheduledFuture<*>? = null
    private val statusChangeListeners = mutableListOf<(String, RepositoryStatus) -> Unit>()
    private val logService = LogService.getInstance(project)

    init {
        startMonitoring()
    }

    /**
     * 获取仓库状态
     */
    fun getStatus(repository: Repository): RepositoryStatus {
        return statusMap.getOrDefault(repository.url, RepositoryStatusInfo(RepositoryStatus.UNKNOWN)).status
    }

    /**
     * 获取仓库状态信息（包含详细错误信息）
     */
    fun getStatusInfo(repository: Repository): RepositoryStatusInfo {
        return statusMap.getOrDefault(repository.url, RepositoryStatusInfo(RepositoryStatus.UNKNOWN))
    }

    /**
     * 获取仓库状态（通过URL）
     */
    fun getStatusByUrl(url: String): RepositoryStatus {
        return statusMap.getOrDefault(url, RepositoryStatusInfo(RepositoryStatus.UNKNOWN)).status
    }

    /**
     * 标记仓库为同步中
     */
    fun markAsSyncing(repository: Repository) {
        syncingRepositories.add(repository.url)
        updateStatus(repository.url, RepositoryStatusInfo(RepositoryStatus.SYNCING))
    }

    /**
     * 标记仓库同步完成
     */
    fun markSyncComplete(repository: Repository) {
        syncingRepositories.remove(repository.url)
        // 立即检查状态
        executor.submit {
            checkRepositoryStatus(repository)
        }
    }

    /**
     * 检查所有仓库状态
     */
    fun checkAllRepositories(repositories: List<Repository>) {
        executor.submit {
            repositories.forEach { repo ->
                if (!syncingRepositories.contains(repo.url)) {
                    checkRepositoryStatus(repo)
                }
            }
        }
    }

    /**
     * 添加状态变化监听器
     */
    fun addStatusChangeListener(listener: (String, RepositoryStatus) -> Unit) {
        statusChangeListeners.add(listener)
    }

    /**
     * 移除状态变化监听器
     */
    fun removeStatusChangeListener(listener: (String, RepositoryStatus) -> Unit) {
        statusChangeListeners.remove(listener)
    }

    /**
     * 开始监控
     */
    private fun startMonitoring() {
        scheduledFuture = executor.scheduleAtFixedRate(
            {
                try {
                    val settings = com.stackfilesync.intellij.settings.SyncSettingsState.getInstance()
                    val repositories = settings.getRepositories()

                    repositories.forEach { repo ->
                        if (!syncingRepositories.contains(repo.url)) {
                            checkRepositoryStatus(repo)
                        }
                    }
                } catch (e: Exception) {
                    // 静默处理异常，避免日志过多
                }
            },
            INITIAL_DELAY,
            STATUS_CHECK_INTERVAL,
            TimeUnit.SECONDS
        )
    }

    /**
     * 检查单个仓库状态
     */
    private fun checkRepositoryStatus(repository: Repository) {
        try {
            val statusInfo = performStatusCheck(repository)
            updateStatus(repository.url, statusInfo)
        } catch (e: Exception) {
            updateStatus(repository.url, RepositoryStatusInfo(
                RepositoryStatus.UNAVAILABLE,
                "检查状态时发生异常: ${e.message}"
            ))
        }
    }

    /**
     * 执行状态检查逻辑
     */
    private fun performStatusCheck(repository: Repository): RepositoryStatusInfo {
        // 1. 检查基本配置
        if (repository.url.isBlank()) {
            return RepositoryStatusInfo(RepositoryStatus.UNAVAILABLE, "仓库URL未配置")
        }

        if (repository.targetDirectory.isBlank()) {
            return RepositoryStatusInfo(RepositoryStatus.UNAVAILABLE, "目标目录未配置")
        }

        // 2. 检查目标目录是否存在（如果不存在会在同步时创建，所以只是警告）
        val targetDir = File(repository.targetDirectory)
        if (!targetDir.exists()) {
            logService.appendLog("目标目录不存在，将在同步时创建: ${repository.targetDirectory}")
        }

        // 3. 检查远程 Git 仓库的可访问性
        // 注意：目标目录不需要是 Git 仓库，它只是文件同步的目标位置
        // 真正的 Git 仓库是通过 repository.url 指定的远程仓库
        val remoteCheckResult = checkGitRemoteUrl(repository)

        return if (remoteCheckResult.first) {
            RepositoryStatusInfo(RepositoryStatus.READY, "仓库就绪，可以同步")
        } else {
            RepositoryStatusInfo(RepositoryStatus.UNAVAILABLE, remoteCheckResult.second)
        }
    }

    /**
     * 检查 Git 远程仓库 URL 的可访问性
     * 使用 git ls-remote 命令检查远程仓库
     * @return Pair<是否成功, 错误信息>
     */
    private fun checkGitRemoteUrl(repository: Repository): Pair<Boolean, String> {
        return try {
            // 使用 git ls-remote 检查远程仓库是否可访问
            // 这个命令会尝试连接远程仓库并列出引用，但不会下载任何内容
            val command = mutableListOf("git", "ls-remote", "--heads")

            // 如果是 HTTPS 且有凭证，需要特殊处理
            val urlToCheck = if (repository.repoType == "HTTPS" &&
                                 !repository.username.isNullOrBlank() &&
                                 !repository.password.isNullOrBlank()) {
                // 在 URL 中嵌入凭证
                repository.url.replace(
                    "https://",
                    "https://${repository.username}:${repository.password}@"
                )
            } else {
                repository.url
            }

            command.add(urlToCheck)

            // 如果指定了分支，只检查该分支
            if (repository.branch.isNotBlank()) {
                command.add("refs/heads/${repository.branch}")
            }

            val process = ProcessBuilder()
                .command(command)
                .redirectErrorStream(true)
                .start()

            val completed = process.waitFor(10, TimeUnit.SECONDS)

            if (!completed) {
                process.destroyForcibly()
                return Pair(false, "检查远程仓库超时（10秒），可能是网络问题或仓库地址错误")
            }

            val exitCode = process.exitValue()
            if (exitCode != 0) {
                val errorOutput = process.inputStream.bufferedReader().readText().trim()
                return when {
                    errorOutput.contains("Authentication failed") ||
                    errorOutput.contains("could not read Username") ||
                    errorOutput.contains("could not read Password") -> {
                        Pair(false, "认证失败：请检查用户名和密码是否正确")
                    }
                    errorOutput.contains("Could not resolve host") -> {
                        Pair(false, "无法解析主机：请检查网络连接和仓库地址")
                    }
                    errorOutput.contains("Repository not found") -> {
                        Pair(false, "仓库不存在：请检查仓库地址是否正确")
                    }
                    else -> {
                        Pair(false, "无法访问远程仓库: $errorOutput")
                    }
                }
            }

            // 检查输出是否包含分支信息
            val output = process.inputStream.bufferedReader().readText().trim()

            // 如果指定了分支，验证分支是否存在
            if (repository.branch.isNotBlank()) {
                if (output.isEmpty() || !output.contains(repository.branch)) {
                    return Pair(false, "远程仓库中不存在分支: ${repository.branch}")
                }
            }

            Pair(true, "")

        } catch (e: Exception) {
            Pair(false, "检查远程仓库时发生异常: ${e.message}")
        }
    }

    /**
     * 更新状态并通知监听器
     */
    private fun updateStatus(url: String, newStatusInfo: RepositoryStatusInfo) {
        val oldStatusInfo = statusMap.put(url, newStatusInfo)
        val oldStatus = oldStatusInfo?.status
        val newStatus = newStatusInfo.status

        if (oldStatus != newStatus) {
            val logMessage = buildString {
                append("仓库状态变更 [$url]: $oldStatus -> $newStatus")
                if (!newStatusInfo.detailMessage.isNullOrBlank()) {
                    append(" (${newStatusInfo.detailMessage})")
                }
            }
            logService.appendLog(logMessage)

            // 通知所有监听器
            statusChangeListeners.forEach { listener ->
                try {
                    listener(url, newStatus)
                } catch (e: Exception) {
                    logService.appendLog("状态变更监听器执行失败: ${e.message}")
                }
            }
        }
    }

    override fun dispose() {
        logService.appendLog("关闭仓库状态监控服务")

        scheduledFuture?.cancel(true)
        executor.shutdown()

        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
            Thread.currentThread().interrupt()
        }

        statusMap.clear()
        syncingRepositories.clear()
        statusChangeListeners.clear()
    }
}
