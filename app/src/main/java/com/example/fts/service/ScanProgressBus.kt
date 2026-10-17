package com.example.fts.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScanProgressState(
    val rootUri: String,
    val count: Int,
    val scanning: Boolean
)

object ScanProgressBus {
    private val _states = MutableStateFlow<Map<String, ScanProgressState>>(emptyMap())
    val states: StateFlow<Map<String, ScanProgressState>> = _states.asStateFlow()

    fun update(rootUri: String, count: Int) {
        _states.value = _states.value + (rootUri to ScanProgressState(rootUri, count, true))
    }

    fun finish(rootUri: String) {
        _states.value = _states.value - rootUri
    }
}
