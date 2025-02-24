package com.stackfilesync.intellij.p2p

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.stackfilesync.intellij.model.P2PTransferRecord
import com.stackfilesync.intellij.model.TransferStatus
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

@Service
class P2PTransferService(private val project: Project) {
    private val connectionManager = P2PConnectionManager(project)
    private val transferRecords = mutableListOf<P2PTransferRecord>()
    
    // 发送文件
    fun sendFile(nodeId: String, file: File): P2PTransferRecord {
        val record = P2PTransferRecord(
            id = UUID.randomUUID().toString(),
            fromAddress = connectionManager.getLocalNode()?.address ?: "",
            toAddress = connectionManager.getConnectedNodes()
                .find { it.id == nodeId }?.address ?: "",
            fileName = file.name,
            fileSize = file.length(),
            timestamp = LocalDateTime.now(),
            status = TransferStatus.PENDING
        )
        
        transferRecords.add(record)
        
        // 开始传输
        connectionManager.sendFile(nodeId, file.absolutePath)
        
        return record
    }
    
    // 获取传输记录
    fun getTransferRecords(): List<P2PTransferRecord> = transferRecords
    
    // 获取传输状态
    fun getTransferStatus(recordId: String): TransferStatus {
        return transferRecords.find { it.id == recordId }?.status 
            ?: TransferStatus.FAILED
    }
} 