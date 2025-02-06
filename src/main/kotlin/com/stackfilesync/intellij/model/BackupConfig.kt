package com.stackfilesync.intellij.model

data class BackupConfig(
    var enabled: Boolean = true,
    var maxBackups: Int = 10
) 