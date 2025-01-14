package com.fiatjaf.volare.data.interactor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ThreadCollapser {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _collapsedIds = MutableStateFlow(setOf<String>())

    val collapsedIds = _collapsedIds
        .stateIn(scope, SharingStarted.Eagerly, _collapsedIds.value)

    fun toggleCollapse(id: String) {
        _collapsedIds.update {
            if (it.contains(id)) it - id else it + id
        }
    }
}
