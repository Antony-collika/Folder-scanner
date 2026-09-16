package com.example.fts.ui.tree

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fts.data.cache.CacheManager
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
    private val _treeItems = MutableLiveData<List<FlatNode>>(); val treeItems: LiveData<List<FlatNode>> = _treeItems
    private val _snapshot = MutableLiveData<Snapshot?>(); val snapshot: LiveData<Snapshot?> = _snapshot
    private val _showDiff = MutableLiveData<Boolean>(); val showDiff: LiveData<Boolean> = _showDiff
    private val _exportResult = MutableLiveData<Result<Unit>>(); val exportResult: LiveData<Result<Unit>> = _exportResult
    private var pendingExport: Pair<String, String>? = null
    private var query = ""

    fun loadData() { viewModelScope.launch { try { cacheManager.load(rootUri)?.let { _snapshot.value = it; updateTree(it) } ?: scanFresh() } catch (_: Exception) { _snapshot.value = null } } }

    private suspend fun scanFresh() {
        try {
            val snapshot = scanner.scan(android.net.Uri.parse(rootUri), displayName, settingsRepository.scanOptions()) { }
            cacheManager.load(rootUri)?.let { cacheManager.savePrevious(rootUri, it) }
            cacheManager.save(rootUri, snapshot); _snapshot.value = snapshot; updateTree(snapshot)
        } catch (_: Exception) { _snapshot.value = null }
    }

    private fun updateTree(snapshot: Snapshot) {
        val allExpanded = if (query.isBlank()) expandedFolders else collectFolderIds(snapshot.root)
        val flattened = TreeFlattener.flatten(snapshot.root, allExpanded)
        _treeItems.value = if (query.isBlank()) flattened else flattened.filter { it.entry.name.contains(query, true) || it.entry.path.contains(query, true) }
    }

    private fun collectFolderIds(root: com.example.fts.data.model.Entry): Set<String> {
        val result = mutableSetOf<String>(); val stack = ArrayDeque<com.example.fts.data.model.Entry>(); stack.add(root)
        while (stack.isNotEmpty()) { val entry = stack.removeLast(); if (entry.type == EntryType.FOLDER) { result.add(entry.documentId); entry.children?.forEach(stack::add) } }
        return result
    }

    fun search(value: String) { query = value.trim(); _snapshot.value?.let(::updateTree) }
    fun toggleFolder(documentId: String) { if (!expandedFolders.add(documentId)) expandedFolders.remove(documentId); _snapshot.value?.let(::updateTree) }
    fun rescan() { viewModelScope.launch { scanFresh() } }
    fun requestShowDiff() { _showDiff.value = true }
    fun prepareExport(content: String, type: String) { pendingExport = content to type }

    fun onExportFileCreated(uri: android.net.Uri) {
        pendingExport?.let { (content, _) -> viewModelScope.launch {
            try { getApplication<Application>().contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) } ?: error("Cannot open output stream"); _exportResult.value = Result.success(Unit) }
            catch (e: Exception) { _exportResult.value = Result.failure(e) }
        } }
        pendingExport = null
    }
    fun markdownModel() = settingsRepository.markdownModel()
    fun showMetadataInMarkdown() = settingsRepository.showMetadataInMarkdown()
}