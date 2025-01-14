package com.fiatjaf.volare.ui.components.button.footer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.ClickNeutralizeVote
import com.fiatjaf.volare.core.ClickUpvote
import com.fiatjaf.volare.core.model.MainEvent
import com.fiatjaf.volare.ui.theme.UpvoteIcon
import com.fiatjaf.volare.ui.theme.UpvoteOffIcon

@Composable
fun CountedUpvoteButton(mainEvent: MainEvent, onUpdate: (UIEvent) -> Unit) {
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
                        targetId = mainEvent.getRelevantId(),
                        mention = mainEvent.getRelevantPubkey()
                    )
                )
            } else {
                onUpdate(
                    ClickUpvote(
                        targetId = mainEvent.getRelevantId(),
                        mention = mainEvent.getRelevantPubkey()
                    )
                )
            }
        },
    )
}
