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

class Scanner(private val context: Context, private val cacheManager: CacheManager) {
    private val treeReader = SafTreeReader(context)

    suspend fun scan(rootUri: Uri, rootName: String, options: ScanOptions, previousSnapshot: Snapshot? = null, onProgress: (ScanProgress) -> Unit): Snapshot = withContext(Dispatchers.IO) {
        val rootEntry = treeReader.readTree(rootUri, options, previousSnapshot?.root) { progress -> coroutineContext.ensureActive(); onProgress(progress) }
        Snapshot(1, rootName, rootUri.toString(), System.currentTimeMillis(), rootEntry, calculateStats(rootEntry))
    }

    private suspend fun calculateStats(entry: Entry): SnapshotStats {
        var totalEntries = 0; var totalFolders = 0; var totalFiles = 0; var totalSize = 0L
        val stack = ArrayDeque<Entry>(); stack.add(entry)
        while (stack.isNotEmpty()) { coroutineContext.ensureActive(); val current = stack.removeLast(); totalEntries++; when (current.type) { EntryType.FOLDER -> totalFolders++; EntryType.FILE -> { totalFiles++; totalSize += current.size ?: 0L } }; current.children?.forEach(stack::add) }
        return SnapshotStats(totalEntries, totalFolders, totalFiles, totalSize)
    }

    suspend fun estimateCount(rootUri: Uri): Int = withContext(Dispatchers.IO) {
        try { val rootDoc = SafDocument.fromTreeUri(context, rootUri) ?: return@withContext 0; var count = 0; val stack = ArrayDeque<SafDocument>(); stack.add(rootDoc); while (stack.isNotEmpty() && count < 10000) { coroutineContext.ensureActive(); val doc = stack.removeLast(); count++; if (doc.isDirectory) doc.listFiles()?.forEach(stack::add) }; count } catch (_: Exception) { 0 }
    }
}