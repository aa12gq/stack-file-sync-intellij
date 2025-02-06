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