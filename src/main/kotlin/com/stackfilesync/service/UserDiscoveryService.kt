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
        
        // 如果没有网络用户，添加一些示例用户
        if (networkUsers.isEmpty()) {
            // 添加一些示例用户
            val exampleUsers = listOf(
                UserInfo("user1", "张三", "https://example.com"),
                UserInfo("user2", "李四", "https://example.com"),
                UserInfo("user3", "王五", "https://example.com"),
                UserInfo("user4", "赵六", "https://example.com")
            )
            connectedUsers.addAll(exampleUsers.filter { it.id != currentUser?.id })
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
    
    fun addListener(listener: UserDiscoveryListener) {
        listeners.add(listener)
    }
    
    fun removeListener(listener: UserDiscoveryListener) {
        listeners.remove(listener)
    }
    
    private fun notifyUserListUpdated() {
        println("通知监听器用户列表已更新，监听器数量: ${listeners.size}")
        listeners.forEach { it.onUserListUpdated(connectedUsers) }
    }
    
    private fun generateUniqueId(): String {
        return System.currentTimeMillis().toString() + "-" + (0..1000).random()
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
        // 使用HTTP或UDP发送消息到目标用户
        // 这里使用简单的UDP实现
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
            val address = InetAddress.getByName(toUser.serverUrl)
            val packet = DatagramPacket(data, data.size, address, 8889) // 使用不同端口接收消息
            
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
    
    fun sendSyncNotification(toUser: UserInfo, moduleName: String) {
        println("发送同步通知给 ${toUser.username}: 模块 $moduleName 需要同步")
        
        val notificationService = NotificationService.getInstance(project)
        
        if (toUser.id == currentUser?.id) {
            notifySyncNotificationReceived(currentUser!!, moduleName)
        } else {
            sendRemoteSyncNotification(toUser, moduleName)
        }
        
        notificationService.showNotification(
            "同步通知已发送",
            "已通知 ${toUser.username} 同步模块 $moduleName",
            NotificationType.INFORMATION
        )
    }
    
    private fun sendRemoteSyncNotification(toUser: UserInfo, moduleName: String) {
        try {
            val socket = DatagramSocket()
            val gson = Gson()
            val notificationData = gson.toJson(SyncNotification(
                fromUserId = currentUser?.id ?: "",
                fromUsername = currentUser?.username ?: "",
                moduleName = moduleName,
                timestamp = System.currentTimeMillis()
            ))
            
            val data = notificationData.toByteArray()
            val address = InetAddress.getByName(toUser.serverUrl)
            val packet = DatagramPacket(data, data.size, address, 8890)
            
            socket.send(packet)
            socket.close()
            
            println("同步通知已发送到 ${toUser.username} (${toUser.serverUrl})")
        } catch (e: Exception) {
            println("发送同步通知失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun notifySyncNotificationReceived(fromUser: UserInfo, moduleName: String) {
        listeners.forEach { 
            if (it is SyncNotificationListener) {
                it.onSyncNotificationReceived(fromUser, moduleName)
            }
        }
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
    val timestamp: Long
) 