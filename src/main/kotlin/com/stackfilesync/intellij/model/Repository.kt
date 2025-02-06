package com.stackfilesync.intellij.model

import com.stackfilesync.intellij.model.AutoSyncConfig
import com.stackfilesync.intellij.model.BackupConfig
import com.stackfilesync.intellij.model.InternalSyncConfig
import com.stackfilesync.intellij.model.PostSyncCommand

data class Repository(
    var name: String = "",
    var url: String = "",
    var branch: String = "main",
    var sourceDirectory: String = "",
    var targetDirectory: String = "",
    var filePatterns: List<String> = listOf("**/*.proto"),
    var excludePatterns: List<String> = listOf("**/backend/**"),
    var autoSync: AutoSyncConfig? = null,
    var postSyncCommands: List<PostSyncCommand> = emptyList(),
    var backupConfig: BackupConfig? = BackupConfig(),
    var internalSync: InternalSyncConfig? = null
) 