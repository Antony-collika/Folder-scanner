package com.example.fts.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.fts.data.model.RootFolder
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RootFolderRepository(private val context: Context) {
    private val sharedPrefs: SharedPreferences by lazy {
        context.getSharedPreferences("root_folders", Context.MODE_PRIVATE)
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val KEY_ROOT_FOLDERS = "root_folders_list"
    private val MAX_FOLDERS = 20

    fun getAll(): List<RootFolder> {
        val jsonString = sharedPrefs.getString(KEY_ROOT_FOLDERS, "[]") ?: "[]"
        return try {
            json.decodeFromString<List<RootFolder>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getByUri(uri: String): RootFolder? = getAll().find { it.uri == uri }

    fun add(rootFolder: RootFolder): Boolean {
        val currentList = getAll().toMutableList()
        if (currentList.any { it.uri == rootFolder.uri }) return false
        if (currentList.size >= MAX_FOLDERS) return false
        currentList.add(rootFolder)
        saveList(currentList)
        return true
    }

    fun update(rootFolder: RootFolder): Boolean {
        val currentList = getAll().toMutableList()
        val index = currentList.indexOfFirst { it.uri == rootFolder.uri }
        if (index == -1) return false
        currentList[index] = rootFolder
        saveList(currentList)
        return true
    }

    fun remove(uri: String): Boolean {
        val currentList = getAll().toMutableList()
        val removed = currentList.removeIf { it.uri == uri }
        if (removed) saveList(currentList)
        return removed
    }

    fun clear() {
        sharedPrefs.edit().remove(KEY_ROOT_FOLDERS).apply()
    }

    private fun saveList(list: List<RootFolder>) {
        sharedPrefs.edit().putString(KEY_ROOT_FOLDERS, json.encodeToString(list)).apply()
    }
}