package com.stackfilesync.intellij.dialog

import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.model.AutoSyncConfig
import com.stackfilesync.intellij.model.BackupConfig
import com.stackfilesync.intellij.model.InternalSyncConfig
import javax.swing.JPanel

class RepositorySettingsPanel {
    private lateinit var panel: DialogPanel
    private var repository = Repository()

    fun createPanel(): JPanel {
        panel = panel {
            // ... 面板内容保持不变 ...
        }
        
        return panel
    }

    fun getRepository(): Repository = repository
    fun setRepository(repo: Repository) {
        repository = repo
        panel.reset()
    }
} 