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
import com.fiatjaf.volare.core.model.MainEvent
import com.fiatjaf.volare.core.model.RootPost
import com.fiatjaf.volare.core.model.ThreadableMainEvent
import com.fiatjaf.volare.core.utils.launchIO
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
import rust.nostr.sdk.Nip19Event

class ThreadViewModel(
    val postDetails: State<PostDetails?>,
    val threadState: LazyListState,
    private val threadProvider: ThreadProvider,
    private val threadCollapser: ThreadCollapser,
) : ViewModel() {

    val isRefreshing = mutableStateOf(false)
    var parentIsAvailable: StateFlow<Boolean> = MutableStateFlow(false)
    var localRoot: StateFlow<ThreadRootCtx?> = MutableStateFlow(null)
    val replies: MutableState<StateFlow<List<ThreadReplyCtx>>> =
        mutableStateOf(MutableStateFlow(emptyList()))
    val totalReplyCount: MutableState<StateFlow<Int>> = mutableStateOf(MutableStateFlow(0))
    private val parentIds = mutableStateOf(emptySet<String>())
    private var nevent: Nip19Event? = null

    fun openThread(nevent: Nip19Event, parentUi: ThreadableMainEvent?) {
        val id = nevent.eventId().toHex()
        if (id == localRoot.value?.mainEvent?.id) return

        replies.value = MutableStateFlow(emptyList())
        this.nevent = nevent

        localRoot = threadProvider
            .getLocalRoot(scope = viewModelScope, nevent = nevent, isInit = true)
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                parentUi?.let { ThreadRootCtx(threadableMainEvent = it) }
            )
        checkParentAvailability(replyId = id, parentUi = parentUi)
        loadReplies(
            rootId = id,
            parentId = id,
            isInit = true,
        )
    }

    fun handle(action: ThreadViewAction) {
        when (action) {
            is ThreadViewRefresh -> refresh()
            is ThreadViewToggleCollapse -> threadCollapser.toggleCollapse(id = action.id)
            is ThreadViewShowReplies -> loadReplies(
                rootId = localRoot.value?.threadableMainEvent?.getRelevantId(),
                parentId = action.id,
                isInit = false,
            )
        }
    }

    private fun refresh() {
        if (isRefreshing.value) return

        val currentNevent = nevent ?: return
        val currentRoot = localRoot.value

        isRefreshing.value = true

        viewModelScope.launchIO {
            localRoot = threadProvider
                .getLocalRoot(scope = viewModelScope, nevent = currentNevent, isInit = false)
                .stateIn(viewModelScope, SharingStarted.Eagerly, currentRoot)
            parentIsAvailable = threadProvider
                .getParentIsAvailableFlow(
                    scope = viewModelScope,
                    replyId = currentNevent.eventId().toHex()
                )
                .stateIn(viewModelScope, SharingStarted.Eagerly, parentIsAvailable.value)
            replies.value = threadProvider.getReplyCtxs(
                rootId = currentNevent.eventId().toHex(),
                parentIds = parentIds.value,
            )
                .stateIn(
                    viewModelScope,
                    SharingStarted.Eagerly,
                    replies.value.value
                )
            delay(DELAY_1SEC)
        }.invokeOnCompletion { isRefreshing.value = false }
    }

    private fun loadReplies(
        rootId: String?,
        parentId: String,
        isInit: Boolean,
    ) {
        if (rootId == null) return

        val init = if (isInit) emptyList() else replies.value.value
        parentIds.value += rootId
        parentIds.value += parentId
        totalReplyCount.value = threadProvider.getTotalReplyCount(rootId = rootId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, 0)
        replies.value = threadProvider
            .getReplyCtxs(rootId = rootId, parentIds = parentIds.value)
            .stateIn(viewModelScope, SharingStarted.Eagerly, init)
    }

    private fun checkParentAvailability(replyId: String, parentUi: MainEvent?) {
        if (parentUi is RootPost) return

        parentIsAvailable = threadProvider
            .getParentIsAvailableFlow(scope = viewModelScope, replyId = replyId)
            .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    }
}
