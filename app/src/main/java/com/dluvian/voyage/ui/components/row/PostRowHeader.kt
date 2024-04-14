package com.dluvian.voyage.ui.components.row

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.dluvian.nostr_kt.createNprofile
import com.dluvian.voyage.core.EventIdHex
import com.dluvian.voyage.core.Fn
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.OpenProfile
import com.dluvian.voyage.core.OpenTopic
import com.dluvian.voyage.core.PubkeyHex
import com.dluvian.voyage.core.ThreadViewToggleCollapse
import com.dluvian.voyage.core.model.TrustType
import com.dluvian.voyage.ui.components.chip.TopicChip
import com.dluvian.voyage.ui.components.chip.TrustChip
import com.dluvian.voyage.ui.components.icon.TrustIcon
import com.dluvian.voyage.ui.components.text.AnnotatedText
import com.dluvian.voyage.ui.components.text.RelativeTime
import com.dluvian.voyage.ui.theme.spacing

@Composable
fun PostRowHeader(
    trustType: TrustType,
    authorName: String,
    pubkey: PubkeyHex,
    isDetailed: Boolean,
    createdAt: Long,
    myTopic: String?,
    id: EventIdHex,
    isOp: Boolean,
    collapsedText: AnnotatedString? = null,
    onUpdate: OnUpdate
) {
    val onOpenProfile = { onUpdate(OpenProfile(nprofile = createNprofile(hex = pubkey))) }

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (isDetailed) TrustChip(
            trustType = trustType,
            name = authorName,
            isOp = isOp,
            onOpenProfile = onOpenProfile
        ) else ClickableTrustIcon(trustType = trustType, onClick = onOpenProfile)
        myTopic?.let { topic ->
            TopicChip(
                modifier = Modifier
                    .weight(weight = 1f, fill = false)
                    .padding(start = spacing.large),
                topic = topic,
                onClick = { onUpdate(OpenTopic(topic = topic)) },
            )
        }
        Spacer(modifier = Modifier.width(spacing.large))
        if (collapsedText == null) RelativeTime(from = createdAt)
        else AnnotatedText(
            text = collapsedText,
            maxLines = 1,
            onClick = { onUpdate(ThreadViewToggleCollapse(id = id)) }
        )
    }
}

@Composable
fun ClickableTrustIcon(trustType: TrustType, onClick: Fn) {
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        TrustIcon(trustType = trustType)
    }
}