package com.example.fts.util

/** Shared binary size-unit conversion for content-export limits. */
object ContentSizeUnits {
    const val BYTES_PER_KB = 1024L
    const val BYTES_PER_MB = 1024L * 1024L

    fun toBytes(value: Int, unit: String): Long {
        require(value > 0) { "Size must be a positive integer" }
        val multiplier = when (unit) {
            "KB" -> BYTES_PER_KB
            "MB" -> BYTES_PER_MB
            else -> throw IllegalArgumentException("Unsupported size unit: $unit")
        }
        return Math.multiplyExact(value.toLong(), multiplier)
    }
}