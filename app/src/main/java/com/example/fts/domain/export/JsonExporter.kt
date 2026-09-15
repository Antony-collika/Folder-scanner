package com.example.fts.domain.export

import com.example.fts.data.model.Snapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object JsonExporter {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    fun export(snapshot: Snapshot): String = json.encodeToString(snapshot)
}