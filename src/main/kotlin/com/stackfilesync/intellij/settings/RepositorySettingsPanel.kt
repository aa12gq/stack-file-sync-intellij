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
import com.stackfilesync.intellij.model.Repository.BackupConfig
import javax.swing.DefaultListModel

class RepositorySettingsPanel(private val repository: Repository) : JPanel() {
    init {
        layout = BorderLayout()
        add(createBackupPanel(), BorderLayout.CENTER)
    }

    private fun createBackupPanel(): JPanel {
        return FormBuilder.createFormBuilder()
            .addComponent(JCheckBox("启用备份").apply {
                isSelected = repository.backupConfig?.enabled ?: true
                addActionListener {
                    repository.backupConfig = repository.backupConfig?.copy(
                        enabled = isSelected
                    ) ?: Repository.BackupConfig(enabled = isSelected)
                }
            })
            .addLabeledComponent("最大备份数:", JSpinner(SpinnerNumberModel(
                repository.backupConfig?.maxBackups ?: 10,
                1, 100, 1
            )).apply {
                addChangeListener {
                    repository.backupConfig = repository.backupConfig?.copy(
                        maxBackups = value as Int
                    ) ?: Repository.BackupConfig(maxBackups = value as Int)
                }
            })
            .addComponent(JCheckBox("同步前备份").apply {
                isSelected = repository.backupConfig?.backupBeforeSync ?: true
                addActionListener {
                    repository.backupConfig = repository.backupConfig?.copy(
                        backupBeforeSync = isSelected
                    ) ?: Repository.BackupConfig(backupBeforeSync = isSelected)
                }
            })
            .addComponent(JCheckBox("恢复前备份").apply {
                isSelected = repository.backupConfig?.backupBeforeRestore ?: true
                addActionListener {
                    repository.backupConfig = repository.backupConfig?.copy(
                        backupBeforeRestore = isSelected
                    ) ?: Repository.BackupConfig(backupBeforeRestore = isSelected)
                }
            })
            .addLabeledComponent("排除模式:", createPatternList(
                repository.backupConfig?.excludePatterns ?: emptyList()
            ) { patterns ->
                repository.backupConfig = repository.backupConfig?.copy(
                    excludePatterns = patterns
                ) ?: Repository.BackupConfig(excludePatterns = patterns)
            })
            .panel
    }

    private fun createPatternList(
        patterns: List<String>,
        onPatternsChanged: (List<String>) -> Unit
    ): JComponent {
        val model = DefaultListModel<String>().apply {
            patterns.forEach { addElement(it) }
        }
        val list = JBList(model)
        
        // 添加编辑功能
        list.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                val selectedPatterns = list.selectedValuesList
                onPatternsChanged(selectedPatterns)
            }
        }
        
        return JBScrollPane(list)
    }
} 