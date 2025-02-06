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
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.JBScrollPane
import javax.swing.JPanel
import java.awt.BorderLayout
import java.text.SimpleDateFormat
import java.util.Date
import java.awt.Color

@Service
class LogService {
    private var consoleView: ConsoleView? = null
    private val dateFormat = SimpleDateFormat("HH:mm:ss")

    fun setConsoleView(console: ConsoleView) {
        consoleView = console
    }

    fun appendLog(message: String) {
        invokeLater {
            // 解析ANSI颜色代码
            val parts = parseAnsiColorCodes(message)
            val timestamp = "[${dateFormat.format(Date())}] "
            
            // 打印时间戳
            consoleView?.print(timestamp, ConsoleViewContentType.NORMAL_OUTPUT)
            
            // 打印每个部分，使用对应的颜色
            parts.forEach { (text, color) ->
                val contentType = when (color) {
                    AnsiColor.GREEN -> ConsoleViewContentType.LOG_INFO_OUTPUT
                    AnsiColor.RED -> ConsoleViewContentType.LOG_ERROR_OUTPUT
                    AnsiColor.YELLOW -> ConsoleViewContentType.LOG_WARNING_OUTPUT
                    else -> ConsoleViewContentType.NORMAL_OUTPUT
                }
                consoleView?.print(text, contentType)
            }
            
            // 添加换行
            consoleView?.print("\n", ConsoleViewContentType.NORMAL_OUTPUT)
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
    
    private enum class AnsiColor {
        NORMAL, RED, GREEN, YELLOW
    }
    
    private fun parseAnsiColorCodes(text: String): List<Pair<String, AnsiColor>> {
        val result = mutableListOf<Pair<String, AnsiColor>>()
        
        // 更精确的ANSI代码匹配模式
        val ansiPattern = "\u001B\\[([0-9;]*)m".toRegex()
        var currentText = text
        var currentColor = AnsiColor.NORMAL
        
        while (currentText.isNotEmpty()) {
            val match = ansiPattern.find(currentText)
            if (match == null) {
                // 如果没有更多的颜色代码，添加剩余文本
                if (currentText.isNotEmpty()) {
                    result.add(currentText to currentColor)
                }
                break
            }
            
            // 添加颜色代码之前的文本
            val beforeCode = currentText.substring(0, match.range.first)
            if (beforeCode.isNotEmpty()) {
                result.add(beforeCode to currentColor)
            }
            
            // 更新颜色
            currentColor = when (match.groupValues[1]) {
                "31" -> AnsiColor.RED
                "32" -> AnsiColor.GREEN
                "33" -> AnsiColor.YELLOW
                "0" -> AnsiColor.NORMAL
                else -> currentColor
            }
            
            // 移除已处理的部分
            currentText = currentText.substring(match.range.last + 1)
        }
        
        // 清理结果，移除(B[m这样的残留代码
        return result.map { (text, color) ->
            text.replace("\\(B\\[m".toRegex(), "") to color
        }
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