package com.fiatjaf.volare.ui.components.row.mainEvent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.ComposableContent
import com.fiatjaf.volare.core.Fn
import com.fiatjaf.volare.core.MAX_CONTENT_LINES
import com.fiatjaf.volare.core.MAX_SUBJECT_LINES
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.OpenThread
import com.fiatjaf.volare.core.OpenThreadRaw
import com.fiatjaf.volare.core.ThreadViewShowReplies
import com.fiatjaf.volare.core.ThreadViewToggleCollapse
import com.fiatjaf.volare.core.VotePollOption
import com.fiatjaf.volare.core.model.Comment
import com.fiatjaf.volare.core.model.CrossPost
import com.fiatjaf.volare.core.model.LegacyReply
import com.fiatjaf.volare.core.model.Poll
import com.fiatjaf.volare.core.model.RootPost
import com.fiatjaf.volare.core.model.ThreadableMainEvent
import com.fiatjaf.volare.data.nostr.createNevent
import com.fiatjaf.volare.data.nostr.getCurrentSecs
import com.fiatjaf.volare.ui.components.button.footer.CountedCommentButton
import com.fiatjaf.volare.ui.components.button.footer.ReplyIconButton
import com.fiatjaf.volare.ui.components.row.PollOptionRow
import com.fiatjaf.volare.ui.components.text.AnnotatedText
import com.fiatjaf.volare.ui.theme.spacing
import com.fiatjaf.volare.ui.views.nonMain.MoreRepliesTextButton

@Composable
fun MainEventRow(
    ctx: MainEventCtx,
    onUpdate: OnUpdate,
    isFocused: Boolean = false,
) {
    when (ctx) {
        is FeedCtx -> MainEventMainRow(
            ctx = ctx,
            onUpdate = onUpdate,
            isFocused = isFocused
        )

        is ThreadRootCtx -> {
            when (ctx.threadableMainEvent) {
                is RootPost, is Poll -> MainEventMainRow(
                    ctx = ctx,
                    onUpdate = onUpdate,
                    isFocused = true
                )

                is LegacyReply, is Comment -> RowWithDivider(level = 1) {
                    MainEventMainRow(
                        ctx = ctx,
                        onUpdate = onUpdate,
                        isFocused = true
                    )
                }
            }
        }

        is ThreadReplyCtx -> {
            RowWithDivider(level = ctx.level) {
                MainEventMainRow(
                    ctx = ctx,
                    onUpdate = onUpdate,
                    isFocused = true
                )
            }
        }
    }
}

