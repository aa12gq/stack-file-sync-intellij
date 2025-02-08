package com.stackfilesync.intellij.window

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.Align
import com.stackfilesync.intellij.settings.SyncSettingsState
import com.stackfilesync.intellij.sync.FileSyncManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.application.invokeLater
import javax.swing.JPanel
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.Box
import javax.swing.BoxLayout
import com.intellij.ui.components.JBList
import javax.swing.DefaultListModel
import javax.swing.ListSelectionModel
import com.stackfilesync.intellij.model.Repository
import com.intellij.icons.AllIcons
import javax.swing.JLabel
import javax.swing.BorderFactory
import java.awt.Dimension
import com.intellij.openapi.options.ShowSettingsUtil

class RepositoriesPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val listModel = DefaultListModel<Repository>()
    private val repositoryList = JBList(listModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = RepositoryListCellRenderer()
    }
    private var isSyncing = false
    private lateinit var syncButton: JButton

    init {
        // 创建顶部工具栏
        val toolbar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(JLabel("仓库列表"))
            add(Box.createHorizontalGlue())
            add(JButton(AllIcons.Actions.Refresh).apply {
                toolTipText = "刷新仓库列表"
                addActionListener {
                    loadRepositories()
                }
            })
            add(Box.createHorizontalStrut(5))  // 添加一点间距
            add(JButton(AllIcons.General.Settings).apply {
                toolTipText = "管理仓库"
                addActionListener {
                    ShowSettingsUtil.getInstance().showSettingsDialog(
                        project,
                        "Stack File Sync"
                    )
                }
            })
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        // 创建仓库列表面板
        val listPanel = JPanel(BorderLayout()).apply {
            add(JBScrollPane(repositoryList), BorderLayout.CENTER)
        }

        // 创建底部操作按钮
        syncButton = JButton("同步").apply {
            addActionListener {
                if (!isSyncing) {
                    val selected = repositoryList.selectedValue
                    if (selected != null) {
                        startSync(selected)
                    }
                }
            }
        }
        
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(Box.createHorizontalGlue())
            add(syncButton)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        // 组装面板
        add(toolbar, BorderLayout.NORTH)
        add(listPanel, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)

        // 加载仓库列表
        loadRepositories()

        // 设置首选大小
        preferredSize = Dimension(300, 400)
    }

    private fun loadRepositories() {
        listModel.clear()
        val settings = SyncSettingsState.getInstance()
        settings.getRepositories().forEach { repository ->
            listModel.addElement(repository)
        }
    }

    fun refresh() {
        loadRepositories()
    }

    private fun startSync(repository: Repository) {
        isSyncing = true
        syncButton.isEnabled = false
        syncButton.text = "同步中..."

        ProgressManager.getInstance().run(
            object : Task.Backgroundable(project, "同步文件", false) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        FileSyncManager(project, indicator).sync(repository)
                    } finally {
                        invokeLater {
                            isSyncing = false
                            syncButton.isEnabled = true
                            syncButton.text = "同步"
                        }
                    }
                }
            }
        )
    }
}

class RepositoryListCellRenderer : javax.swing.DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: javax.swing.JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): java.awt.Component {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
        if (value is Repository) {
            text = value.name
            icon = AllIcons.Nodes.Module
        }
        return this
    }
} 