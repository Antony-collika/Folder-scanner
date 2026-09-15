package com.example.fts.ui.tree

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fts.data.cache.CacheManager
import com.example.fts.data.model.Snapshot
import com.example.fts.domain.flatten.FlatNode
import com.example.fts.domain.flatten.TreeFlattener
import kotlinx.coroutines.launch

class TreeViewModel(
    private val rootUri: String,
    val displayName: String,
    application: Application
) : AndroidViewModel(application) {

    private val cacheManager = CacheManager(application)
    private val expandedFolders = mutableSetOf<String>()

    private val _treeItems = MutableLiveData<List<FlatNode>>()
    val treeItems: LiveData<List<FlatNode>> = _treeItems

    private val _snapshot = MutableLiveData<Snapshot?>()
    val snapshot: LiveData<Snapshot?> = _snapshot

    private val _showDiff = MutableLiveData<Boolean>()
    val showDiff: LiveData<Boolean> = _showDiff

    private val _exportResult = MutableLiveData<Result<Unit>>()
    val exportResult: LiveData<Result<Unit>> = _exportResult

    private var pendingExport: Pair<String, String>? = null

    fun loadData() {
        viewModelScope.launch {
            try {
                val snapshot = cacheManager.load(rootUri)
                _snapshot.value = snapshot
                snapshot?.let { updateTree(it) }
            } catch (e: Exception) {
                _snapshot.value = null
            }
        }
    }

    private fun updateTree(snapshot: Snapshot) {
        val flatNodes = TreeFlattener.flatten(snapshot.root, expandedFolders)
        _treeItems.value = flatNodes
    }

    fun toggleFolder(documentId: String) {
        if (expandedFolders.contains(documentId)) {
            expandedFolders.remove(documentId)
        } else {
            expandedFolders.add(documentId)
        }
        _snapshot.value?.let { updateTree(it) }
    }

    fun rescan() {
        loadData()
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
}