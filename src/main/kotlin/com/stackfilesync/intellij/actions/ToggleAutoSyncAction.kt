package com.stackfilesync.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.stackfilesync.intellij.service.RepositoryService
import com.stackfilesync.intellij.utils.NotificationUtils
import com.stackfilesync.intellij.model.AutoSyncConfig
import com.stackfilesync.intellij.model.Repository

class ToggleAutoSyncAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repository = RepositoryService.getInstance(project).getRepository() ?: return
        
        // 切换自动同步状态
        val isEnabled = repository.autoSync?.enabled ?: false
        repository.autoSync = if (!isEnabled) {
            AutoSyncConfig(enabled = true)
        } else {
            null
        }
        
        // 保存配置
        RepositoryService.getInstance(project).saveRepository(repository)
        
        // 显示通知
        val message = if (repository.autoSync?.enabled == true) {
            "已启用自动同步"
        } else {
            "已禁用自动同步"
        }
        NotificationUtils.showInfo(project, "自动同步", message)
    }
} 