override fun update(e: AnActionEvent) {
    val repository = e.getData(CommonDataKeys.PSI_ELEMENT) as? Repository
    e.presentation.isEnabled = repository != null
    
    if (repository != null) {
        val enabled = repository.autoSync?.enabled ?: false
        e.presentation.text = if (enabled) "禁用自动同步" else "启用自动同步"
        e.presentation.icon = if (enabled) {
            StackFileSync.AutoSync
        } else {
            StackFileSync.AutoSyncDisabled
        }
    }
} 