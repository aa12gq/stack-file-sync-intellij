package com.stackfilesync.intellij.utils

import com.intellij.notification.*
import com.intellij.openapi.project.Project

object NotificationUtils {
    private const val GROUP_ID = "Stack File Sync"
    private val notificationGroup = NotificationGroupManager.getInstance()
        .getNotificationGroup(GROUP_ID)
    
    fun showError(project: Project?, title: String, content: String) {
        notify(project, title, content, NotificationType.ERROR)
    }
    
    fun showWarning(project: Project?, title: String, content: String) {
        notify(project, title, content, NotificationType.WARNING)
    }
    
    fun showInfo(project: Project?, title: String, content: String) {
        notify(project, title, content, NotificationType.INFORMATION)
    }
    
    private fun notify(
        project: Project?,
        title: String,
        content: String,
        type: NotificationType
    ) {
        notificationGroup
            .createNotification(title, content, type)
            .notify(project)
    }
} 