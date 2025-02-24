package com.stackfilesync.intellij.model

import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection

@Tag("P2PNodeConfig")
data class P2PNodeConfig(
    @Tag("name")
    var name: String = "",
    
    @Tag("address")
    var address: String = "",
    
    @Tag("port")
    var port: Int = 0,
    
    @Tag("targetDirectory")
    var targetDirectory: String = "",
    
    @Tag("enabled")
    var enabled: Boolean = true,
    
    @Tag("autoAccept")
    var autoAccept: Boolean = false,
    
    @Tag("filePatterns")
    @XCollection(style = XCollection.Style.v2)
    var filePatterns: List<String> = listOf("**/*"),
    
    @Tag("excludePatterns")
    @XCollection(style = XCollection.Style.v2)
    var excludePatterns: List<String> = emptyList()
) 