package com.stackfilesync.intellij.dialog

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.stackfilesync.intellij.model.Repository
import javax.swing.*

class RepositoryChooserDialog(
    project: Project,
    private val repositories: List<Repository>
) : DialogWrapper(project) {
    
    private val list: JBList<Repository>
    
    init {
        title = "选择要同步的仓库"
        
        // 创建仓库列表
        list = JBList(repositories).apply {
            cellRenderer = RepositoryListCellRenderer()
            selectionMode = ListSelectionModel.SINGLE_SELECTION
            if (repositories.isNotEmpty()) {
                selectedIndex = 0
            }
        }
        
        init()
    }
    
    override fun createCenterPanel(): JComponent {
        return JBScrollPane(list)
    }
    
    fun getSelectedRepository(): Repository? {
        return list.selectedValue
    }
    
    private inner class RepositoryListCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
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
                append(repo.url)
                append(" - ")
                append(repo.branch)
                append(")")
            }
            
            icon = if (repo.autoSync?.enabled == true) {
                // TODO: 添加自动同步图标
                null
            } else {
                null
            }
            
            return this
        }
    }
} 