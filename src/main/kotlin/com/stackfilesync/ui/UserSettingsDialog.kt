package com.stackfilesync.ui

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.stackfilesync.service.UserDiscoveryService
import javax.swing.*
import java.awt.BorderLayout
import java.awt.Dimension

class UserSettingsDialog(private val project: Project) : DialogWrapper(project) {
    
    private val usernameField = JTextField()
    private val userDiscoveryService = UserDiscoveryService.getInstance(project)
    
    init {
        title = "用户设置"
        init()
        
        // 加载当前用户名称
        val currentUser = userDiscoveryService.getCurrentUser()
        usernameField.text = currentUser?.username ?: ""
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 创建表单面板
        val formPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        }
        
        // 用户名称输入
        val usernamePanel = JPanel(BorderLayout())
        usernamePanel.add(JLabel("用户名称:"), BorderLayout.WEST)
        usernameField.preferredSize = Dimension(200, 30)
        usernamePanel.add(usernameField, BorderLayout.CENTER)
        
        formPanel.add(usernamePanel)
        formPanel.add(Box.createVerticalStrut(10))
        
        // 添加提示信息
        val tipLabel = JLabel("<html>设置后的用户名将在下次启动时生效<br>用户名后会自动添加编号，无需手动添加</html>")
        tipLabel.foreground = java.awt.Color.GRAY
        formPanel.add(tipLabel)
        
        panel.add(formPanel, BorderLayout.CENTER)
        
        return panel
    }
    
    override fun doOKAction() {
        val newUsername = usernameField.text.trim()
        if (newUsername.isNotEmpty()) {
            // 保存用户名到设置
            PropertiesComponent.getInstance().setValue("stack.file.sync.username", newUsername)
            
            // 显示提示
            JOptionPane.showMessageDialog(
                null,
                "用户名已保存，将在下次启动时生效",
                "设置成功",
                JOptionPane.INFORMATION_MESSAGE
            )
            
            super.doOKAction()
        } else {
            JOptionPane.showMessageDialog(
                null,
                "用户名不能为空",
                "输入错误",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }
} 