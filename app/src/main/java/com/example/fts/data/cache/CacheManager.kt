package com.example.fts.data.cache

import android.content.Context
import android.util.Log
import com.example.fts.data.model.Snapshot
import com.example.fts.util.Constants
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.security.MessageDigest

class CacheManager(private val context: Context) {
    private val cacheDir: File
        get() = File(context.filesDir, Constants.CACHE_DIR_NAME).apply { mkdirs() }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun save(rootUri: String, snapshot: Snapshot) {
        write(cacheFileFor(rootUri), snapshot)
    }

    fun savePrevious(rootUri: String, snapshot: Snapshot) {
        write(previousCacheFileFor(rootUri), snapshot)
    }

    fun load(rootUri: String): Snapshot? = read(cacheFileFor(rootUri))

    fun loadPrevious(rootUri: String): Snapshot? = read(previousCacheFileFor(rootUri))

    fun delete(rootUri: String) {
        cacheFileFor(rootUri).delete()
        previousCacheFileFor(rootUri).delete()
    }

    fun deleteAll() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun sizeOf(rootUri: String): Long = cacheFileFor(rootUri).length()

    fun exists(rootUri: String): Boolean = cacheFileFor(rootUri).exists()

    private fun write(file: File, snapshot: Snapshot) {
        try {
            file.writeText(json.encodeToString(snapshot))
        } catch (e: Exception) {
            Log.e("CacheManager", "Lỗi khi lưu cache: ${file.name}", e)
        }
    }

    private fun read(file: File): Snapshot? {
        if (!file.exists()) return null
        return try {
            json.decodeFromString<Snapshot>(file.readText())
        } catch (e: Exception) {
            Log.e("CacheManager", "Cache bị hỏng: ${file.name}", e)
            file.delete()
            null
        }
    }

    private fun cacheFileFor(rootUri: String): File =
        File(cacheDir, "snapshot_${stableHash(rootUri)}.json")

    private fun previousCacheFileFor(rootUri: String): File =
        File(cacheDir, "previous_${stableHash(rootUri)}.json")

    private fun stableHash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
