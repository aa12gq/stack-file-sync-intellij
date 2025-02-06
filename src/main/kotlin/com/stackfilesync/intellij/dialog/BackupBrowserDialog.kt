package com.stackfilesync.intellij.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.stackfilesync.intellij.backup.BackupManager
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.utils.NotificationUtils
import java.awt.BorderLayout
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.table.AbstractTableModel
import com.intellij.ui.SearchTextField
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class BackupBrowserDialog(
    private val project: Project,
    private val repository: Repository
) : DialogWrapper(project) {
    
    private val backupManager = BackupManager(project)
    private val table: JBTable
    private val tableModel: BackupTableModel
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val searchField = SearchTextField()
    
    init {
        title = "备份历史 - ${repository.name}"
        
        tableModel = BackupTableModel()
        table = JBTable(tableModel).apply {
            setShowGrid(true)
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        }
        
        init()
        loadBackups()
        
        // 添加搜索监听
        searchField.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = filterBackups()
            override fun removeUpdate(e: DocumentEvent) = filterBackups()
            override fun changedUpdate(e: DocumentEvent) = filterBackups()
        })
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = JBUI.size(600, 400)
        
        // 添加搜索面板
        val searchPanel = JPanel(BorderLayout()).apply {
            add(JLabel("搜索:"), BorderLayout.WEST)
            add(searchField, BorderLayout.CENTER)
        }
        
        // 添加工具栏
        val toolbar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            
            add(JButton("恢复选中的备份").apply {
                addActionListener {
                    restoreSelectedBackup()
                }
            })
            
            add(Box.createHorizontalStrut(10))
            
            add(JButton("导出备份").apply {
                addActionListener {
                    exportSelectedBackup()
                }
            })
            
            add(Box.createHorizontalStrut(10))
            
            add(JButton("刷新").apply {
                addActionListener {
                    loadBackups()
                }
            })
            
            add(JButton("查看统计").apply {
                addActionListener {
                    showBackupStats()
                }
            })
            
            add(Box.createHorizontalStrut(10))
            
            add(JButton("比较备份").apply {
                addActionListener {
                    compareBackups()
                }
            })
            
            add(Box.createHorizontalGlue())
        }
        
        val topPanel = JPanel(BorderLayout()).apply {
            add(searchPanel, BorderLayout.CENTER)
            add(toolbar, BorderLayout.SOUTH)
        }
        
        panel.add(topPanel, BorderLayout.NORTH)
        panel.add(JBScrollPane(table), BorderLayout.CENTER)
        
        return panel
    }
    
    private fun loadBackups() {
        tableModel.backups = backupManager.getBackups(repository)
        tableModel.fireTableDataChanged()
    }
    
    private fun filterBackups() {
        val searchText = searchField.text.lowercase()
        tableModel.backups = if (searchText.isBlank()) {
            backupManager.getBackups(repository)
        } else {
            backupManager.getBackups(repository).filter { backup ->
                dateFormat.format(backup.timestamp).lowercase().contains(searchText) ||
                backup.fileCount.toString().contains(searchText)
            }
        }
        tableModel.fireTableDataChanged()
    }
    
    private fun restoreSelectedBackup() {
        val selectedRow = table.selectedRow
        if (selectedRow < 0) {
            return
        }
        
        val backup = tableModel.backups[selectedRow]
        
        val result = JOptionPane.showConfirmDialog(
            contentPanel,
            """
            确定要恢复此备份吗？
            时间: ${dateFormat.format(backup.timestamp)}
            文件数: ${backup.fileCount}
            
            注意: 当前文件将会被备份，恢复操作后可以撤销。
            """.trimIndent(),
            "确认恢复",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        )
        
        if (result == JOptionPane.YES_OPTION) {
            try {
                backupManager.restoreBackup(backup, repository)
                NotificationUtils.showInfo(
                    project,
                    "恢复成功",
                    "已成功恢复备份: ${dateFormat.format(backup.timestamp)}"
                )
                close(OK_EXIT_CODE)
            } catch (e: Exception) {
                NotificationUtils.showError(
                    project,
                    "恢复失败",
                    e.message ?: "未知错误"
                )
            }
        }
    }
    
    private fun exportSelectedBackup() {
        val selectedRow = table.selectedRow
        if (selectedRow < 0) {
            return
        }
        
        val backup = tableModel.backups[selectedRow]
        val timestamp = dateFormat.format(backup.timestamp).replace(":", "-")
        val defaultName = "${repository.name}_backup_$timestamp.zip"
        
        val fileChooser = JFileChooser().apply {
            dialogTitle = "导出备份"
            fileFilter = FileNameExtensionFilter("ZIP files (*.zip)", "zip")
            selectedFile = File(defaultName)
        }
        
        if (fileChooser.showSaveDialog(contentPanel) == JFileChooser.APPROVE_OPTION) {
            try {
                var targetFile = fileChooser.selectedFile
                if (!targetFile.name.lowercase().endsWith(".zip")) {
                    targetFile = File(targetFile.parentFile, "${targetFile.name}.zip")
                }
                
                backupManager.exportBackup(backup, targetFile)
                
                NotificationUtils.showInfo(
                    project,
                    "导出成功",
                    "备份已导出到: ${targetFile.absolutePath}"
                )
            } catch (e: Exception) {
                NotificationUtils.showError(
                    project,
                    "导出失败",
                    e.message ?: "未知错误"
                )
            }
        }
    }
    
    private fun showBackupStats() {
        val selectedRow = table.selectedRow
        if (selectedRow < 0) {
            return
        }
        
        val backup = tableModel.backups[selectedRow]
        try {
            val stats = backupManager.getBackupStats(backup)
            BackupStatsDialog(project, backup, stats).show()
        } catch (e: Exception) {
            NotificationUtils.showError(
                project,
                "获取统计信息失败",
                e.message ?: "未知错误"
            )
        }
    }
    
    private fun compareBackups() {
        val selectedRows = table.selectedRows
        if (selectedRows.size != 2) {
            NotificationUtils.showWarning(
                project,
                "选择备份",
                "请选择两个备份进行比较"
            )
            return
        }
        
        val backup1 = tableModel.backups[selectedRows[0]]
        val backup2 = tableModel.backups[selectedRows[1]]
        
        try {
            val diff = backupManager.compareBackups(backup1, backup2)
            BackupDiffDialog(project, backup1, backup2, diff).show()
        } catch (e: Exception) {
            NotificationUtils.showError(
                project,
                "比较备份失败",
                e.message ?: "未知错误"
            )
        }
    }
    
    private inner class BackupTableModel : AbstractTableModel() {
        var backups: List<BackupManager.BackupInfo> = emptyList()
        private val columns = listOf("时间", "文件数")
        
        override fun getRowCount(): Int = backups.size
        override fun getColumnCount(): Int = columns.size
        override fun getColumnName(column: Int): String = columns[column]
        
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val backup = backups[rowIndex]
            return when (columnIndex) {
                0 -> dateFormat.format(backup.timestamp)
                1 -> backup.fileCount
                else -> ""
            }
        }
    }
} 