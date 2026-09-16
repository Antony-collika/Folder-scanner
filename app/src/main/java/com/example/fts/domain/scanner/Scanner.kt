package com.example.fts.domain.scanner

import android.content.Context
import android.net.Uri
import com.example.fts.data.cache.CacheManager
import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType
import com.example.fts.data.model.ScanOptions
import com.example.fts.data.model.Snapshot
import com.example.fts.data.model.SnapshotStats
import com.example.fts.data.saf.SafDocument
import com.example.fts.data.saf.SafTreeReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

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
        val rootEntry = treeReader.readTree(rootUri, options) { progress ->
            coroutineContext.ensureActive()
            onProgress(progress)
        }

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

    private suspend fun calculateStats(entry: Entry): SnapshotStats {
        var totalEntries = 0
        var totalFolders = 0
        var totalFiles = 0
        var totalSize = 0L
        val stack = ArrayDeque<Entry>()
        stack.addLast(entry)

        while (stack.isNotEmpty()) {
            coroutineContext.ensureActive()
            val current = stack.removeLast()
            totalEntries++
            when (current.type) {
                EntryType.FOLDER -> totalFolders++
                EntryType.FILE -> {
                    totalFiles++
                    totalSize += current.size ?: 0L
                }
            }
            current.children?.forEach(stack::addLast)
        }

        return SnapshotStats(totalEntries, totalFolders, totalFiles, totalSize)
    }

    suspend fun estimateCount(rootUri: Uri): Int = withContext(Dispatchers.IO) {
        try {
            val rootDoc = SafDocument.fromTreeUri(context, rootUri) ?: return@withContext 0
            var count = 0
            val stack = ArrayDeque<SafDocument>()
            stack.addLast(rootDoc)
            val maxCount = 10000
            while (stack.isNotEmpty() && count < maxCount) {
                coroutineContext.ensureActive()
                val doc = stack.removeLast()
                count++
                if (doc.isDirectory) {
                    doc.listFiles()?.forEach(stack::addLast)
                }
            }
            count
        } catch (e: Exception) {
            0
        }
    }
}