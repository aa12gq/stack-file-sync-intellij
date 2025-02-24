package com.stackfilesync.intellij.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.stackfilesync.intellij.model.P2PTransferRecord
import com.stackfilesync.intellij.p2p.P2PConnectionManager
import com.stackfilesync.intellij.p2p.P2PConnection
import com.stackfilesync.intellij.p2p.TransferProgressListener
import com.stackfilesync.intellij.utils.NotificationUtils
import java.io.File

@Service(Service.Level.PROJECT)
class P2PTransferService(private val project: Project) {
    private val connectionManager = P2PConnectionManager.getInstance(project)
    private val historyService = P2PTransferHistoryService.getInstance(project)
    private var progressListener: TransferProgressListener? = null
    
    fun setProgressListener(listener: TransferProgressListener) {
        this.progressListener = listener
    }
    
    fun sendFile(nodeId: String, file: File): P2PTransferRecord {
        try {
            // 连接到节点
            val node = connectionManager.getAvailableNodes()
                .find { it.id == nodeId }
                ?: throw IllegalArgumentException("找不到指定的节点")
                
            connectionManager.connectTo(node)
            
            // 发送文件
            val connection = P2PConnection(node)
            progressListener?.let { listener ->
                connection.setProgressListener(listener)
            }
            
            val record = connection.sendFile(file.absolutePath)
            
            // 保存传输记录
            historyService.addRecord(record)
            
            return record
        } catch (e: Exception) {
            progressListener?.onError(e.message ?: "未知错误")
            throw e
        }
    }
    
    companion object {
        fun getInstance(project: Project): P2PTransferService {
            return project.getService(P2PTransferService::class.java)
        }
    }
} 