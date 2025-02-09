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
import com.intellij.ui.dsl.builder.bindText
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import com.intellij.icons.AllIcons

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
                    row("远程目录:") {
                        textField()
                            .bindText({ repository.sourceDirectory }, { repository.sourceDirectory = it })
                            .validationOnInput {
                                if (it.text.isBlank()) error("远程目录不能为空") else null
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
                        val autoSyncCheckBox = JCheckBox("启用自动同步")
                        autoSyncCheckBox.isSelected = repository.autoSync?.enabled ?: false
                        autoSyncCheckBox.addActionListener {
                            repository.autoSync = if (autoSyncCheckBox.isSelected) {
                                AutoSyncConfig(enabled = true)
                            } else null
                        }
                        cell(autoSyncCheckBox)
                            .comment("开启后将使用全量同步模式，无需手动选择文件")
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
                            .comment("设置自动同步的时间间隔，范围：0-3600秒")
                    }
                }
                
                group("备份设置") {
                    row {
                        val backupCheckBox = JCheckBox("启用备份")
                        backupCheckBox.isSelected = repository.backupConfig?.enabled ?: true
                        backupCheckBox.addActionListener {
                            repository.backupConfig = BackupConfig(enabled = backupCheckBox.isSelected)
                        }
                        cell(backupCheckBox)
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
                        val internalSyncCheckBox = JCheckBox("启用内网同步")
                        internalSyncCheckBox.isSelected = repository.internalSync?.enabled ?: false
                        internalSyncCheckBox.addActionListener {
                            repository.internalSync = if (internalSyncCheckBox.isSelected) {
                                InternalSyncConfig(enabled = true)
                            } else null
                        }
                        cell(internalSyncCheckBox)
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
                                text = "[${command.order}] ${command.directory}: ${command.command}"
                                return this
                            }
                        }
                    }
                    
                    // 加载现有命令并按序号排序
                    repository.postSyncCommands.sortedBy { it.order }.forEach { command ->
                        listModel.addElement(command)
                    }
                    
                    row {
                        cell(JBScrollPane(commandList))
                            .align(Align.FILL)
                            .resizableColumn()
                    }
                    
                    row {
                        button("添加命令") {
                            val dialog = object : DialogWrapper(project, false) {
                                private val orderField = JBTextField().apply {
                                    preferredSize = Dimension(100, 30)
                                    text = "0"
                                    toolTipText = "请输入非负整数，数字越小执行顺序越靠前"
                                    document.addDocumentListener(object : DocumentListener {
                                        override fun insertUpdate(e: DocumentEvent) = validateOrder()
                                        override fun removeUpdate(e: DocumentEvent) = validateOrder()
                                        override fun changedUpdate(e: DocumentEvent) = validateOrder()
                                    })
                                }
                                private val directoryField = JBTextField().apply {
                                    preferredSize = Dimension(300, 30)
                                }
                                private val commandField = JBTextField().apply {
                                    preferredSize = Dimension(300, 30)
                                }

                                private fun validateOrder() {
                                    try {
                                        val order = orderField.text.toInt()
                                        if (order < 0) {
                                            setErrorText("执行序号不能为负数", orderField)
                                        } else {
                                            setErrorText(null, orderField)
                                        }
                                    } catch (e: NumberFormatException) {
                                        setErrorText("请输入有效的整数", orderField)
                                    }
                                }

                                init {
                                    init()
                                    title = "添加后处理命令"
                                }

                                override fun createCenterPanel(): JComponent {
                                    return JPanel(GridBagLayout()).apply {
                                        border = JBUI.Borders.empty(10)
                                        preferredSize = Dimension(400, 150)
                                        
                                        val gbc = GridBagConstraints().apply {
                                            fill = GridBagConstraints.HORIZONTAL
                                            insets = Insets(5, 5, 5, 5)
                                        }
                                        
                                        // 序号
                                        gbc.apply {
                                            gridx = 0
                                            gridy = 0
                                            weightx = 0.0
                                        }
                                        add(JLabel("执行序号:"), gbc)
                                        
                                        // 序号说明
                                        gbc.apply {
                                            gridx = 0
                                            gridy = 1
                                            gridwidth = 2
                                        }
                                        add(JLabel("说明：数字越小执行顺序越靠前，必须为非负整数").apply {
                                            font = font.deriveFont(font.size2D - 2f)
                                            foreground = UIManager.getColor("Label.disabledForeground")
                                        }, gbc)
                                        
                                        // 目录
                                        gbc.apply {
                                            gridx = 0
                                            gridy = 2
                                            gridwidth = 1
                                            weightx = 0.0
                                        }
                                        add(JLabel("目录:"), gbc)
                                        
                                        gbc.apply {
                                            gridx = 1
                                            weightx = 1.0
                                        }
                                        add(directoryField, gbc)
                                        
                                        // 命令
                                        gbc.apply {
                                            gridx = 0
                                            gridy = 3
                                            weightx = 0.0
                                        }
                                        add(JLabel("命令:"), gbc)
                                        
                                        gbc.apply {
                                            gridx = 1
                                            weightx = 1.0
                                        }
                                        add(commandField, gbc)
                                    }
                                }

                                override fun getPreferredFocusedComponent() = directoryField
                                
                                override fun doOKAction() {
                                    val order = try {
                                        val value = orderField.text.toInt()
                                        if (value < 0) {
                                            setErrorText("执行序号不能为负数", orderField)
                                            return
                                        }
                                        value
                                    } catch (e: NumberFormatException) {
                                        setErrorText("请输入有效的整数", orderField)
                                        return
                                    }
                                    val dir = directoryField.text.trim()
                                    val cmd = commandField.text.trim()
                                    if (dir.isBlank() || cmd.isBlank()) return
                                    
                                    val newCommand = PostSyncCommand(
                                        directory = dir,
                                        command = cmd,
                                        order = order
                                    )
                                    listModel.addElement(newCommand)
                                    // 重新排序列表
                                    val sortedCommands = (0 until listModel.size())
                                        .map { listModel.getElementAt(it) }
                                        .sortedBy { it.order }
                                    listModel.clear()
                                    sortedCommands.forEach { listModel.addElement(it) }
                                    updateCommands()
                                    super.doOKAction()
                                }
                            }
                            dialog.show()
                        }
                        
                        cell(JButton("删除命令").apply {
                            icon = AllIcons.Actions.GC
                            toolTipText = "删除选中的命令"
                            addActionListener {
                                val selected = commandList.selectedIndex
                                if (selected >= 0) {
                                    listModel.remove(selected)
                                    updateCommands()
                                }
                            }
                        }).align(Align.FILL)
                        
                        button("编辑命令") {
                            val selected = commandList.selectedIndex
                            if (selected >= 0) {
                                val command = listModel.getElementAt(selected)
                                val dialog = object : DialogWrapper(project, false) {
                                    private val orderField = JBTextField().apply {
                                        text = command.order.toString()
                                        preferredSize = Dimension(100, 30)
                                        toolTipText = "请输入非负整数，数字越小执行顺序越靠前"
                                        document.addDocumentListener(object : DocumentListener {
                                            override fun insertUpdate(e: DocumentEvent) = validateOrder()
                                            override fun removeUpdate(e: DocumentEvent) = validateOrder()
                                            override fun changedUpdate(e: DocumentEvent) = validateOrder()
                                        })
                                    }
                                    private val directoryField = JBTextField().apply {
                                        text = command.directory
                                        preferredSize = Dimension(300, 30)
                                    }
                                    private val commandField = JBTextField().apply {
                                        text = command.command
                                        preferredSize = Dimension(300, 30)
                                    }
                                    
                                    private fun validateOrder() {
                                        try {
                                            val order = orderField.text.toInt()
                                            if (order < 0) {
                                                setErrorText("执行序号不能为负数", orderField)
                                            } else {
                                                setErrorText(null, orderField)
                                            }
                                        } catch (e: NumberFormatException) {
                                            setErrorText("请输入有效的整数", orderField)
                                        }
                                    }
                                    
                                    init {
                                        title = "编辑后处理命令"
                                        init()
                                    }
                                    
                                    override fun createCenterPanel(): JComponent {
                                        return JPanel(GridBagLayout()).apply {
                                            border = JBUI.Borders.empty(10)
                                            preferredSize = Dimension(400, 150)
                                            
                                            val gbc = GridBagConstraints().apply {
                                                fill = GridBagConstraints.HORIZONTAL
                                                insets = Insets(5, 5, 5, 5)
                                            }
                                            
                                            // 序号
                                            gbc.apply {
                                                gridx = 0
                                                gridy = 0
                                                weightx = 0.0
                                            }
                                            add(JLabel("执行序号:"), gbc)
                                            
                                            gbc.apply {
                                                gridx = 1
                                                weightx = 1.0
                                            }
                                            add(orderField, gbc)
                                            
                                            // 序号说明
                                            gbc.apply {
                                                gridx = 0
                                                gridy = 1
                                                gridwidth = 2
                                            }
                                            add(JLabel("说明：数字越小执行顺序越靠前，必须为非负整数").apply {
                                                font = font.deriveFont(font.size2D - 2f)
                                                foreground = UIManager.getColor("Label.disabledForeground")
                                            }, gbc)
                                            
                                            // 目录
                                            gbc.apply {
                                                gridx = 0
                                                gridy = 2
                                                gridwidth = 1
                                                weightx = 0.0
                                            }
                                            add(JLabel("目录:"), gbc)
                                            
                                            gbc.apply {
                                                gridx = 1
                                                weightx = 1.0
                                            }
                                            add(directoryField, gbc)
                                            
                                            // 命令
                                            gbc.apply {
                                                gridx = 0
                                                gridy = 3
                                                weightx = 0.0
                                            }
                                            add(JLabel("命令:"), gbc)
                                            
                                            gbc.apply {
                                                gridx = 1
                                                weightx = 1.0
                                            }
                                            add(commandField, gbc)
                                        }
                                    }
                                    
                                    override fun getPreferredFocusedComponent() = directoryField
                                    
                                    override fun doOKAction() {
                                        val order = try {
                                            val value = orderField.text.toInt()
                                            if (value < 0) {
                                                setErrorText("执行序号不能为负数", orderField)
                                                return
                                            }
                                            value
                                        } catch (e: NumberFormatException) {
                                            setErrorText("请输入有效的整数", orderField)
                                            return
                                        }
                                        val dir = directoryField.text.trim()
                                        val cmd = commandField.text.trim()
                                        if (dir.isBlank() || cmd.isBlank()) return
                                        
                                        val newCommand = PostSyncCommand(
                                            directory = dir,
                                            command = cmd,
                                            order = order
                                        )
                                        listModel.set(selected, newCommand)
                                        // 重新排序列表
                                        val sortedCommands = (0 until listModel.size())
                                            .map { listModel.getElementAt(it) }
                                            .sortedBy { it.order }
                                        listModel.clear()
                                        sortedCommands.forEach { listModel.addElement(it) }
                                        updateCommands()
                                        super.doOKAction()
                                    }
                                }
                                
                                if (dialog.showAndGet()) {
                                    updateCommands()
                                }
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