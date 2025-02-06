package com.stackfilesync.intellij.window

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.text.DefaultCaret

class LogsPanel(project: Project) : JPanel() {
    private val textArea: JTextArea
    private var autoScroll = true
    
    init {
        layout = BorderLayout()
        border = JBUI.Borders.empty(10)
        
        // 创建日志文本区域
        textArea = JTextArea().apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            font = JBUI.Fonts.create("Monospaced", 12)
            
            // 设置自动滚动
            (caret as DefaultCaret).updatePolicy = DefaultCaret.ALWAYS_UPDATE
        }
        
        // 创建工具栏
        val toolbar = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            
            // 自动滚动开关
            val autoScrollCheckBox = JCheckBox("自动滚动", true).apply {
                addActionListener {
                    autoScroll = isSelected
                    if (autoScroll) {
                        textArea.caretPosition = textArea.document.length
                    }
                }
            }
            add(autoScrollCheckBox)
            
            // 清除按钮
            val clearButton = JButton("清除").apply {
                addActionListener {
                    textArea.text = ""
                }
            }
            add(Box.createHorizontalStrut(10))
            add(clearButton)
            
            add(Box.createHorizontalGlue())
        }
        
        add(toolbar, BorderLayout.NORTH)
        add(JBScrollPane(textArea), BorderLayout.CENTER)
    }
    
    fun appendLog(message: String) {
        SwingUtilities.invokeLater {
            textArea.append("${message}\n")
            if (autoScroll) {
                textArea.caretPosition = textArea.document.length
            }
        }
    }
    
    fun clear() {
        SwingUtilities.invokeLater {
            textArea.text = ""
        }
    }
} 