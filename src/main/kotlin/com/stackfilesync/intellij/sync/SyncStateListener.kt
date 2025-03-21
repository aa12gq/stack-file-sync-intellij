package com.stackfilesync.intellij.sync

import com.intellij.util.messages.Topic

interface SyncStateListener {
    companion object {
        val TOPIC = Topic.create("同步状态变化", SyncStateListener::class.java)
    }
    
    fun onSyncStarted() {}
    fun onSyncFinished() {}
} 