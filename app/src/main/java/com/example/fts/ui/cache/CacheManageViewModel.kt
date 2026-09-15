package com.example.fts.ui.cache

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.fts.data.cache.CacheManager
import com.example.fts.data.model.RootFolder
import com.example.fts.data.repository.RootFolderRepository
import kotlinx.coroutines.launch

class CacheManageViewModel(application: Application) : AndroidViewModel(application) {

    private val cacheManager = CacheManager(application)
    private val rootFolderRepository = RootFolderRepository(application)

    private val _cacheItems = MutableLiveData<List<CacheItem>>()
    val cacheItems: LiveData<List<CacheItem>> = _cacheItems

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    data class CacheItem(
        val rootFolder: RootFolder,
        val cacheSize: Long
    )

    fun loadCacheItems() {
        viewModelScope.launch {
            try {
                val rootFolders = rootFolderRepository.getAll()
                val items = rootFolders.map { rootFolder ->
                    val size = try {
                        cacheManager.sizeOf(rootFolder.uri)
                    } catch (e: Exception) {
                        0L
                    }
                    CacheItem(rootFolder, size)
                }
                _cacheItems.value = items
            } catch (e: Exception) {
                _cacheItems.value = emptyList()
            }
        }
    }

    fun deleteCache(rootFolder: RootFolder) {
        viewModelScope.launch {
            try {
                cacheManager.delete(rootFolder.uri)
                _deleteResult.value = Result.success(Unit)
                loadCacheItems()
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
            }
        }
    }

    fun deleteAllCache() {
        viewModelScope.launch {
            try {
                cacheManager.deleteAll()
                _deleteResult.value = Result.success(Unit)
                loadCacheItems()
            } catch (e: Exception) {
                _deleteResult.value = Result.failure(e)
            }
        }
    }
}