package com.stackfilesync.intellij.settings

import com.intellij.openapi.components.*
import com.stackfilesync.intellij.model.Repository

@State(
    name = "StackFileSyncSettings",
    storages = [Storage("stackFileSync.xml")]
)
class PluginSettingsState : PersistentStateComponent<PluginSettingsState.State> {
    
    data class State(
        var repositories: MutableList<Repository> = mutableListOf(),
        var backupEnabled: Boolean = true
    )

    private var myState = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        myState = state
    }

    var repositories: List<Repository>
        get() = myState.repositories
        set(value) {
            myState.repositories.clear()
            myState.repositories.addAll(value)
        }

    var backupEnabled: Boolean
        get() = myState.backupEnabled
        set(value) {
            myState.backupEnabled = value
        }

    companion object {
        fun getInstance(): PluginSettingsState = service()
    }
} 