package com.stackfilesync.intellij.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.stackfilesync.intellij.model.P2PNodeConfig
import com.stackfilesync.intellij.service.P2PNodeConfigService
import com.stackfilesync.intellij.p2p.P2PConnectionManager
import javax.swing.*
import javax.swing.table.AbstractTableModel
import java.awt.BorderLayout
import java.awt.Dimension

class P2PConfigDialog(private val project: Project) : DialogWrapper(project) {
    private val configService = P2PNodeConfigService.getInstance(project)
    private val connectionManager = P2PConnectionManager.getInstance(project)
    private val tableModel = NodeTableModel()
    private val table = JBTable(tableModel)
    
    init {
        title = "P2P 节点配置"
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 添加表格
        val scrollPane = JBScrollPane(table)
        scrollPane.preferredSize = Dimension(600, 300)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // 添加按钮面板
        val buttonPanel = JPanel().apply {
            add(JButton("添加节点").apply {
                addActionListener {
                    val dialog = P2PNodeConfigDialog(project)
                    if (dialog.showAndGet()) {
                        val config = dialog.getNodeConfig()
                        configService.addNode(config)
                        tableModel.fireTableDataChanged()
                        // 如果节点已启用，立即启动它
                        if (config.enabled) {
                            connectionManager.startNode(config)
                        }
                    }
                }
            })
            
            add(JButton("编辑").apply {
                addActionListener {
                    val selectedRow = table.selectedRow
                    if (selectedRow >= 0) {
                        val config = configService.getNodes()[selectedRow]
                        val dialog = P2PNodeConfigDialog(project, config)
                        if (dialog.showAndGet()) {
                            configService.updateNode(selectedRow, dialog.getNodeConfig())
                            tableModel.fireTableDataChanged()
                        }
                    }
                }
            })
            
            add(JButton("删除").apply {
                addActionListener {
                    val selectedRow = table.selectedRow
                    if (selectedRow >= 0) {
                        if (JOptionPane.showConfirmDialog(
                                null,
                                "确定要删除选中的节点吗？",
                                "确认删除",
                                JOptionPane.YES_NO_OPTION
                            ) == JOptionPane.YES_OPTION) {
                            configService.removeNode(selectedRow)
                            tableModel.fireTableDataChanged()
                        }
                    }
                }
            })
        }
        panel.add(buttonPanel, BorderLayout.SOUTH)
        
        return panel
    }
    
    private inner class NodeTableModel : AbstractTableModel() {
        private val columns = arrayOf("名称", "地址", "端口", "目标目录", "状态")
        
        override fun getRowCount(): Int = configService.getNodes().size
        override fun getColumnCount(): Int = columns.size
        override fun getColumnName(column: Int): String = columns[column]
        
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val node = configService.getNodes()[rowIndex]
            return when (columnIndex) {
                0 -> node.name
                1 -> node.address
                2 -> node.port
                3 -> node.targetDirectory
                4 -> if (node.enabled) "启用" else "禁用"
                else -> ""
            }
        }
    }
} 