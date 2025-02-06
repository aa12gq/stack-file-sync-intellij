package com.stackfilesync.intellij.model

data class AutoSyncConfig(
    var enabled: Boolean = false,
    var interval: Int = 300
) 