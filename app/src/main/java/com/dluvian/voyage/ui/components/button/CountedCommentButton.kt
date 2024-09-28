package com.dluvian.voyage.ui.components.button

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dluvian.voyage.R
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.OpenReplyCreation
import com.dluvian.voyage.ui.components.row.mainEvent.MainEventCtx
import com.dluvian.voyage.ui.theme.CommentIcon

@Composable
fun CountedCommentButton(ctx: MainEventCtx, onUpdate: OnUpdate) {
    CountedIconButton(
        count = ctx.mainEvent.replyCount,
        icon = CommentIcon,
        description = stringResource(id = R.string.comment),
        onClick = { onUpdate(OpenReplyCreation(parent = ctx.mainEvent)) },
    )
}
