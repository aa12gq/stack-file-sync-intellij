package com.stackfilesync.intellij.window

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.OnePixelSplitter
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.impl.ConsoleViewImpl
import com.stackfilesync.intellij.logs.LogService
import javax.swing.JPanel
import javax.swing.JLabel
import javax.swing.JComponent
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.BorderFactory
import com.intellij.icons.AllIcons

class SyncToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = JPanel(BorderLayout())
        
        // 创建上部分的仓库列表面板
        val repositoryPanel = createRepositoryPanel(project)
        
        // 创建下部分的日志面板
        val logService = LogService.getInstance(project)
        val consoleView = ConsoleViewImpl(project, true).apply {
            allowHeavyFilters()
        }
        logService.setConsoleView(consoleView)
        
        // 创建日志面板的标题栏
        val logTitlePanel = JPanel(BorderLayout()).apply {
            add(JLabel("同步日志"), BorderLayout.WEST)
            add(JButton(AllIcons.Actions.GC).apply {  // 使用垃圾桶图标
                toolTipText = "清除日志"
                addActionListener {
                    logService.clear()
                }
            }, BorderLayout.EAST)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }
        
        // 创建一个带标题的日志面板
        val logPanel = JPanel(BorderLayout()).apply {
            add(logTitlePanel, BorderLayout.NORTH)
            add(JBScrollPane(consoleView.component), BorderLayout.CENTER)
        }
        
        // 使用分隔面板分隔仓库列表和日志
        val splitPane = OnePixelSplitter(true, 0.6f).apply {
            firstComponent = repositoryPanel
            secondComponent = logPanel
            dividerWidth = 3
        }
        
        panel.add(splitPane, BorderLayout.CENTER)
        
        // 添加到工具窗口
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createRepositoryPanel(project: Project): JComponent {
        return RepositoriesPanel(project)
    }
} 