package com.stackfilesync.intellij.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.settings.RepositorySettingsPanel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JButton
import javax.swing.BoxLayout
import com.google.gson.GsonBuilder
import com.intellij.ui.components.JBScrollPane
import javax.swing.JTextArea
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BorderFactory

class RepositorySettingsDialog(
    private val currentProject: Project,
    private val repository: Repository
) : DialogWrapper(currentProject) {
    private val panel = RepositorySettingsPanel(currentProject)
    
    init {
        title = "仓库设置"
        init()
        panel.setRepository(repository.copy())
    }
    
    override fun createCenterPanel(): JComponent {
        val mainPanel = JPanel(BorderLayout())
        
        // 添加设置面板
        mainPanel.add(panel.createPanel(), BorderLayout.CENTER)
        
        // 添加查看原始配置按钮
        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
            add(JButton("查看原始配置").apply {
                addActionListener {
                    showRawConfig()
                }
            })
        }
        mainPanel.add(buttonPanel, BorderLayout.SOUTH)
        
        return mainPanel
    }
    
    private fun showRawConfig() {
        val dialog = object : DialogWrapper(currentProject) {
            init {
                title = "原始配置"
                init()
            }
            
            override fun createCenterPanel(): JComponent {
                val gson = GsonBuilder()
                    .setPrettyPrinting()
                    .serializeNulls()
                    .create()
                    
                val jsonConfig = gson.toJson(repository)
                
                val textArea = JTextArea(jsonConfig).apply {
                    isEditable = false
                    font = font.deriveFont(14f)
                    lineWrap = true
                    wrapStyleWord = true
                    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                }
                
                return JPanel(BorderLayout()).apply {
                    add(JBScrollPane(textArea), BorderLayout.CENTER)
                    preferredSize = Dimension(600, 400)
                }
            }
        }
        
        dialog.show()
    }
    
    fun getRepository(): Repository = panel.getRepository()
} 