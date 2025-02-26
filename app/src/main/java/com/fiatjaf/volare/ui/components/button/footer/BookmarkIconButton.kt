package com.fiatjaf.volare.ui.components.button.footer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.UnbookmarkPost
import com.fiatjaf.volare.ui.theme.BookmarkIcon

@Composable
fun BookmarkIconButton(relevantId: EventIdHex, onUpdate: OnUpdate) {
    FooterIconButton(
        icon = BookmarkIcon,
        description = stringResource(id = R.string.remove_bookmark),
        onClick = { onUpdate(UnbookmarkPost(postId = relevantId)) })
}
