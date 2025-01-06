package com.fiatjaf.volare.ui.components.row.mainEvent

sealed class NoteCtx(open val note: backend.Note) {
    fun isCollapsedReply(): Boolean {
        return when (this) {
            is ThreadReplyCtx -> this.isCollapsed
            is FeedCtx, is ThreadRootCtx -> false
        }
    }
}

data class ThreadRootCtx(
    val threadableNote: backend.Note
) : NoteCtx(note = threadableNote)

data class ThreadReplyCtx(
    val reply: backend.Note,
    val isOp: Boolean,
    val level: Int,
    val isCollapsed: Boolean,
    val hasLoadedReplies: Boolean,
) : NoteCtx(note = reply)

data class FeedCtx(override val note: backend.Note) : NoteCtx(note = note)