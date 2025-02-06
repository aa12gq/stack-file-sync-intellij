package com.stackfilesync.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.model.Repository.AutoSyncConfig
import com.stackfilesync.intellij.icons.StackFileSync

class ToggleAutoSyncAction : AnAction() {
    companion object {
        val REPOSITORY_KEY = DataKey.create<Repository>("repository")
    }

    override fun actionPerformed(e: AnActionEvent) {
        val repository = e.getData(REPOSITORY_KEY) ?: return
        
        repository.autoSync = if (repository.autoSync?.enabled == true) {
            null
        } else {
            AutoSyncConfig(enabled = true)
        }
        
        e.presentation.icon = if (repository.autoSync?.enabled == true) {
            StackFileSync.AutoSync
        } else {
            StackFileSync.AutoSyncDisabled
        }
    }

    override fun update(e: AnActionEvent) {
        val repository = e.getData(REPOSITORY_KEY)
        e.presentation.isEnabled = repository != null
        
        if (repository != null) {
            val enabled = repository.autoSync?.enabled ?: false
            e.presentation.text = if (enabled) "禁用自动同步" else "启用自动同步"
            e.presentation.icon = if (enabled) {
                StackFileSync.AutoSync
            } else {
                StackFileSync.AutoSyncDisabled
            }
        }
    }
} 