package com.example.fts.domain.flatten

import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType

data class FlatNode(val entry: Entry, val depth: Int, val isExpanded: Boolean, val hasChildren: Boolean)

object TreeFlattener {
    fun flatten(root: Entry, expanded: Set<String>, sortOrder: String = "name"): List<FlatNode> {
        val result = mutableListOf<FlatNode>()
        flattenRecursive(root, 0, expanded, sortOrder, result)
        return result
    }

    private fun flattenRecursive(entry: Entry, depth: Int, expanded: Set<String>, sortOrder: String, result: MutableList<FlatNode>) {
        val hasChildren = !entry.children.isNullOrEmpty()
        val isExpanded = entry.documentId in expanded
        result.add(FlatNode(entry, depth, isExpanded, hasChildren))
        if (isExpanded && hasChildren) {
            sortChildren(entry.children!!, sortOrder).forEach { flattenRecursive(it, depth + 1, expanded, sortOrder, result) }
        }
    }

    private fun sortChildren(children: List<Entry>, sortOrder: String): List<Entry> = when (sortOrder) {
        "date" -> children.sortedWith(compareByDescending<Entry> { it.modified }.thenBy { it.name.lowercase() })
        "size" -> children.sortedWith(compareByDescending<Entry> { it.size ?: -1L }.thenBy { it.name.lowercase() })
        "type" -> children.sortedWith(compareBy<Entry> { if (it.type == EntryType.FOLDER) 0 else 1 }.thenBy { it.name.lowercase() })
        else -> children.sortedWith(compareBy<Entry> { if (it.type == EntryType.FOLDER) 0 else 1 }.thenBy { it.name.lowercase() })
    }
}
