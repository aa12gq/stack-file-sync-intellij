package com.stackfilesync.intellij.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import javax.swing.*
import javax.swing.table.AbstractTableModel
import com.stackfilesync.intellij.model.Repository

class SyncSettingsConfigurable : Configurable {
    private var mainPanel: JPanel? = null
    private var repositoriesTable: JBTable? = null
    private var backupCheckBox: JBCheckBox? = null
    private val settings = SyncSettings.getInstance()
    private var repositories = mutableListOf<Repository>()

    override fun getDisplayName(): String = "Stack File Sync"

    override fun createComponent(): JComponent {
        // 创建仓库表格
        repositoriesTable = JBTable(RepositoriesTableModel())
        repositoriesTable?.setShowGrid(true)

        // 创建备份选项
        backupCheckBox = JBCheckBox("同步前备份文件", settings.isBackupEnabled())

        // 创建按钮
        val addButton = JButton("添加仓库")
        val removeButton = JButton("删除仓库")
        val buttonPanel = JPanel()
        buttonPanel.add(addButton)
        buttonPanel.add(removeButton)

        // 添加按钮事件
        addButton.addActionListener { addRepository() }
        removeButton.addActionListener { removeSelectedRepository() }

        // 构建主面板
        mainPanel = FormBuilder.createFormBuilder()
            .addComponent(JBScrollPane(repositoriesTable))
            .addComponent(buttonPanel)
            .addComponent(backupCheckBox!!)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        loadSettings()
        return mainPanel!!
    }

    override fun isModified(): Boolean {
        return backupCheckBox?.isSelected != settings.isBackupEnabled() ||
                repositories != settings.getRepositories()
    }

    override fun apply() {
        settings.setBackupEnabled(backupCheckBox?.isSelected ?: true)
        settings.setRepositories(repositories)
    }

    private fun loadSettings() {
        repositories.clear()
        repositories.addAll(settings.getRepositories())
        backupCheckBox?.isSelected = settings.isBackupEnabled()
        repositoriesTable?.updateUI()
    }

    private fun addRepository() {
        repositories.add(Repository())
        repositoriesTable?.updateUI()
    }

    private fun removeSelectedRepository() {
        val selectedRow = repositoriesTable?.selectedRow ?: -1
        if (selectedRow >= 0) {
            repositories.removeAt(selectedRow)
            repositoriesTable?.updateUI()
        }
    }

    private inner class RepositoriesTableModel : AbstractTableModel() {
        private val columns = listOf("名称", "URL", "分支", "源目录", "目标目录")

        override fun getRowCount(): Int = repositories.size
        override fun getColumnCount(): Int = columns.size
        override fun getColumnName(column: Int): String = columns[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val repo = repositories[rowIndex]
            return when (columnIndex) {
                0 -> repo.name
                1 -> repo.url
                2 -> repo.branch
                3 -> repo.sourceDirectory
                4 -> repo.targetDirectory
                else -> ""
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = true

        override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
            val repo = repositories[rowIndex]
            when (columnIndex) {
                0 -> repo.name = value?.toString() ?: ""
                1 -> repo.url = value?.toString() ?: ""
                2 -> repo.branch = value?.toString() ?: "main"
                3 -> repo.sourceDirectory = value?.toString() ?: ""
                4 -> repo.targetDirectory = value?.toString() ?: ""
            }
            fireTableCellUpdated(rowIndex, columnIndex)
        }
    }
} 