package com.stackfilesync.intellij.settings

import com.intellij.openapi.components.*
import com.stackfilesync.intellij.model.Repository

@State(
    name = "StackFileSyncSettings",
    storages = [Storage("stackFileSync.xml")]
)
class SyncSettingsState : PersistentStateComponent<SyncSettingsState.State> {
    
    data class State(
        var repositories: MutableList<Repository> = mutableListOf(),
        var backupEnabled: Boolean = true
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    fun getRepositories(): List<Repository> = myState.repositories

    fun setRepositories(repositories: List<Repository>) {
        myState.repositories.clear()
        myState.repositories.addAll(repositories)
    }

    fun isBackupEnabled(): Boolean = myState.backupEnabled

    fun setBackupEnabled(enabled: Boolean) {
        myState.backupEnabled = enabled
    }

    companion object {
        fun getInstance(): SyncSettingsState = service()
    }
} 