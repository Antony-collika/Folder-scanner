package com.example.fts.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.fts.data.model.RootFolder
import com.example.fts.data.repository.RootFolderRepository

class MainViewModel(private val repository: RootFolderRepository) : ViewModel() {
    
    private val _rootFolders = MutableLiveData<List<RootFolder>>()
    val rootFolders: LiveData<List<RootFolder>> = _rootFolders
    
    init {
        loadRootFolders()
    }
    
    fun loadRootFolders() {
        _rootFolders.value = repository.getAll().sortedByDescending { it.lastScannedAt ?: it.addedAt }
    }
    
    fun addRootFolder(rootFolder: RootFolder): Boolean {
        val success = repository.add(rootFolder)
        if (success) {
            loadRootFolders()
        }
        return success
    }
    
    fun removeRootFolder(uri: String): Boolean {
        val success = repository.remove(uri)
        if (success) {
            loadRootFolders()
        }
        return success
    }
    
    fun getFolderByUri(uri: String): RootFolder? {
        return repository.getByUri(uri)
    }

}