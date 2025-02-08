package com.stackfilesync.intellij.model

import com.intellij.util.xmlb.annotations.Tag

@Tag("PostSyncCommand")
data class PostSyncCommand(
    @Tag("directory")
    var directory: String = "",
    
    @Tag("command")
    var command: String = "",

    @Tag("order")
    var order: Int = 0
) 