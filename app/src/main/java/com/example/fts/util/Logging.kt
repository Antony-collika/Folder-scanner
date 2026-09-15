package com.example.fts.util

import android.util.Log

object Logging {
    
    private const val TAG = "FTS"
    private var debugEnabled = true
    
    fun d(message: String) {
        if (debugEnabled) Log.d(TAG, message)
    }
    
    fun e(message: String, throwable: Throwable? = null) {
        if (debugEnabled) Log.e(TAG, message, throwable)
    }
    
    fun i(message: String) {
        if (debugEnabled) Log.i(TAG, message)
    }
    
    fun w(message: String) {
        if (debugEnabled) Log.w(TAG, message)
    }
    
    fun setDebugEnabled(enabled: Boolean) {
        debugEnabled = enabled
    }
}