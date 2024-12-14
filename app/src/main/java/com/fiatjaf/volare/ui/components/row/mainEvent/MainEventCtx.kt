package com.fiatjaf.volare.ui.components.row.mainEvent

import com.fiatjaf.volare.core.model.MainEvent
import com.fiatjaf.volare.core.model.SomeReply
import com.fiatjaf.volare.core.model.ThreadableMainEvent

sealed class MainEventCtx(open val mainEvent: MainEvent) {
    fun isCollapsedReply(): Boolean {
        return when (this) {
            is ThreadReplyCtx -> this.isCollapsed
            is FeedCtx, is ThreadRootCtx -> false
        }
    }
}

data class ThreadRootCtx(
    val threadableMainEvent: ThreadableMainEvent
) : MainEventCtx(mainEvent = threadableMainEvent)

data class ThreadReplyCtx(
    val reply: SomeReply,
    val isOp: Boolean,
    val level: Int,
    val isCollapsed: Boolean,
    val hasLoadedReplies: Boolean,
) : MainEventCtx(mainEvent = reply)

data class FeedCtx(override val mainEvent: MainEvent) : MainEventCtx(mainEvent = mainEvent)
