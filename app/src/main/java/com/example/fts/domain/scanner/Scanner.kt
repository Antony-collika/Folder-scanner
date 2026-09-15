package com.example.fts.domain.scanner

import android.content.Context
import android.net.Uri
import com.example.fts.data.cache.CacheManager
import com.example.fts.data.model.Entry
import com.example.fts.data.model.ScanOptions
import com.example.fts.data.model.Snapshot
import com.example.fts.data.model.SnapshotStats
import com.example.fts.data.saf.SafTreeReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class Scanner(
    private val context: Context,
    private val cacheManager: CacheManager
) {
    
    private val treeReader = SafTreeReader(context)
    
    suspend fun scan(
        rootUri: Uri,
        rootName: String,
        options: ScanOptions,
        onProgress: (ScanProgress) -> Unit
    ): Snapshot = withContext(Dispatchers.IO) {
        val rootEntry = treeReader.readTree(rootUri, options, onProgress)
        
        val stats = calculateStats(rootEntry)
        
        Snapshot(
            version = 1,
            rootName = rootName,
            rootUri = rootUri.toString(),
            scannedAt = System.currentTimeMillis(),
            root = rootEntry,
            stats = stats
        )
    }
    
    private fun calculateStats(entry: Entry): SnapshotStats {
        var totalEntries = 0
        var totalFolders = 0
        var totalFiles = 0
        var totalSize: Long = 0
        
        val stack = ArrayDeque<Entry>()
        stack.addLast(entry)
        
        while (stack.isNotEmpty()) {
            val current = stack.removeLast()
            totalEntries++
            
            when (current.type) {
                com.example.fts.data.model.EntryType.FOLDER -> totalFolders++
                com.example.fts.data.model.EntryType.FILE -> {
                    totalFiles++
                    current.size?.let { totalSize += it }
                }
            }
            
            current.children?.forEach { stack.addLast(it) }
        }
        
        return SnapshotStats(
            totalEntries = totalEntries,
            totalFolders = totalFolders,
            totalFiles = totalFiles,
            totalSize = totalSize
        )
    }
    
    suspend fun estimateCount(rootUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val rootDoc = com.example.fts.data.saf.SafDocument.fromTreeUri(context, rootUri)
            rootDoc?.listFiles()?.count() ?: 0
        } catch (e: Exception) {
            0
        }
    }
}