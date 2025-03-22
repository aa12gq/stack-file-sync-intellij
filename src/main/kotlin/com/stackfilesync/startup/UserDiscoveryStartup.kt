package com.stackfilesync.startup

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.stackfilesync.service.UserDiscoveryService
import com.stackfilesync.service.NetworkDiscoveryService
import com.stackfilesync.service.MessageReceiveService
import com.intellij.ide.util.PropertiesComponent

class UserDiscoveryStartup : StartupActivity {
    override fun runActivity(project: Project) {
        // 打印日志以确认启动活动已执行
        println("UserDiscoveryStartup 已启动")
        
        val properties = PropertiesComponent.getInstance()
        val username = properties.getValue("stack.file.sync.username") ?: System.getProperty("user.name")
        
        // 初始化用户发现服务
        val userDiscoveryService = UserDiscoveryService.getInstance(project)
        userDiscoveryService.initialize(username, "")
        
        // 启动网络发现服务
        val networkDiscoveryService = NetworkDiscoveryService.getInstance(project)
        networkDiscoveryService.startDiscovery()
        
        // 启动消息接收服务
        val messageReceiveService = MessageReceiveService.getInstance(project)
        messageReceiveService.startListening()
    }
} 