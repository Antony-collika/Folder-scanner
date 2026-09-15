package com.example.fts.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    private const val DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss"
    private const val DATE_PATTERN = "yyyy-MM-dd"
    private const val TIME_PATTERN = "HH:mm:ss"

    private val dateTimeFormat = SimpleDateFormat(DATE_TIME_PATTERN, Locale.getDefault())
    private val dateFormat = SimpleDateFormat(DATE_PATTERN, Locale.getDefault())
    private val timeFormat = SimpleDateFormat(TIME_PATTERN, Locale.getDefault())

    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        return timeFormat.format(Date(timestamp))
    }
}