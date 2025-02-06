package com.stackfilesync.intellij.window

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.text.SimpleDateFormat
import java.util.*
import javax.swing.*
import javax.swing.table.AbstractTableModel
import com.stackfilesync.intellij.service.SyncHistoryService

class HistoryPanel(private val project: Project) : JPanel() {
    private val table: JBTable
    private val tableModel: HistoryTableModel
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    
    init {
        layout = BorderLayout()
        border = JBUI.Borders.empty(10)
        
        // 创建工具栏
        val toolbar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            
            // 刷新按钮
            val refreshButton = JButton("刷新").apply {
                addActionListener {
                    refresh()
                }
            }
            add(refreshButton)
            
            // 清除按钮
            val clearButton = JButton("清除历史").apply {
                addActionListener {
                    clearHistory()
                }
            }
            add(Box.createHorizontalStrut(10))
            add(clearButton)
            
            add(Box.createHorizontalGlue())
        }
        
        // 创建表格
        tableModel = HistoryTableModel()
        table = JBTable(tableModel).apply {
            setShowGrid(true)
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        }
        
        add(toolbar, BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)
        
        // 加载数据
        refresh()
    }
    
    fun refresh() {
        val historyService = SyncHistoryService.getInstance()
        tableModel.data.clear()
        tableModel.data.addAll(historyService.getHistoryItems().map { item ->
            HistoryItem(
                timestamp = item.timestamp,
                repository = item.repository,
                branch = item.branch,
                success = item.success,
                fileCount = item.fileCount,
                duration = item.duration,
                error = item.error,
                syncType = item.syncType
            )
        })
        tableModel.fireTableDataChanged()
    }
    
    private fun clearHistory() {
        val result = JOptionPane.showConfirmDialog(
            this,
            "确定要清除所有同步历史记录吗？",
            "确认清除",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        
        if (result == JOptionPane.YES_OPTION) {
            SyncHistoryService.getInstance().clearHistory()
            refresh()
        }
    }
    
    private inner class HistoryTableModel : AbstractTableModel() {
        private val columns = listOf("时间", "仓库", "分支", "状态", "文件数", "耗时", "类型")
        val data = mutableListOf<HistoryItem>()
        
        override fun getRowCount(): Int = data.size
        override fun getColumnCount(): Int = columns.size
        override fun getColumnName(column: Int): String = columns[column]
        
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val item = data[rowIndex]
            return when (columnIndex) {
                0 -> dateFormat.format(Date(item.timestamp))
                1 -> item.repository
                2 -> item.branch
                3 -> when {
                    item.success -> "成功"
                    item.error != null -> "失败: ${item.error}"
                    else -> "失败"
                }
                4 -> item.fileCount
                5 -> "${item.duration / 1000}秒"
                6 -> if (item.syncType == "auto") "自动" else "手动"
                else -> ""
            }
        }
    }
    
    data class HistoryItem(
        val timestamp: Long,
        val repository: String,
        val branch: String,
        val success: Boolean,
        val fileCount: Int,
        val duration: Long,
        val error: String? = null,
        val syncType: String = "manual"
    )
} 