package com.fiatjaf.volare.ui.components.row.mainEvent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.OpenProfile
import com.fiatjaf.volare.core.OpenTopic
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.core.Topic
import com.fiatjaf.volare.core.model.Comment
import com.fiatjaf.volare.core.model.CrossPost
import com.fiatjaf.volare.core.model.LegacyReply
import com.fiatjaf.volare.core.model.Poll
import com.fiatjaf.volare.core.model.RootPost
import com.fiatjaf.volare.core.utils.toShortenedBech32
import com.fiatjaf.volare.data.nostr.createNprofile
import com.fiatjaf.volare.ui.components.button.OptionsButton
import com.fiatjaf.volare.ui.components.icon.ClickableTrustIcon
import com.fiatjaf.volare.ui.components.text.AnnotatedText
import com.fiatjaf.volare.ui.components.text.RelativeTime
import com.fiatjaf.volare.ui.theme.CrossPostIcon
import com.fiatjaf.volare.ui.theme.Sky600
import com.fiatjaf.volare.ui.theme.OnBgLight
import com.fiatjaf.volare.ui.theme.light
import com.fiatjaf.volare.ui.theme.sizing
import com.fiatjaf.volare.ui.theme.spacing

@Composable
fun MainEventHeader(
    ctx: MainEventCtx,
    onUpdate: OnUpdate
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainEventHeaderIconsAndName(ctx = ctx, onUpdate = onUpdate)
            if (ctx.isCollapsedReply()) AnnotatedText(
                items = ctx.mainEvent.content,
                modifier = Modifier.padding(start = spacing.large),
                maxLines = 1
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            when (val mainEvent = ctx.mainEvent) {
                is RootPost -> mainEvent.myTopic
                is CrossPost -> mainEvent.myTopic
                is Poll -> mainEvent.myTopic
                is LegacyReply -> null
                is Comment -> null
            }?.let { topic ->
                BorderedTopic(topic = topic, onUpdate = onUpdate)
                Spacer(modifier = Modifier.width(spacing.large))
            }
            if (!ctx.isCollapsedReply()) RelativeTime(from = ctx.mainEvent.createdAt)
            OptionsButton(mainEvent = ctx.mainEvent, onUpdate = onUpdate)
        }
    }
}

@Composable
private fun MainEventHeaderIconsAndName(
    ctx: MainEventCtx,
    onUpdate: OnUpdate
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (val mainEvent = ctx.mainEvent) {
            is CrossPost -> CrossPostIcon(
                crossPost = mainEvent,
                onUpdate = onUpdate
            )

            is LegacyReply, is RootPost, is Comment, is Poll -> {}
        }
        ClickableTrustIcon(
            trustType = ctx.mainEvent.trustType,
            isOp = when (ctx) {
                is FeedCtx -> false
                is ThreadRootCtx -> true
                is ThreadReplyCtx -> ctx.isOp
            },
            authorName = getUiAuthorName(
                name = ctx.mainEvent.authorName,
                pubkey = ctx.mainEvent.pubkey
            ),
            onClick = {
                onUpdate(OpenProfile(nprofile = createNprofile(hex = ctx.mainEvent.pubkey)))
            }
        )
    }
}

@Composable
private fun CrossPostIcon(crossPost: CrossPost, onUpdate: OnUpdate) {
    ClickableTrustIcon(
        trustType = crossPost.crossPostedTrustType,
        authorName = getUiAuthorName(
            name = crossPost.crossPostedAuthorName,
            pubkey = crossPost.crossPostedPubkey
        ),
        onClick = {
            onUpdate(OpenProfile(nprofile = createNprofile(hex = crossPost.crossPostedPubkey)))
        }
    )
    Icon(
        modifier = Modifier
            .size(sizing.smallIndicator)
            .padding(horizontal = spacing.small),
        imageVector = CrossPostIcon,
        contentDescription = stringResource(id = R.string.cross_posted),
        tint = MaterialTheme.colorScheme.onBackground.light(0.6f)
    )
}

@Composable
private fun BorderedTopic(topic: Topic, onUpdate: OnUpdate) {
    Box(
        modifier = Modifier
            .border(
                border = BorderStroke(width = 1.dp, color = Sky600),
                shape = RoundedCornerShape(corner = CornerSize(spacing.medium))
            )
            .clickable(onClick = { onUpdate(OpenTopic(topic = topic)) }),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = spacing.large),
            text = "#$topic",
            fontSize = 12.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            color = OnBgLight,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Stable
@Composable
private fun getUiAuthorName(name: String?, pubkey: PubkeyHex): String {
    return name.orEmpty().ifEmpty { pubkey.toShortenedBech32() }
}
