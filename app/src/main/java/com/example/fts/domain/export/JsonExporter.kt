package com.example.fts.domain.export

import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType
import com.example.fts.data.model.Snapshot
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object JsonExporter {
    private val isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC)
    fun export(snapshot: Snapshot): String = buildSnapshot(snapshot).toString()

    private fun buildSnapshot(snapshot: Snapshot): JsonObject = buildJsonObject {
        put("version", "1.0")
        put("scanned_at", isoFormatter.format(Instant.ofEpochMilli(snapshot.scannedAt)))
        put("root", buildEntry(snapshot.root))
        put("stats", buildJsonObject {
            put("total_entries", snapshot.stats.totalEntries)
            put("total_folders", snapshot.stats.totalFolders)
            put("total_files", snapshot.stats.totalFiles)
            put("total_size", snapshot.stats.totalSize)
        })
    }

    private fun buildEntry(entry: Entry): JsonObject = buildJsonObject {
        put("name", entry.name)
        put("type", if (entry.type == EntryType.FOLDER) "folder" else "file")
        put("path", entry.path)
        put("document_id", entry.documentId)
        put("modified", isoFormatter.format(Instant.ofEpochMilli(entry.modified)))
        if (entry.type == EntryType.FILE) {
            if (entry.size != null) put("size", entry.size) else put("size", null as String?)
            entry.mime?.let { put("mime", it) }
            put("children", null as String?)
        } else {
            put("size", null as String?)
            put("children", JsonArray(entry.children?.map(::buildEntry) ?: emptyList()))
        }
    }
}