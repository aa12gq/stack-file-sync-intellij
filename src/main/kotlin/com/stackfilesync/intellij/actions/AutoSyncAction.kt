package com.stackfilesync.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.settings.SyncSettings
import com.stackfilesync.intellij.sync.AutoSyncManager

class ToggleAutoSyncAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val repository = e.getData(CommonDataKeys.PSI_ELEMENT) as? Repository ?: return
        
        // 切换自动同步状态
        val settings = SyncSettings.getInstance()
        val repositories = settings.getRepositories().toMutableList()
        val index = repositories.indexOfFirst { it.name == repository.name }
        
        if (index >= 0) {
            val repo = repositories[index]
            repo.autoSync = repo.autoSync?.copy(
                enabled = !(repo.autoSync?.enabled ?: false)
            ) ?: Repository.AutoSyncConfig(enabled = true)
            
            settings.setRepositories(repositories)
            
            // 更新自动同步任务
            val autoSyncManager = AutoSyncManager.getInstance(project)
            if (repo.autoSync?.enabled == true) {
                autoSyncManager.startAllAutoSync()
            } else {
                autoSyncManager.stopAutoSync(repo)
            }
        }
    }
    
    override fun update(e: AnActionEvent) {
        val repository = e.getData(CommonDataKeys.PSI_ELEMENT) as? Repository
        e.presentation.isEnabled = repository != null
        
        // 更新图标和文本
        if (repository != null) {
            val enabled = repository.autoSync?.enabled ?: false
            e.presentation.text = if (enabled) "禁用自动同步" else "启用自动同步"
            // TODO: 设置相应的图标
        }
    }
} 