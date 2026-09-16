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

    fun notificationOnScanComplete(): Boolean = preferences.getBoolean("notification_on_scan_complete", true)
}