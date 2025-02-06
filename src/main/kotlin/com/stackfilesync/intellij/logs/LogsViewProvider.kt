package com.stackfilesync.intellij.logs

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.application.invokeLater
import java.text.SimpleDateFormat
import java.util.Date
import com.intellij.ui.content.ContentFactory
import javax.swing.JPanel
import java.awt.BorderLayout
import com.intellij.ui.components.JBScrollPane

@Service
class LogService {
    private var consoleView: ConsoleView? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss")

    fun setConsoleView(console: ConsoleView) {
        consoleView = console
    }

    fun appendLog(message: String) {
        invokeLater {
            consoleView?.print(
                "[${dateFormat.format(Date())}] $message\n",
                ConsoleViewContentType.NORMAL_OUTPUT
            )
        }
    }

    fun clear() {
        invokeLater {
            consoleView?.clear()
        }
    }

    companion object {
        fun getInstance(project: Project): LogService = project.service()
    }
}

class LogsViewProvider : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val logService = LogService.getInstance(project)
        
        // 创建面板
        val panel = JPanel(BorderLayout())
        
        // 创建控制台视图
        val consoleView = ConsoleViewImpl(project, true).apply {
            allowHeavyFilters()
        }
        
        // 设置控制台视图到服务
        logService.setConsoleView(consoleView)
        
        // 添加滚动面板
        val scrollPane = JBScrollPane(consoleView.component)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // 创建工具窗口内容
        val content = ContentFactory.getInstance().createContent(
            panel,
            "同步日志",  // 添加标题
            false
        )
        
        // 添加到工具窗口
        toolWindow.contentManager.addContent(content)
        
        // 设置工具窗口属性
        toolWindow.setToHideOnEmptyContent(false)
        toolWindow.setAutoHide(false)
    }
} 