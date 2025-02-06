package com.stackfilesync.intellij.sync

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.settings.SyncSettingsState
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class AutoSyncManager(private val project: Project) {
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val syncTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()
    private val settings = SyncSettingsState.getInstance()
    
    init {
        // 启动所有自动同步任务
        startAllAutoSync()
    }
    
    fun startAllAutoSync() {
        // 停止所有现有任务
        stopAllAutoSync()
        
        // 获取配置
        val repositories = settings.getRepositories()
        
        // 启动新任务
        repositories.forEach { repo ->
            if (repo.autoSync?.enabled == true) {
                startAutoSync(repo)
            }
        }
    }
    
    private fun startAutoSync(repository: Repository) {
        val interval = repository.autoSync?.interval?.toLong() ?: 300L // 默认5分钟
        
        val future = executor.scheduleWithFixedDelay({
            try {
                // 在后台执行同步
                ProgressManager.getInstance().run(object : Task.Backgroundable(
                    project,
                    "自动同步: ${repository.name}",
                    false
                ) {
                    override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                        val syncManager = FileSyncManager(project, indicator)
                        syncManager.sync(repository)
                    }
                })
            } catch (e: Exception) {
                // 记录错误但不中断调度
                e.printStackTrace()
            }
        }, interval, interval, TimeUnit.SECONDS)
        
        syncTasks[repository.name] = future
    }
    
    fun stopAutoSync(repository: Repository) {
        syncTasks[repository.name]?.let { future ->
            future.cancel(false)
            syncTasks.remove(repository.name)
        }
    }
    
    fun stopAllAutoSync() {
        syncTasks.forEach { (_, future) ->
            future.cancel(false)
        }
        syncTasks.clear()
    }
    
    fun dispose() {
        stopAllAutoSync()
        executor.shutdown()
    }
    
    companion object {
        fun getInstance(project: Project): AutoSyncManager = project.service()
    }
} 