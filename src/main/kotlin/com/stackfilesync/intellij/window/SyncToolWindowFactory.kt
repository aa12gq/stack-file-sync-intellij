package com.stackfilesync.intellij.window

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel
import javax.swing.JTabbedPane

class SyncToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        
        // 创建主面板
        val mainPanel = JTabbedPane()
        
        // 添加仓库列表面板
        val repositoriesPanel = createRepositoriesPanel(project)
        mainPanel.addTab("仓库", repositoriesPanel)
        
        // 添加日志面板
        val logsPanel = createLogsPanel(project)
        mainPanel.addTab("日志", logsPanel)
        
        // 添加同步历史面板
        val historyPanel = createHistoryPanel(project)
        mainPanel.addTab("历史", historyPanel)
        
        val content = contentFactory.createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createRepositoriesPanel(project: Project): JPanel {
        return RepositoriesPanel(project)
    }

    private fun createLogsPanel(project: Project): JPanel {
        return LogsPanel(project)
    }

    private fun createHistoryPanel(project: Project): JPanel {
        return HistoryPanel(project)
    }
} 