package com.example.fts.data.saf

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

class SafDocument(private val documentFile: DocumentFile) {
    
    val uri: Uri
        get() = documentFile.uri
    
    val name: String?
        get() = documentFile.name
    
    val isDirectory: Boolean
        get() = documentFile.isDirectory
    
    val isFile: Boolean
        get() = documentFile.isFile
    
    val type: String?
        get() = documentFile.type
    
    val lastModified: Long
        get() = documentFile.lastModified()
    
    val length: Long
        get() = documentFile.length()
    
    fun listFiles(): List<SafDocument> {
        return documentFile.listFiles()?.map { SafDocument(it) } ?: emptyList()
    }
    
    companion object {
        fun fromTreeUri(context: Context, uri: Uri): SafDocument? {
            return DocumentFile.fromTreeUri(context, uri)?.let { SafDocument(it) }
        }
        
        fun fromSingleUri(context: Context, uri: Uri): SafDocument? {
            return DocumentFile.fromSingleUri(context, uri)?.let { SafDocument(it) }
        }
    }
}