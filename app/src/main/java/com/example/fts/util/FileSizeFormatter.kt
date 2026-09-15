package com.example.fts.util

object FileSizeFormatter {
    
    private const val KB = 1024
    private const val MB = KB * 1024
    private const val GB = MB * 1024
    private const val TB = GB * 1024
    
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= TB -> "%.2f TB".format(bytes.toDouble() / TB)
            bytes >= GB -> "%.2f GB".format(bytes.toDouble() / GB)
            bytes >= MB -> "%.2f MB".format(bytes.toDouble() / MB)
            bytes >= KB -> "%.2f KB".format(bytes.toDouble() / KB)
            else -> "$bytes byte"
        }
    }
    
    fun formatCount(count: Int): String {
        return when {
            count >= 1000000 -> "%.2fM".format(count.toDouble() / 1000000)
            count >= 1000 -> "%.1fK".format(count.toDouble() / 1000)
            else -> count.toString()
        }
    }
}