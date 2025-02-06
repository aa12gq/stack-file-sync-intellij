package com.stackfilesync.intellij.service

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.stackfilesync.intellij.model.Repository
import com.intellij.openapi.components.service

@Service(Service.Level.PROJECT)
@State(
    name = "StackFileSync.Repository",
    storages = [Storage("stackFileSync.xml")]
)
class RepositoryService : PersistentStateComponent<Repository> {
    private var repository: Repository = Repository()

    companion object {
        fun getInstance(project: Project): RepositoryService = project.service()
    }

    override fun getState(): Repository = repository

    override fun loadState(state: Repository) {
        repository = state
    }

    fun getRepository(): Repository = repository

    fun saveRepository(repo: Repository) {
        repository = repo
    }
} 