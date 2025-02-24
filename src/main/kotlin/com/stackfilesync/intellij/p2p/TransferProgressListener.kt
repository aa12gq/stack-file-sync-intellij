package com.stackfilesync.intellij.p2p

interface TransferProgressListener {
    fun onProgress(bytesTransferred: Long, totalBytes: Long)
    fun onComplete()
    fun onError(error: String)
} 