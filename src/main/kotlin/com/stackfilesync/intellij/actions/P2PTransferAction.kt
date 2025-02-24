package com.stackfilesync.intellij.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.stackfilesync.intellij.dialog.P2PTransferDialog
import com.stackfilesync.intellij.p2p.P2PTransferService
import java.io.File
import com.intellij.openapi.actionSystem.ActionUpdateThread

class P2PTransferAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return
        
        P2PTransferDialog(project, files.toList()).showAndGet()
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        
        e.presentation.apply {
            // 只要有项目和文件就启用
            isEnabled = project != null && files != null && files.isNotEmpty()
            isVisible = true
            
            text = when {
                files?.size ?: 0 > 1 -> "P2P传输 (${files?.size} 个文件)"
                else -> "P2P传输"
            }
        }
    }
    
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
    
    companion object {
        private val supportedExtensions = setOf(
            "proto",
            "txt",
            "json",
            "xml",
            "yaml",
            "yml",
            "properties",
            "conf",
            "config",
            "dart",
            "java",
            "kt",
            "kts",
            "gradle",
            "md",
            "sql"
            // 可以添加更多支持的文件类型
        )
    }
} 