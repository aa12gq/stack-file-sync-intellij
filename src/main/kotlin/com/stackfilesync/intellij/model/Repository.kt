package com.stackfilesync.intellij.model

data class Repository(
    var name: String = "",
    var url: String = "",
    var branch: String = "master",
    var sourceDirectory: String = "",
    var targetDirectory: String = "",
    var filePatterns: List<String> = listOf(),
    var excludePatterns: List<String> = listOf(),
    var postSyncCommands: List<PostSyncCommand> = emptyList(),
    var autoSync: AutoSyncConfig? = null,
    var backupConfig: BackupConfig? = null
) {
    data class PostSyncCommand(
        var directory: String = "",
        var command: String = ""
    )

    data class AutoSyncConfig(
        var enabled: Boolean = false,
        var interval: Int = 300
    )

    data class BackupConfig(
        var enabled: Boolean = true,
        var maxBackups: Int = 10,
        var backupBeforeSync: Boolean = true,
        var backupBeforeRestore: Boolean = true,
        var excludePatterns: List<String> = listOf()
    )
} 