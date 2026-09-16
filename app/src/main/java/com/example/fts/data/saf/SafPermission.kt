package com.example.fts.data.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build

object SafPermission {
    fun takePersistableUriPermission(context: Context, uri: Uri): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
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
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            true
        } catch (e: SecurityException) {
            false
        }
    }

    fun hasPersistableUriPermission(context: Context, uri: Uri): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && (it.isReadPermission || it.isWritePermission)
            }
        } else {
            true
        }
    }
}
