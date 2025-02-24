package com.stackfilesync.intellij.service

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection
import com.stackfilesync.intellij.model.P2PTransferRecord
import com.stackfilesync.intellij.model.TransferStatus
import java.time.LocalDateTime
import java.util.*

@Service
@State(
    name = "P2PTransferHistory",
    storages = [Storage("p2pTransferHistory.xml")]
)
class P2PTransferHistoryService(private val project: Project) : PersistentStateComponent<P2PTransferHistoryService.State> {
    
    @Tag("P2PTransferHistory")
    class State {
        @Property
        @XCollection(style = XCollection.Style.v2)
        var records: MutableList<TransferRecordState> = mutableListOf()
    }
    
    @Tag("TransferRecord")
    class TransferRecordState {
        @Property var id: String = ""
        @Property var fromAddress: String = ""
        @Property var toAddress: String = ""
        @Property var fileName: String = ""
        @Property var fileSize: Long = 0
        @Property var timestamp: String = ""  // 使用字符串存储时间
        @Property var status: String = ""     // 使用字符串存储状态
        
        fun toRecord(): P2PTransferRecord {
            return P2PTransferRecord(
                id = id,
                fromAddress = fromAddress,
                toAddress = toAddress,
                fileName = fileName,
                fileSize = fileSize,
                timestamp = LocalDateTime.parse(timestamp),
                status = TransferStatus.valueOf(status)
            )
        }
        
        companion object {
            fun fromRecord(record: P2PTransferRecord): TransferRecordState {
                return TransferRecordState().apply {
                    id = record.id
                    fromAddress = record.fromAddress
                    toAddress = record.toAddress
                    fileName = record.fileName
                    fileSize = record.fileSize
                    timestamp = record.timestamp.toString()
                    status = record.status.name
                }
            }
        }
    }
    
    private var myState = State()
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        myState = state
    }
    
    fun addRecord(record: P2PTransferRecord) {
        myState.records.add(TransferRecordState.fromRecord(record))
        // 保留最近100条记录
        if (myState.records.size > 100) {
            myState.records = myState.records.takeLast(100).toMutableList()
        }
    }
    
    fun getRecords(): List<P2PTransferRecord> {
        return myState.records.map { it.toRecord() }
    }
    
    companion object {
        fun getInstance(project: Project): P2PTransferHistoryService = project.service()
    }
} 