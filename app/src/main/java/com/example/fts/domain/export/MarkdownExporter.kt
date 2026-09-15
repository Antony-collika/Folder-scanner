package com.example.fts.domain.export

import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType
import com.example.fts.data.model.MarkdownModel
import com.example.fts.data.model.Snapshot
import com.example.fts.util.DateFormatter
import com.example.fts.util.FileSizeFormatter
import com.example.fts.util.MarkdownEscape

object MarkdownExporter {
    
    fun export(snapshot: Snapshot, model: MarkdownModel, showMetadata: Boolean = false): String {
        val sb = StringBuilder()
        appendHeader(sb, snapshot, showMetadata)
        
        when (model) {
            MarkdownModel.A -> exportModelA(sb, snapshot.root, 1, showMetadata)
            MarkdownModel.B -> exportModelB(sb, snapshot.root, 1, showMetadata)
            MarkdownModel.C -> exportModelC(sb, snapshot.root, 0, showMetadata)
            MarkdownModel.D -> exportModelD(sb, snapshot.root, showMetadata)
        }
        
        return sb.toString()
    }
    
    private fun appendHeader(sb: StringBuilder, snapshot: Snapshot, showMetadata: Boolean) {
        sb.appendLine("# Cay thu muc: ${MarkdownEscape.escape(snapshot.rootName)}")
        sb.appendLine()
        sb.appendLine("- **Quet luc:** ${DateFormatter.format(snapshot.scannedAt)}")
        sb.appendLine("- **Tong so muc:** ${FileSizeFormatter.formatCount(snapshot.stats.totalEntries)}")
        sb.appendLine("- **So folder:** ${FileSizeFormatter.formatCount(snapshot.stats.totalFolders)}")
        sb.appendLine("- **So file:** ${FileSizeFormatter.formatCount(snapshot.stats.totalFiles)}")
        sb.appendLine("- **Tong dung luong:** ${FileSizeFormatter.formatSize(snapshot.stats.totalSize)}")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
    }
    
    private fun exportModelA(sb: StringBuilder, entry: Entry, level: Int, showMetadata: Boolean) {
        val indent = "  ".repeat(level - 1)
        
        if (entry.type == EntryType.FOLDER) {
            sb.appendLine("${indent}" + "#".repeat(level) + " ${MarkdownEscape.escape(entry.name)}")
            entry.children?.forEach {
                exportModelA(sb, it, level + 1, showMetadata)
            }
        } else {
            sb.appendLine("${indent}- ${MarkdownEscape.escape(entry.name)}")
        }
    }
    
    private fun exportModelB(sb: StringBuilder, entry: Entry, level: Int, showMetadata: Boolean) {
        val indent = "  ".repeat(level - 1)
        
        if (entry.type == EntryType.FOLDER) {
            sb.appendLine("${indent}" + "#".repeat(level) + " ${MarkdownEscape.escape(entry.name)}")
            sb.appendLine("${indent}---")
            entry.children?.forEach {
                exportModelB(sb, it, level + 1, showMetadata)
            }
        } else {
            sb.appendLine("${indent}${MarkdownEscape.escape(entry.name)}")
        }
    }
    
    private fun exportModelC(sb: StringBuilder, entry: Entry, depth: Int, showMetadata: Boolean) {
        val indent = "  ".repeat(depth)
        val suffix = if (entry.type == EntryType.FOLDER) "/" else ""
        
        sb.appendLine("${indent}- ${MarkdownEscape.escape(entry.name)}${suffix}")
        
        entry.children?.forEach {
            exportModelC(sb, it, depth + 1, showMetadata)
        }
    }
    
    private fun exportModelD(sb: StringBuilder, root: Entry, showMetadata: Boolean) {
        sb.appendLine("${MarkdownEscape.escape(root.name)}/")
        appendAsciiTree(sb, root, "")
    }
    
    private fun appendAsciiTree(sb: StringBuilder, entry: Entry, prefix: String) {
        val children = entry.children ?: return
        
        children.forEachIndexed { index, child ->
            val isLast = index == children.lastIndex
            val connector = if (isLast) "|-- " else "|-- "
            val suffix = if (child.type == EntryType.FOLDER) "/" else ""
            sb.appendLine("${prefix}${connector}${MarkdownEscape.escape(child.name)}${suffix}")
            
            if (child.type == EntryType.FOLDER) {
                val newPrefix = prefix + if (isLast) "    " else "|   "
                appendAsciiTree(sb, child, newPrefix)
            }
        }
    }
}