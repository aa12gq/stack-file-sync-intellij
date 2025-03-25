package com.stackfilesync.service

interface SyncNotificationListener {
    fun onSyncNotificationReceived(
        fromUser: UserInfo, 
        moduleName: String, 
        isBroadcast: Boolean,
        remark: String = ""
    )
} 