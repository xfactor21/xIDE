package com.aistudio.xide.core.workspace.explorer

import com.aistudio.xide.core.provider.FileNode

data class ExplorerNode(
    val fileNode: FileNode,
    val isExpanded: Boolean = false,
    val children: List<ExplorerNode>? = null
)

interface ProjectExplorer {
    fun setRoot(path: String)
    suspend fun loadChildren(node: ExplorerNode)
    fun toggleExpansion(node: ExplorerNode)
    fun selectNode(node: ExplorerNode, multiSelect: Boolean = false)
    fun getSelectedNodes(): List<ExplorerNode>
}
