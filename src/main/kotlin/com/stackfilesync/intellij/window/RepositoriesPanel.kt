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
import javax.swing.JFileChooser

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
            add(JButton(AllIcons.Actions.Download).apply {
                toolTipText = "导入配置"
                addActionListener {
                    val fileChooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.FILES_ONLY
                        fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                            "JSON 文件 (*.json)", "json"
                        )
                    }
                    if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                        try {
                            val file = fileChooser.selectedFile
                            val json = file.readText()
                            val settings = SyncSettingsState.getInstance()
                            val repositories = com.google.gson.Gson().fromJson(
                                json,
                                Array<Repository>::class.java
                            ).toList()
                            settings.setRepositories(repositories)
                            loadRepositories()
                            com.intellij.openapi.ui.Messages.showInfoMessage(
                                "成功导入 ${repositories.size} 个仓库配置",
                                "导入成功"
                            )
                        } catch (e: Exception) {
                            com.intellij.openapi.ui.Messages.showErrorDialog(
                                "导入配置失败：${e.message}",
                                "导入失败"
                            )
                        }
                    }
                }
            })
            add(Box.createHorizontalStrut(5))
            add(JButton(AllIcons.Actions.Upload).apply {
                toolTipText = "导出配置"
                addActionListener {
                    val selectedRepo = repositoryList.selectedValue
                    val settings = SyncSettingsState.getInstance()
                    val repositories = if (selectedRepo != null) {
                        // 如果选中了仓库，只导出选中的仓库
                        listOf(selectedRepo)
                    } else {
                        // 如果没有选中仓库，导出所有仓库
                        settings.getRepositories()
                    }
                    
                    // 如果没有可导出的仓库，显示提示
                    if (repositories.isEmpty()) {
                        com.intellij.openapi.ui.Messages.showWarningDialog(
                            "没有可导出的仓库配置",
                            "导出失败"
                        )
                        return@addActionListener
                    }
                    
                    // 设置默认文件名
                    val defaultFileName = if (selectedRepo != null) {
                        "${selectedRepo.name}_config.json"
                    } else {
                        "all_repositories_config.json"
                    }
                    
                    val fileChooser = JFileChooser().apply {
                        fileSelectionMode = JFileChooser.FILES_ONLY
                        selectedFile = java.io.File(defaultFileName)
                        fileFilter = javax.swing.filechooser.FileNameExtensionFilter(
                            "JSON 文件 (*.json)", "json"
                        )
                    }
                    
                    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                        try {
                            var file = fileChooser.selectedFile
                            // 如果文件名没有.json后缀，自动添加
                            if (!file.name.toLowerCase().endsWith(".json")) {
                                file = java.io.File(file.absolutePath + ".json")
                            }
                            
                            val json = com.google.gson.GsonBuilder()
                                .setPrettyPrinting()
                                .create()
                                .toJson(repositories)
                            file.writeText(json)
                            
                            val message = if (selectedRepo != null) {
                                "仓库 [${selectedRepo.name}] 的配置已导出到：${file.absolutePath}"
                            } else {
                                "所有仓库配置（共 ${repositories.size} 个）已导出到：${file.absolutePath}"
                            }
                            
                            com.intellij.openapi.ui.Messages.showInfoMessage(
                                message,
                                "导出成功"
                            )
                        } catch (e: Exception) {
                            com.intellij.openapi.ui.Messages.showErrorDialog(
                                "导出配置失败：${e.message}",
                                "导出失败"
                            )
                        }
                    }
                }
            })
            add(Box.createHorizontalStrut(5))
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
                            
                            // 更新配置，设置变更监听器会处理自动同步的启动和停止
                            val repos = settings.getRepositories().map { 
                                if (it.name == updatedRepo.name) updatedRepo else it 
                            }
                            settings.setRepositories(repos)
                            
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