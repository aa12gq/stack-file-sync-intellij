package com.stackfilesync.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.google.gson.Gson
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.concurrent.thread

@Service(Service.Level.PROJECT)
class MessageReceiveService(private val project: Project) {
    
    private var running = false
    private var socket: DatagramSocket? = null
    
    // 监听通用消息的端口
    private val NOTIFICATION_PORT = 8889
    // 监听同步通知的端口
    private val SYNC_NOTIFICATION_PORTS = listOf(8890, 8880, 9890, 7890)
    // 同步回执的端口
    private val SYNC_RESPONSE_PORT = 8891
    
    fun startListening() {
        if (running) return
        
        running = true
        
        // 启动通用消息监听线程
        thread(start = true, name = "NotificationReceiver") {
            listenForNotifications()
        }
        
        // 启动多个端口的同步通知监听
        SYNC_NOTIFICATION_PORTS.forEach { port ->
            thread(start = true, name = "SyncNotificationReceiver-$port") {
                listenForSyncNotifications(port)
            }
        }
        
        // 添加回执监听线程
        thread(start = true, name = "SyncResponseReceiver") {
            listenForSyncResponses()
        }
        
        println("消息接收服务已启动，监听端口: $NOTIFICATION_PORT, $SYNC_NOTIFICATION_PORTS, $SYNC_RESPONSE_PORT")
    }
    
    private fun listenForNotifications() {
        try {
            // 创建一个特定端口的Socket
            val socket = DatagramSocket(NOTIFICATION_PORT)
            this.socket = socket
            
            val buffer = ByteArray(4096) // 增大缓冲区大小
            
            println("开始监听通知消息，端口: $NOTIFICATION_PORT")
            
            while (running) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    
                    // 处理接收到的数据
                    val data = String(packet.data, 0, packet.length)
                    println("收到消息: $data")
                    
                    // 解析消息
                    val gson = Gson()
                    val notification = gson.fromJson(data, RemoteNotification::class.java)
                    
                    // 查找发送者信息
                    val userService = UserDiscoveryService.getInstance(project)
                    val fromUser = UserInfo(
                        id = notification.fromUserId,
                        username = notification.fromUsername,
                        serverUrl = packet.address.hostAddress
                    )
                    
                    // 通知服务处理消息
                    userService.handleReceivedNotification(fromUser, notification.message)
                    
                    // 自动将发送者添加到用户列表
                    userService.addOrUpdateNetworkUser(fromUser)
                    
                } catch (e: Exception) {
                    if (running) {
                        println("处理消息时出错: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            if (running) {
                println("启动消息监听失败: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun listenForSyncNotifications(port: Int) {
        try {
            val syncSocket = DatagramSocket(port)
            
            val buffer = ByteArray(4096)
            
            println("开始监听同步通知消息，端口: $port")
            
            while (running) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    syncSocket.receive(packet)
                    
                    // 处理接收到的数据
                    val data = String(packet.data, 0, packet.length)
                    println("收到同步通知: $data (端口: $port)")
                    
                    // 解析消息
                    val gson = Gson()
                    val notification = gson.fromJson(data, SyncNotification::class.java)
                    
                    // 查找发送者信息
                    val userService = UserDiscoveryService.getInstance(project)
                    val fromUser = UserInfo(
                        id = notification.fromUserId,
                        username = notification.fromUsername,
                        serverUrl = packet.address.hostAddress
                    )
                    
                    // 处理同步通知，添加广播标志和备注
                    userService.notifySyncNotificationReceived(
                        fromUser, 
                        notification.moduleName,
                        notification.isBroadcast,
                        notification.remark
                    )
                    
                    // 自动将发送者添加到用户列表
                    userService.addOrUpdateNetworkUser(fromUser)
                    
                } catch (e: Exception) {
                    if (running) {
                        // 端口级别错误降级为警告，不中断服务
                        println("处理同步通知时出错 (端口: $port): ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            if (running) {
                println("启动同步通知监听失败 (端口: $port): ${e.message}")
            }
        }
    }
    
    // 添加监听同步回执的方法
    private fun listenForSyncResponses() {
        try {
            val responseSocket = DatagramSocket(SYNC_RESPONSE_PORT)
            
            val buffer = ByteArray(4096)
            
            println("开始监听同步回执消息，端口: $SYNC_RESPONSE_PORT")
            
            while (running) {
                try {
                    val packet = DatagramPacket(buffer, buffer.size)
                    responseSocket.receive(packet)
                    
                    val data = String(packet.data, 0, packet.length)
                    println("收到同步回执: $data")
                    
                    val gson = Gson()
                    val response = gson.fromJson(data, SyncResponseNotification::class.java)
                    
                    val userService = UserDiscoveryService.getInstance(project)
                    val fromUser = UserInfo(
                        id = response.fromUserId,
                        username = response.fromUsername,
                        serverUrl = packet.address.hostAddress
                    )
                    
                    // 处理回执
                    val accepted = response.action == "ACCEPTED"
                    userService.notifySyncResponseReceived(fromUser, response.moduleName, accepted)
                    
                    // 添加用户到列表
                    userService.addOrUpdateNetworkUser(fromUser)
                    
                } catch (e: Exception) {
                    if (running) {
                        println("处理同步回执时出错: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        } catch (e: Exception) {
            if (running) {
                println("启动同步回执监听失败: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun stopListening() {
        running = false
        socket?.close()
        socket = null
    }
    
    companion object {
        fun getInstance(project: Project): MessageReceiveService = project.service()
    }
} 