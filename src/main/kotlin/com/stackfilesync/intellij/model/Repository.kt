package com.stackfilesync.intellij.model

import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.stackfilesync.intellij.model.PostSyncCommand

@Tag("Repository")
data class Repository(
    @Tag("name")
    var name: String = "",
    
    @Tag("url")
    var url: String = "",
    
    @Tag("branch")
    var branch: String = "main",
    
    @Tag("sourceDirectory")
    var sourceDirectory: String = "",
    
    @Tag("targetDirectory")
    var targetDirectory: String = "",
    
    @Tag("filePatterns")
    @XCollection(style = XCollection.Style.v2)
    var filePatterns: List<String> = listOf("**/*.proto"),
    
    @Tag("excludePatterns")
    @XCollection(style = XCollection.Style.v2)
    var excludePatterns: List<String> = listOf("**/backend/**"),
    
    @Tag("autoSync")
    var autoSync: AutoSyncConfig? = null,
    
    @Tag("backupConfig")
    var backupConfig: BackupConfig? = null,
    
    @Tag("internalSync")
    var internalSync: InternalSyncConfig? = null,
    
    @Tag("postSyncCommands")
    @XCollection(style = XCollection.Style.v2)
    var postSyncCommands: List<PostSyncCommand> = listOf()
)

@Tag("AutoSyncConfig")
data class AutoSyncConfig(
    @Tag("enabled")
    var enabled: Boolean = false,
    
    @Tag("interval")
    var interval: Int = 300
)

@Tag("BackupConfig")
data class BackupConfig(
    @Tag("enabled")
    var enabled: Boolean = true,
    
    @Tag("maxBackups")
    var maxBackups: Int = 10
)

@Tag("InternalSyncConfig")
data class InternalSyncConfig(
    @Tag("enabled")
    var enabled: Boolean = false,
    
    @Tag("networkPath")
    var networkPath: String = ""
) 