package com.stackfilesync.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.google.gson.Gson
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.util.concurrent.Executors
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class MessageReceiveService(private val project: Project) {
    
    private val port = 8889
    private var socket: DatagramSocket? = null
    private val executorService = Executors.newSingleThreadExecutor()
    private var isRunning = false
    private val gson = Gson()
    private val userDiscoveryService = UserDiscoveryService.getInstance(project)
    
    fun startListening() {
        if (isRunning) return
        
        try {
            socket = DatagramSocket(port)
            isRunning = true
            
            executorService.submit { listenForMessages() }
            println("消息接收服务已启动，监听端口: $port")
        } catch (e: Exception) {
            println("启动消息接收服务失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun stopListening() {
        if (!isRunning) return
        
        try {
            isRunning = false
            executorService.shutdown()
            socket?.close()
            socket = null
            println("消息接收服务已停止")
        } catch (e: Exception) {
            println("停止消息接收服务失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun listenForMessages() {
        val buffer = ByteArray(4096)
        val packet = DatagramPacket(buffer, buffer.size)
        
        while (isRunning) {
            try {
                socket?.receive(packet)
                val message = String(packet.data, 0, packet.length)
                SwingUtilities.invokeLater { handleReceivedMessage(message) }
            } catch (e: Exception) {
                if (isRunning) {
                    println("接收消息失败: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
    
    private fun handleReceivedMessage(message: String) {
        try {
            val notification = gson.fromJson(message, RemoteNotification::class.java)
            println("收到来自 ${notification.fromUsername} 的消息: ${notification.message}")
            
            // 创建临时用户信息对象来表示发送方
            val fromUser = UserInfo(
                id = notification.fromUserId,
                username = notification.fromUsername,
                serverUrl = "" // 我们没有发送方的IP，这个字段可以留空
            )
            
            // 通知用户发现服务处理收到的消息
            userDiscoveryService.handleReceivedNotification(fromUser, notification.message)
        } catch (e: Exception) {
            println("处理接收到的消息失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    companion object {
        fun getInstance(project: Project): MessageReceiveService = project.service()
    }
} 