package com.stackfilesync.intellij.settings

import com.intellij.openapi.components.*
import com.stackfilesync.intellij.model.Repository
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.messages.Topic
import com.intellij.openapi.application.ApplicationManager

@State(
    name = "StackFileSyncSettings",
    storages = [Storage("stackFileSync.xml")]
)
class SyncSettingsState : PersistentStateComponent<SyncSettingsState> {
    @Tag("repositories")
    @XCollection(style = XCollection.Style.v2)
    var repositoryList: MutableList<Repository> = mutableListOf()

    @Tag("backupEnabled")
    var backupEnabledFlag: Boolean = true

    // 添加方法以支持保存和加载最近的文件选择
    private val recentFileSelections = mutableListOf<String>()
    private val MAX_RECENT_SELECTIONS = 10

    override fun getState(): SyncSettingsState = this

    override fun loadState(state: SyncSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun getRepositories(): List<Repository> = repositoryList.toList()

    fun setRepositories(repositories: List<Repository>) {
        repositoryList.clear()
        repositoryList.addAll(repositories.map { it.copy() })
        // 发布设置变更事件
        ApplicationManager.getApplication().messageBus
            .syncPublisher(SETTINGS_CHANGED)
            .settingsChanged()
    }

    fun getBackupEnabled(): Boolean = backupEnabledFlag

    fun setBackupEnabled(enabled: Boolean) {
        backupEnabledFlag = enabled
    }

    fun getRecentFileSelections(): List<String> {
        return recentFileSelections.toList()
    }

    fun addRecentFileSelection(pattern: String) {
        // 如果模式已存在，先移除
        recentFileSelections.remove(pattern)
        
        // 添加到列表头部
        recentFileSelections.add(0, pattern)
        
        // 限制列表大小
        while (recentFileSelections.size > MAX_RECENT_SELECTIONS) {
            recentFileSelections.removeAt(recentFileSelections.size - 1)
        }
    }

    companion object {
        fun getInstance(): SyncSettingsState = service()
        
        // 定义设置变更事件
        val SETTINGS_CHANGED = Topic.create("StackFileSyncSettingsChanged", SettingsChangeListener::class.java)
    }
    
    // 设置变更监听器接口
    interface SettingsChangeListener {
        fun settingsChanged()
    }
} 