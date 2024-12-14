package com.dluvian.volare.ui.components.button.footer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.OpenReplyCreation
import com.dluvian.volare.ui.components.row.mainEvent.MainEventCtx
import com.dluvian.volare.ui.theme.CommentIcon

@Composable
fun CountedCommentButton(ctx: MainEventCtx, modifier: Modifier = Modifier, onUpdate: OnUpdate) {
    CountedIconButton(
        modifier = modifier,
        count = ctx.mainEvent.replyCount,
        icon = CommentIcon,
        description = stringResource(id = R.string.comment),
        onClick = { onUpdate(OpenReplyCreation(parent = ctx.mainEvent)) },
    )
}