@Composable
private fun MainEventMainRow(
    ctx: MainEventCtx,
    onUpdate: OnUpdate,
    isFocused: Boolean = false
) {
    val onClickRow = {
        when (ctx) {
            is ThreadReplyCtx -> onUpdate(ThreadViewToggleCollapse(id = ctx.reply.id))
            is FeedCtx -> {
                when (val event = ctx.mainEvent) {
                    is ThreadableMainEvent -> onUpdate(OpenThread(mainEvent = event))
                    is CrossPost -> onUpdate(OpenThreadRaw(nevent = createNevent(hex = event.crossPostedId)))
                }
            }
            is ThreadRootCtx -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClickRow)
            .padding(vertical = spacing.bigScreenEdge)
            .padding(start = spacing.bigScreenEdge)
    ) {
        MainEventHeader(
            ctx = ctx,
            onUpdate = onUpdate,
        )
        Spacer(modifier = Modifier.height(spacing.large))

        // Another col for end padding excluding header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = spacing.bigScreenEdge)
        ) {
            ctx.mainEvent.getRelevantSubject()?.let { subject ->
                if (subject.isNotEmpty()) {
                    AnnotatedText(
                        text = subject,
                        maxLines = MAX_SUBJECT_LINES,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(spacing.large))
                }
            }

            AnimatedVisibility(
                visible = !ctx.isCollapsedReply(),
                exit = slideOutVertically(animationSpec = tween(durationMillis = 0))
            ) {
                AnnotatedText(
                    items = ctx.mainEvent.content,
                    maxLines = when (ctx) {
                        is ThreadReplyCtx, is ThreadRootCtx -> Int.MAX_VALUE
                        is FeedCtx -> MAX_CONTENT_LINES
                    },
                    preload = isFocused
                )
                Spacer(modifier = Modifier.height(spacing.large))
            }

            when (val event = ctx.mainEvent) {
                is Poll -> PollColumn(poll = event, onUpdate = onUpdate, onClickRow = onClickRow)
                is CrossPost,
                is RootPost,
                is Comment,
                is LegacyReply -> {
                }
            }

            if (!ctx.isCollapsedReply()) MainEventActions(
                mainEvent = ctx.mainEvent,
                onUpdate = onUpdate,
                additionalStartAction = {
                    when (ctx) {
                        is ThreadReplyCtx -> {
                            if (ctx.reply.replyCount > 0 && !ctx.hasLoadedReplies) {
                                MoreRepliesTextButton(
                                    replyCount = ctx.reply.replyCount,
                                    onShowReplies = {
                                        onUpdate(ThreadViewShowReplies(id = ctx.reply.id))
                                    }
                                )
                            }
                        }

                        is FeedCtx, is ThreadRootCtx -> {}
                    }

                },
                additionalEndAction = {
                    when (ctx) {
                        is ThreadReplyCtx -> ReplyIconButton(ctx = ctx, onUpdate = onUpdate)

                        is ThreadRootCtx -> CountedCommentButton(ctx = ctx, onUpdate = onUpdate)

                        is FeedCtx -> {
                            when (ctx.mainEvent) {
                                is RootPost,
                                is Poll,
                                is CrossPost -> CountedCommentButton(ctx = ctx, onUpdate = onUpdate)

                                is LegacyReply, is Comment -> ReplyIconButton(
                                    ctx = ctx,
                                    onUpdate = onUpdate
                                )
                            }
                        }
                    }
                })
        }
    }
}

@Composable
private fun RowWithDivider(level: Int, content: ComposableContent) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        repeat(times = level) {
            VerticalDivider(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(start = spacing.large, end = spacing.medium)
            )
        }
        content()
    }
}

@Composable
private fun PollColumn(poll: Poll, onUpdate: OnUpdate, onClickRow: Fn) {
    val myVote: String? = null // TODO: somehow figure this out
    val isExpired = remember(poll.endsAt) {
        poll.endsAt != null && poll.endsAt <= getCurrentSecs()
    }
    val clickedId = remember {
        mutableStateOf<String?>(null)
    }
    val alreadyVoted = myVote != null
    val isRevealed = remember(isExpired, alreadyVoted) {
        alreadyVoted || isExpired
    }
    val topVotes = remember(poll) {
        poll.options.maxOfOrNull { it.voteCount } ?: 0
    }
    val totalVotes = remember(poll) {
        poll.options.sumOf { it.voteCount }
    }
    Column {
        for (option in poll.options) {
            PollOptionRow(
                label = option.label,
                isSelected = if (clickedId.value != null) clickedId.value == option.optionId else option.optionId == myVote,
                isRevealed = isRevealed,
                percentage = remember(option.voteCount, totalVotes) {
                    if (totalVotes == 0) 0
                    else option.voteCount.toFloat().div(totalVotes).times(100).toInt()
                },
                progress = remember(option.voteCount, topVotes) {
                    if (topVotes == 0) 0f else option.voteCount.toFloat().div(topVotes)
                },
                onClick = {
                    if (isRevealed) onClickRow.invoke() else clickedId.value = option.optionId
                },
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                modifier = Modifier.padding(start = spacing.large),
                text = if (totalVotes == 0) stringResource(id = R.string.no_votes)
                else stringResource(id = R.string.n_votes, totalVotes),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.width(spacing.medium))
            if (isExpired) Text(
                text = stringResource(id = R.string.poll_has_ended),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (!alreadyVoted) clickedId.value?.let { optionId ->
            Button(onClick = {
                onUpdate(VotePollOption(pollId = poll.id, optionId = optionId))
            }) {
                Text(stringResource(id = R.string.vote))
            }
        }
    }
}
