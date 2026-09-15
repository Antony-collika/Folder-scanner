package com.example.fts.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fts.data.model.RootFolder
import com.example.fts.data.repository.RootFolderRepository
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RootFolderRepository(application)

    private val _rootFolders = MutableLiveData<List<RootFolder>>()
    val rootFolders: LiveData<List<RootFolder>> = _rootFolders

    init {
        loadRootFolders()
    }

    fun loadRootFolders() {
        viewModelScope.launch {
            _rootFolders.value = repository.getAll()
        }
    }

    fun saveRootFolder(rootFolder: RootFolder) {
        viewModelScope.launch {
            repository.save(rootFolder)
            loadRootFolders()
        }
    }

    fun deleteRootFolder(uri: String) {
        viewModelScope.launch {
            repository.delete(uri)
            loadRootFolders()
        }
    }
}