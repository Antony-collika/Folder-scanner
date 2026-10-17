package com.example.fts.data.repository

import android.content.Context
import androidx.preference.PreferenceManager
import com.example.fts.data.model.MarkdownModel
import com.example.fts.data.model.ScanOptions
import com.example.fts.util.Constants

class SettingsRepository(context: Context) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun scanOptions(): ScanOptions = ScanOptions(
        skipHiddenFolders = preferences.getBoolean("skip_hidden_folders", true),
        skipHiddenFiles = preferences.getBoolean("skip_hidden_files", true),
        skipSystemJunk = preferences.getBoolean("skip_system_junk", true),
        maxDepth = preferences.getString("max_depth", "")?.trim()?.toIntOrNull()?.takeIf { it >= 0 },
        sortOrder = sortOrder()
    )

    fun markdownModel(): MarkdownModel = when (preferences.getString("markdown_model", Constants.DEFAULT_MARKDOWN_MODEL)) {
        "A" -> MarkdownModel.A
        "B" -> MarkdownModel.B
        "C" -> MarkdownModel.C
        else -> MarkdownModel.D
    }
    fun showMetadataInMarkdown(): Boolean = preferences.getBoolean("show_metadata_in_md", false)
    fun sortOrder(): String = preferences.getString("sort_order", "name") ?: "name"
    fun setSortOrder(value: String) { preferences.edit().putString("sort_order", value).apply() }
    fun notificationOnScanComplete(): Boolean = preferences.getBoolean("notification_on_scan_complete", true)

    fun contentRootLimitBytes(): Long = sizeToBytes(
        preferences.getInt(KEY_ROOT_LIMIT_VALUE, 2),
        preferences.getString(KEY_ROOT_LIMIT_UNIT, "MB") ?: "MB"
    )
    fun contentPerFileLimitBytes(): Long = sizeToBytes(
        preferences.getInt(KEY_FILE_LIMIT_VALUE, 200),
        preferences.getString(KEY_FILE_LIMIT_UNIT, "KB") ?: "KB"
    )
    fun contentRootLimitValue(): Int = preferences.getInt(KEY_ROOT_LIMIT_VALUE, 2)
    fun contentRootLimitUnit(): String = preferences.getString(KEY_ROOT_LIMIT_UNIT, "MB") ?: "MB"
    fun contentPerFileLimitValue(): Int = preferences.getInt(KEY_FILE_LIMIT_VALUE, 200)
    fun contentPerFileLimitUnit(): String = preferences.getString(KEY_FILE_LIMIT_UNIT, "KB") ?: "KB"

    fun setContentRootLimit(value: Int, unit: String) {
        require(value > 0 && unit in UNITS)
        preferences.edit().putInt(KEY_ROOT_LIMIT_VALUE, value).putString(KEY_ROOT_LIMIT_UNIT, unit).apply()
    }
    fun setContentPerFileLimit(value: Int, unit: String) {
        require(value > 0 && unit in UNITS)
        preferences.edit().putInt(KEY_FILE_LIMIT_VALUE, value).putString(KEY_FILE_LIMIT_UNIT, unit).apply()
    }

    private fun sizeToBytes(value: Int, unit: String): Long =
        value.toLong() * if (unit == "MB") 1024L * 1024L else 1024L

    companion object {
        private const val KEY_ROOT_LIMIT_VALUE = "content_root_limit_value"
        private const val KEY_ROOT_LIMIT_UNIT = "content_root_limit_unit"
        private const val KEY_FILE_LIMIT_VALUE = "content_file_limit_value"
        private const val KEY_FILE_LIMIT_UNIT = "content_file_limit_unit"
        val UNITS = arrayOf("KB", "MB")
    }
}