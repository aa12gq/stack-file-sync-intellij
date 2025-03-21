package com.stackfilesync.intellij.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.invokeLater
import javax.swing.JButton

/**
 * 同步状态服务，用于管理同步按钮的状态
 */
@Service
class SyncStateService(private val project: Project) {
    companion object {
        fun getInstance(project: Project): SyncStateService = project.service()
    }
    
    private val syncButtons = mutableListOf<JButton>()
    private var syncInProgress = false
    
    /**
     * 注册同步按钮，使其状态被服务管理
     */
    fun registerSyncButton(button: JButton) {
        if (!syncButtons.contains(button)) {
            syncButtons.add(button)
            
            // 根据当前状态设置按钮
            if (syncInProgress) {
                button.text = "同步中..."
                button.isEnabled = false
            } else {
                button.text = "同步"
                button.isEnabled = true
            }
        }
    }
    
    /**
     * 设置同步开始状态
     */
    fun setSyncStarted() {
        syncInProgress = true
        invokeLater {
            syncButtons.forEach { button ->
                button.text = "同步中..."
                button.isEnabled = false
            }
        }
    }
    
    /**
     * 设置同步完成状态
     */
    fun setSyncFinished() {
        syncInProgress = false
        invokeLater {
            syncButtons.forEach { button ->
                button.text = "同步"
                button.isEnabled = true
            }
        }
    }
    
    /**
     * 清除已注册的按钮
     */
    fun clearButtons() {
        syncButtons.clear()
    }
    
    /**
     * 当前是否有同步正在进行
     */
    fun isSyncInProgress(): Boolean = syncInProgress
} 