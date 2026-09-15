package com.example.fts.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Snapshot(
    val version: Int = 1,
    val rootName: String,
    val rootUri: String,
    val scannedAt: Long,
    val root: Entry,
    val stats: SnapshotStats
)

@Serializable
data class SnapshotStats(
    val totalEntries: Int,
    val totalFolders: Int,
    val totalFiles: Int,
    val totalSize: Long
)