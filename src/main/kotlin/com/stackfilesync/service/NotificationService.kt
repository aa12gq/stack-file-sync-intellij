package com.stackfilesync.service

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import javax.swing.JOptionPane
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class NotificationService(private val project: Project) {
    
    fun showNotification(title: String, content: String, type: NotificationType = NotificationType.INFORMATION) {
        try {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Stack File Sync Notifications")
                .createNotification(title, content, type)
                .notify(project)
        } catch (e: Exception) {
            // 记录错误以帮助调试
            println("显示通知时出错: ${e.message}")
            e.printStackTrace()
        }
    }
    
    fun handleUserNotification(fromUser: UserInfo, message: String) {
        // 使用更明显的通知方式显示收到的消息
        showNotification(
            "来自 ${fromUser.username} 的消息",
            message,
            NotificationType.INFORMATION
        )
        
        // 同时使用对话框显示，确保用户能看到
        SwingUtilities.invokeLater {
            JOptionPane.showMessageDialog(
                null,
                "消息内容: $message",
                "收到来自 ${fromUser.username} 的消息",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
    }
    
    companion object {
        fun getInstance(project: Project): NotificationService = project.service()
    }
} 