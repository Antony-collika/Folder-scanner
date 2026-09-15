package com.example.fts.util

object Constants {
    const val NOTIFICATION_CHANNEL_ID = "scan_channel"
    const val NOTIFICATION_CHANNEL_NAME = "Quet thu muc"
    const val NOTIFICATION_ID_SCAN = 1001
    const val NOTIFICATION_ID_SCAN_COMPLETE = 1002
    
    const val SERVICE_EXTRA_URI = "extra_uri"
    const val SERVICE_EXTRA_ROOT_NAME = "extra_root_name"
    const val SERVICE_EXTRA_IS_INCREMENTAL = "extra_is_incremental"
    const val SERVICE_ACTION_CANCEL = "action_cancel"
    
    const val REQUEST_CODE_PICK_FOLDER = 100
    const val REQUEST_CODE_CREATE_DOCUMENT = 101
    
    const val PREFS_NAME = "fts_prefs"
    const val PREFS_KEY_MARKDOWN_MODEL = "markdown_model"
    const val PREFS_KEY_SKIP_HIDDEN_FOLDERS = "skip_hidden_folders"
    const val PREFS_KEY_SKIP_HIDDEN_FILES = "skip_hidden_files"
    const val PREFS_KEY_SKIP_SYSTEM_JUNK = "skip_system_junk"
    const val PREFS_KEY_MAX_DEPTH = "max_depth"
    const val PREFS_KEY_SHOW_METADATA_IN_MD = "show_metadata_in_md"
    const val PREFS_KEY_NOTIFICATION_ENABLED = "notification_enabled"
    
    const val DEFAULT_MAX_DEPTH = -1
    const val MAX_FOLDERS = 20
    const val PROGRESS_UPDATE_INTERVAL = 100
}