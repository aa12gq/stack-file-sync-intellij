package com.stackfilesync.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.stackfilesync.intellij.dialog.P2PConfigDialog

class P2PConfigAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val dialog = P2PConfigDialog(project)
        dialog.show()
    }
} 