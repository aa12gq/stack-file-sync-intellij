package com.stackfilesync.intellij.service

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.stackfilesync.intellij.model.P2PNodeConfig

@Service(Service.Level.PROJECT)
@State(
    name = "P2PNodeConfigs",
    storages = [Storage("p2pNodeConfigs.xml")]
)
class P2PNodeConfigService : PersistentStateComponent<P2PNodeConfigService.State> {
    
    data class State(
        var nodes: MutableList<P2PNodeConfig> = mutableListOf()
    )
    
    private var myState = State()
    
    override fun getState(): State = myState
    
    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }
    
    fun getNodes(): List<P2PNodeConfig> = myState.nodes.toList()
    
    fun addNode(node: P2PNodeConfig) {
        myState.nodes.add(node)
    }
    
    fun updateNode(index: Int, node: P2PNodeConfig) {
        if (index in myState.nodes.indices) {
            myState.nodes[index] = node
        }
    }
    
    fun removeNode(index: Int) {
        if (index in myState.nodes.indices) {
            myState.nodes.removeAt(index)
        }
    }
    
    companion object {
        fun getInstance(project: Project): P2PNodeConfigService = project.service()
    }
} 