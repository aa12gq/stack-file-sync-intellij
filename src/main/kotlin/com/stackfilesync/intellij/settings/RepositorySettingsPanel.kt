package com.stackfilesync.intellij.settings

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.JBUI
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import java.awt.BorderLayout
import java.awt.Component
import com.stackfilesync.intellij.model.Repository
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.components.JBList
import com.stackfilesync.intellij.model.BackupConfig
import javax.swing.DefaultListModel
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBTextField
import com.stackfilesync.intellij.model.AutoSyncConfig
import com.stackfilesync.intellij.model.InternalSyncConfig
import javax.swing.JPanel
import com.stackfilesync.intellij.model.PostSyncCommand
import com.intellij.openapi.project.Project
import java.awt.Dimension
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

class RepositorySettingsPanel(private val project: Project) {
    private var panel: DialogPanel? = null
    private var repository = Repository()
    private lateinit var commandList: JBList<PostSyncCommand>
    private lateinit var listModel: DefaultListModel<PostSyncCommand>

    fun createPanel(): JPanel {
        if (panel == null) {
            panel = panel {
                group("基本设置") {
                    row("仓库名称:") {
                        textField()
                            .bindText({ repository.name }, { repository.name = it })
                            .focused()  // 让名称输入框获得焦点
                            .validationOnInput {
                                if (it.text.isBlank()) error("仓库名称不能为空") else null
                            }
                    }
                    
                    row("Git仓库URL:") {
                        textField()
                            .bindText({ repository.url }, { repository.url = it })
                            .validationOnInput {
                                if (it.text.isBlank()) error("Git仓库URL不能为空") else null
                            }
                    }
                    
                    row("分支:") {
                        textField()
                            .bindText({ repository.branch }, { repository.branch = it })
                            .validationOnInput {
                                if (it.text.isBlank()) error("分支不能为空") else null
                            }
                    }
                }
                
                group("目录设置") {
                    row("源目录:") {
                        textField()
                            .bindText({ repository.sourceDirectory }, { repository.sourceDirectory = it })
                            .validationOnInput {
                                if (it.text.isBlank()) error("源目录不能为空") else null
                            }
                    }
                    
                    row("目标目录:") {
                        textField()
                            .bindText({ repository.targetDirectory }, { repository.targetDirectory = it })
                            .validationOnInput {
                                if (it.text.isBlank()) error("目标目录不能为空") else null
                            }
                    }
                }
                
                group("文件过滤") {
                    row("文件匹配模式:") {
                        textField()
                            .bindText({ repository.filePatterns.joinToString(",") }) { text ->
                                repository.filePatterns = text.split(",").map { it.trim() }
                            }
                    }
                    
                    row("排除模式:") {
                        textField()
                            .bindText({ repository.excludePatterns.joinToString(",") }) { text ->
                                repository.excludePatterns = text.split(",").map { it.trim() }
                            }
                    }
                }
                
                group("自动同步") {
                    row {
                        checkBox("启用自动同步")
                            .bindSelected(
                                { repository.autoSync?.enabled ?: false },
                                { enabled ->
                                    repository.autoSync = if (enabled) {
                                        AutoSyncConfig(enabled = true)
                                    } else null
                                }
                            )
                    }
                    
                    row("同步间隔(秒):") {
                        intTextField(0..3600)
                            .bindIntText(
                                { repository.autoSync?.interval ?: 300 },
                                { interval ->
                                    val current = repository.autoSync
                                    repository.autoSync = if (current != null) {
                                        current.copy(interval = interval)
                                    } else {
                                        AutoSyncConfig(interval = interval)
                                    }
                                }
                            )
                    }
                }
                
                group("备份设置") {
                    row {
                        checkBox("启用备份")
                            .bindSelected(
                                { repository.backupConfig?.enabled ?: true },
                                { enabled ->
                                    repository.backupConfig = BackupConfig(enabled = enabled)
                                }
                            )
                    }
                    
                    row("最大备份数:") {
                        intTextField(1..100)
                            .bindIntText(
                                { repository.backupConfig?.maxBackups ?: 10 },
                                { maxBackups ->
                                    val current = repository.backupConfig
                                    repository.backupConfig = if (current != null) {
                                        current.copy(maxBackups = maxBackups)
                                    } else {
                                        BackupConfig(maxBackups = maxBackups)
                                    }
                                }
                            )
                    }
                }
                
                group("内网同步") {
                    row {
                        checkBox("启用内网同步")
                            .bindSelected(
                                { repository.internalSync?.enabled ?: false },
                                { enabled ->
                                    repository.internalSync = if (enabled) {
                                        InternalSyncConfig(enabled = true)
                                    } else null
                                }
                            )
                    }
                    
                    row("网络路径:") {
                        textField()
                            .bindText(
                                { repository.internalSync?.networkPath ?: "" },
                                { path ->
                                    val current = repository.internalSync
                                    repository.internalSync = if (current != null) {
                                        current.copy(networkPath = path)
                                    } else {
                                        InternalSyncConfig(networkPath = path)
                                    }
                                }
                            )
                    }
                }

                group("后处理命令") {
                    row {
                        comment("""
                            后处理命令会在文件同步完成后执行。常见用途：
                            1. 编译 proto 文件，例如：
                               目录: proto
                               命令: protoc --go_out=. *.proto
                            
                            2. 更新依赖，例如：
                               目录: .
                               命令: go mod tidy
                            
                            3. 生成代码，例如：
                               目录: api
                               命令: swag init
                            
                            4. 执行构建，例如：
                               目录: .
                               命令: mvn compile
                            
                            注意：目录路径可以是相对于项目根目录的路径，也可以是绝对路径
                        """.trimIndent())
                    }
                    
                    // 初始化命令列表
                    commandList = JBList<PostSyncCommand>().apply {
                        model = DefaultListModel<PostSyncCommand>().also { listModel = it }
                        cellRenderer = object : DefaultListCellRenderer() {
                            override fun getListCellRendererComponent(
                                list: JList<*>,
                                value: Any?,
                                index: Int,
                                isSelected: Boolean,
                                cellHasFocus: Boolean
                            ): Component {
                                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                                val command = value as PostSyncCommand
                                text = "${command.directory}: ${command.command}"
                                return this
                            }
                        }
                    }
                    
                    // 加载现有命令
                    repository.postSyncCommands.forEach { command ->
                        listModel.addElement(command)
                    }
                    
                    row {
                        cell(JBScrollPane(commandList))
                            .align(Align.FILL)
                            .resizableColumn()
                    }
                    
                    row {
                        button("添加命令") {
                            val dialog = object : DialogWrapper(project, false) {  // 改为 false
                                private val directoryField = JBTextField().apply {
                                    preferredSize = Dimension(300, 30)  // 设置输入框大小
                                }
                                private val commandField = JBTextField().apply {
                                    preferredSize = Dimension(300, 30)
                                }
                                
                                init {
                                    title = "添加后处理命令"
                                    isResizable = true  // 允许调整大小
                                    init()  // 必须最后调用
                                }
                                
                                override fun createCenterPanel(): JComponent {
                                    return JPanel(BorderLayout(10, 10)).apply {
                                        border = JBUI.Borders.empty(10)
                                        preferredSize = Dimension(400, 150)
                                        
                                        add(JPanel(BorderLayout(5, 5)).apply {
                                            add(JLabel("目录:"), BorderLayout.WEST)
                                            add(directoryField, BorderLayout.CENTER)
                                        }, BorderLayout.NORTH)
                                        
                                        add(JPanel(BorderLayout(5, 5)).apply {
                                            add(JLabel("命令:"), BorderLayout.WEST)
                                            add(commandField, BorderLayout.CENTER)
                                        }, BorderLayout.CENTER)
                                        
                                        add(JLabel("注意：目录路径可以是相对于项目根目录的路径，也可以是绝对路径"), 
                                            BorderLayout.SOUTH)
                                    }
                                }
                                
                                override fun getPreferredFocusedComponent() = directoryField
                                
                                fun getCommand(): PostSyncCommand? {
                                    val dir = directoryField.text.trim()
                                    val cmd = commandField.text.trim()
                                    if (dir.isBlank() || cmd.isBlank()) return null
                                    return PostSyncCommand(directory = dir, command = cmd)
                                }
                            }
                            
                            if (dialog.showAndGet()) {
                                dialog.getCommand()?.let { command ->
                                    listModel.addElement(command)
                                    updateCommands()
                                }
                            }
                        }
                        
                        button("删除命令") {
                            val selected = commandList.selectedIndex
                            if (selected >= 0) {
                                listModel.remove(selected)
                                updateCommands()
                            }
                        }
                    }
                }
            }
            // 确保命令列表被正确初始化
            updateCommandList()
        }
        
        // 创建一个带滚动条的面板
        val scrollPane = JBScrollPane(panel).apply {
            border = null  // 移除滚动面板的边框
            verticalScrollBar.unitIncrement = 16  // 设置滚动速度
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER  // 禁用水平滚动条
        }
        
        // 创建一个容器面板来持有滚动面板
        return JPanel(BorderLayout()).apply {
            add(scrollPane, BorderLayout.CENTER)
            preferredSize = Dimension(800, 600)  // 设置首选大小
        }
    }

    fun getRepository(): Repository {
        panel?.apply()  // 确保所有更改都被应用到 repository 对象
        return repository
    }

    fun setRepository(repo: Repository) {
        repository = repo.copy()
        if (panel == null) {
            createPanel()
        }
        updateCommandList()  // 更新命令列表
        panel?.reset()  // 重置面板以显示新的数据
    }

    // 更新命令列表
    private fun updateCommandList() {
        if (::listModel.isInitialized) {
            listModel.clear()
            repository.postSyncCommands.forEach { command ->
                listModel.addElement(command)
            }
        }
    }

    // 更新命令列表到仓库对象
    private fun updateCommands() {
        val commands = mutableListOf<PostSyncCommand>()
        for (i in 0 until listModel.size) {
            commands.add(listModel.getElementAt(i))
        }
        repository.postSyncCommands = commands
        panel?.apply()  // 应用更改到面板
    }
} 