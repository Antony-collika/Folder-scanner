package com.example.fts.domain.scanner

import com.example.fts.data.saf.SafDocument
import com.example.fts.data.model.ScanOptions

object ScanFilter {
    
    fun shouldSkip(doc: SafDocument, options: ScanOptions): Boolean {
        val name = doc.name ?: return true
        
        if (options.skipHiddenFolders && doc.isDirectory && name.startsWith(".")) {
            return true
        }
        
        if (options.skipHiddenFiles && doc.isFile && name.startsWith(".")) {
            return true
        }
        
        if (options.skipSystemJunk && isSystemJunk(name)) {
            return true
        }
        
        return false
    }
    
    private fun isSystemJunk(name: String): Boolean {
        return name in setOf(
            ".thumbnails", ".cache", "cache", ".temp", ".trash",
            "Android/data", "Android/obb", "Android/media"
        )
    }
}