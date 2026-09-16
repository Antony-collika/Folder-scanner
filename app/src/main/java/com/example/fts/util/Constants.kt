package com.example.fts.util

object Constants {
    // Notification
    const val NOTIFICATION_CHANNEL_ID = "fts_scan_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Folder scan"
    const val NOTIFICATION_ID_SCAN = 1001
    const val NOTIFICATION_ID_SCAN_COMPLETE = 1002
    
    // Extras
    const val EXTRA_URI = "extra_uri"
    const val EXTRA_DISPLAY_NAME = "extra_display_name"
    const val EXTRA_INCREMENTAL = "extra_incremental"
    
    // Service extras
    const val SERVICE_EXTRA_URI = EXTRA_URI
    const val SERVICE_EXTRA_ROOT_NAME = EXTRA_DISPLAY_NAME
    const val SERVICE_EXTRA_IS_INCREMENTAL = EXTRA_INCREMENTAL
    
    // Scan options
    const val MAX_DEPTH_UNLIMITED = -1
    const val DEFAULT_MARKDOWN_MODEL = "D"
    
    // Cache
    const val CACHE_DIR_NAME = "snapshots"
    const val MAX_FOLDERS = 20
}