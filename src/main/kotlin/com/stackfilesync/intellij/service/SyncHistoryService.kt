package com.stackfilesync.intellij.service

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil
import java.util.*

@State(
    name = "StackFileSyncHistory",
    storages = [Storage("stackFileSyncHistory.xml")]
)
class SyncHistoryService : PersistentStateComponent<SyncHistoryService.State> {
    
    data class State(
        var historyItems: MutableList<HistoryItem> = mutableListOf()
    )
    
    data class HistoryItem(
        var id: String = UUID.randomUUID().toString(),
        var timestamp: Long = 0,
        var repository: String = "",
        var branch: String = "",
        var success: Boolean = false,
        var error: String? = null,
        var fileCount: Int = 0,
        var duration: Long = 0,
        var syncType: String = "manual" // "manual" 或 "auto"
    )
    
    private var myState = State()
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }
    
    fun addHistoryItem(item: HistoryItem) {
        myState.historyItems.add(0, item) // 添加到列表开头
        // 保留最近1000条记录
        if (myState.historyItems.size > 1000) {
            myState.historyItems = myState.historyItems.take(1000).toMutableList()
        }
    }
    
    fun getHistoryItems(): List<HistoryItem> = myState.historyItems.toList()
    
    fun clearHistory() {
        myState.historyItems.clear()
    }
    
    companion object {
        fun getInstance(): SyncHistoryService = service()
    }
} 