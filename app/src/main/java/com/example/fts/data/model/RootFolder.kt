package com.example.fts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RootFolder(
    val uri: String,
    val displayName: String,
    val addedAt: Long,
    val lastScannedAt: Long? = null,
    val entryCount: Int? = null
)