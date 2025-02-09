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
import javax.swing.JCheckBox
import com.stackfilesync.intellij.model.AutoSyncConfig
import com.stackfilesync.intellij.sync.AutoSyncManager

class RepositoriesPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val listModel = DefaultListModel<Repository>()
    private val repositoryList = JBList(listModel).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = RepositoryListCellRenderer()
    }
    private var isSyncing = false
    private lateinit var buttonPanel: JPanel

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
            
            // 添加自动同步复选框
            val autoSyncPanel = JPanel(BorderLayout()).apply {
                border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                val autoSyncCheckBox = JCheckBox("自动同步").apply {
                    toolTipText = "开启后将使用全量同步模式，无需手动选择文件"
                    addActionListener {
                        val selected = repositoryList.selectedValue
                        if (selected != null) {
                            // 更新仓库配置
                            val settings = SyncSettingsState.getInstance()
                            val updatedRepo = selected.copy(
                                autoSync = if (isSelected) {
                                    // 保留原有的时间间隔设置，如果没有则使用默认值
                                    val currentInterval = selected.autoSync?.interval ?: 300
                                    AutoSyncConfig(enabled = true, interval = currentInterval)
                                } else null
                            )
                            
                            // 保存更新后的仓库配置
                            val repos = settings.getRepositories().map { 
                                if (it.name == updatedRepo.name) updatedRepo else it 
                            }
                            settings.setRepositories(repos)
                            
                            // 更新自动同步管理器
                            val autoSyncManager = AutoSyncManager.getInstance(project)
                            if (isSelected) {
                                autoSyncManager.startAllAutoSync()
                            } else {
                                autoSyncManager.stopAllAutoSync()
                            }
                            
                            // 刷新列表
                            loadRepositories()
                        }
                    }
                }
                add(autoSyncCheckBox, BorderLayout.EAST)
            }
            add(autoSyncPanel, BorderLayout.SOUTH)
        }

        // 创建底部操作按钮
        buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            add(Box.createHorizontalGlue())
            
            // 全量同步按钮
            add(JButton("全量同步").apply {
                addActionListener {
                    if (!isSyncing) {
                        val selected = repositoryList.selectedValue
                        if (selected != null) {
                            startSync(selected, false)
                        }
                    }
                }
            })
            
            add(Box.createHorizontalStrut(10))  // 添加间距
            
            // 选择性同步按钮
            add(JButton("选择性同步").apply {
                addActionListener {
                    if (!isSyncing) {
                        val selected = repositoryList.selectedValue
                        if (selected != null) {
                            startSync(selected, true)
                        }
                    }
                }
            })
            
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        }

        // 组装面板
        add(toolbar, BorderLayout.NORTH)
        add(listPanel, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)

        // 添加列表选择监听器
        repositoryList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                val selected = repositoryList.selectedValue
                if (selected != null) {
                    // 更新自动同步复选框状态
                    val autoSyncCheckBox = (listPanel.components.last() as JPanel)
                        .components.last() as JCheckBox
                    autoSyncCheckBox.isSelected = selected.autoSync?.enabled ?: false
                }
            }
        }

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

    private fun startSync(repository: Repository, showFileSelection: Boolean = true) {
        isSyncing = true
        val syncButton = (buttonPanel.components.last() as JButton)
        val fullSyncButton = (buttonPanel.components[1] as JButton)
        syncButton.isEnabled = false
        fullSyncButton.isEnabled = false
        syncButton.text = "同步中..."
        fullSyncButton.text = "同步中..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(
            project,
            "同步文件",
            false
        ) {
            override fun run(indicator: ProgressIndicator) {
                val syncManager = FileSyncManager(project, indicator)
                syncManager.sync(repository, showFileSelection, isAutoSync = false)
            }
        })
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
            // 显示仓库名称和自动同步状态
            text = buildString {
                append(value.name)
                if (value.autoSync?.enabled == true) {
                    append(" [自动同步已开启]")
                }
            }
            icon = AllIcons.Nodes.Module
            
            // 设置提示信息
            toolTipText = buildString {
                append("仓库: ${value.name}")
                append("\n源目录: ${value.sourceDirectory}")
                append("\n目标目录: ${value.targetDirectory}")
                if (value.autoSync?.enabled == true) {
                    append("\n自动同步: 已开启 (间隔: ${value.autoSync?.interval ?: 300}秒)")
                }
            }
        }
        return this
    }
} 