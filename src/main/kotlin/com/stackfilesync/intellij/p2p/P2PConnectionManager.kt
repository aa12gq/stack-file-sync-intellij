package com.stackfilesync.intellij.p2p

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.stackfilesync.intellij.model.P2PNode
import com.stackfilesync.intellij.model.P2PTransferRecord
import com.stackfilesync.intellij.model.P2PNodeConfig
import com.stackfilesync.intellij.model.TransferStatus
import com.stackfilesync.intellij.service.P2PNodeConfigService
import com.stackfilesync.intellij.utils.NotificationUtils
import com.intellij.openapi.components.service
import com.intellij.openapi.application.invokeLater
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import com.intellij.openapi.diagnostic.Logger
import java.io.IOException
import java.util.UUID
import java.time.LocalDateTime

@Service
class P2PConnectionManager(private val project: Project) {
    private val executor = Executors.newCachedThreadPool()
    private val connections = ConcurrentHashMap<String, P2PConnection>()
    private val serverSockets = ConcurrentHashMap<Int, ServerSocket>()
    private val localNodes = ConcurrentHashMap<String, P2PNode>()
    private var isStarted = false
    private val LOG = Logger.getInstance(P2PConnectionManager::class.java)
    
    init {
        // 在初始化时启动已配置的节点
        startConfiguredNodes()
    }
    
    private fun startConfiguredNodes() {
        val configService = P2PNodeConfigService.getInstance(project)
        configService.getNodes().forEach { config: P2PNodeConfig ->
            if (config.enabled) {
                try {
                    startNode(config)
                } catch (e: Exception) {
                    NotificationUtils.showError(
                        project,
                        "节点启动失败",
                        "节点 ${config.name} (${config.address}:${config.port}) 启动失败: ${e.message}"
                    )
                }
            }
        }
    }
    
    companion object {
        // 用于测试的端口范围
        private val TEST_PORT_RANGE = 8001..8010
        private val testNodes = mutableListOf<P2PNode>()
        
        fun getInstance(project: Project): P2PConnectionManager = project.service()
        
        // 启动测试模式
        fun startTestMode() {
            testNodes.clear()
            // 找到可用的端口
            val availablePorts = TEST_PORT_RANGE.filter { port ->
                try {
                    ServerSocket(port).use { true }
                } catch (e: Exception) {
                    false
                }
            }
            
            // 使用找到的可用端口创建测试节点
            availablePorts.take(4).forEachIndexed { index, port ->
                testNodes.add(P2PNode(
                    id = "test-node-$index",
                    name = "测试节点 ${index + 1}",
                    address = "127.0.0.1",
                    port = port,
                    online = true
                ))
            }
        }
    }
    
    // 获取可用节点列表
    fun getAvailableNodes(): List<P2PNode> {
        val configService = P2PNodeConfigService.getInstance(project)
        val configuredNodes = configService.getNodes()
            .filter { it.enabled }
            .map { config: P2PNodeConfig ->
                P2PNode(
                    id = "${config.address}:${config.port}",
                    name = config.name,
                    address = config.address,
                    port = config.port,
                    online = true
                )
            }
        
        // 添加本地节点到列表中
        val allNodes = if (isStarted) {
            (testNodes.toList() + configuredNodes)
        } else {
            configuredNodes
        }
        
        return allNodes.distinctBy { "${it.address}:${it.port}" }
    }
    
    // 启动P2P服务
    @Synchronized
    fun start(testMode: Boolean = false) {
        // 如果服务已经启动，先停止它
        if (isStarted) {
            stop()
        }
        
        isStarted = true
        
        if (testMode) {
            startTestMode()
        }
        
        // 启动已配置的节点
        startConfiguredNodes()
    }
    
    // 连接到远程节点
    fun connectTo(node: P2PNode) {
        try {
            if (connections.containsKey(node.id)) {
                return
            }
            
            val connection = P2PConnection(node)
            connection.connect()
            connections[node.id] = connection
        } catch (e: Exception) {
            throw RuntimeException("连接到节点 ${node.name} (${node.address}:${node.port}) 失败: ${e.message}")
        }
    }
    
