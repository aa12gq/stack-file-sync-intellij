package com.stackfilesync.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.settings.SyncSettingsState
import com.stackfilesync.intellij.sync.FileSyncManager
import com.stackfilesync.intellij.dialog.RepositoryChooserDialog

class SyncFilesAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val settings = SyncSettingsState.getInstance()
        val repositories = settings.getRepositories()

        if (repositories.isEmpty()) {
            // TODO: 显示配置对话框
            return
        }

        // 显示仓库选择对话框
        val selectedRepo = selectRepository(project, repositories) ?: return

        // 在后台执行同步
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "同步文件", true) {
            override fun run(indicator: ProgressIndicator) {
                val syncManager = FileSyncManager(project, indicator)
                syncManager.sync(selectedRepo, showFileSelection = true, isAutoSync = false)
            }
        })
    }

    private fun selectRepository(project: Project, repositories: List<Repository>): Repository? {
        val dialog = RepositoryChooserDialog(project, repositories)
        return if (dialog.showAndGet()) {
            dialog.getSelectedRepository()
        } else {
            null
        }
    }
} 