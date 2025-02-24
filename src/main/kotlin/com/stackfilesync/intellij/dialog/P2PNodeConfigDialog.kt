package com.stackfilesync.intellij.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.stackfilesync.intellij.model.P2PNodeConfig
import javax.swing.*
import java.awt.*
import javax.swing.border.TitledBorder
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBLabel
import com.intellij.ui.DocumentAdapter
import javax.swing.event.DocumentEvent

class P2PNodeConfigDialog(
    project: Project,
    private val nodeConfig: P2PNodeConfig = P2PNodeConfig()
) : DialogWrapper(project) {

    private val nameField = JBTextField().apply {
        putClientProperty("StatusVisibleFunction", null)  // 启用占位符文本
        putClientProperty("JTextField.placeholderText", "例如: 我的工作电脑")
        toolTipText = "为这个节点起一个容易识别的名字"
    }
    
    private val addressField = JBTextField().apply {
        putClientProperty("StatusVisibleFunction", null)
        putClientProperty("JTextField.placeholderText", "例如: 192.168.1.100 或 localhost")
        toolTipText = "输入IP地址或主机名，本机可以使用localhost"
    }
    
    private val portField = JBTextField().apply {
        putClientProperty("StatusVisibleFunction", null)
        putClientProperty("JTextField.placeholderText", "例如: 8001")
        toolTipText = "输入1024-65535之间的端口号"
    }
    
    private val directoryChooser = TextFieldWithBrowseButton().apply {
        textField.putClientProperty("StatusVisibleFunction", null)
        textField.putClientProperty("JTextField.placeholderText", "选择接收文件的目标目录")
        addBrowseFolderListener(
            "选择目标目录",
            "选择接收到的文件要保存的位置",
            project,
            FileChooserDescriptor(false, true, false, false, false, false)
        )
    }
    
    private val patternsField = JBTextField().apply {
        putClientProperty("StatusVisibleFunction", null)
        putClientProperty("JTextField.placeholderText", "例如: *.txt,*.java,src/**/*")
        toolTipText = "使用逗号分隔的文件匹配模式，支持通配符 * 和 **"
    }
    
    private val excludePatternsField = JBTextField().apply {
        putClientProperty("StatusVisibleFunction", null)
        putClientProperty("JTextField.placeholderText", "例如: *.class,*.jar,target/**/*")
        toolTipText = "使用逗号分隔的排除文件模式，支持通配符 * 和 **"
    }

    private val enabledCheckBox = JCheckBox("启用节点").apply {
        isSelected = nodeConfig.enabled
        toolTipText = "是否启用此节点接收文件"
    }

    private val autoAcceptCheckBox = JCheckBox("自动接受文件").apply {
        isSelected = nodeConfig.autoAccept
        toolTipText = "是否自动接受符合过滤规则的文件"
    }

    init {
        title = if (nodeConfig.name.isEmpty()) "添加新节点" else "编辑节点"
        
        // 初始化字段值
        nameField.text = nodeConfig.name
        addressField.text = nodeConfig.address
        portField.text = if (nodeConfig.port > 0) nodeConfig.port.toString() else ""
        directoryChooser.text = nodeConfig.targetDirectory
        patternsField.text = nodeConfig.filePatterns.joinToString(",")
        excludePatternsField.text = nodeConfig.excludePatterns.joinToString(",")
        
        init()
    }

    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout(10, 10))
        
        // 创建表单面板
        val formPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            
            add(createSettingsGroup("基本设置") {
                add(createLabeledField("节点名称:", nameField))
                add(Box.createVerticalStrut(5))
                add(createLabeledField("监听地址:", addressField))
                add(Box.createVerticalStrut(5))
                add(createLabeledField("监听端口:", portField))
            })
            
            add(Box.createVerticalStrut(10))
            
            add(createSettingsGroup("目录设置") {
                add(createLabeledField("目标目录:", directoryChooser))
            })
            
            add(Box.createVerticalStrut(10))
            
            add(createSettingsGroup("文件过滤") {
                add(createLabeledField("文件匹配模式:", patternsField))
                add(Box.createVerticalStrut(5))
                add(createLabeledField("排除模式:", excludePatternsField))
            })
            
            add(Box.createVerticalStrut(10))
            
            add(createSettingsGroup("其他设置") {
                add(enabledCheckBox)
                add(Box.createVerticalStrut(5))
                add(autoAcceptCheckBox)
            })
        }
        
        mainPanel.add(formPanel, BorderLayout.CENTER)
        
        // 添加帮助信息
        mainPanel.add(createHelpPanel(), BorderLayout.SOUTH)
        
        return JBScrollPane(mainPanel).apply {
            preferredSize = Dimension(500, 600)
            border = null
        }
    }
    
    private fun createSettingsGroup(title: String, content: JPanel.() -> Unit): JPanel {
        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                title,
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION
            )
            alignmentX = Component.LEFT_ALIGNMENT
            content()
        }
    }
    
    private fun createLabeledField(label: String, component: JComponent): JPanel {
        return JPanel(BorderLayout(5, 0)).apply {
            add(JBLabel(label), BorderLayout.WEST)
            add(component, BorderLayout.CENTER)
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, component.preferredSize.height)
        }
    }
    
    private fun createHelpPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("帮助信息")
            add(JBTextArea().apply {
                text = """
                    提示：
                    1. 节点名称：给这个节点起一个容易识别的名字
                    2. 监听地址：本机使用localhost，或具体IP地址
                    3. 监听端口：建议使用1024以上的端口
                    4. 目标目录：接收到的文件将保存在这个目录
                    5. 文件匹配：
                       - 使用逗号分隔多个模式
                       - * 匹配任意字符
                       - ** 匹配任意目录
                       - 例如：*.txt,*.java,src/**/*
                    6. 自动接受：启用后将自动接收符合规则的文件
                """.trimIndent()
                isEditable = false
                background = null
                border = null
            }, BorderLayout.CENTER)
        }
    }

    override fun doOKAction() {
        // 验证输入
        if (!validateInput()) {
            return
        }
        
        // 更新配置
        nodeConfig.apply {
            name = nameField.text
            address = addressField.text
            port = portField.text.toIntOrNull() ?: 0
            targetDirectory = directoryChooser.text
            filePatterns = patternsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            excludePatterns = excludePatternsField.text.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            enabled = enabledCheckBox.isSelected
            autoAccept = autoAcceptCheckBox.isSelected
        }
        
        super.doOKAction()
    }
    
    private fun validateInput(): Boolean {
        when {
            nameField.text.isBlank() -> {
                showError("请输入节点名称")
                return false
            }
            addressField.text.isBlank() -> {
                showError("请输入监听地址")
                return false
            }
            portField.text.toIntOrNull() == null || portField.text.toInt() !in 1024..65535 -> {
                showError("请输入有效的端口号(1024-65535)")
                return false
            }
            directoryChooser.text.isBlank() -> {
                showError("请选择目标目录")
                return false
            }
        }
        return true
    }
    
    private fun showError(message: String) {
        Messages.showErrorDialog(message, "输入错误")
    }
    
    fun getNodeConfig(): P2PNodeConfig = nodeConfig
} 