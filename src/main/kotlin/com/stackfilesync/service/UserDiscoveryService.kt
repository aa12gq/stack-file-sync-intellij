package com.stackfilesync.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.notification.NotificationType
import com.google.gson.Gson
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.DatagramPacket
import com.intellij.ide.util.PropertiesComponent

@Service(Service.Level.PROJECT)
class UserDiscoveryService(private val project: Project) {
    
    // 用户发现开关
    private var discoveryEnabled = true
    
    // 存储已连接用户列表
    private val connectedUsers = mutableListOf<UserInfo>()
    
    // 用户ID和名称，可以从插件配置或系统信息中获取
    private var currentUser: UserInfo? = null
    
    // 监听器列表
    private val listeners = mutableListOf<UserDiscoveryListener>()
    
    // 存储网络发现的用户，使用Map便于快速更新
    private val networkUsers = mutableMapOf<String, UserInfo>()
    
    // 添加日志级别控制
    private val LOG_ENABLED = false // 或设置环境变量控制
    
    // 优化日志系统
    enum class LogLevel { DEBUG, INFO, WARN, ERROR }

    private val CURRENT_LOG_LEVEL = LogLevel.WARN // 调整这个级别来控制日志输出

    private fun log(level: LogLevel, message: String) {
        if (level.ordinal >= CURRENT_LOG_LEVEL.ordinal) {
            val prefix = when (level) {
                LogLevel.DEBUG -> "[DEBUG]"
                LogLevel.INFO -> "[INFO]"
                LogLevel.WARN -> "[WARN]"
                LogLevel.ERROR -> "[ERROR]"
            }
            println("$prefix $message")
        }
    }
    
    init {
        // 从项目配置中读取发现功能开关状态
        val properties = PropertiesComponent.getInstance(project)
        discoveryEnabled = properties.getBoolean(DISCOVERY_ENABLED_KEY, true)
        log(LogLevel.INFO, "用户发现功能状态: ${if (discoveryEnabled) "已启用" else "已禁用"}")
    }
    
    // 设置用户发现功能的开关
    fun setDiscoveryEnabled(enabled: Boolean) {
        if (discoveryEnabled != enabled) {
            discoveryEnabled = enabled
            // 保存设置以便下次启动时保持状态
            val properties = PropertiesComponent.getInstance(project)
            properties.setValue(DISCOVERY_ENABLED_KEY, enabled)
            
            log(LogLevel.INFO, "用户发现功能状态已变更为: ${if (enabled) "启用" else "禁用"}")
            
            if (enabled) {
                // 如果启用了功能，刷新用户列表
                refreshUserList()
            } else {
                // 如果禁用了功能，清空用户列表并通知UI更新
                connectedUsers.clear()
                networkUsers.clear()
                // 但保留当前用户
                currentUser?.let { connectedUsers.add(it) }
                notifyUserListUpdated()
            }
        }
    }
    
    // 获取当前发现功能的状态
    fun isDiscoveryEnabled(): Boolean {
        return discoveryEnabled
    }
    
    fun initialize(username: String, serverUrl: String) {
        currentUser = UserInfo(generateUniqueId(), username, serverUrl)
        log(LogLevel.INFO, "初始化当前用户: ${currentUser?.username}, ID: ${currentUser?.id}")
        
        // 如果功能启用，向服务器注册当前用户
        if (discoveryEnabled) {
            registerUserToServer()
        }
    }
    
    // 添加公共刷新方法，让UI组件可以主动调用
    fun refreshUserList() {
        if (discoveryEnabled) {
            fetchConnectedUsers()
        } else {
            log(LogLevel.INFO, "用户发现功能已禁用，跳过刷新用户列表")
        }
    }
    
    private fun registerUserToServer() {
        // 检查功能是否启用
        if (!discoveryEnabled) {
            log(LogLevel.INFO, "用户发现功能已禁用，跳过注册用户")
            return
        }
        
        // 实现向服务器注册用户的逻辑
        log(LogLevel.INFO, "正在注册用户: ${currentUser?.username}")
        
        // 注册成功后获取已连接用户列表
        fetchConnectedUsers()
    }
    
