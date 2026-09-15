package com.example.fts.domain.flatten

import com.example.fts.data.model.Entry

data class FlatNode(
    val entry: Entry,
    val depth: Int,
    val isExpanded: Boolean,
    val hasChildren: Boolean
)

object TreeFlattener {
    
    fun flatten(root: Entry, expanded: Set<String>): List<FlatNode> {
        val result = mutableListOf<FlatNode>()
        flattenRecursive(root, 0, expanded, result)
        return result
    }
    
    private fun flattenRecursive(
        entry: Entry,
        depth: Int,
        expanded: Set<String>,
        result: MutableList<FlatNode>
    ) {
        val hasChildren = !entry.children.isNullOrEmpty()
        val isExpanded = entry.documentId in expanded
        
        result.add(FlatNode(entry, depth, isExpanded, hasChildren))
        
        if (isExpanded && hasChildren) {
            entry.children!!.forEach {
                flattenRecursive(it, depth + 1, expanded, result)
            }
        }
    }
}