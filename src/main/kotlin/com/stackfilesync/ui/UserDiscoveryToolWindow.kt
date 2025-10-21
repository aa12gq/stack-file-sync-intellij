package com.stackfilesync.ui

import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.stackfilesync.service.NotificationService
import com.stackfilesync.service.UserDiscoveryListener
import com.stackfilesync.service.UserDiscoveryService
import com.stackfilesync.service.UserInfo
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.*
import com.stackfilesync.intellij.window.RepositoriesPanel
import com.stackfilesync.service.SyncNotificationListener
import com.stackfilesync.service.SyncResponseListener

class UserDiscoveryToolWindow : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val userDiscoveryPanel = UserDiscoveryPanel(project)
        val content = ContentFactory.getInstance().createContent(userDiscoveryPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class UserDiscoveryPanel(private val project: Project) : JPanel(), UserDiscoveryListener, SyncNotificationListener, SyncResponseListener {
    
    private val userDiscoveryService = UserDiscoveryService.getInstance(project)
    private val notificationService = NotificationService.getInstance(project)
    
    private val userList = JBList<UserInfo>()
    private val userListModel = DefaultListModel<UserInfo>()
    private val currentUserLabel = JLabel("当前用户：未知")
    
    // 缓存存储最近处理过的消息ID
    private val processedMessageIds = mutableSetOf<String>()
    
    // 声明按钮面板为类级别变量，这样updateButtonsState方法可以访问它
    private val buttonPanel = JPanel()
    
    init {
        layout = BorderLayout()
        
        // 创建顶部面板显示当前用户信息
        val topPanel = JPanel(BorderLayout())
        topPanel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        
        // 添加用户信息和发现开关的面板
        val userInfoPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        userInfoPanel.add(currentUserLabel)
        
        // 添加发现功能开关
        val discoveryToggleCheckbox = JCheckBox("启用用户发现", userDiscoveryService.isDiscoveryEnabled())
        discoveryToggleCheckbox.addActionListener {
            val isEnabled = discoveryToggleCheckbox.isSelected
            userDiscoveryService.setDiscoveryEnabled(isEnabled)
            // 如果启用，立即刷新用户列表
            if (isEnabled) {
                userDiscoveryService.refreshUserList()
            }
            // 更新按钮状态
            updateButtonsState(isEnabled)
        }
        userInfoPanel.add(discoveryToggleCheckbox)
        
        topPanel.add(userInfoPanel, BorderLayout.WEST)
        
        // 在顶部面板添加设置按钮
        val settingsButton = JButton("设置")
        settingsButton.addActionListener {
            val dialog = UserSettingsDialog(project)
            if (dialog.showAndGet()) {
                // 如果用户确认设置，则刷新当前用户显示
                // 注意新的用户名将在下次启动时生效
                updateCurrentUserDisplay()
            }
        }
        topPanel.add(settingsButton, BorderLayout.EAST)
        
        // 配置用户列表
        userList.model = userListModel
        userList.cellRenderer = UserListCellRenderer(userDiscoveryService.getCurrentUser())
        
        // 创建主面板
        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(JBScrollPane(userList), BorderLayout.CENTER)
        
        val sendNotificationButton = JButton("发送通知")
        sendNotificationButton.addActionListener {
            val selectedUser = userList.selectedValue
            if (selectedUser != null) {
                val message = JOptionPane.showInputDialog(this, "输入要发送给 ${selectedUser.username} 的消息:")
                if (!message.isNullOrBlank()) {
                    userDiscoveryService.sendNotification(selectedUser, message)
                }
            } else {
                notificationService.showNotification("错误", "请先选择一个用户", NotificationType.ERROR)
            }
        }
        buttonPanel.add(sendNotificationButton)
        
        val syncButton = JButton("发送同步通知")
        syncButton.addActionListener {
            val selectedUser = userList.selectedValue
            if (selectedUser != null) {
                // 创建带备注的输入对话框
                val panel = JPanel(BorderLayout())
                
                // 模块名称输入
                val modulePanel = JPanel(BorderLayout())
                modulePanel.border = BorderFactory.createTitledBorder("模块名称")
                val moduleField = JTextField(20)
                modulePanel.add(moduleField, BorderLayout.CENTER)
                
                // 备注输入
                val remarkPanel = JPanel(BorderLayout())
                remarkPanel.border = BorderFactory.createTitledBorder("备注 (可选)")
                val remarkField = JTextField(20)
                remarkPanel.add(remarkField, BorderLayout.CENTER)
                
                // 组装面板
                panel.add(modulePanel, BorderLayout.NORTH)
                panel.add(remarkPanel, BorderLayout.CENTER)
                
                // 显示对话框
                val result = JOptionPane.showConfirmDialog(
                    this,
                    panel,
                    "发送同步通知给 ${selectedUser.username}",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE
                )
                
                if (result == JOptionPane.OK_OPTION) {
                    val moduleName = moduleField.text.trim()
                    val remark = remarkField.text.trim()
                    
                    if (moduleName.isNotBlank()) {
                        userDiscoveryService.sendSyncNotification(selectedUser, moduleName, remark)
                    } else {
                        notificationService.showNotification("错误", "模块名称不能为空", NotificationType.ERROR)
                    }
                }
            } else {
                notificationService.showNotification("错误", "请先选择一个用户", NotificationType.ERROR)
            }
        }
        buttonPanel.add(syncButton)
        
        val broadcastButton = JButton("广播同步通知")
        broadcastButton.addActionListener {
            // 创建带备注的输入对话框
            val panel = JPanel(BorderLayout())
            
            // 模块名称输入
            val modulePanel = JPanel(BorderLayout())
            modulePanel.border = BorderFactory.createTitledBorder("模块名称")
            val moduleField = JTextField(20)
            modulePanel.add(moduleField, BorderLayout.CENTER)
            
            // 备注输入
            val remarkPanel = JPanel(BorderLayout())
            remarkPanel.border = BorderFactory.createTitledBorder("备注 (可选)")
            val remarkField = JTextField(20)
            remarkPanel.add(remarkField, BorderLayout.CENTER)
            
            // 组装面板
            panel.add(modulePanel, BorderLayout.NORTH)
            panel.add(remarkPanel, BorderLayout.CENTER)
            
            // 显示对话框
            val result = JOptionPane.showConfirmDialog(
                this,
                panel,
                "广播同步通知",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
            )
            
            if (result == JOptionPane.OK_OPTION) {
                val moduleName = moduleField.text.trim()
                val remark = remarkField.text.trim()
                
                if (moduleName.isNotBlank()) {
                    userDiscoveryService.broadcastSyncNotification(moduleName, remark)
                } else {
                    notificationService.showNotification("错误", "模块名称不能为空", NotificationType.ERROR)
                }
            }
        }
        buttonPanel.add(broadcastButton)
        
        // 添加刷新按钮
        val refreshButton = JButton("刷新用户列表")
        refreshButton.addActionListener {
            userDiscoveryService.refreshUserList()
            updateCurrentUserDisplay()
        }
        buttonPanel.add(refreshButton)
        
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        // 将所有面板添加到主布局
        add(topPanel, BorderLayout.NORTH)
        add(mainPanel, BorderLayout.CENTER)
        
        // 注册监听器
        userDiscoveryService.addListener(this)
        
        // 手动请求初始化数据，确保即使 StartupActivity 没有执行也能显示数据
        SwingUtilities.invokeLater {
            userDiscoveryService.refreshUserList()
            updateCurrentUserDisplay()
        }
        
        // 初始化时设置按钮状态
        updateButtonsState(userDiscoveryService.isDiscoveryEnabled())
    }
    
    private fun updateCurrentUserDisplay() {
        val currentUser = userDiscoveryService.getCurrentUser()
        if (currentUser != null) {
            currentUserLabel.text = "当前用户：${currentUser.username} (${currentUser.id})"
        } else {
            currentUserLabel.text = "当前用户：未知"
        }
    }
    
    override fun onUserListUpdated(users: List<UserInfo>) {
        SwingUtilities.invokeLater {
            userListModel.clear()
            users.forEach { userListModel.addElement(it) }
            // 更新当前用户显示
            updateCurrentUserDisplay()
        }
    }
    
    override fun onNotificationReceived(fromUser: UserInfo, message: String) {
        SwingUtilities.invokeLater {
            // 添加日志以确认消息接收
            println("收到来自 ${fromUser.username} 的消息: $message")
            
            // 处理通知
            notificationService.handleUserNotification(fromUser, message)
        }
    }
    
    override fun onSyncNotificationReceived(
        fromUser: UserInfo, 
        moduleName: String, 
        isBroadcast: Boolean, 
        remark: String,
        messageId: String
    ) {
        println("UserDiscoveryPanel.onSyncNotificationReceived: 从 ${fromUser.username}, 模块: $moduleName, ID: $messageId")
        
        SwingUtilities.invokeLater {
            // 检查消息ID去重
            if (messageId.isNotBlank() && !processedMessageIds.add(messageId)) {
                println("忽略重复的同步通知: $messageId")
                return@invokeLater
            }
            
            // 限制缓存大小，避免内存泄漏
            if (processedMessageIds.size > 100) {
                processedMessageIds.clear()
            }
            
            println("处理同步通知对话框: $messageId")
            
            val messagePrefix = if (isBroadcast) "【广播通知】" else ""
            val remarkText = if (remark.isNotBlank()) "\n\n备注: $remark" else ""
            
            val result = JOptionPane.showConfirmDialog(
                this,
                "$messagePrefix${fromUser.username} 请求同步模块: $moduleName$remarkText\n是否立即同步？",
                if (isBroadcast) "广播同步请求" else "同步请求",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
            )
            
            val accepted = result == JOptionPane.YES_OPTION
            println("用户响应: ${if (accepted) "接受" else "拒绝"} 同步请求")
            
            userDiscoveryService.sendSyncResponse(fromUser, moduleName, accepted)
            
            if (accepted) {
                showRepositorySelectionDialog(moduleName)
            }
        }
    }
    
    override fun onSyncResponseReceived(fromUser: UserInfo, moduleName: String, accepted: Boolean) {
        SwingUtilities.invokeLater {
            val action = if (accepted) "已接受" else "已拒绝"
            
            // 显示通知
            notificationService.showNotification(
                "同步请求回执",
                "用户 ${fromUser.username} $action 您对模块 '$moduleName' 的同步请求",
                if (accepted) NotificationType.INFORMATION else NotificationType.WARNING
            )
            
            // 打印日志
            println("收到同步回执: 用户 ${fromUser.username} $action 模块 '$moduleName' 的同步请求")
        }
    }
    
    private fun showRepositorySelectionDialog(moduleName: String) {
        val settings = com.stackfilesync.intellij.settings.SyncSettingsState.getInstance()
        val repositories = settings.getRepositories()
        
        if (repositories.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "没有可用的仓库配置。\n请先在设置中添加仓库。",
                "错误",
                JOptionPane.ERROR_MESSAGE
            )
            return
        }
        
        val matchingRepos = repositories.filter { repo ->
            repo.sourceDirectory.contains(moduleName, ignoreCase = true) ||
            repo.name.contains(moduleName, ignoreCase = true)
        }
        
        val reposToShow = if (matchingRepos.isNotEmpty()) matchingRepos else repositories
        
        val repoNames = reposToShow.map { it.name }.toTypedArray()
        
        val selectedRepo = JOptionPane.showInputDialog(
            this,
            "请选择要同步的仓库：\n(模块名称: $moduleName)",
            "选择仓库",
            JOptionPane.QUESTION_MESSAGE,
            null,
            repoNames,
            if (matchingRepos.isNotEmpty()) repoNames[0] else null
        ) as? String
        
        if (selectedRepo == null) {
            return
        }
        
        val repository = reposToShow.find { it.name == selectedRepo }
        
        if (repository != null) {
            val repositoriesPanel = RepositoriesPanel(project)
            repositoriesPanel.startSyncWithModuleAndRepo(moduleName, repository)
        }
    }
    
    private class UserListCellRenderer(private val currentUser: UserInfo?) : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            val userInfo = value as UserInfo
            
            // 从用户ID中提取短编号，只使用最后4位数字
            val shortId = userInfo.id.split("-").lastOrNull()?.takeLast(4) ?: "????"
            
            // 如果是当前用户，则显示特殊标记
            if (currentUser != null && userInfo.id == currentUser.id) {
                text = "${userInfo.username} #${shortId} (当前用户)"
                font = font.deriveFont(java.awt.Font.BOLD)
            } else {
                text = "${userInfo.username} #${shortId}"
            }
            return this
        }
    }
    
    // 添加方法来更新按钮状态
    private fun updateButtonsState(enabled: Boolean) {
        // 找到所有需要根据发现功能开关状态禁用/启用的按钮
        for (component in buttonPanel.components) {
            if (component is JButton) {
                // 排除不需要禁用的按钮，比如设置按钮
                if (component.text != "设置") {
                    component.isEnabled = enabled
                }
            }
        }
        
        // 用户列表是否可选
        userList.isEnabled = enabled
    }
} 