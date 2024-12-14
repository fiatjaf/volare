package com.dluvian.volare.ui.components.button.footer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.ClickNeutralizeVote
import com.dluvian.volare.core.ClickUpvote
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.model.MainEvent
import com.dluvian.volare.ui.theme.UpvoteIcon
import com.dluvian.volare.ui.theme.UpvoteOffIcon

@Composable
fun CountedUpvoteButton(mainEvent: MainEvent, onUpdate: OnUpdate) {
    CountedIconButton(
        count = mainEvent.upvoteCount,
        icon = if (mainEvent.isUpvoted) UpvoteIcon else UpvoteOffIcon,
        description = if (mainEvent.isUpvoted) {
            stringResource(id = R.string.remove_upvote)
        } else {
            stringResource(id = R.string.upvote)
        },
        onClick = {
            if (mainEvent.isUpvoted) {
                onUpdate(
                    ClickNeutralizeVote(
                        postId = mainEvent.getRelevantId(),
                        mention = mainEvent.getRelevantPubkey()
                    )
                )
            } else {
                onUpdate(
                    ClickUpvote(
                        postId = mainEvent.getRelevantId(),
                        mention = mainEvent.getRelevantPubkey()
                    )
                )
            }
        },
    )
}
