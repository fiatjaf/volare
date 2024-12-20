package com.fiatjaf.volare.ui.components.icon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import com.fiatjaf.volare.core.model.FriendTrust
import com.fiatjaf.volare.core.model.IsInListTrust
import com.fiatjaf.volare.core.model.Muted
import com.fiatjaf.volare.core.model.NoTrust
import com.fiatjaf.volare.core.model.Oneself
import com.fiatjaf.volare.core.model.TrustType
import com.fiatjaf.volare.core.model.WebTrust
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import com.fiatjaf.volare.ui.theme.getTrustColor
import com.fiatjaf.volare.ui.theme.sizing

private const val X_RATIO = 0.45f

@Stable
@Composable
fun TrustIcon(trustType: TrustType, size: Dp = sizing.trustIndicator) {
    val color = getTrustColor(trustType = trustType)
    when (trustType) {
        FriendTrust, WebTrust, NoTrust, Muted -> TrustBox(size = size, color = color)
        IsInListTrust -> ListTrustBox(size = size, color = color)
        Oneself -> {
            /* nothing for oneself */
        }
    }
}

@Stable
@Composable
fun TrustIcon(profile: AdvancedProfileView) {
    TrustIcon(
        trustType = TrustType.from(
            isOneself = false, // TODO: get this to work again
            isFriend = profile.isFriend,
            isWebOfTrust = profile.isWebOfTrust,
            isMuted = profile.isMuted,
            isInList = profile.isInList,
        )
    )
}

@Stable
@Composable
private fun TrustBox(size: Dp, color: Color) {
    Box(
        modifier = Modifier
            .height(height = size)
            .width(width = size.times(X_RATIO))
            .background(color = color)
    )
}

@Stable
@Composable
private fun ListTrustBox(size: Dp, color: Color) {
    Column(
        modifier = Modifier
            .heightIn(
                min = size,
                max = size
            )
            .width(width = size.times(X_RATIO)),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height = size.div(5))
                    .background(color = color)
            )
        }
    }
}

@Stable
@Composable
private fun MuteTriangle(size: Dp, color: Color) {
    val xRatio = 0.7f
    Box(
        modifier = Modifier
            .drawWithCache {
                onDrawBehind {
                    val maxX = size
                        .toPx()
                        .times(xRatio)
                    val maxY = size.toPx()
                    drawPath(
                        path = Path().apply {
                            moveTo(maxX.div(2), 0f)
                            lineTo(maxX, maxY)
                            lineTo(0f, maxY)
                            close()
                        },
                        color = color,
                    )
                }
            }
            .height(height = size)
            .width(width = size.times(xRatio)),
    )
}
