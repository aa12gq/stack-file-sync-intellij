package com.stackfilesync.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.concurrent.thread

@Service(Service.Level.PROJECT)
class NetworkConnectivityService(private val project: Project) {
    
    // 尝试开放的端口范围
    private val BASE_PORT = 8889
    private val PORT_COUNT = 10
    
    // 存储连接状态
    private val connectionStatus = mutableMapOf<String, Boolean>()
    
    // 检查与特定用户的连接
    fun checkConnection(user: UserInfo): Boolean {
        // 如果已经检查过并且是连接的，直接返回
        if (connectionStatus[user.id] == true) {
            return true
        }
        
        // 开始连接测试
        val connected = testConnection(user)
        connectionStatus[user.id] = connected
        
        return connected
    }
    
    // 测试连接
    private fun testConnection(user: UserInfo): Boolean {
        try {
            // 发送一个简单的ping消息
            val socket = DatagramSocket()
            val message = "PING:${System.currentTimeMillis()}"
            val data = message.toByteArray()
            val address = InetAddress.getByName(user.serverUrl)
            
            // 尝试连续几个端口
            for (portOffset in 0 until PORT_COUNT) {
                try {
                    val port = BASE_PORT + portOffset
                    val packet = DatagramPacket(data, data.size, address, port)
                    socket.send(packet)
                    println("发送连接测试包到 ${user.username} (${user.serverUrl}:$port)")
                } catch (e: Exception) {
                    // 忽略单个端口的发送错误
                }
            }
            
            socket.close()
            return true
        } catch (e: Exception) {
            println("无法连接到用户 ${user.username}: ${e.message}")
            return false
        }
    }
    
    // 添加测试回显功能
    fun startEchoService() {
        // 启动回显服务，用于测试网络连接
        for (portOffset in 0 until PORT_COUNT) {
            val port = BASE_PORT + portOffset
            thread(start = true, name = "EchoService-$port") {
                try {
                    val socket = DatagramSocket(port)
                    println("启动回显服务于端口: $port")
                    
                    val buffer = ByteArray(1024)
                    
                    while (true) {
                        try {
                            val packet = DatagramPacket(buffer, buffer.size)
                            socket.receive(packet)
                            
                            val receivedData = String(packet.data, 0, packet.length)
                            println("回显服务收到消息: $receivedData 来自: ${packet.address.hostAddress}:${packet.port}")
                            
                            // 如果是PING消息，发送PONG回复
                            if (receivedData.startsWith("PING:")) {
                                val pongMessage = "PONG:${receivedData.substring(5)}"
                                val pongData = pongMessage.toByteArray()
                                val pongPacket = DatagramPacket(
                                    pongData,
                                    pongData.size,
                                    packet.address,
                                    packet.port
                                )
                                socket.send(pongPacket)
                                println("回显服务发送回复: $pongMessage 到: ${packet.address.hostAddress}:${packet.port}")
                            }
                        } catch (e: Exception) {
                            // 忽略单个消息的接收错误
                        }
                    }
                } catch (e: Exception) {
                    println("端口 $port 的回显服务启动失败: ${e.message}")
                }
            }
        }
    }
    
    companion object {
        fun getInstance(project: Project): NetworkConnectivityService = project.service()
    }
} 