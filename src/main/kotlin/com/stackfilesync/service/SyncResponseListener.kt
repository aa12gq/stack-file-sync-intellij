package com.stackfilesync.service

interface SyncResponseListener {
    fun onSyncResponseReceived(fromUser: UserInfo, moduleName: String, accepted: Boolean)
} 