    // 发送文件
    fun sendFile(nodeId: String, filePath: String): P2PTransferRecord {
        val targetNode = getAvailableNodes()
            .find { "${it.address}:${it.port}" == nodeId }
            ?: throw IllegalArgumentException("找不到指定的节点")
        
        // 如果是发送到本地节点
        if (targetNode.address == "127.0.0.1" || targetNode.address == "localhost") {
            val configService = P2PNodeConfigService.getInstance(project)
            val nodeConfig = configService.getNodes()
                .find { it.port == targetNode.port }
                ?: throw IllegalStateException("找不到目标节点配置")
            
            // 直接复制文件到目标目录
            val sourceFile = File(filePath)
            val targetDir = File(nodeConfig.targetDirectory)
            val targetFile = File(targetDir, sourceFile.name)
            
            try {
                // 确保目标目录存在
                targetDir.mkdirs()
                
                // 检查文件是否符合过滤规则
                if (shouldAcceptFile(sourceFile.name, nodeConfig)) {
                    // 复制文件
                    sourceFile.copyTo(targetFile, overwrite = true)
                    
                    // 创建传输记录
                    val record = P2PTransferRecord(
                        id = UUID.randomUUID().toString(),
                        fromAddress = "127.0.0.1",
                        toAddress = "127.0.0.1",
                        fileName = sourceFile.name,
                        fileSize = sourceFile.length(),
                        timestamp = LocalDateTime.now(),
                        status = TransferStatus.COMPLETED
                    )
                    
                    // 通知成功
                    invokeLater {
                        NotificationUtils.showInfo(
                            project,
                            "文件传输完成",
                            """
                            文件已复制到本地目录:
                            ${sourceFile.name}
                            大小: ${formatFileSize(sourceFile.length())}
                            保存位置: ${nodeConfig.targetDirectory}
                            """.trimIndent()
                        )
                    }
                    
                    return record
                } else {
                    throw IOException("文件不符合接收规则")
                }
            } catch (e: Exception) {
                throw RuntimeException("本地传输失败: ${e.message}")
            }
        }
        
        // 如果是发送到远程节点，使用原有逻辑
        val connection = connections[nodeId] ?: run {
            connectTo(targetNode)
            connections[nodeId]
        } ?: throw IllegalStateException("无法连接到节点")
        
        return connection.sendFile(filePath)
    }
    
    // 获取本地节点信息
    fun getLocalNode(): P2PNode? = localNodes.values.firstOrNull()
    
    // 获取所有连接的节点
    fun getConnectedNodes(): List<P2PNode> {
        return connections.values.map { it.remoteNode }
    }
    
    // 停止服务
    @Synchronized
    fun stop() {
        isStarted = false
        
        LOG.info("正在停止所有节点...")
        
        // 关闭所有连接
        connections.values.forEach { 
            try {
                it.close()
            } catch (e: Exception) {
                LOG.error("关闭连接时发生错误", e)
            }
        }
        connections.clear()
        
        // 关闭所有服务器套接字
        serverSockets.values.forEach { 
            try {
                it.close()
                LOG.info("关闭端口 ${it.localPort} 的服务器socket")
            } catch (e: Exception) {
                LOG.error("关闭服务器socket时发生错误", e)
            }
        }
        serverSockets.clear()
        
        // 清理节点信息
        localNodes.clear()
        LOG.info("所有节点已停止")
    }
    
    // 检查服务是否已启动
    fun isServiceStarted(): Boolean = isStarted
    
