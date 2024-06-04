package com.dluvian.voyage.ui.components.chip

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.dluvian.voyage.ui.components.button.FollowButton
import com.dluvian.voyage.ui.model.Followable
import com.dluvian.voyage.ui.theme.spacing

@Composable
fun FollowChip(item: Followable) {
    Row(
        modifier = Modifier
            .padding(spacing.medium)
            .clip(ButtonDefaults.outlinedShape)
            .clickable(onClick = item.onOpen)
            .border(
                width = 1.dp,
                shape = ButtonDefaults.outlinedShape,
                color = MaterialTheme.colorScheme.onBackground
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        item.icon()
        Text(
            modifier = Modifier.padding(horizontal = spacing.large),
            text = item.label,
            style = MaterialTheme.typography.labelLarge
        )
        FollowButton(
            isFollowed = item.isFollowed,
            onFollow = item.onFollow,
            onUnfollow = item.onUnfollow
        )
    }
}
