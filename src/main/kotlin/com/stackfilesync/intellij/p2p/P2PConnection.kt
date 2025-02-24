package com.stackfilesync.intellij.p2p

import com.stackfilesync.intellij.model.P2PNode
import com.stackfilesync.intellij.model.P2PTransferRecord
import com.stackfilesync.intellij.model.TransferStatus
import java.io.*
import java.net.Socket
import java.net.InetSocketAddress
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture
import com.intellij.openapi.diagnostic.Logger

class P2PConnection(
    val remoteNode: P2PNode,
    private val receiveDir: File = File(System.getProperty("java.io.tmpdir"), "p2p-received")
) {
    private val LOG = Logger.getInstance(P2PConnection::class.java)
    
    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: DataInputStream? = null
    private val bufferSize = 8192
    private var progressListener: TransferProgressListener? = null
    
    init {
        // 确保接收目录存在
        receiveDir.mkdirs()
    }
    
    @Synchronized
    fun connect() {
        try {
            if (socket?.isConnected == true) {
                return
            }
            
            LOG.info("正在连接到节点: ${remoteNode.address}:${remoteNode.port}")
            socket = Socket()
            socket?.connect(InetSocketAddress(remoteNode.address, remoteNode.port), 5000)
            socket?.keepAlive = true  // 启用 TCP keepalive
            socket?.soTimeout = 30000 // 设置读取超时为30秒
            
            outputStream = DataOutputStream(BufferedOutputStream(socket?.getOutputStream()))
            inputStream = DataInputStream(BufferedInputStream(socket?.getInputStream()))
            LOG.info("成功连接到节点")
        } catch (e: Exception) {
            LOG.error("连接失败", e)
            close()
            throw IOException("连接到节点失败: ${e.message}")
        }
    }
    
    @Synchronized
    fun initWithSocket(socket: Socket) {
        try {
            this.socket = socket
            socket.keepAlive = true
            socket.soTimeout = 30000
            outputStream = DataOutputStream(BufferedOutputStream(socket.getOutputStream()))
            inputStream = DataInputStream(BufferedInputStream(socket.getInputStream()))
            LOG.info("成功初始化连接: ${socket.remoteSocketAddress}")
        } catch (e: Exception) {
            LOG.error("初始化连接失败", e)
            close()
            throw IOException("初始化连接失败: ${e.message}")
        }
    }
    
    fun setProgressListener(listener: TransferProgressListener?) {
        this.progressListener = listener
    }
    
    @Synchronized
    fun sendFile(filePath: String): P2PTransferRecord {
        LOG.info("开始发送文件: $filePath")
        val file = File(filePath)
        if (!file.exists()) {
            LOG.error("文件不存在: $filePath")
            throw IllegalArgumentException("文件不存在: $filePath")
        }
        
        // 确保连接已建立
        if (socket == null || !socket!!.isConnected) {
            connect()
        }
        
        val record = P2PTransferRecord(
            id = UUID.randomUUID().toString(),
            fromAddress = socket?.localAddress?.hostAddress ?: "",
            toAddress = remoteNode.address,
            fileName = file.name,
            fileSize = file.length(),
            timestamp = LocalDateTime.now(),
            status = TransferStatus.PENDING
        )
        
        try {
            val out = outputStream ?: throw IOException("连接已关闭")
            val input = inputStream ?: throw IOException("连接已关闭")
            
            LOG.info("发送文件元信息: ${record.fileName}, 大小: ${record.fileSize}")
            
            synchronized(this) {  // 使用 this 作为同步对象
                try {
                    // 发送文件元信息
                    out.writeUTF(record.id)
                    out.writeUTF(record.fileName)
                    out.writeLong(record.fileSize)
                    out.flush()
                    
                    // 等待接收方响应
                    var response: String? = null
                    val startTime = System.currentTimeMillis()
                    while (response == null) {
                        if (System.currentTimeMillis() - startTime > 5000) {
                            throw IOException("等待响应超时")
                        }
                        if (input.available() > 0) {
                            response = input.readUTF()
                        }
                        Thread.sleep(100)
                    }
                    
                    LOG.info("收到接收方响应: $response")
                    if (response != "ACCEPT") {
                        LOG.error("接收方拒绝接收文件")
                        throw IOException("接收方拒绝接收文件")
                    }
                    
                    // 发送文件内容
                    LOG.info("开始传输文件内容")
                    FileInputStream(file).use { fis ->
                        val buffer = ByteArray(bufferSize)
                        var totalSent: Long = 0
                        var bytesRead: Int
                        
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            out.write(buffer, 0, bytesRead)
                            totalSent += bytesRead
                            progressListener?.onProgress(totalSent, record.fileSize)
                            out.flush()  // 每次写入后都刷新
                        }
                    }
                    
                    // 等待完成确认
                    response = null
                    val confirmStartTime = System.currentTimeMillis()
                    while (response == null) {
                        if (System.currentTimeMillis() - confirmStartTime > 5000) {
                            throw IOException("等待完成确认超时")
                        }
                        if (input.available() > 0) {
                            response = input.readUTF()
                        }
                        Thread.sleep(100)
                    }
                    
                    LOG.info("收到完成确认: $response")
                    if (response != "COMPLETE") {
                        throw IOException("文件传输未完成")
                    }
                } catch (e: Exception) {
                    LOG.error("传输过程中发生错误", e)
                    throw e
                }
            }
            
            progressListener?.onComplete()
            return record.copy(status = TransferStatus.COMPLETED)
            
        } catch (e: Exception) {
            LOG.error("发送文件失败", e)
            progressListener?.onError(e.message ?: "未知错误")
            close()  // 发生错误时关闭连接
            throw e
        }
    }
    
    fun receiveFile(): CompletableFuture<P2PTransferRecord> {
        LOG.info("准备接收文件")
        val future = CompletableFuture<P2PTransferRecord>()
        
        try {
            // 读取文件元信息
            val fileId = inputStream?.readUTF() ?: throw IOException("连接已关闭")
            val fileName = inputStream?.readUTF() ?: throw IOException("连接已关闭")
            val fileSize = inputStream?.readLong() ?: throw IOException("连接已关闭")
            
            LOG.info("收到文件元信息: $fileName, 大小: $fileSize")
            
            val record = P2PTransferRecord(
                id = fileId,
                fromAddress = socket?.remoteSocketAddress?.toString() ?: "",
                toAddress = socket?.localAddress?.hostAddress ?: "",
                fileName = fileName,
                fileSize = fileSize,
                timestamp = LocalDateTime.now(),
                status = TransferStatus.PENDING
            )
            
            // 检查目标目录是否可写
            if (!receiveDir.exists() && !receiveDir.mkdirs()) {
                LOG.error("无法创建接收目录: ${receiveDir.absolutePath}")
                throw IOException("无法创建接收目录")
            }
            
            if (!receiveDir.canWrite()) {
                LOG.error("接收目录无写入权限: ${receiveDir.absolutePath}")
                throw IOException("接收目录无写入权限")
            }
            
            val targetFile = File(receiveDir, fileName)
            if (targetFile.exists() && !targetFile.canWrite()) {
                LOG.error("目标文件已存在且无法写入: ${targetFile.absolutePath}")
                throw IOException("目标文件已存在且无法写入")
            }
            
            LOG.info("发送接受确认")
            outputStream?.writeUTF("ACCEPT")
            outputStream?.flush()
            
            // 接收文件内容
            LOG.info("开始接收文件内容")
            FileOutputStream(targetFile).use { fos ->
                val buffer = ByteArray(bufferSize)
                var remainingBytes = fileSize
                var totalReceived: Long = 0
                
                while (remainingBytes > 0) {
                    val bytesRead = inputStream?.read(buffer, 0, minOf(bufferSize, remainingBytes.toInt())) ?: -1
                    if (bytesRead == -1) {
                        throw IOException("文件传输中断")
                    }
                    fos.write(buffer, 0, bytesRead)
                    remainingBytes -= bytesRead
                    totalReceived += bytesRead
                    progressListener?.onProgress(totalReceived, fileSize)
                }
                fos.flush()
            }
            
            LOG.info("发送完成确认")
            outputStream?.writeUTF("COMPLETE")
            outputStream?.flush()
            
            progressListener?.onComplete()
            future.complete(record.copy(status = TransferStatus.COMPLETED))
        } catch (e: Exception) {
            LOG.error("接收文件失败", e)
            progressListener?.onError(e.message ?: "未知错误")
            future.completeExceptionally(e)
        }
        
        return future
    }
    
    @Synchronized
    fun close() {
        LOG.info("关闭连接")
        try {
            outputStream?.flush()
            outputStream?.close()
            inputStream?.close()
            socket?.close()
        } catch (e: Exception) {
            LOG.error("关闭连接时发生错误", e)
        } finally {
            outputStream = null
            inputStream = null
            socket = null
        }
    }

    // 获取接收目录
    fun getReceiveDir(): File = receiveDir
} 