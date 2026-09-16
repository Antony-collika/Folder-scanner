package com.example.fts.data.model

data class ScanOptions(
    val skipHiddenFolders: Boolean = true,
    val skipHiddenFiles: Boolean = true,
    val skipSystemJunk: Boolean = true,
    val maxDepth: Int? = null,
    val sortOrder: String = "name"
)