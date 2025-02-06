package com.stackfilesync.intellij.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.stackfilesync.intellij.backup.BackupManager
import java.awt.BorderLayout
import java.text.DecimalFormat
import javax.swing.*

class BackupStatsDialog(
    project: Project,
    private val backup: BackupManager.BackupInfo,
    private val stats: BackupManager.BackupStats
) : DialogWrapper(project) {
    
    init {
        title = "备份统计 - ${backup.repository}"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = JBUI.size(500, 400)
        
        val content = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10)
            
            // 基本信息
            add(createSection("基本信息", listOf(
                "总大小: ${formatSize(stats.totalSize)}",
                "文件数: ${backup.fileCount}"
            )))
            
            // 文件类型统计
            add(createSection("文件类型", stats.fileTypes.map { (ext, count) ->
                "${if (ext.isEmpty()) "<无扩展名>" else ".$ext"}: $count 个文件"
            }))
            
            // 最大文件
            add(createSection("最大文件 (Top 10)", stats.largestFiles.map { (file, size) ->
                "${file.fileName}: ${formatSize(size)}"
            }))
        }
        
        panel.add(JBScrollPane(content), BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createSection(title: String, items: List<String>): JPanel {
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder(title)
            
            val list = JList(items.toTypedArray()).apply {
                selectionMode = ListSelectionModel.SINGLE_SELECTION
            }
            
            add(JBScrollPane(list), BorderLayout.CENTER)
        }
    }
    
    private fun formatSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var value = size.toDouble()
        var unit = 0
        
        while (value >= 1024 && unit < units.size - 1) {
            value /= 1024
            unit++
        }
        
        return DecimalFormat("#,##0.##").format(value) + " " + units[unit]
    }
} 