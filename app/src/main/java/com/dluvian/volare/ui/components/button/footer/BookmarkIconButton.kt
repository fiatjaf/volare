package com.dluvian.volare.ui.components.button.footer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.EventIdHex
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.UnbookmarkPost
import com.dluvian.volare.ui.theme.BookmarkIcon

@Composable
fun BookmarkIconButton(relevantId: EventIdHex, onUpdate: OnUpdate) {
    FooterIconButton(
        icon = BookmarkIcon,
        description = stringResource(id = R.string.remove_bookmark),
        onClick = { onUpdate(UnbookmarkPost(postId = relevantId)) })
}
