package com.example.fts.domain.scanner

import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType
import com.example.fts.data.model.ScanOptions
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.attribute.BasicFileAttributes
import java.util.ArrayDeque

/**
 * Fast filesystem reader used when MANAGE_EXTERNAL_STORAGE is granted.
 * It avoids the per-document IPC overhead of DocumentFile/SAF.
 */
class FileTreeReader {
    fun readTree(root: File, options: ScanOptions, previousRoot: Entry? = null, onProgress: (ScanProgress) -> Unit): Entry {
        require(root.isDirectory && root.canRead()) { "Không thể đọc thư mục gốc: ${root.absolutePath}" }
        val rootId = stableId(root)
        val rootEntry = Entry(root.name.ifBlank { "Internal storage" }, EntryType.FOLDER, "", rootId, root.lastModified(), null, null, mutableListOf())
        data class Work(val file: File, val entry: Entry, val depth: Int)
        val stack = ArrayDeque<Work>()
        stack.addLast(Work(root, rootEntry, 0))
        var count = 0
        while (stack.isNotEmpty()) {
            val work = stack.removeLast()
            val children = try { work.file.listFiles() } catch (_: SecurityException) { null }
            if (children == null) continue
            val sorted = children.asSequence()
                .filter { shouldKeep(it, options) }
                .sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
                .toList()
            for (child in sorted.asReversed()) {
                val relative = if (work.entry.path.isBlank()) child.name else "${work.entry.path}/${child.name}"
                val isDirectory = child.isDirectory
                val childEntry = Entry(
                    name = child.name,
                    type = if (isDirectory) EntryType.FOLDER else EntryType.FILE,
                    path = relative,
                    documentId = stableId(child),
                    modified = child.lastModified(),
                    size = if (isDirectory) null else child.length().takeIf { it >= 0L },
                    mime = if (isDirectory) null else mimeType(child),
                    children = if (isDirectory) mutableListOf() else null
                )
                work.entry.children?.add(0, childEntry)
                count++
                onProgress(ScanProgress(count, childEntry.path))
                if (isDirectory && (options.maxDepth == null || work.depth < options.maxDepth)) {
                    stack.addLast(Work(child, childEntry, work.depth + 1))
                }
            }
        }
        return rootEntry
    }

    private fun shouldKeep(file: File, options: ScanOptions): Boolean {
        val name = file.name
        if (name.isEmpty()) return false
        if (name.startsWith(".") && ((file.isDirectory && options.skipHiddenFolders) || (file.isFile && options.skipHiddenFiles))) return false
        if (options.skipSystemJunk && name in setOf(".thumbnails", ".cache", "cache", ".temp", ".trash")) return false
        return true
    }

    private fun stableId(file: File): String {
        return try {
            val attrs = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
            attrs.fileKey()?.toString() ?: file.canonicalPath
        } catch (_: Exception) {
            file.absolutePath
        }
    }

    private fun mimeType(file: File): String? {
        val ext = file.extension.lowercase()
        if (ext.isBlank()) return null
        return android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }
}
