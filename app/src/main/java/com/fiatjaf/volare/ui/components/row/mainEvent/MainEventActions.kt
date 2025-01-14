package com.fiatjaf.volare.ui.components.row.mainEvent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.MaterialTheme
import com.fiatjaf.volare.ui.theme.extendedColors
import com.fiatjaf.volare.core.model.Comment
import com.fiatjaf.volare.core.model.CrossPost
import com.fiatjaf.volare.core.model.LegacyReply
import com.fiatjaf.volare.core.model.MainEvent
import com.fiatjaf.volare.core.model.Poll
import com.fiatjaf.volare.core.model.RootPost
import com.fiatjaf.volare.ui.components.button.footer.BookmarkIconButton
import com.fiatjaf.volare.ui.components.button.footer.CountedUpvoteButton
import com.fiatjaf.volare.ui.components.button.footer.CrossPostIconButton
import com.fiatjaf.volare.ui.theme.spacing

@Composable
fun MainEventActions(
    mainEvent: MainEvent,
    onUpdate: (UIEvent) -> Unit,
    additionalStartAction:  () -> Unit = {},
    additionalEndAction:  () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = spacing.large),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            additionalStartAction()
        }
        Spacer(modifier = Modifier.width(spacing.tiny))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (mainEvent.isBookmarked) {
                BookmarkIconButton(relevantId = mainEvent.getRelevantId(), onUpdate = onUpdate)
                Spacer(modifier = Modifier.width(spacing.large))
            }
            when (mainEvent) {
                is Poll -> {}
                is CrossPost,
                is RootPost,
                is Comment,
                is LegacyReply -> {
                    CrossPostIconButton(relevantId = mainEvent.getRelevantId(), onUpdate = onUpdate)
                    Spacer(modifier = Modifier.width(spacing.large))
                }
            }
            additionalEndAction()
            Spacer(modifier = Modifier.width(spacing.large))
            MaterialTheme.extendedColors.opLabel
            CountedUpvoteButton(mainEvent = mainEvent, onUpdate = onUpdate)
        }
    }
}
