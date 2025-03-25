package com.stackfilesync.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.stackfilesync.ui.UserSettingsDialog

class UserSettingsAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = UserSettingsDialog(project)
        dialog.show()
    }
} 