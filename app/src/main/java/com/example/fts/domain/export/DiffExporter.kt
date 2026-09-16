package com.example.fts.domain.export

import com.example.fts.data.model.Diff
import com.example.fts.util.DateFormatter
import com.example.fts.util.MarkdownEscape

object DiffExporter {
    
    fun exportMarkdown(diff: Diff, displayName: String): String {
        val sb = StringBuilder()
        
        // Header
        sb.appendLine("# Diff: ${MarkdownEscape.escape(displayName)}")
        sb.appendLine()
        sb.appendLine("- **Từ:** ${DateFormatter.formatDateTime(diff.fromScannedAt)}")
        sb.appendLine("- **Đến:** ${DateFormatter.formatDateTime(diff.toScannedAt)}")
        val totalChanges = diff.added.size + diff.removed.size + diff.modified.size + 
                         diff.renamed.size + diff.moved.size
        sb.appendLine("- **Tổng thay đổi:** $totalChanges")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()
        
        // Added
        if (diff.added.isNotEmpty()) {
            sb.appendLine("## Thêm mới (${diff.added.size})")
            diff.added.forEach { item ->
                sb.appendLine("- ${MarkdownEscape.escape(item.path)}")
            }
            sb.appendLine()
        }
        
        // Removed
        if (diff.removed.isNotEmpty()) {
            sb.appendLine("## Xóa (${diff.removed.size})")
            diff.removed.forEach { item ->
                sb.appendLine("- ${MarkdownEscape.escape(item.path)}")
            }
            sb.appendLine()
        }
        
        // Modified
        if (diff.modified.isNotEmpty()) {
            sb.appendLine("## Sửa (${diff.modified.size})")
            diff.modified.forEach { item ->
                val changesText = item.changes.entries.joinToString(", ") { (key, pair) ->
                    "$key: ${pair.first} → ${pair.second}"
                }
                sb.appendLine("- ${MarkdownEscape.escape(item.path)} ($changesText)")
            }
            sb.appendLine()
        }
        
        // Renamed
        if (diff.renamed.isNotEmpty()) {
            sb.appendLine("## Đổi tên (${diff.renamed.size})")
            diff.renamed.forEach { item ->
                sb.appendLine("- ${MarkdownEscape.escape(item.oldPath)} → ${MarkdownEscape.escape(item.newPath)}")
            }
            sb.appendLine()
        }
        
        // Moved
        if (diff.moved.isNotEmpty()) {
            sb.appendLine("## Di chuyển (${diff.moved.size})")
            diff.moved.forEach { item ->
                sb.appendLine("- ${MarkdownEscape.escape(item.oldPath)} → ${MarkdownEscape.escape(item.newPath)}")
            }
            sb.appendLine()
        }
        
        return sb.toString()
    }
}