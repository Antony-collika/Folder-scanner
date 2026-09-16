package com.example.fts.domain.export

import com.example.fts.data.model.Diff
import com.example.fts.util.DateFormatter
import com.example.fts.util.MarkdownEscape
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.Json

object DiffExporter {
    private val json = Json { prettyPrint = true }

    fun exportMarkdown(diff: Diff, displayName: String): String {
        val sb = StringBuilder()
        sb.appendLine("# Diff: ${MarkdownEscape.escape(displayName)}")
        sb.appendLine()
        sb.appendLine("- **Từ:** ${DateFormatter.formatDateTime(diff.fromScannedAt)}")
        sb.appendLine("- **Đến:** ${DateFormatter.formatDateTime(diff.toScannedAt)}")
        val totalChanges = diff.added.size + diff.removed.size + diff.modified.size + diff.renamed.size + diff.moved.size
        sb.appendLine("- **Tổng thay đổi:** $totalChanges")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        if (diff.added.isNotEmpty()) {
            sb.appendLine("## Thêm mới (${diff.added.size})")
            diff.added.forEach { sb.appendLine("- ${MarkdownEscape.escape(it.path)}") }
            sb.appendLine()
        }
        if (diff.removed.isNotEmpty()) {
            sb.appendLine("## Xóa (${diff.removed.size})")
            diff.removed.forEach { sb.appendLine("- ${MarkdownEscape.escape(it.path)}") }
            sb.appendLine()
        }
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
        if (diff.renamed.isNotEmpty()) {
            sb.appendLine("## Đổi tên (${diff.renamed.size})")
            diff.renamed.forEach { sb.appendLine("- ${MarkdownEscape.escape(it.oldPath)} → ${MarkdownEscape.escape(it.newPath)}") }
            sb.appendLine()
        }
        if (diff.moved.isNotEmpty()) {
            sb.appendLine("## Di chuyển (${diff.moved.size})")
            diff.moved.forEach { sb.appendLine("- ${MarkdownEscape.escape(it.oldPath)} → ${MarkdownEscape.escape(it.newPath)}") }
            sb.appendLine()
        }
        return sb.toString()
    }

    fun exportJson(diff: Diff): String {
        val root = buildJsonObject {
            put("from_scanned_at", DateFormatter.formatDateTime(diff.fromScannedAt))
            put("to_scanned_at", DateFormatter.formatDateTime(diff.toScannedAt))
            put("added", buildJsonArray {
                diff.added.forEach { item ->
                    add(buildJsonObject {
                        put("path", item.path)
                        put("type", item.type.name)
                    })
                }
            })
            put("removed", buildJsonArray {
                diff.removed.forEach { item ->
                    add(buildJsonObject {
                        put("path", item.path)
                        put("type", item.type.name)
                    })
                }
            })
            put("modified", buildJsonArray {
                diff.modified.forEach { item ->
                    add(buildJsonObject {
                        put("path", item.path)
                        put("changes", buildJsonObject {
                            item.changes.forEach { (key, value) ->
                                put(key, "${value.first} → ${value.second}")
                            }
                        })
                    })
                }
            })
            put("renamed", buildJsonArray {
                diff.renamed.forEach { item ->
                    add(buildJsonObject {
                        put("document_id", item.documentId)
                        put("old_path", item.oldPath)
                        put("new_path", item.newPath)
                    })
                }
            })
            put("moved", buildJsonArray {
                diff.moved.forEach { item ->
                    add(buildJsonObject {
                        put("document_id", item.documentId)
                        put("old_path", item.oldPath)
                        put("new_path", item.newPath)
                    })
                }
            })
        }
        return json.encodeToString(root)
    }
}