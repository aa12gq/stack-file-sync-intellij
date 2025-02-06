package com.stackfilesync.intellij.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.settings.RepositorySettingsPanel
import javax.swing.JComponent

class RepositorySettingsDialog(
    project: Project,
    private val repository: Repository
) : DialogWrapper(project) {
    private val panel = RepositorySettingsPanel()
    
    init {
        title = "仓库设置"
        init()
        panel.setRepository(repository)
    }
    
    override fun createCenterPanel(): JComponent {
        return panel.createPanel()
    }
    
    fun getRepository(): Repository {
        return panel.getRepository()
    }
} 