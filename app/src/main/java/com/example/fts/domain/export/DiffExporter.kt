package com.example.fts.domain.export

import com.example.fts.data.model.Diff
import com.example.fts.data.model.EntryType
import com.example.fts.util.DateFormatter
import com.example.fts.util.FileSizeFormatter
import com.example.fts.util.MarkdownEscape
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DiffExporter {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    fun exportMarkdown(diff: Diff, rootName: String): String {
        val sb = StringBuilder()
        
        sb.appendLine("# Diff: ${MarkdownEscape.escape(rootName)}")
        sb.appendLine()
        sb.appendLine("- **Tu:** ${DateFormatter.format(diff.fromScannedAt)}")
        sb.appendLine("- **Den:** ${DateFormatter.format(diff.toScannedAt)}")
        sb.appendLine("- **Tong thay doi:** ${diff.totalChanges}")
        sb.appendLine()
        
        if (diff.added.isNotEmpty()) {
            sb.appendLine("## Them moi (${diff.added.size})")
            diff.added.forEach { sb.appendLine("- ${MarkdownEscape.escape(it.path)}") }
            sb.appendLine()
        }
        
        if (diff.removed.isNotEmpty()) {
            sb.appendLine("## Xoa (${diff.removed.size})")
            diff.removed.forEach { sb.appendLine("- ${MarkdownEscape.escape(it.path)}") }
            sb.appendLine()
        }
        
        if (diff.modified.isNotEmpty()) {
            sb.appendLine("## Sua (${diff.modified.size})")
            diff.modified.forEach { item ->
                val sizeChange = item.changes["size"]
                val changeText = if (sizeChange != null) {
                    val oldSize = (sizeChange.first as? Long) ?: 0
                    val newSize = (sizeChange.second as? Long) ?: 0
                    "(kich thuoc: ${FileSizeFormatter.formatSize(oldSize)} -> ${FileSizeFormatter.formatSize(newSize)})"
                } else ""
                sb.appendLine("- ${MarkdownEscape.escape(item.path)} ${changeText}")
            }
            sb.appendLine()
        }
        
        if (diff.renamed.isNotEmpty()) {
            sb.appendLine("## Doi ten (${diff.renamed.size})")
            diff.renamed.forEach { 
                sb.appendLine("- ${MarkdownEscape.escape(it.oldPath)} -> ${MarkdownEscape.escape(it.newPath)}") 
            }
            sb.appendLine()
        }
        
        if (diff.moved.isNotEmpty()) {
            sb.appendLine("## Di chuyen (${diff.moved.size})")
            diff.moved.forEach { 
                sb.appendLine("- ${MarkdownEscape.escape(it.oldPath)} -> ${MarkdownEscape.escape(it.newPath)}") 
            }
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    fun exportJson(diff: Diff, rootName: String): String {
        val diffWithMetadata = mapOf(
            "version" to "1.0",
            "root_name" to rootName,
            "from" to diff.fromScannedAt,
            "to" to diff.toScannedAt,
            "changes" to mapOf(
                "added" to diff.added.map { 
                    mapOf("path" to it.path, "type" to it.type.toString())
                },
                "removed" to diff.removed.map { 
                    mapOf("path" to it.path, "type" to it.type.toString())
                },
                "modified" to diff.modified.map { 
                    mapOf("path" to it.path, "changes" to it.changes)
                },
                "renamed" to diff.renamed.map { 
                    mapOf("document_id" to it.documentId, "old_path" to it.oldPath, "new_path" to it.newPath)
                },
                "moved" to diff.moved.map { 
                    mapOf("document_id" to it.documentId, "old_path" to it.oldPath, "new_path" to it.newPath)
                }
            )
        )
        
        return json.encodeToString(diffWithMetadata)
    }
}

val Diff.totalChanges: Int
    get() = added.size + removed.size + modified.size + renamed.size + moved.size