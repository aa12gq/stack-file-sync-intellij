package com.stackfilesync.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.google.gson.Gson
import java.net.*
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class NetworkDiscoveryService(private val project: Project) {
    
    private val multicastGroup = "239.255.255.250"
    private val port = 8888
    private val gson = Gson()
    private var socket: MulticastSocket? = null
    private val executorService = Executors.newScheduledThreadPool(2)
    private var isRunning = false
    private val userDiscoveryService = UserDiscoveryService.getInstance(project)
    
    fun startDiscovery() {
        if (isRunning) return
        
        try {
            socket = MulticastSocket(port)
            val group = InetAddress.getByName(multicastGroup)
            socket?.joinGroup(group)
            isRunning = true
            
            // 启动广播线程 - 定期广播自己的存在
            executorService.scheduleAtFixedRate(
                { broadcastSelf() },
                0,
                5,
                TimeUnit.SECONDS
            )
            
            // 启动监听线程 - 持续监听其他用户的广播
            executorService.submit { listenForOtherUsers() }
            
            println("网络发现服务已启动")
        } catch (e: Exception) {
            println("启动网络发现服务失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun stopDiscovery() {
        if (!isRunning) return
        
        try {
            isRunning = false
            executorService.shutdown()
            socket?.leaveGroup(InetAddress.getByName(multicastGroup))
            socket?.close()
            socket = null
            println("网络发现服务已停止")
        } catch (e: Exception) {
            println("停止网络发现服务失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun broadcastSelf() {
        val currentUser = userDiscoveryService.getCurrentUser() ?: return
        try {
            val userJson = gson.toJson(NetworkUserInfo(
                id = currentUser.id,
                username = currentUser.username,
                ipAddress = getLocalIpAddress(),
                timestamp = System.currentTimeMillis()
            ))
            
            val data = userJson.toByteArray()
            val group = InetAddress.getByName(multicastGroup)
            val packet = DatagramPacket(data, data.size, group, port)
            
            socket?.send(packet)
            println("广播用户信息: $userJson")
        } catch (e: Exception) {
            println("广播用户信息失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun listenForOtherUsers() {
        val buffer = ByteArray(4096)
        val packet = DatagramPacket(buffer, buffer.size)
        
        while (isRunning) {
            try {
                socket?.receive(packet)
                val message = String(packet.data, 0, packet.length)
                handleUserBroadcast(message)
            } catch (e: IOException) {
                if (isRunning) {
                    println("接收用户广播失败: ${e.message}")
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                println("处理用户广播失败: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    private fun handleUserBroadcast(message: String) {
        try {
            val networkUserInfo = gson.fromJson(message, NetworkUserInfo::class.java)
            val currentUser = userDiscoveryService.getCurrentUser()
            
            // 忽略自己的广播
            if (currentUser != null && networkUserInfo.id == currentUser.id) {
                return
            }
            
            println("接收到用户广播: $networkUserInfo")
            
            // 将网络用户信息转换为应用用户信息，并通知服务更新
            val userInfo = UserInfo(
                id = networkUserInfo.id,
                username = networkUserInfo.username,
                serverUrl = networkUserInfo.ipAddress
            )
            
            userDiscoveryService.addOrUpdateNetworkUser(userInfo)
        } catch (e: Exception) {
            println("解析用户广播失败: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun getLocalIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    // 排除回环地址和IPv6地址
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            println("获取本地IP地址失败: ${e.message}")
            e.printStackTrace()
        }
        
        return "127.0.0.1" // 默认回环地址
    }
    
    companion object {
        fun getInstance(project: Project): NetworkDiscoveryService = project.service()
    }
}

data class NetworkUserInfo(
    val id: String,
    val username: String,
    val ipAddress: String,
    val timestamp: Long
) 