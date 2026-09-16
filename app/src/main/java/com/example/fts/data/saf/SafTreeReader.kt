package com.example.fts.data.saf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType
import com.example.fts.data.model.ScanOptions
import com.example.fts.domain.scanner.ScanProgress

class SafTreeReader(private val context: Context) {
    suspend fun readTree(
        rootUri: Uri,
        options: ScanOptions,
        onProgress: (ScanProgress) -> Unit
    ): Entry {
        val rootDoc = SafDocument.fromTreeUri(context, rootUri)
            ?: throw SafException("Không thể mở thư mục gốc")

        val rootEntry = Entry(
            name = rootDoc.name ?: "root",
            type = EntryType.FOLDER,
            path = "",
            documentId = getDocumentId(rootDoc) ?: "",
            modified = rootDoc.lastModified,
            size = null,
            mime = null,
            children = mutableListOf()
        )

        val stack = ArrayDeque<Pair<SafDocument, Entry>>()
        stack.addLast(rootDoc to rootEntry)
        var counter = 0

        while (stack.isNotEmpty()) {
            val (doc, entry) = stack.removeLast()
            val children = try {
                doc.listFiles()
            } catch (_: Exception) {
                continue
            }
                .asSequence()
                .filter { passesFilter(it, options) }
                .sortedWith(comparator(options.sortOrder))
                .toList()

            for (child in children) {
                val childEntry = Entry(
                    name = child.name ?: "",
                    type = if (child.isDirectory) EntryType.FOLDER else EntryType.FILE,
                    path = buildPath(entry.path, child.name ?: ""),
                    documentId = getDocumentId(child) ?: "",
                    modified = child.lastModified,
                    size = if (child.isFile) child.length else null,
                    mime = child.type,
                    children = if (child.isDirectory) mutableListOf() else null
                )
                entry.children?.add(childEntry)
                if (child.isDirectory && (options.maxDepth == null || getDepth(childEntry.path) < options.maxDepth)) {
                    stack.addLast(child to childEntry)
                }
                counter++
                if (counter % 100 == 0) onProgress(ScanProgress(counter, childEntry.path))
            }
        }
        return rootEntry
    }

    private fun comparator(sortOrder: String): Comparator<SafDocument> =
        Comparator { a, b ->
            val aDir = a.isDirectory
            val bDir = b.isDirectory
            if (aDir != bDir) return@Comparator if (aDir) -1 else 1
            when (sortOrder) {
                "modified" -> compareValues(b.lastModified, a.lastModified).takeIf { it != 0 }
                    ?: compareValues(a.name.orEmpty().lowercase(), b.name.orEmpty().lowercase())
                "size" -> compareValues(b.length, a.length).takeIf { it != 0 }
                    ?: compareValues(a.name.orEmpty().lowercase(), b.name.orEmpty().lowercase())
                else -> compareValues(a.name.orEmpty().lowercase(), b.name.orEmpty().lowercase())
            }
        }

    private fun getDocumentId(doc: SafDocument): String? = DocumentsContract.getDocumentId(doc.uri)

    private fun passesFilter(doc: SafDocument, options: ScanOptions): Boolean {
        val name = doc.name ?: return false
        if (options.skipHiddenFolders && doc.isDirectory && name.startsWith(".")) return false
        if (options.skipHiddenFiles && doc.isFile && name.startsWith(".")) return false
        if (options.skipSystemJunk && isSystemJunk(name)) return false
        return true
    }

    private fun isSystemJunk(name: String): Boolean =
        name in setOf(".thumbnails", ".cache", "cache", ".temp", ".trash", "Android/data")

    private fun buildPath(parentPath: String, name: String): String =
        if (parentPath.isEmpty()) name else "$parentPath/$name"

    private fun getDepth(path: String): Int = if (path.isEmpty()) 0 else path.count { it == '/' } + 1
}