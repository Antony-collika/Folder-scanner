package com.example.fts.data.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object SafPermission {
    
    fun takePersistableUriPermission(context: Context, uri: Uri): Boolean {
        return try {
            val contentResolver = context.contentResolver
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                contentResolver.takePersistableUriPermission(uri, flags)
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }
    
    fun releasePersistableUriPermission(context: Context, uri: Uri): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                context.contentResolver.releasePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }
    
    fun hasPersistableUriPermission(context: Context, uri: Uri): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            val flags = context.contentResolver.persistedUriPermissions
                .find { it.uri == uri }?.persistedUriPermissions
            flags != null
        } else {
            true
        }
    }
}