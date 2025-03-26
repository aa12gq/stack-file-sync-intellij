package com.stackfilesync.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.notification.NotificationType
import com.google.gson.Gson
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.DatagramPacket

@Service(Service.Level.PROJECT)
class UserDiscoveryService(private val project: Project) {
    
    // 存储已连接用户列表
    private val connectedUsers = mutableListOf<UserInfo>()
    
    // 用户ID和名称，可以从插件配置或系统信息中获取
    private var currentUser: UserInfo? = null
    
    // 监听器列表
    private val listeners = mutableListOf<UserDiscoveryListener>()
    
    // 存储网络发现的用户，使用Map便于快速更新
    private val networkUsers = mutableMapOf<String, UserInfo>()
    
    fun initialize(username: String, serverUrl: String) {
        currentUser = UserInfo(generateUniqueId(), username, serverUrl)
        println("初始化当前用户: ${currentUser?.username}, ID: ${currentUser?.id}")
        
        // 向服务器注册当前用户
        registerUserToServer()
    }
    
    // 添加公共刷新方法，让UI组件可以主动调用
    fun refreshUserList() {
        fetchConnectedUsers()
    }
    
    private fun registerUserToServer() {
        // 实现向服务器注册用户的逻辑
        // 可以使用HTTP请求或WebSocket
        println("正在注册用户: ${currentUser?.username}")
        
        // 注册成功后获取已连接用户列表
        fetchConnectedUsers()
    }
    
    private fun fetchConnectedUsers() {
        println("正在获取连接的用户列表")
        
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
        
        println("获取到 ${connectedUsers.size} 个用户")
        
        // 通知监听器用户列表已更新
        notifyUserListUpdated()
    }
    
    fun sendNotification(toUser: UserInfo, message: String) {
        println("发送通知给 ${toUser.username}: $message")
        
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
        println("通知监听器用户列表已更新，监听器数量: ${listeners.size}")
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
            // 这里有问题！在本地开发环境中，所有用户都是localhost
            // 但是端口可能不正确，或者消息接收服务没有正确监听
            val address = InetAddress.getByName(toUser.serverUrl)
            val packet = DatagramPacket(data, data.size, address, 8889)
            
            socket.send(packet)
            socket.close()
            
            println("远程通知已发送到 ${toUser.username} (${toUser.serverUrl})")
        } catch (e: Exception) {
            println("发送远程通知失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    // 处理接收到的通知
    fun handleReceivedNotification(fromUser: UserInfo, message: String) {
        // 通知监听器收到了消息
        notifyNotificationReceived(fromUser, message)
    }
    
    fun sendSyncNotification(toUser: UserInfo, moduleName: String, remark: String = "") {
        println("发送同步通知给 ${toUser.username}: 模块 $moduleName 需要同步${if (remark.isNotBlank()) "，备注: $remark" else ""}")
        
        val notificationService = NotificationService.getInstance(project)
        
        if (toUser.id == currentUser?.id) {
            notifySyncNotificationReceived(currentUser!!, moduleName, false, remark)
        } else {
            sendRemoteSyncNotification(toUser, moduleName, false, remark)
        }
        
        val remarkText = if (remark.isNotBlank()) "（备注: $remark）" else ""
        notificationService.showNotification(
            "同步通知已发送",
            "已通知 ${toUser.username} 同步模块 $moduleName$remarkText",
            NotificationType.INFORMATION
        )
    }
    
    private fun sendRemoteSyncNotification(toUser: UserInfo, moduleName: String, isBroadcast: Boolean = false, remark: String = "") {
        try {
            val socket = DatagramSocket()
            val gson = Gson()
            val notificationData = gson.toJson(SyncNotification(
                fromUserId = currentUser?.id ?: "",
                fromUsername = currentUser?.username ?: "",
                moduleName = moduleName,
                timestamp = System.currentTimeMillis(),
                isBroadcast = isBroadcast,
                remark = remark
            ))
            
            val data = notificationData.toByteArray()
            val address = InetAddress.getByName(toUser.serverUrl)
            
            // 只使用一个固定的端口
            val port = 8890
            try {
                val packet = DatagramPacket(data, data.size, address, port)
                socket.send(packet)
                val broadcastText = if (isBroadcast) "(广播)" else ""
                println("同步通知${broadcastText}已发送到 ${toUser.username} (${toUser.serverUrl}:$port)")
            } catch (e: Exception) {
                println("发送到端口 $port 失败: ${e.message}")
                throw e
            } finally {
                socket.close()
            }
        } catch (e: Exception) {
            println("发送同步通知失败: ${e.message}")
            e.printStackTrace()
            throw e
        }
    }
    
    fun notifySyncNotificationReceived(fromUser: UserInfo, moduleName: String, isBroadcast: Boolean = false, remark: String = "") {
        listeners.forEach { 
            if (it is SyncNotificationListener) {
                it.onSyncNotificationReceived(fromUser, moduleName, isBroadcast, remark)
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
            
            println("同步回执已发送到 ${toUser.username} (${toUser.serverUrl}): $action")
        } catch (e: Exception) {
            println("发送同步回执失败: ${e.message}")
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
        println("广播同步通知: 模块 $moduleName 需要同步${if (remark.isNotBlank()) "，备注: $remark" else ""}")
        
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
        
        // 向所有用户发送同步通知
        var successCount = 0
        otherUsers.forEach { user ->
            try {
                sendRemoteSyncNotification(user, moduleName, true, remark)
                successCount++
            } catch (e: Exception) {
                println("向用户 ${user.username} 发送广播失败: ${e.message}")
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
    val remark: String = ""
)

data class SyncResponseNotification(
    val fromUserId: String,
    val fromUsername: String,
    val toUserId: String,
    val moduleName: String,
    val action: String, // "ACCEPTED" 或 "REJECTED"
    val timestamp: Long
) 