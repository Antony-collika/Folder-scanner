package com.example.fts.util

object FileSizeFormatter {
    private const val KB = 1024L
    private const val MB = KB * 1024
    private const val GB = MB * 1024
    private const val TB = GB * 1024

    fun format(bytes: Long): String {
        return when {
            bytes >= TB -> String.format("%.2f TB", bytes.toDouble() / TB)
            bytes >= GB -> String.format("%.2f GB", bytes.toDouble() / GB)
            bytes >= MB -> String.format("%.2f MB", bytes.toDouble() / MB)
            bytes >= KB -> String.format("%.2f KB", bytes.toDouble() / KB)
            else -> "$bytes bytes"
        }
    }

    fun formatSize(bytes: Long): String = format(bytes)
    fun formatCount(count: Int): String = count.toString()
}