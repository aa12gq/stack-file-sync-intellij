package com.stackfilesync.intellij.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import com.stackfilesync.intellij.backup.BackupManager
import java.awt.BorderLayout
import java.time.format.DateTimeFormatter
import javax.swing.*

class BackupDiffDialog(
    project: Project,
    private val backup1: BackupManager.BackupInfo,
    private val backup2: BackupManager.BackupInfo,
    private val diff: BackupManager.BackupDiff
) : DialogWrapper(project) {
    
    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    
    init {
        title = "备份比较"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = JBUI.size(600, 500)
        
        // 添加备份信息
        val infoPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = JBUI.Borders.empty(10)
            
            add(JLabel("备份1: ${dateFormat.format(backup1.timestamp)} (${backup1.fileCount}个文件)"))
            add(Box.createVerticalStrut(5))
            add(JLabel("备份2: ${dateFormat.format(backup2.timestamp)} (${backup2.fileCount}个文件)"))
            add(Box.createVerticalStrut(5))
            add(JSeparator())
            add(Box.createVerticalStrut(5))
            add(JLabel("变更统计:"))
            add(JLabel("  新增: ${diff.addedFiles.size}个文件"))
            add(JLabel("  修改: ${diff.modifiedFiles.size}个文件"))
            add(JLabel("  删除: ${diff.deletedFiles.size}个文件"))
        }
        
        // 添加变更详情标签页
        val tabbedPane = JBTabbedPane().apply {
            add("新增文件", createFileList(diff.addedFiles))
            add("修改文件", createFileList(diff.modifiedFiles))
            add("删除文件", createFileList(diff.deletedFiles))
        }
        
        panel.add(infoPanel, BorderLayout.NORTH)
        panel.add(tabbedPane, BorderLayout.CENTER)
        
        return panel
    }
    
    private fun createFileList(files: List<String>): JComponent {
        val list = JList(files.toTypedArray()).apply {
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            cellRenderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    
                    // 添加图标
                    icon = when {
                        value in diff.addedFiles -> AllIcons.General.Add
                        value in diff.modifiedFiles -> AllIcons.General.Modified
                        value in diff.deletedFiles -> AllIcons.General.Remove
                        else -> null
                    }
                    
                    return this
                }
            }
        }
        
        // 添加右键菜单
        val popupMenu = JPopupMenu().apply {
            add(JMenuItem("复制路径").apply {
                addActionListener {
                    val selectedValue = list.selectedValue as? String
                    if (selectedValue != null) {
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(StringSelection(selectedValue), null)
                    }
                }
            })
        }
        
        list.componentPopupMenu = popupMenu
        
        return JBScrollPane(list)
    }
    
    override fun createActions(): Array<Action> {
        return arrayOf(okAction)
    }
} 