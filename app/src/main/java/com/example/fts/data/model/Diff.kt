package com.example.fts.data.model

import kotlinx.serialization.Serializable

data class Diff(
    val fromScannedAt: Long,
    val toScannedAt: Long,
    val added: List<DiffItem>,
    val removed: List<DiffItem>,
    val modified: List<ModifiedItem>,
    val renamed: List<RenamedItem>,
    val moved: List<MovedItem>
)

@Serializable
data class DiffItem(
    val path: String,
    val type: EntryType
)

@Serializable
data class ModifiedItem(
    val path: String,
    val changes: Map<String, Pair<Any?, Any?>>
)

@Serializable
data class RenamedItem(
    val documentId: String,
    val oldPath: String,
    val newPath: String
)

@Serializable
data class MovedItem(
    val documentId: String,
    val oldPath: String,
    val newPath: String
)