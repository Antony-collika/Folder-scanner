package com.example.fts.ui.tree

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.example.fts.data.cache.CacheManager
import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType
import com.example.fts.data.model.Snapshot
import com.example.fts.data.repository.SettingsRepository
import com.example.fts.domain.flatten.FlatNode
import com.example.fts.domain.flatten.TreeFlattener
import com.example.fts.domain.scanner.Scanner
import kotlinx.coroutines.launch

class TreeViewModel(private val rootUri: String, val displayName: String, application: Application) : AndroidViewModel(application) {
    private val cacheManager = CacheManager(application)
    private val scanner = Scanner(application, cacheManager)
    private val settingsRepository = SettingsRepository(application)
    private val expandedFolders = mutableSetOf<String>()
    private val _treeItems = MutableLiveData<List<FlatNode>>()
    val treeItems: LiveData<List<FlatNode>> = _treeItems
    private val _snapshot = MutableLiveData<Snapshot?>()
    val snapshot: LiveData<Snapshot?> = _snapshot
    private val _showDiff = MutableLiveData<Boolean>()
    val showDiff: LiveData<Boolean> = _showDiff
    private val _exportResult = MutableLiveData<Result<Unit>>()
    val exportResult: LiveData<Result<Unit>> = _exportResult
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading
    private var pendingExport: Pair<String, String>? = null
    private var query = ""

    fun loadData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                cacheManager.load(rootUri)?.let { snapshot ->
                    _snapshot.value = snapshot
                    expandedFolders.add(snapshot.root.documentId)
                    updateTree(snapshot)
                } ?: scanFresh()
            } catch (_: Exception) { _snapshot.value = null }
            finally { _isLoading.value = false }
        }
    }

    private suspend fun scanFresh() {
        try {
            val previous = cacheManager.load(rootUri)
            val snapshot = scanner.scan(Uri.parse(rootUri), displayName, settingsRepository.scanOptions()) { }
            if (previous != null) cacheManager.savePrevious(rootUri, previous)
            cacheManager.save(rootUri, snapshot)
            _snapshot.value = snapshot
            expandedFolders.clear()
            expandedFolders.add(snapshot.root.documentId)
            updateTree(snapshot)
        } catch (_: Exception) { _snapshot.value = null }
    }

    private fun updateTree(snapshot: Snapshot) {
        val flattened = TreeFlattener.flatten(snapshot.root, expandedFolders, settingsRepository.sortOrder())
        if (query.isBlank()) { _treeItems.value = flattened; return }
        val matches = flattened.filter { it.entry.name.contains(query, true) || it.entry.path.contains(query, true) }
        val visibleIds = matches.mapTo(mutableSetOf()) { it.entry.documentId }
        matches.forEach { match ->
            val parts = match.entry.path.split('/').filter { it.isNotBlank() }
            var prefix = ""
            parts.dropLast(1).forEach { part ->
                prefix += if (prefix.isEmpty()) part else "/$part"
                flattened.firstOrNull { it.entry.path == prefix }?.let { visibleIds.add(it.entry.documentId) }
            }
        }
        _treeItems.value = flattened.filter { it.entry.documentId in visibleIds }
    }

    fun search(value: String) { query = value.trim(); _snapshot.value?.let(::updateTree) }
    fun toggleFolder(documentId: String) { if (!expandedFolders.add(documentId)) expandedFolders.remove(documentId); _snapshot.value?.let(::updateTree) }
    fun rescan() { viewModelScope.launch { _isLoading.value = true; scanFresh(); _isLoading.value = false } }
    fun requestShowDiff() { _showDiff.value = true }
    fun prepareExport(content: String, type: String) { pendingExport = content to type }
    fun onExportFileCreated(uri: Uri) {
        pendingExport?.let { (content, _) -> viewModelScope.launch { try { getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) } ?: error("Không thể mở tệp đích"); _exportResult.value = Result.success(Unit) } catch (e: Exception) { _exportResult.value = Result.failure(e) } } }
        pendingExport = null
    }
    fun setSortOrder(value: String) { settingsRepository.setSortOrder(value); _snapshot.value?.let(::updateTree) }
    fun markdownModel() = settingsRepository.markdownModel()
    fun showMetadataInMarkdown() = settingsRepository.showMetadataInMarkdown()
    fun sortOrder() = settingsRepository.sortOrder()
}