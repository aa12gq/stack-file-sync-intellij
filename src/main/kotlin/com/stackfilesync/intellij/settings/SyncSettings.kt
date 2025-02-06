package com.stackfilesync.intellij.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.*
import javax.swing.JComponent
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.stackfilesync.intellij.model.Repository
import com.stackfilesync.intellij.dialog.RepositorySettingsDialog
import javax.swing.DefaultListModel
import javax.swing.DefaultListCellRenderer
import java.awt.Component
import javax.swing.JList
import com.intellij.openapi.project.ProjectManager

class SyncSettings : Configurable {
    private lateinit var repositoryList: JBList<Repository>
    private val listModel = DefaultListModel<Repository>()
    private val settings = SyncSettingsState.getInstance()
    private var modified = false
    
    // 获取当前项目
    private val project: Project
        get() = ProjectManager.getInstance().defaultProject

    override fun createComponent(): JComponent = panel {
        group("仓库列表") {
            row {
                cell(JBScrollPane(JBList(listModel).also { list ->
                    repositoryList = list
                    list.cellRenderer = object : DefaultListCellRenderer() {
                        override fun getListCellRendererComponent(
                            list: JList<*>,
                            value: Any?,
                            index: Int,
                            isSelected: Boolean,
                            cellHasFocus: Boolean
                        ): Component {
                            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                            val repo = value as Repository
                            text = buildString {
                                append(repo.name)
                                append(" (")
                                append(repo.sourceDirectory)
                                append(" → ")
                                append(repo.targetDirectory)
                                append(")")
                                if (repo.postSyncCommands.isNotEmpty()) {
                                    append(" [后处理命令: ${repo.postSyncCommands.size}]")
                                }
                            }
                            return this
                        }
                    }
                }))
                    .align(Align.FILL)
                    .resizableColumn()
            }

            row {
                button("添加仓库") {
                    val dialog = RepositorySettingsDialog(project, Repository())
                    if (dialog.showAndGet()) {
                        val repository = dialog.getRepository()
                        listModel.addElement(repository)
                        modified = true
                    }
                }

                button("编辑仓库") {
                    val selected = repositoryList.selectedValue
                    if (selected != null) {
                        val dialog = RepositorySettingsDialog(project, selected)
                        if (dialog.showAndGet()) {
                            val index = repositoryList.selectedIndex
                            listModel.set(index, dialog.getRepository())
                            modified = true
                        }
                    }
                }

                button("删除仓库") {
                    val index = repositoryList.selectedIndex
                    if (index >= 0) {
                        listModel.remove(index)
                        modified = true
                    }
                }
            }
        }
    }.also {
        reset()  // 初始化列表数据
    }

    private fun updateSettings() {
        val repos = mutableListOf<Repository>()
        for (i in 0 until listModel.size()) {
            repos.add(listModel.getElementAt(i))
        }
        settings.setRepositories(repos)
        modified = false
    }

    override fun isModified(): Boolean = modified

    override fun apply() {
        updateSettings()
    }

    override fun reset() {
        listModel.clear()
        settings.getRepositories().forEach { repo -> 
            listModel.addElement(repo)
        }
        modified = false
    }

    override fun getDisplayName(): String = "Stack File Sync"
} 