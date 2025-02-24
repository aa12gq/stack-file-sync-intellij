package com.stackfilesync.intellij.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.stackfilesync.intellij.model.P2PNode
import com.stackfilesync.intellij.p2p.P2PConnectionManager
import com.stackfilesync.intellij.service.P2PTransferService
import com.stackfilesync.intellij.p2p.TransferProgressListener
import com.stackfilesync.intellij.utils.NotificationUtils
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.ui.Messages
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Component
import java.awt.FlowLayout
import java.io.File
import javax.swing.*

class P2PTransferDialog(
    private val project: Project,
    private var files: List<VirtualFile>
) : DialogWrapper(project) {

    private val nodeList = JBList<P2PNode>()
    private val fileListModel = DefaultListModel<VirtualFile>()
    private val fileList = JBList(fileListModel)
    private val connectionManager = P2PConnectionManager.getInstance(project)
    private val progressBar = JProgressBar(0, 100)
    private val statusLabel = JLabel("准备传输...")
    private val transferButton = JButton("开始传输")
    
    init {
        title = "P2P传输"
        init()
        updateNodeList()
        initFileList()
        setOKButtonText("关闭")
        setCancelButtonText("取消")
    }
    
    private fun initFileList() {
        files.forEach { fileListModel.addElement(it) }
    }
    
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 添加本地节点信息面板
        val localInfoPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("本机信息")
            val localNode = connectionManager.getLocalNode()
            val localInfo = if (localNode != null) {
                """
                名称: ${localNode.name}
                地址: ${localNode.address}:${localNode.port}
                """.trimIndent()
            } else {
                "未连接"
            }
            add(JLabel(localInfo), BorderLayout.CENTER)
        }
        panel.add(localInfoPanel, BorderLayout.NORTH)
        
        // 添加中央内容面板
        val contentPanel = JPanel(BorderLayout()).apply {
            // 添加文件列表面板
            val filesPanel = JPanel(BorderLayout()).apply {
                border = BorderFactory.createTitledBorder("文件列表")
                
                // 设置文件列表渲染器
                fileList.cellRenderer = object : DefaultListCellRenderer() {
                    override fun getListCellRendererComponent(
                        list: JList<*>,
                        value: Any?,
                        index: Int,
                        isSelected: Boolean,
                        cellHasFocus: Boolean
                    ): Component {
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                        if (value is VirtualFile) {
                            text = "${value.name} (${formatFileSize(value.length)})"
                        }
                        return this
                    }
                }
                
                val scrollPane = JBScrollPane(fileList)
                scrollPane.preferredSize = Dimension(300, 100)
                add(scrollPane, BorderLayout.CENTER)
                
                // 添加文件操作按钮
                val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
                buttonPanel.add(JButton("添加文件").apply {
                    addActionListener { addFiles() }
                })
                buttonPanel.add(JButton("移除选中").apply {
                    addActionListener { removeSelectedFiles() }
                })
                add(buttonPanel, BorderLayout.SOUTH)
            }
            add(filesPanel, BorderLayout.NORTH)
            
            // 添加节点列表面板
            val listPanel = JPanel(BorderLayout()).apply {
                border = BorderFactory.createTitledBorder("可用节点")
                val scrollPane = JBScrollPane(nodeList)
                scrollPane.preferredSize = Dimension(300, 200)
                add(scrollPane, BorderLayout.CENTER)
                
                // 添加刷新按钮
                val refreshButton = JButton("刷新节点列表").apply {
                    addActionListener { updateNodeList() }
                }
                add(refreshButton, BorderLayout.SOUTH)
            }
            add(listPanel, BorderLayout.CENTER)
        }
        panel.add(contentPanel, BorderLayout.CENTER)
        
        // 添加传输控制面板
        val controlPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createTitledBorder("传输控制")
            
            // 添加传输按钮
            transferButton.addActionListener { startTransfer() }
            add(transferButton, BorderLayout.NORTH)
            
            // 添加进度面板
            val progressPanel = JPanel(BorderLayout())
            progressPanel.add(progressBar, BorderLayout.CENTER)
            progressPanel.add(statusLabel, BorderLayout.SOUTH)
            add(progressPanel, BorderLayout.CENTER)
        }
        panel.add(controlPanel, BorderLayout.SOUTH)
        
        panel.preferredSize = Dimension(400, 600)
        return panel
    }
    
    private fun addFiles() {
        val descriptor = FileChooserDescriptor(
            true,  // 允许选择文件
            false, // 不允许选择目录
            false, // 不允许选择jar
            false, // 不允许选择jar目录
            false, // 不允许选择软链接
            true   // 允许选择多个文件
        )
        
        FileChooser.chooseFiles(descriptor, project, null).forEach { virtualFile ->
            if (!fileListModel.contains(virtualFile)) {
                fileListModel.addElement(virtualFile)
            }
        }
    }
    
    private fun removeSelectedFiles() {
        fileList.selectedValuesList.forEach { fileListModel.removeElement(it) }
    }
    
    private fun startTransfer() {
        val selectedNode = nodeList.selectedValue ?: run {
            Messages.showErrorDialog(project, "请选择一个目标节点", "错误")
            return
        }
        
        if (fileListModel.isEmpty) {
            Messages.showErrorDialog(project, "请选择要传输的文件", "错误")
            return
        }
        
        transferButton.isEnabled = false
        val service = P2PTransferService.getInstance(project)
        
        // 设置进度监听器
        service.setProgressListener(object : TransferProgressListener {
            override fun onProgress(bytesTransferred: Long, totalBytes: Long) {
                invokeLater {
                    val percent = (bytesTransferred * 100 / totalBytes).toInt()
                    progressBar.value = percent
                    statusLabel.text = "已传输: ${formatFileSize(bytesTransferred)} / ${formatFileSize(totalBytes)}"
                }
            }
            
            override fun onComplete() {
                invokeLater {
                    statusLabel.text = "传输完成"
                    transferButton.isEnabled = true
                }
            }
            
            override fun onError(error: String) {
                invokeLater {
                    statusLabel.text = "传输失败: $error"
                    transferButton.isEnabled = true
                }
            }
        })
        
        // 开始传输
        for (i in 0 until fileListModel.size()) {
            val file = fileListModel.get(i)
            try {
                service.sendFile(selectedNode.id, File(file.path))
            } catch (e: Exception) {
                NotificationUtils.showError(
                    project,
                    "传输失败",
                    "文件 ${file.name} 传输失败: ${e.message}"
                )
            }
        }
    }
    
    override fun doOKAction() {
        close(OK_EXIT_CODE)
    }
    
    private fun updateNodeList() {
        val nodes = connectionManager.getAvailableNodes()
        nodeList.setListData(nodes.toTypedArray())
        
        if (nodes.isEmpty()) {
            setErrorText("没有可用的节点。请先在 Tools -> P2P 节点配置 中添加节点。")
        }
    }
    
    fun getSelectedNode(): P2PNode = nodeList.selectedValue
    
    private fun formatFileSize(size: Long): String {
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = size.toDouble()
        var unit = 0
        while (value > 1024 && unit < units.size - 1) {
            value /= 1024
            unit++
        }
        return "%.2f %s".format(value, units[unit])
    }
    
    override fun dispose() {
        // 不在这里停止服务，让服务继续运行
        super.dispose()
    }
} 