    private fun handleNewConnection(socket: java.net.Socket) {
        executor.submit {
            try {
                // 创建一个临时的连接对象来处理这个新连接
                val tempConnection = P2PConnection(
                    P2PNode(
                        id = "temp-${System.currentTimeMillis()}",
                        name = "临时连接",
                        address = socket.inetAddress.hostAddress,
                        port = socket.port
                    )
                ).apply {
                    // 使用已有的socket
                    initWithSocket(socket)
                }

                // 开始接收文件
                tempConnection.receiveFile()
                    .thenAccept { record ->
                        // 通知用户文件接收完成
                        invokeLater {
                            NotificationUtils.showInfo(
                                project,
                                "文件接收完成",
                                """
                                从 ${record.fromAddress} 接收文件:
                                ${record.fileName}
                                大小: ${formatFileSize(record.fileSize)}
                                保存位置: ${tempConnection.getReceiveDir()}
                                """.trimIndent()
                            )
                        }
                    }
                    .exceptionally { e ->
                        // 处理接收错误
                        invokeLater {
                            NotificationUtils.showError(
                                project,
                                "文件接收失败",
                                "从 ${socket.inetAddress.hostAddress} 接收文件时发生错误: ${e.message}"
                            )
                        }
                        null
                    }
                    .whenComplete { _, _ ->
                        // 完成后关闭连接
                        tempConnection.close()
                    }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun formatFileSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = size.toDouble()
        var unit = 0
        while (value > 1024 && unit < units.size - 1) {
            value /= 1024
            unit++
        }
        return "%.2f %s".format(value, units[unit])
    }
    
    private fun getFreePort(): Int {
        ServerSocket(0).use { 
            return it.localPort
        }
    }
    
    private fun generateNodeId(): String {
        return java.util.UUID.randomUUID().toString()
    }
    
    @Synchronized
    fun startNode(config: P2PNodeConfig) {
        if (!config.enabled) return
        
        try {
            // 检查端口是否已被使用
            if (serverSockets.containsKey(config.port)) {
                LOG.info("节点 ${config.name} 已经在运行在端口 ${config.port}")
                return
            }
            
            // 尝试创建服务器socket
            val socket = try {
                ServerSocket(config.port).also { 
                    LOG.info("成功在端口 ${config.port} 上创建服务器socket")
                }
            } catch (e: Exception) {
                LOG.error("无法在端口 ${config.port} 上创建服务器socket", e)
                throw IOException("端口 ${config.port} 已被占用，请尝试其他端口")
            }
            
            serverSockets[config.port] = socket
            
            val node = P2PNode(
                id = generateNodeId(),
                name = config.name,
                address = config.address,
                port = config.port,
                online = true
            )
            localNodes[node.id] = node
            
            executor.submit {
                LOG.info("开始监听端口 ${config.port} 的连接")
                while (!socket.isClosed) {
                    try {
                        val client = socket.accept()
                        LOG.info("接收到来自 ${client.remoteSocketAddress} 的连接")
                        handleNewConnection(client, config)
                    } catch (e: Exception) {
                        if (!socket.isClosed) {
                            LOG.error("处理连接时发生错误", e)
                            e.printStackTrace()
                        }
                    }
                }
            }
            
            LOG.info("节点 ${config.name} 启动成功，监听地址: ${config.address}:${config.port}")
            
        } catch (e: Exception) {
            LOG.error("启动节点失败", e)
            // 清理可能已创建的资源
            serverSockets[config.port]?.close()
            serverSockets.remove(config.port)
            throw RuntimeException("启动节点失败: ${e.message}")
        }
    }
    
    private fun handleNewConnection(socket: Socket, config: P2PNodeConfig) {
        LOG.info("收到新连接: ${socket.remoteSocketAddress}")
        executor.submit {
            try {
                // 创建一个临时的连接对象来处理这个新连接
                val tempConnection = P2PConnection(
                    remoteNode = P2PNode(
                        id = "temp-${System.currentTimeMillis()}",
                        name = "临时连接",
                        address = socket.inetAddress.hostAddress,
                        port = socket.port
                    ),
                    receiveDir = File(config.targetDirectory)
                ).apply {
                    initWithSocket(socket)
                }

                LOG.info("开始接收文件，目标目录: ${config.targetDirectory}")
                tempConnection.receiveFile()
                    .thenAccept { record ->
                        LOG.info("文件接收完成: ${record.fileName}")
                        // 检查文件是否符合过滤规则
                        if (shouldAcceptFile(record.fileName, config)) {
                            // 通知用户文件接收完成
                            invokeLater {
                                NotificationUtils.showInfo(
                                    project,
                                    "文件接收完成",
                                    """
                                    从 ${record.fromAddress} 接收文件:
                                    ${record.fileName}
                                    大小: ${formatFileSize(record.fileSize)}
                                    保存位置: ${config.targetDirectory}
                                    """.trimIndent()
                                )
                            }
                        } else {
                            LOG.info("文件不符合过滤规则，已拒绝: ${record.fileName}")
                            // 文件不符合过滤规则，删除接收的文件
                            File(config.targetDirectory, record.fileName).delete()
                            invokeLater {
                                NotificationUtils.showWarning(
                                    project,
                                    "文件已拒绝",
                                    "文件 ${record.fileName} 不符合接收规则，已拒绝接收"
                                )
                            }
                        }
                    }
                    .exceptionally { e ->
                        LOG.error("文件接收失败", e)
                        // 处理接收错误
                        invokeLater {
                            NotificationUtils.showError(
                                project,
                                "文件接收失败",
                                "从 ${socket.inetAddress.hostAddress} 接收文件时发生错误: ${e.message}"
                            )
                        }
                        null
                    }
                    .whenComplete { _, _ ->
                        // 完成后关闭连接
                        tempConnection.close()
                    }

            } catch (e: Exception) {
                LOG.error("处理连接失败", e)
                e.printStackTrace()
                invokeLater {
                    NotificationUtils.showError(
                        project,
                        "连接处理失败",
                        "处理来自 ${socket.inetAddress.hostAddress} 的连接时发生错误: ${e.message}"
                    )
                }
            }
        }
    }

    // 检查文件是否符合过滤规则
    private fun shouldAcceptFile(fileName: String, config: P2PNodeConfig): Boolean {
        // 如果没有启用自动接收，总是返回true（让用户手动确认）
        if (!config.autoAccept) {
            return true
        }
        
        // 将通配符模式转换为正则表达式模式
        fun String.toFilePattern(): String {
            return this.trim()
                .replace(".", "\\.")
                .replace("**/", "(.*/)?")
                .replace("**", ".*")
                .replace("*", "[^/]*")
                .let { "^$it$" }
        }
        
        // 检查文件是否匹配任何排除模式
        if (config.excludePatterns.any { pattern ->
            try {
                fileName.matches(pattern.toFilePattern().toRegex())
            } catch (e: Exception) {
                false  // 如果模式无效，忽略它
            }
        }) {
            return false
        }
        
        // 如果没有指定包含模式，接受所有未被排除的文件
        if (config.filePatterns.isEmpty()) {
            return true
        }
        
        // 检查文件是否匹配任何包含模式
        return config.filePatterns.any { pattern ->
            try {
                fileName.matches(pattern.toFilePattern().toRegex())
            } catch (e: Exception) {
                false  // 如果模式无效，忽略它
            }
        }
    }
} 