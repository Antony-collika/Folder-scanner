package com.example.fts.domain.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.example.fts.data.model.ScanOptions
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.charset.CodingErrorAction
import java.util.Locale

/** Exports bounded text-file contents. Checks the root limit before producing output. */
class ContentMarkdownExporter(private val context: Context) {
    data class Limits(val rootMaxBytes: Long, val perFileMaxBytes: Long)
    private data class SourceFile(val path: String, val size: Long, val open: () -> InputStream)

    fun export(rootUri: Uri, rootName: String, limits: Limits, options: ScanOptions): String {
        require(limits.rootMaxBytes > 0L) { "Giới hạn thư mục phải lớn hơn 0" }
        require(limits.perFileMaxBytes > 0L) { "Giới hạn mỗi tệp phải lớn hơn 0" }
        val files = if (rootUri.scheme == "file") {
            collectFilesystem(File(requireNotNull(rootUri.path) { "Thiếu đường dẫn thư mục" }), options)
        } else collectSaf(rootUri, options)

        var totalBytes = 0L
        for (file in files) {
            totalBytes = try { Math.addExact(totalBytes, file.size.coerceAtLeast(0L)) }
            catch (_: ArithmeticException) { Long.MAX_VALUE }
            if (totalBytes > limits.rootMaxBytes) {
                throw IllegalStateException("Tổng dung lượng tệp trong thư mục vượt giới hạn " +
                    formatBytes(limits.rootMaxBytes) + " (đã phát hiện ít nhất " + formatBytes(totalBytes) +
                    "). Không tạo file Markdown.")
            }
        }

        val included = mutableListOf<Pair<SourceFile, String>>()
        val skipped = mutableListOf<Pair<String, String>>()
        for (file in files) {
            if (file.size > limits.perFileMaxBytes) {
                skipped += file.path to ("Vượt giới hạn mỗi tệp (" + formatBytes(file.size) +
                    " > " + formatBytes(limits.perFileMaxBytes) + ")")
                continue
            }
            try {
                val bytes = file.open().use { readBounded(it, limits.perFileMaxBytes) }
                if (bytes.size.toLong() > limits.perFileMaxBytes) {
                    skipped += file.path to "Nội dung thực tế lớn hơn giới hạn mỗi tệp"
                    continue
                }
                val text = decodeText(bytes)
                if (text == null) skipped += file.path to "Có vẻ là tệp nhị phân hoặc không phải UTF-8"
                else included += file to text
            } catch (_: FileTooLargeException) {
                skipped += file.path to "Nội dung thực tế lớn hơn giới hạn mỗi tệp"
            } catch (e: Exception) {
                skipped += file.path to ("Không đọc được: " + (e.message ?: e.javaClass.simpleName))
            }
        }

        return buildString {
            append("# ").append(rootName).append(" — nội dung thư mục\n\n")
            append("- Tổng dung lượng tệp trong thư mục: ").append(formatBytes(totalBytes)).append('\n')
            append("- Đã đưa vào: ").append(included.size).append(" tệp\n")
            append("- Bỏ qua: ").append(skipped.size).append(" tệp\n\n")
            append("## Cây thư mục\n\n")
            append("📁 ").append(rootName).append("/\n")
            val paths = (included.map { it.first.path } + skipped.map { it.first }).distinct().sorted()
            appendTree(this, paths)
            append("\n---\n\n## Nội dung tệp\n")
            if (included.isEmpty()) append("\nKhông có tệp văn bản phù hợp để đưa vào.\n")
            val tick = 96.toChar().toString()
            for ((file, content) in included) {
                append("\n### ").append(file.path.replace(tick.toString(), "\\" + tick)).append("\n\n")
                val fence = tick.repeat(maxOf(3, longestTickRun(content) + 1))
                append(fence).append('\n').append(content)
                if (!content.endsWith("\n")) append('\n')
                append(fence).append('\n')
            }
            append("\n## Tệp bị bỏ qua\n\n")
            if (skipped.isEmpty()) append("Không có.\n")
            else for ((path, reason) in skipped) {
                append("- ").append(tick).append(path.replace(tick.toString(), "\\" + tick)).append(tick)
                    .append(": ").append(reason).append('\n')
            }
        }
    }

