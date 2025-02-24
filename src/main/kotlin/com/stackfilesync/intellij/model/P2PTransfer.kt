package com.stackfilesync.intellij.model

import java.time.LocalDateTime

// P2P传输记录
data class P2PTransferRecord(
    val id: String,
    val fromAddress: String,
    val toAddress: String,
    val fileName: String,
    val fileSize: Long,
    val timestamp: LocalDateTime,
    val status: TransferStatus
)

// 传输状态
enum class TransferStatus {
    PENDING,    // 等待传输
    ACCEPTED,   // 已接受
    COMPLETED,  // 传输完成
    FAILED,     // 传输失败
    REJECTED    // 被拒绝
}

// P2P节点信息
data class P2PNode(
    val id: String,
    val name: String,
    val address: String,
    val port: Int,
    val online: Boolean = false
) 