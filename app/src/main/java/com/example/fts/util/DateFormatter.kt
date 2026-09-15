package com.example.fts.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormatter {
    
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val iso8601Format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
    
    fun format(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }
    
    fun formatIso8601(timestamp: Long): String {
        return iso8601Format.format(Date(timestamp))
    }
}