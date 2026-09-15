package com.example.fts.data.model

import kotlinx.serialization.Serializable

@Serializable
enum class EntryType {
    FILE,
    FOLDER
}

@Serializable
data class Entry(
    val name: String,
    val type: EntryType,
    val path: String,
    val documentId: String,
    val modified: Long,
    val size: Long?,
    val mime: String?,
    val children: MutableList<Entry>? = null
)