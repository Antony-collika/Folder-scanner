package com.example.fts.data.cache

import android.content.Context
import android.util.Log
import com.example.fts.data.model.Snapshot
import com.example.fts.util.Constants
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class CacheManager(private val context: Context) {
    
    private val cacheDir: File
        get() = File(context.filesDir, Constants.CACHE_DIR_NAME).apply { mkdirs() }
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    fun save(rootUri: String, snapshot: Snapshot) {
        val file = cacheFileFor(rootUri)
        try {
            file.writeText(json.encodeToString(snapshot))
        } catch (e: Exception) {
            Log.e("CacheManager", "Lỗi khi lưu cache: ${file.name}", e)
        }
    }
    
    fun load(rootUri: String): Snapshot? {
        val file = cacheFileFor(rootUri)
        if (!file.exists()) return null
        
        return try {
            json.decodeFromString<Snapshot>(file.readText())
        } catch (e: Exception) {
            Log.e("CacheManager", "Cache bị hỏng: ${file.name}", e)
            file.delete()
            null
        }
    }
    
    fun delete(rootUri: String) {
        cacheFileFor(rootUri).delete()
    }
    
    fun deleteAll() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }
    
    fun sizeOf(rootUri: String): Long = cacheFileFor(rootUri).length()
    
    fun exists(rootUri: String): Boolean = cacheFileFor(rootUri).exists()
    
    private fun cacheFileFor(rootUri: String): File {
        val hash = rootUri.hashCode().toString()
        return File(cacheDir, "snapshot_$hash.json")
    }
}