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
import javax.swing.*

class UserDiscoveryToolWindow : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val userDiscoveryPanel = UserDiscoveryPanel(project)
        val content = ContentFactory.getInstance().createContent(userDiscoveryPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }
}

class UserDiscoveryPanel(private val project: Project) : JPanel(), UserDiscoveryListener {
    
    private val userDiscoveryService = UserDiscoveryService.getInstance(project)
    private val notificationService = NotificationService.getInstance(project)
    
    private val userList = JBList<UserInfo>()
    private val userListModel = DefaultListModel<UserInfo>()
    private val currentUserLabel = JLabel("当前用户：未知")
    
    init {
        layout = BorderLayout()
        
        // 创建顶部面板显示当前用户信息
        val topPanel = JPanel(BorderLayout())
        topPanel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        topPanel.add(currentUserLabel, BorderLayout.WEST)
        
        // 配置用户列表
        userList.model = userListModel
        userList.cellRenderer = UserListCellRenderer(userDiscoveryService.getCurrentUser())
        
        // 创建主面板
        val mainPanel = JPanel(BorderLayout())
        mainPanel.add(JBScrollPane(userList), BorderLayout.CENTER)
        
        // 创建按钮面板
        val buttonPanel = JPanel()
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
            
            // 如果是当前用户，则显示特殊标记
            if (currentUser != null && userInfo.id == currentUser.id) {
                text = "${userInfo.username} (当前用户)"
                font = font.deriveFont(java.awt.Font.BOLD)
            } else {
                text = userInfo.username
            }
            return this
        }
    }
} 