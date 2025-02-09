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
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.StartupActivity

class AutoSyncStartupActivity : StartupActivity {
    override fun runActivity(project: Project) {
        // 确保在项目打开时初始化 AutoSyncManager
        project.getService(AutoSyncManager::class.java)
    }
}

@Service(Service.Level.PROJECT)
class AutoSyncManager(private val project: Project) {
    private val LOG = Logger.getInstance(AutoSyncManager::class.java)
    private val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val syncTasks = ConcurrentHashMap<String, ScheduledFuture<*>>()
    private val settings = SyncSettingsState.getInstance()
    
    init {
        LOG.info("初始化 AutoSyncManager")
        // 启动所有自动同步任务
        startAllAutoSync()
        
        // 注册设置变更监听器
        ApplicationManager.getApplication().messageBus.connect().subscribe(
            SyncSettingsState.SETTINGS_CHANGED,
            object : SyncSettingsState.SettingsChangeListener {
                override fun settingsChanged() {
                    LOG.info("收到设置变更事件")
                    ApplicationManager.getApplication().invokeLater {
                        startAllAutoSync()
                    }
                }
            }
        )
    }
    
    fun startAllAutoSync() {
        LOG.info("开始启动所有自动同步任务")
        // 停止所有现有任务
        stopAllAutoSync()
        
        // 获取配置
        val repositories = settings.getRepositories()
        LOG.info("获取到 ${repositories.size} 个仓库配置")
        
        // 启动新任务
        repositories.forEach { repo ->
            if (repo.autoSync?.enabled == true) {
                LOG.info("启动仓库 ${repo.name} 的自动同步，间隔: ${repo.autoSync?.interval ?: 300}秒")
                startAutoSync(repo)
            } else {
                LOG.info("仓库 ${repo.name} 未启用自动同步")
            }
        }
    }
    
    private fun startAutoSync(repository: Repository) {
        val interval = repository.autoSync?.interval?.toLong() ?: 300L // 默认5分钟
        
        val future = executor.scheduleWithFixedDelay({
            try {
                LOG.info("执行仓库 ${repository.name} 的自动同步任务")
                // 在后台执行同步
                ApplicationManager.getApplication().invokeLater {
                    ProgressManager.getInstance().run(object : Task.Backgroundable(
                        project,
                        "自动同步: ${repository.name}",
                        false
                    ) {
                        override fun run(indicator: com.intellij.openapi.progress.ProgressIndicator) {
                            LOG.info("开始同步仓库 ${repository.name}")
                            val syncManager = FileSyncManager(project, indicator)
                            syncManager.sync(repository, showFileSelection = false, isAutoSync = true)
                            LOG.info("完成同步仓库 ${repository.name}")
                        }
                    })
                }
            } catch (e: Exception) {
                LOG.error("自动同步仓库 ${repository.name} 时发生错误", e)
            }
        }, 0, interval, TimeUnit.SECONDS)  // 修改为立即开始第一次同步
        
        syncTasks[repository.name] = future
    }
    
    fun stopAutoSync(repository: Repository) {
        LOG.info("停止仓库 ${repository.name} 的自动同步")
        syncTasks[repository.name]?.let { future ->
            future.cancel(false)
            syncTasks.remove(repository.name)
        }
    }
    
    fun stopAllAutoSync() {
        LOG.info("停止所有自动同步任务")
        syncTasks.forEach { (name, future) ->
            LOG.info("停止仓库 $name 的自动同步")
            future.cancel(false)
        }
        syncTasks.clear()
    }
    
    fun dispose() {
        LOG.info("销毁 AutoSyncManager")
        stopAllAutoSync()
        executor.shutdown()
    }
    
    companion object {
        fun getInstance(project: Project): AutoSyncManager = project.service()
    }
} 