    private fun collectFilesystem(root: File, options: ScanOptions): List<SourceFile> {
        require(root.isDirectory && root.canRead()) { "Không thể đọc thư mục gốc: " + root.absolutePath }
        val result = mutableListOf<SourceFile>()
        val stack = ArrayDeque<Pair<File, String>>()
        stack.add(root to "")
        while (stack.isNotEmpty()) {
            val (directory, parent) = stack.removeLast()
            val children = directory.listFiles()
                ?: throw IllegalStateException("Không thể liệt kê thư mục: " + directory.absolutePath)
            for (child in children.sortedBy { it.name.lowercase(Locale.ROOT) }) {
                if (!keep(child.name, child.isDirectory, options)) continue
                val path = if (parent.isEmpty()) child.name else "$parent/" + child.name
                if (Files.isSymbolicLink(child.toPath())) {
                    result += SourceFile(path, 0L) {
                        throw IllegalStateException("Liên kết tượng trưng được bỏ qua để tránh đọc ngoài thư mục gốc")
                    }
                } else if (child.isDirectory) stack.add(child to path)
                else if (child.isFile) {
                    val file = child
                    result += SourceFile(path, file.length()) { file.inputStream() }
                }
            }
        }
        return result.sortedBy { it.path }
    }

    private fun collectSaf(rootUri: Uri, options: ScanOptions): List<SourceFile> {
        val root = DocumentFile.fromTreeUri(context, rootUri)
            ?: throw IllegalStateException("Không thể mở thư mục đã chọn")
        val result = mutableListOf<SourceFile>()
        val stack = ArrayDeque<Pair<DocumentFile, String>>()
        stack.add(root to "")
        while (stack.isNotEmpty()) {
            val (directory, parent) = stack.removeLast()
            val children = try { directory.listFiles() }
            catch (e: Exception) {
                throw IllegalStateException("Không thể liệt kê thư mục: " + parent.ifBlank { root.name ?: "root" }, e)
            }
            for (child in children.sortedBy { it.name?.lowercase(Locale.ROOT).orEmpty() }) {
                val name = child.name ?: continue
                if (!keep(name, child.isDirectory, options)) continue
                val path = if (parent.isEmpty()) name else "$parent/$name"
                if (child.isDirectory) stack.add(child to path)
                else if (child.isFile) {
                    result += SourceFile(path, child.length().coerceAtLeast(0L)) {
                        context.contentResolver.openInputStream(child.uri)
                            ?: throw IllegalStateException("Không thể mở tệp")
                    }
                }
            }
        }
        return result.sortedBy { it.path }
    }

    private fun keep(name: String, isDirectory: Boolean, options: ScanOptions): Boolean {
        if (name.startsWith(".") && ((isDirectory && options.skipHiddenFolders) ||
                (!isDirectory && options.skipHiddenFiles))) return false
        if (options.skipSystemJunk && name in setOf(".thumbnails", ".cache", "cache", ".temp", ".trash")) return false
        return true
    }

    private class FileTooLargeException : Exception()

    private fun readBounded(input: InputStream, maxBytes: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        input.use {
            while (true) {
                val count = it.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxBytes) throw FileTooLargeException()
                out.write(buffer, 0, count)
            }
        }
        return out.toByteArray()
    }

    private fun decodeText(bytes: ByteArray): String? {
        if (bytes.any { it == 0.toByte() }) return null
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes)).toString()
        } catch (_: Exception) { null }
    }

    private fun appendTree(out: StringBuilder, paths: List<String>) {
        val folders = sortedSetOf<String>()
        paths.forEach { path ->
            val parts = path.split('/')
            for (i in 1 until parts.size) folders += parts.take(i).joinToString("/")
        }
        val all = (folders.map { it to true } + paths.map { it to false }).distinct()
            .sortedWith(compareBy({ it.first }, { !it.second }))
        for ((path, folder) in all) {
            val depth = path.count { it == '/' }
            out.append("  ".repeat(depth)).append(if (folder) "📁 " else "📄 ")
                .append(if (folder) path.substringAfterLast('/') + "/" else path.substringAfterLast('/'))
                .append('\n')
        }
    }

    private fun longestTickRun(text: String): Int {
        val tick = 96.toChar()
        var longest = 0
        var current = 0
        for (char in text) {
            if (char == tick) { current++; if (current > longest) longest = current }
            else current = 0
        }
        return longest
    }

    private fun formatBytes(bytes: Long): String {
        if (bytes < 1024L) return "$bytes B"
        if (bytes < 1024L * 1024L) return String.format(Locale.ROOT, "%.1f KB", bytes / 1024.0)
        return String.format(Locale.ROOT, "%.2f MB", bytes / (1024.0 * 1024.0))
    }
}