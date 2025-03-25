package com.stackfilesync.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.stackfilesync.service.UserDiscoveryService
import com.stackfilesync.service.NetworkDiscoveryService
import com.stackfilesync.service.MessageReceiveService
import com.intellij.ide.util.PropertiesComponent
import java.util.*
import com.stackfilesync.service.NetworkConnectivityService

class UserDiscoveryStartup : StartupActivity {
    override fun runActivity(project: Project) {
        // 打印日志以确认启动活动已执行
        println("UserDiscoveryStartup 已启动")
        
        // 从设置中读取用户名，如果没有则使用系统用户名
        val properties = PropertiesComponent.getInstance()
        val savedUsername = properties.getValue("stack.file.sync.username")
        val username = if (!savedUsername.isNullOrBlank()) {
            savedUsername
        } else {
            // 使用系统用户名或环境变量作为默认值
            System.getProperty("user.name") ?: 
            System.getenv("USERNAME") ?: 
            System.getenv("USER") ?: 
            "User"
        }
        
        println("使用用户名: $username")
        
        // 获取本机IP地址，而不是使用localhost
        val serverUrl = try {
            val localHost = java.net.InetAddress.getLocalHost()
            localHost.hostAddress ?: "localhost"
        } catch (e: Exception) {
            "localhost" // 如果获取失败，退回到localhost
        }
        
        println("使用服务地址: $serverUrl")
        
        // 初始化用户发现服务
        val userDiscoveryService = UserDiscoveryService.getInstance(project)
        userDiscoveryService.initialize(username, serverUrl)
        
        // 启动网络发现服务
        val networkDiscoveryService = NetworkDiscoveryService.getInstance(project)
        networkDiscoveryService.startDiscovery()
        
        // 启动网络连接测试服务
        val networkConnectivityService = NetworkConnectivityService.getInstance(project)
        networkConnectivityService.startEchoService()
        
        // 启动消息接收服务，监听更多端口
        val messageReceiveService = MessageReceiveService.getInstance(project)
        messageReceiveService.startListening()
        
        // 启动时强制刷新一次用户列表
        userDiscoveryService.refreshUserList()
        
        // 定期刷新用户列表
        Timer().schedule(object : TimerTask() {
            override fun run() {
                userDiscoveryService.refreshUserList()
            }
        }, 5000, 30000) // 5秒后开始，每30秒执行一次
    }
} 