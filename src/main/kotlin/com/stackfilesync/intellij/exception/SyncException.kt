package com.stackfilesync.intellij.exception

sealed class SyncException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class GitException(message: String, cause: Throwable? = null) : 
        SyncException("Git操作失败: $message", cause)
    
    class NetworkException(message: String, cause: Throwable? = null) : 
        SyncException("网络访问失败: $message", cause)
    
    class FileException(message: String, cause: Throwable? = null) : 
        SyncException("文件操作失败: $message", cause)
    
    class CommandException(message: String, cause: Throwable? = null) : 
        SyncException(message, cause)
    
    class ConfigException(message: String) : 
        SyncException("配置错误: $message")
} 