    private fun fetchConnectedUsers() {
        // 检查功能是否启用
        if (!discoveryEnabled) {
            log(LogLevel.INFO, "用户发现功能已禁用，跳过获取用户列表")
            return
        }
        
        log(LogLevel.INFO, "正在获取连接的用户列表")
        
        connectedUsers.clear()
        
        // 将当前用户添加到列表中
        currentUser?.let {
            connectedUsers.add(it)
        }
        
        // 添加从网络发现的用户
        connectedUsers.addAll(networkUsers.values)
        
        // 移除示例用户逻辑，只保留真实用户
        // 如果在开发调试环境下确实需要一些测试用户，可以通过配置开关来控制
        val properties = com.intellij.ide.util.PropertiesComponent.getInstance()
        val enableTestUsers = properties.getBoolean("stack.file.sync.enableTestUsers", false)
        
        if (enableTestUsers && networkUsers.isEmpty() && connectedUsers.size <= 1) {
            // 只在特定配置下才添加测试用户，默认关闭
            val testUserCount = 2 // 限制测试用户数量
            for (i in 1..testUserCount) {
                // 使用明确的编号，避免重名
                val testUserId = "test-${System.currentTimeMillis()}-$i"
                val testUser = UserInfo(
                    id = testUserId,
                    username = "测试用户-$i", 
                    serverUrl = "localhost"
                )
                if (testUser.id != currentUser?.id) {
                    connectedUsers.add(testUser)
                }
            }
        }
        
        log(LogLevel.INFO, "获取到 ${connectedUsers.size} 个用户")
        
        // 通知监听器用户列表已更新
        notifyUserListUpdated()
    }
    
    fun sendNotification(toUser: UserInfo, message: String) {
        // 检查功能是否启用
        if (!discoveryEnabled) {
            val notificationService = NotificationService.getInstance(project)
            notificationService.showNotification(
                "功能已禁用",
                "用户发现功能已禁用，无法发送通知",
                NotificationType.WARNING
            )
            return
        }
        
        log(LogLevel.INFO, "发送通知给 ${toUser.username}: $message")
        
        // 获取通知服务
        val notificationService = NotificationService.getInstance(project)
        
        // 如果是发送给自己的消息，直接在本地处理
        if (toUser.id == currentUser?.id) {
            notifyNotificationReceived(currentUser!!, message)
        } else {
            // 向远程用户发送消息
            sendRemoteNotification(toUser, message)
        }
        
        // 无论是否发给自己，都显示发送成功通知
        notificationService.showNotification(
            "通知已发送",
            "消息已成功发送给 ${toUser.username}",
            NotificationType.INFORMATION
        )
    }
    
    // 添加通知接收方法
    private fun notifyNotificationReceived(fromUser: UserInfo, message: String) {
        listeners.forEach { it.onNotificationReceived(fromUser, message) }
    }
    
    fun addListener(listener: Any) {
        if (listener is UserDiscoveryListener) {
            listeners.add(listener)
        }
    }
    
    fun removeListener(listener: UserDiscoveryListener) {
        listeners.remove(listener)
    }
    
    private fun notifyUserListUpdated() {
        log(LogLevel.INFO, "通知监听器用户列表已更新，监听器数量: ${listeners.size}")
        listeners.forEach { it.onUserListUpdated(connectedUsers) }
    }
    
    private fun generateUniqueId(): String {
        val timestamp = System.currentTimeMillis()
        val random = (1000..9999).random()
        return "$timestamp-$random"
    }
    
    // 获取当前用户信息
    fun getCurrentUser(): UserInfo? {
        return currentUser
    }
    
    // 添加或更新从网络发现的用户
    fun addOrUpdateNetworkUser(user: UserInfo) {
        // 检查功能是否启用
        if (!discoveryEnabled) {
            log(LogLevel.INFO, "用户发现功能已禁用，跳过添加网络用户")
            return
        }
        
        val isNewUser = !networkUsers.containsKey(user.id)
        networkUsers[user.id] = user
        
        // 更新连接用户列表
        refreshUserList()
        
        // 如果是新用户，显示通知
        if (isNewUser) {
            val notificationService = NotificationService.getInstance(project)
            notificationService.showNotification(
                "发现新用户",
                "用户 ${user.username} 已连接到网络",
                NotificationType.INFORMATION
            )
        }
    }
    
