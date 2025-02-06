package com.stackfilesync.intellij.model

data class InternalSyncConfig(
    var enabled: Boolean = false,
    var networkPath: String = ""
) 