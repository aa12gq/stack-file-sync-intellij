package com.stackfilesync.intellij.window

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.settings.SyncSettings
import javax.swing.*
import javax.swing.table.AbstractTableModel
import com.intellij.openapi.actionSystem.AnActionEvent
import javax.swing.table.DefaultTableCellRenderer
import java.awt.Component
import com.stackfilesync.intellij.dialog.BackupBrowserDialog
import com.intellij.ui.JBColor
import com.intellij.icons.AllIcons
import com.stackfilesync.intellij.icons.StackFileSync
import com.intellij.util.ui.UIUtil
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.actionSystem.DataKey

class RepositoriesPanel(private val project: Project) : JPanel() {
    companion object {
        private val REPOSITORY_KEY = DataKey.create<Repository>("repository")
    }

    private val table: JBTable
    private val tableModel: RepositoriesTableModel
    
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(10)
        
        // 创建工具栏
        val actionGroup = DefaultActionGroup().apply {
            add(ActionManager.getInstance().getAction("StackFileSync.SyncFiles"))
            add(ActionManager.getInstance().getAction("StackFileSync.Configure"))
            addSeparator()
            add(ActionManager.getInstance().getAction("StackFileSync.RefreshRepositories"))
        }
        
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("StackFileSyncToolbar", actionGroup, true)
        add(toolbar.component)
        
        // 创建表格
        tableModel = RepositoriesTableModel()
        table = JBTable(tableModel).apply {
            setShowGrid(true)
            autoResizeMode = JTable.AUTO_RESIZE_ALL_COLUMNS
        }
        
        add(JBScrollPane(table))
        
        // 设置自动同步列的渲染器
        table.getColumnModel().getColumn(3).cellRenderer = AutoSyncColumnRenderer()
        
        // 设置表格样式
        table.apply {
            gridColor = JBColor.border()
            rowHeight = JBUI.scale(24)
            intercellSpacing = JBUI.size(1)
            
            // 设置选择样式
            selectionBackground = UIUtil.getTableSelectionBackground(true)
            selectionForeground = UIUtil.getTableSelectionForeground(true)
        }
        
        // 加载数据
        loadRepositories()
    }
    
    fun refresh() {
        loadRepositories()
        tableModel.fireTableDataChanged()
    }
    
    private fun loadRepositories() {
        tableModel.repositories = SyncSettings.getInstance().getRepositories()
    }
    
    private inner class RepositoriesTableModel : AbstractTableModel() {
        var repositories: List<Repository> = emptyList()
        private val columns = listOf("名称", "URL", "分支", "自动同步", "最后同步时间")
        
        override fun getRowCount(): Int = repositories.size
        override fun getColumnCount(): Int = columns.size
        override fun getColumnName(column: Int): String = columns[column]
        
        override fun getColumnClass(columnIndex: Int): Class<*> {
            return when (columnIndex) {
                3 -> Icon::class.java // 自动同步状态列显示图标
                else -> String::class.java
            }
        }
        
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val repo = repositories[rowIndex]
            return when (columnIndex) {
                0 -> repo.name
                1 -> repo.url
                2 -> repo.branch
                3 -> if (repo.autoSync?.enabled == true) {
                    StackFileSync.AutoSync
                } else {
                    StackFileSync.AutoSyncDisabled
                }
                4 -> "N/A" // TODO: 实现最后同步时间
                else -> ""
            }
        }
    }

    // 添加右键菜单
    private fun createPopupMenu(): JPopupMenu {
        return JPopupMenu().apply {
            add(JMenuItem("同步文件").apply {
                addActionListener {
                    val selectedRow = table.selectedRow
                    if (selectedRow >= 0) {
                        val repo = tableModel.repositories[selectedRow]
                        ActionManager.getInstance()
                            .getAction("StackFileSync.SyncFiles")
                            .actionPerformed(AnActionEvent(
                                null,
                                SimpleDataContext.builder()
                                    .add(CommonDataKeys.PROJECT, project)
                                    .add(REPOSITORY_KEY, repo)
                                    .build(),
                                "StackFileSync",
                                Presentation(),
                                ActionManager.getInstance(),
                                0
                            ))
                    }
                }
            })
            
            addSeparator()
            
            add(JMenuItem("启用/禁用自动同步").apply {
                addActionListener {
                    val selectedRow = table.selectedRow
                    if (selectedRow >= 0) {
                        val repo = tableModel.repositories[selectedRow]
                        val action = ActionManager.getInstance()
                            .getAction("StackFileSync.ToggleAutoSync")
                        
                        val dataContext = SimpleDataContext.builder()
                            .add(CommonDataKeys.PROJECT, project)
                            .add(REPOSITORY_KEY, repo)
                            .build()
                        
                        val event = AnActionEvent(
                            null,
                            dataContext,
                            "StackFileSync",
                            Presentation(),
                            ActionManager.getInstance(),
                            0
                        )
                        
                        action.actionPerformed(event)
                        tableModel.fireTableDataChanged()
                    }
                }
            })
            
            addSeparator()
            
            add(JMenuItem("浏览备份历史").apply {
                addActionListener {
                    val selectedRow = table.selectedRow
                    if (selectedRow >= 0) {
                        val repo = tableModel.repositories[selectedRow]
                        BackupBrowserDialog(project, repo).show()
                    }
                }
            })
        }
    }

    // 自定义表格渲染器
    private class AutoSyncColumnRenderer : DefaultTableCellRenderer() {
        override fun getTableCellRendererComponent(
            table: JTable?,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
            
            if (value is Icon) {
                icon = value
                text = if ((value as? ImageIcon)?.description == "enabled") {
                    "已启用"
                } else {
                    "已禁用"
                }
            }
            
            return this
        }
    }
} 