package com.example.fts.ui.diff

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fts.data.cache.CacheManager
import com.example.fts.data.model.Diff
import com.example.fts.domain.diff.DiffEngine
import kotlinx.coroutines.launch

class DiffViewModel(
    private val rootUri: String,
    val displayName: String,
    application: Application
) : AndroidViewModel(application) {

    private val cacheManager = CacheManager(application)

    private val _diff = MutableLiveData<Diff?>()
    val diff: LiveData<Diff?> = _diff

    private val _diffItems = MutableLiveData<List<DiffItem>>()
    val diffItems: LiveData<List<DiffItem>> = _diffItems

    private val _exportResult = MutableLiveData<Result<Unit>>()
    val exportResult: LiveData<Result<Unit>> = _exportResult

    private var pendingExport: Pair<String, String>? = null

    fun loadDiff() {
        viewModelScope.launch {
            try {
                val oldSnapshot = cacheManager.loadOldSnapshot(rootUri)
                val newSnapshot = cacheManager.load(rootUri)

                if (oldSnapshot != null && newSnapshot != null) {
                    val diff = DiffEngine.compare(oldSnapshot, newSnapshot)
                    _diff.value = diff
                    updateDiffItems(diff)
                }
            } catch (e: Exception) {
                _diff.value = null
            }
        }
    }

    private fun updateDiffItems(diff: Diff) {
        val items = mutableListOf<DiffItem>()

        if (diff.added.isNotEmpty()) {
            items.add(DiffItem.Header("Added (" + diff.added.size + ")"))
            diff.added.forEach { item ->
                items.add(DiffItem.Added(item.path, item.type))
            }
        }

        if (diff.removed.isNotEmpty()) {
            items.add(DiffItem.Header("Removed (" + diff.removed.size + ")"))
            diff.removed.forEach { item ->
                items.add(DiffItem.Removed(item.path, item.type))
            }
        }

        if (diff.modified.isNotEmpty()) {
            items.add(DiffItem.Header("Modified (" + diff.modified.size + ")"))
            diff.modified.forEach { item ->
                items.add(DiffItem.Modified(item.path, item.changes))
            }
        }

        if (diff.renamed.isNotEmpty()) {
            items.add(DiffItem.Header("Renamed (" + diff.renamed.size + ")"))
            diff.renamed.forEach { item ->
                items.add(DiffItem.Renamed(item.oldPath, item.newPath))
            }
        }

        if (diff.moved.isNotEmpty()) {
            items.add(DiffItem.Header("Moved (" + diff.moved.size + ")"))
            diff.moved.forEach { item ->
                items.add(DiffItem.Moved(item.oldPath, item.newPath))
            }
        }

        _diffItems.value = items
    }

    fun prepareExport(content: String, type: String) {
        pendingExport = content to type
    }

    fun onExportFileCreated(uri: android.net.Uri) {
        pendingExport?.let { (content, type) ->
            viewModelScope.launch {
                try {
                    val outputStream = getApplication<Application>().contentResolver.openOutputStream(uri)
                    outputStream?.use { it.write(content.toByteArray()) }
                    _exportResult.value = Result.success(Unit)
                } catch (e: Exception) {
                    _exportResult.value = Result.failure(e)
                }
            }
        }
        pendingExport = null
    }

    sealed class DiffItem {
        data class Header(val text: String) : DiffItem()
        data class Added(val path: String, val type: com.example.fts.data.model.EntryType) : DiffItem()
        data class Removed(val path: String, val type: com.example.fts.data.model.EntryType) : DiffItem()
        data class Modified(val path: String, val changes: Map<String, Pair<Any?, Any?>>) : DiffItem()
        data class Renamed(val oldPath: String, val newPath: String) : DiffItem()
        data class Moved(val oldPath: String, val newPath: String) : DiffItem()
    }
}