    // 向远程用户发送通知
    private fun sendRemoteNotification(toUser: UserInfo, message: String) {
        // 检查功能是否启用
        if (!discoveryEnabled) {
            log(LogLevel.INFO, "用户发现功能已禁用，跳过发送远程通知")
            return
        }
        
        try {
            val socket = DatagramSocket()
            val gson = Gson()
            val notificationData = gson.toJson(RemoteNotification(
                fromUserId = currentUser?.id ?: "",
                fromUsername = currentUser?.username ?: "",
                message = message,
                timestamp = System.currentTimeMillis()
            ))
            
            val data = notificationData.toByteArray()
            // todo: 这里有问题！在本地开发环境中，所有用户都是localhost
            // 但是端口可能不正确，或者消息接收服务没有正确监听
            val address = InetAddress.getByName(toUser.serverUrl)
            val packet = DatagramPacket(data, data.size, address, 8889)
            
            socket.send(packet)
            socket.close()
            
            log(LogLevel.INFO, "远程通知已发送到 ${toUser.username} (${toUser.serverUrl})")
        } catch (e: Exception) {
            log(LogLevel.ERROR, "发送远程通知失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // 处理接收到的通知
    fun handleReceivedNotification(fromUser: UserInfo, message: String) {
        // 检查功能是否启用
        if (!discoveryEnabled) {
            log(LogLevel.INFO, "用户发现功能已禁用，跳过处理接收到的通知")
            return
        }
        
        // 通知监听器收到了消息
        notifyNotificationReceived(fromUser, message)
    }
    
    fun sendSyncNotification(toUser: UserInfo, moduleName: String, remark: String = "") {
        // 检查功能是否启用
        if (!discoveryEnabled) {
            val notificationService = NotificationService.getInstance(project)
            notificationService.showNotification(
                "功能已禁用",
                "用户发现功能已禁用，无法发送同步通知",
                NotificationType.WARNING
            )
            return
        }
        
        log(LogLevel.INFO, "发送同步通知给 ${toUser.username}: 模块 $moduleName 需要同步${if (remark.isNotBlank()) "，备注: $remark" else ""}")
        
        val notificationService = NotificationService.getInstance(project)
        
        val messageId = "${System.currentTimeMillis()}-${(1000..9999).random()}"
        
        if (toUser.id == currentUser?.id) {
            notifySyncNotificationReceived(currentUser!!, moduleName, false, remark, messageId)
        } else {
            sendRemoteSyncNotification(toUser, moduleName, false, remark, messageId)
        }
        
        val remarkText = if (remark.isNotBlank()) "（备注: $remark）" else ""
        notificationService.showNotification(
            "同步通知已发送",
            "已通知 ${toUser.username} 同步模块 $moduleName$remarkText",
            NotificationType.INFORMATION
        )
    }
    
    private fun sendRemoteSyncNotification(toUser: UserInfo, moduleName: String, isBroadcast: Boolean = false, remark: String = "", messageId: String) {
        try {
            // 尝试3次发送
            var success = false
            var lastException: Exception? = null
            
            for (attempt in 1..3) {
                try {
                    val socket = DatagramSocket()
                    val gson = Gson()
                    val notificationData = gson.toJson(SyncNotification(
                        fromUserId = currentUser?.id ?: "",
                        fromUsername = currentUser?.username ?: "",
                        moduleName = moduleName,
                        timestamp = System.currentTimeMillis(),
                        isBroadcast = isBroadcast,
                        remark = remark,
                        messageId = messageId
                    ))
                    
                    val data = notificationData.toByteArray()
                    val address = InetAddress.getByName(toUser.serverUrl)
                    
                    val port = 8890
                    val packet = DatagramPacket(data, data.size, address, port)
                    socket.send(packet)
                    log(LogLevel.INFO, "同步通知已发送到 ${toUser.username} (${toUser.serverUrl}:$port)")
                    
                    socket.close()
                    success = true
                    break
                } catch (e: Exception) {
                    lastException = e
                    log(LogLevel.ERROR, "发送同步通知失败(尝试 $attempt/3): ${e.message}")
                    if (attempt < 3) {
                        Thread.sleep(100) // 稍等一下再重试
                    }
                }
            }
            
            if (!success && lastException != null) {
                throw lastException
            }
        } catch (e: Exception) {
            log(LogLevel.ERROR, "发送同步通知失败: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    fun notifySyncNotificationReceived(fromUser: UserInfo, moduleName: String, isBroadcast: Boolean = false, remark: String = "", messageId: String = "") {
        log(LogLevel.INFO, "处理同步通知: 从 ${fromUser.username}, 模块: $moduleName, 广播: $isBroadcast, 消息ID: $messageId")
        
        listeners.forEach { 
            if (it is SyncNotificationListener) {
                try {
                    it.onSyncNotificationReceived(fromUser, moduleName, isBroadcast, remark, messageId)
                    log(LogLevel.INFO, "通知监听器: ${it.javaClass.simpleName} 成功处理同步通知")
                } catch (e: Exception) {
                    log(LogLevel.ERROR, "监听器处理同步通知时出错: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
    
    fun sendSyncResponse(toUser: UserInfo, moduleName: String, accepted: Boolean) {
        try {
            val socket = DatagramSocket()
            val gson = Gson()
            val action = if (accepted) "ACCEPTED" else "REJECTED"
            
            val notificationData = gson.toJson(SyncResponseNotification(
                fromUserId = currentUser?.id ?: "",
                fromUsername = currentUser?.username ?: "",
                toUserId = toUser.id,
                moduleName = moduleName,
                action = action,
                timestamp = System.currentTimeMillis()
            ))
            
            val data = notificationData.toByteArray()
            val address = InetAddress.getByName(toUser.serverUrl)
            val packet = DatagramPacket(data, data.size, address, 8891) // 使用新端口
            
            socket.send(packet)
            socket.close()
            
            log(LogLevel.INFO, "同步回执已发送到 ${toUser.username} (${toUser.serverUrl}): $action")
        } catch (e: Exception) {
            log(LogLevel.ERROR, "发送同步回执失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun notifySyncResponseReceived(fromUser: UserInfo, moduleName: String, accepted: Boolean) {
        listeners.forEach { 
            if (it is SyncResponseListener) {
                it.onSyncResponseReceived(fromUser, moduleName, accepted)
            }
        }
    }
    
    // 添加广播同步通知方法
    fun broadcastSyncNotification(moduleName: String, remark: String = "") {
        // 检查功能是否启用
        if (!discoveryEnabled) {
            val notificationService = NotificationService.getInstance(project)
            notificationService.showNotification(
                "功能已禁用",
                "用户发现功能已禁用，无法发送广播同步通知",
                NotificationType.WARNING
            )
            return
        }
        
        log(LogLevel.INFO, "广播同步通知: 模块 $moduleName 需要同步${if (remark.isNotBlank()) "，备注: $remark" else ""}")
        
        val notificationService = NotificationService.getInstance(project)
        
        // 获取除自己以外的所有连接用户
        val otherUsers = connectedUsers.filter { it.id != currentUser?.id }
        
        if (otherUsers.isEmpty()) {
            notificationService.showNotification(
                "广播同步通知",
                "没有其他用户可接收通知",
                NotificationType.WARNING
            )
            return
        }
        
        // 生成一个统一的消息ID用于广播
        val messageId = "${System.currentTimeMillis()}-${(1000..9999).random()}"
        
        // 向所有用户发送同步通知
        var successCount = 0
        otherUsers.forEach { user ->
            try {
                sendRemoteSyncNotification(user, moduleName, true, remark, messageId)
                successCount++
            } catch (e: Exception) {
                log(LogLevel.ERROR, "向用户 ${user.username} 发送广播失败: ${e.message}")
            }
        }
        
        val remarkText = if (remark.isNotBlank()) "（备注: $remark）" else ""
        // 显示发送结果
        notificationService.showNotification(
            "广播同步通知已发送",
            "已向 $successCount/${otherUsers.size} 个用户广播模块 $moduleName 的同步通知$remarkText",
            NotificationType.INFORMATION
        )
    }
    
    companion object {
        fun getInstance(project: Project): UserDiscoveryService = project.service()
        
        // 设置键名
        private const val DISCOVERY_ENABLED_KEY = "stack.file.sync.discoveryEnabled"
    }
}

data class UserInfo(
    val id: String,
    val username: String,
    val serverUrl: String
)

interface UserDiscoveryListener {
    fun onUserListUpdated(users: List<UserInfo>)
    fun onNotificationReceived(fromUser: UserInfo, message: String)
}

data class RemoteNotification(
    val fromUserId: String,
    val fromUsername: String,
    val message: String,
    val timestamp: Long
)

data class SyncNotification(
    val fromUserId: String,
    val fromUsername: String,
    val moduleName: String,
    val timestamp: Long,
    val isBroadcast: Boolean = false,
    val remark: String = "",
    val messageId: String = ""  // 添加唯一消息ID
)

data class SyncResponseNotification(
    val fromUserId: String,
    val fromUsername: String,
    val toUserId: String,
    val moduleName: String,
    val action: String, // "ACCEPTED" 或 "REJECTED"
    val timestamp: Long
)

interface SyncNotificationListener {
    fun onSyncNotificationReceived(
        fromUser: UserInfo, 
        moduleName: String, 
        isBroadcast: Boolean, 
        remark: String,
        messageId: String = ""
    )
} 