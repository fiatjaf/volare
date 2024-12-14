package com.dluvian.volare.ui.components.button.footer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.OpenReplyCreation
import com.dluvian.volare.ui.components.row.mainEvent.MainEventCtx
import com.dluvian.volare.ui.theme.ReplyIcon

@Composable
fun ReplyIconButton(ctx: MainEventCtx, modifier: Modifier = Modifier, onUpdate: OnUpdate) {
    FooterIconButton(
        modifier = modifier,
        icon = ReplyIcon,
        description = stringResource(id = R.string.reply),
        onClick = { onUpdate(OpenReplyCreation(parent = ctx.mainEvent)) })
}
