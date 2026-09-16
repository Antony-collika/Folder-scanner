package com.example.fts.domain.export

import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType
import com.example.fts.data.model.MarkdownModel
import com.example.fts.data.model.Snapshot
import com.example.fts.util.DateFormatter
import com.example.fts.util.FileSizeFormatter
import com.example.fts.util.MarkdownEscape

object MarkdownExporter {
    fun export(snapshot: Snapshot, model: MarkdownModel, showMetadata: Boolean = false, sortOrder: String = "name"): String {
        val sb = StringBuilder()
        appendHeader(sb, snapshot)
        when (model) {
            MarkdownModel.A -> exportModelA(sb, snapshot.root, 1, showMetadata, sortOrder)
            MarkdownModel.B -> exportModelB(sb, snapshot.root, 1, showMetadata, sortOrder)
            MarkdownModel.C -> exportModelC(sb, snapshot.root, 0, showMetadata, sortOrder)
            MarkdownModel.D -> exportModelD(sb, snapshot.root, showMetadata, sortOrder)
        }
        return sb.toString()
    }

    private fun appendHeader(sb: StringBuilder, snapshot: Snapshot) {
        sb.appendLine("# Cây thư mục: ${MarkdownEscape.escape(snapshot.rootName)}")
        sb.appendLine()
        sb.appendLine("- **Quét lúc:** ${DateFormatter.formatDateTime(snapshot.scannedAt)}")
        sb.appendLine("- **Tổng số mục:** ${FileSizeFormatter.formatCount(snapshot.stats.totalEntries)}")
        sb.appendLine("- **Số folder:** ${FileSizeFormatter.formatCount(snapshot.stats.totalFolders)}")
        sb.appendLine("- **Số file:** ${FileSizeFormatter.formatCount(snapshot.stats.totalFiles)}")
        sb.appendLine("- **Tổng dung lượng:** ${FileSizeFormatter.formatSize(snapshot.stats.totalSize)}")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
    }

    private fun appendMetadata(sb: StringBuilder, entry: Entry, indent: String) {
        val size = entry.size?.let(FileSizeFormatter::format) ?: if (entry.type == EntryType.FOLDER) "folder" else "N/A"
        sb.appendLine("${indent}> Metadata: size=$size; modified=${DateFormatter.formatDateTime(entry.modified)}; mime=${entry.mime ?: "N/A"}")
    }

    private fun exportModelA(sb: StringBuilder, entry: Entry, level: Int, showMetadata: Boolean, sortOrder: String) {
        val indent = "  ".repeat(level - 1)
        if (entry.type == EntryType.FOLDER) {
            sb.appendLine("$indent${"#".repeat(level)} ${MarkdownEscape.escape(entry.name)}")
            if (showMetadata) appendMetadata(sb, entry, indent)
            sortedChildren(entry, sortOrder).forEach { exportModelA(sb, it, level + 1, showMetadata, sortOrder) }
        } else {
            sb.appendLine("$indent- ${MarkdownEscape.escape(entry.name)}")
            if (showMetadata) appendMetadata(sb, entry, indent + "  ")
        }
    }

    private fun exportModelB(sb: StringBuilder, entry: Entry, level: Int, showMetadata: Boolean, sortOrder: String) {
        val indent = "  ".repeat(level - 1)
        if (entry.type == EntryType.FOLDER) {
            sb.appendLine("$indent${"#".repeat(level)} ${MarkdownEscape.escape(entry.name)}")
            if (showMetadata) appendMetadata(sb, entry, indent)
            sb.appendLine("$indent---")
            sortedChildren(entry, sortOrder).forEach { exportModelB(sb, it, level + 1, showMetadata, sortOrder) }
        } else {
            sb.appendLine("$indent${MarkdownEscape.escape(entry.name)}")
            if (showMetadata) appendMetadata(sb, entry, indent)
        }
    }

    private fun exportModelC(sb: StringBuilder, entry: Entry, depth: Int, showMetadata: Boolean, sortOrder: String) {
        val indent = "  ".repeat(depth)
        val suffix = if (entry.type == EntryType.FOLDER) "/" else ""
        sb.appendLine("$indent- ${MarkdownEscape.escape(entry.name)}$suffix")
        if (showMetadata) appendMetadata(sb, entry, indent + "  ")
        sortedChildren(entry, sortOrder).forEach { exportModelC(sb, it, depth + 1, showMetadata, sortOrder) }
    }

    private fun exportModelD(sb: StringBuilder, root: Entry, showMetadata: Boolean, sortOrder: String) {
        sb.appendLine("${MarkdownEscape.escape(root.name)}/")
        if (showMetadata) appendMetadata(sb, root, "")
        appendAsciiTree(sb, root, "", showMetadata, sortOrder)
    }

    private fun appendAsciiTree(sb: StringBuilder, entry: Entry, prefix: String, showMetadata: Boolean, sortOrder: String) {
        val children = sortedChildren(entry, sortOrder)
        children.forEachIndexed { index, child ->
            val isLast = index == children.lastIndex
            val connector = if (isLast) "└── " else "├── "
            val suffix = if (child.type == EntryType.FOLDER) "/" else ""
            sb.appendLine("$prefix$connector${MarkdownEscape.escape(child.name)}$suffix")
            if (showMetadata) appendMetadata(sb, child, prefix + if (isLast) "    " else "│   ")
            if (child.type == EntryType.FOLDER) appendAsciiTree(sb, child, prefix + if (isLast) "    " else "│   ", showMetadata, sortOrder)
        }
    }

    private fun sortedChildren(entry: Entry, sortOrder: String): List<Entry> = when (sortOrder) {
        "date" -> (entry.children ?: emptyList()).sortedWith(compareByDescending<Entry> { it.modified }.thenBy { it.name.lowercase() })
        "size" -> (entry.children ?: emptyList()).sortedWith(compareByDescending<Entry> { it.size ?: -1L }.thenBy { it.name.lowercase() })
        "type" -> (entry.children ?: emptyList()).sortedWith(compareBy<Entry> { if (it.type == EntryType.FOLDER) 0 else 1 }.thenBy { it.name.lowercase() })
        else -> (entry.children ?: emptyList()).sortedWith(compareBy<Entry> { if (it.type == EntryType.FOLDER) 0 else 1 }.thenBy { it.name.lowercase() })
    }
}
