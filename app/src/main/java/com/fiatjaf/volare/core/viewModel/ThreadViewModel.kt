package com.fiatjaf.volare.core.viewModel

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fiatjaf.volare.core.DELAY_1SEC
import com.fiatjaf.volare.core.ThreadViewAction
import com.fiatjaf.volare.core.ThreadViewRefresh
import com.fiatjaf.volare.core.ThreadViewShowReplies
import com.fiatjaf.volare.core.ThreadViewToggleCollapse
import com.fiatjaf.volare.core.utils.launchIO
import com.fiatjaf.volare.data.BackendDatabase
import com.fiatjaf.volare.data.interactor.ThreadCollapser
import com.fiatjaf.volare.data.model.PostDetails
import com.fiatjaf.volare.data.provider.ThreadProvider
import com.fiatjaf.volare.ui.components.row.mainEvent.ThreadReplyCtx
import com.fiatjaf.volare.ui.components.row.mainEvent.ThreadRootCtx
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ThreadViewModel(
    val postDetails: State<PostDetails?>,
    val threadState: LazyListState,
    private val backendDB: BackendDatabase,
    private val threadProvider: ThreadProvider,
    private val threadCollapser: ThreadCollapser,
) : ViewModel() {
    val isRefreshing = mutableStateOf(false)
    val parentIsAvailable: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val localRoot: MutableStateFlow<ThreadRootCtx?> = MutableStateFlow(null)
    val replies: MutableState<StateFlow<List<ThreadReplyCtx>>> =
        mutableStateOf(MutableStateFlow(emptyList()))
    val totalReplyCount: MutableState<StateFlow<Int>> = mutableStateOf(MutableStateFlow(0))
    private var pointer: backend.EventPointer? = null

    fun openThread(pointer: backend.EventPointer, parentUi: backend.Note?) {
        if (localRoot.value?.note != null && pointer.matchesNote(localRoot.value?.note)) return

        replies.value = MutableStateFlow(emptyList())
        this.pointer = pointer

        viewModelScope.launchIO {
            val current = backendDB.getNote(pointer.id())
            if (current != null) {
                localRoot.emit(ThreadRootCtx(current))
                this@ThreadViewModel.parentIsAvailable.emit(current.parent() != "")

                loadReplies(current)
            }
        }
    }

    fun handle(action: ThreadViewAction) {
        when (action) {
            is ThreadViewRefresh -> refresh()
            is ThreadViewToggleCollapse -> threadCollapser.toggleCollapse(id = action.id)
            is ThreadViewShowReplies -> {
                val root = localRoot.value?.threadableNote
                if (root != null) {
                    loadReplies(root)
                }
            }
        }
    }

    private fun refresh() {
        if (isRefreshing.value) return

        val currentPointer = pointer ?: return
        // val currentRoot = localRoot.value

        isRefreshing.value = true

        viewModelScope.launchIO {
            val current = backendDB.getNote(currentPointer.id())
            if (current != null) {
                localRoot.emit(ThreadRootCtx(current))
                parentIsAvailable.emit(current.parent() != "")

                replies.value = threadProvider.getReplyCtxs(current)
                    .stateIn(
                        viewModelScope,
                        SharingStarted.Eagerly,
                        replies.value.value
                    )

                delay(DELAY_1SEC)
            }
        }.invokeOnCompletion { isRefreshing.value = false }
    }

    private fun loadReplies(root: backend.Note) {
        totalReplyCount.value = threadProvider.getTotalReplyCount(rootId = root.id())
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        replies.value = threadProvider
            .getReplyCtxs(root)
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    }
}
