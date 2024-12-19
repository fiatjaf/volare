package com.fiatjaf.volare.ui.components.indicator

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fiatjaf.volare.ui.theme.Sky600
import com.fiatjaf.volare.ui.theme.Red600

@Composable
fun ComingSoon() {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val animatedColor by infiniteTransition.animateColor(
        initialValue = Red600,
        targetValue = Sky600,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "color"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.headlineLarge,
            text = "Coming soon", color = animatedColor
        )
    }
}
