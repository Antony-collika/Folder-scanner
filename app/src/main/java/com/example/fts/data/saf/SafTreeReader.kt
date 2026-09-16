package com.example.fts.data.saf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType
import com.example.fts.data.model.ScanOptions
import com.example.fts.domain.scanner.ScanProgress

class SafTreeReader(private val context: Context) {
    suspend fun readTree(rootUri: Uri, options: ScanOptions, previousRoot: Entry? = null, onProgress: (ScanProgress) -> Unit): Entry {
        val rootDoc = SafDocument.fromTreeUri(context, rootUri) ?: throw SafException("Không thể mở thư mục gốc")
        val rootEntry = Entry(rootDoc.name ?: "root", EntryType.FOLDER, "", getDocumentId(rootDoc) ?: "", rootDoc.lastModified, null, null, mutableListOf())
        val stack = ArrayDeque<Pair<SafDocument, Entry>>(); stack.add(rootDoc to rootEntry)
        val oldStack = ArrayDeque<Map<String, Entry>>(); oldStack.add(previousRoot?.children.orEmpty().associateBy { it.documentId })
        var counter = 0
        while (stack.isNotEmpty()) {
            val (doc, entry) = stack.removeLast(); val oldChildren = oldStack.removeLast()
            val children = try { doc.listFiles() } catch (_: Exception) { continue }
                .asSequence().filter { passesFilter(it, options) }.sortedWith(comparator(options.sortOrder)).toList()
            for (child in children) {
                val id = getDocumentId(child) ?: ""; val old = oldChildren[id]
                val type = if (child.isDirectory) EntryType.FOLDER else EntryType.FILE
                val unchanged = old != null && old.modified == child.lastModified && old.type == type
                val childPath = buildPath(entry.path, child.name ?: "")
                val childEntry = if (unchanged) rebase(old, childPath, child.name ?: "") else Entry(child.name ?: "", type, childPath, id, child.lastModified, if (child.isFile) child.length else null, child.type, if (child.isDirectory) mutableListOf() else null)
                entry.children?.add(childEntry)
                if (child.isDirectory && !unchanged && (options.maxDepth == null || getDepth(childPath) < options.maxDepth)) {
                    stack.add(child to childEntry); oldStack.add(old?.children.orEmpty().associateBy { it.documentId })
                }
                counter++; if (counter % 100 == 0) onProgress(ScanProgress(counter, childEntry.path))
            }
        }
        return rootEntry
    }
    private fun rebase(entry: Entry, path: String, name: String): Entry = entry.copy(name = name, path = path, children = entry.children?.map { rebase(it, buildPath(path, it.name), it.name) }?.toMutableList())
    private fun comparator(sortOrder: String): Comparator<SafDocument> = Comparator { a, b ->
        if (a.isDirectory != b.isDirectory) return@Comparator if (a.isDirectory) -1 else 1
        when (sortOrder) {
            "modified" -> compareValues(b.lastModified, a.lastModified).takeIf { it != 0 } ?: compareValues(a.name.orEmpty().lowercase(), b.name.orEmpty().lowercase())
            "size" -> compareValues(b.length, a.length).takeIf { it != 0 } ?: compareValues(a.name.orEmpty().lowercase(), b.name.orEmpty().lowercase())
            else -> compareValues(a.name.orEmpty().lowercase(), b.name.orEmpty().lowercase())
        }
    }
    private fun getDocumentId(doc: SafDocument): String? = try { DocumentsContract.getDocumentId(doc.uri) } catch (_: Exception) { null }
    private fun passesFilter(doc: SafDocument, options: ScanOptions): Boolean {
        val name = doc.name ?: return false
        if (options.skipHiddenFolders && doc.isDirectory && name.startsWith(".")) return false
        if (options.skipHiddenFiles && doc.isFile && name.startsWith(".")) return false
        if (options.skipSystemJunk && name in setOf(".thumbnails", ".cache", "cache", ".temp", ".trash", "Android/data")) return false
        return true
    }
    private fun buildPath(parent: String, name: String) = if (parent.isEmpty()) name else "$parent/$name"
    private fun getDepth(path: String) = if (path.isEmpty()) 0 else path.count { it == '/' } + 1
}