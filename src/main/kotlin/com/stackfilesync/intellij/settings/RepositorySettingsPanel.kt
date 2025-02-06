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

class RepositorySettingsPanel {
    private lateinit var panel: DialogPanel
    private var repository = Repository()

    fun createPanel(): JPanel {
        panel = panel {
            group("基本设置") {
                row("仓库名称:") {
                    textField()
                        .bindText(repository::name)
                        .validationOnInput {
                            if (it.text.isBlank()) error("仓库名称不能为空") else null
                        }
                }
                
                row("Git仓库URL:") {
                    textField()
                        .bindText(repository::url)
                        .validationOnInput {
                            if (it.text.isBlank()) error("Git仓库URL不能为空") else null
                        }
                }
                
                row("分支:") {
                    textField()
                        .bindText(repository::branch)
                        .validationOnInput {
                            if (it.text.isBlank()) error("分支不能为空") else null
                        }
                }
            }
            
            group("目录设置") {
                row("源目录:") {
                    textField()
                        .bindText(repository::sourceDirectory)
                        .validationOnInput {
                            if (it.text.isBlank()) error("源目录不能为空") else null
                        }
                }
                
                row("目标目录:") {
                    textField()
                        .bindText(repository::targetDirectory)
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
        }
        
        return panel
    }

    fun getRepository(): Repository = repository
    fun setRepository(repo: Repository) {
        repository = repo
        panel.reset()
    }
} 