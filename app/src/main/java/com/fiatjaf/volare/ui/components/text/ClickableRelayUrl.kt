package com.fiatjaf.volare.ui.components.text

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fiatjaf.volare.core.OpenRelayProfile

@Composable
fun ClickableRelayUrl(relayUrl: String, onUpdate: (UIEvent) -> Unit, onClickAddition: () -> Unit = {}) {
    Text(
        modifier = Modifier.clickable {
            onUpdate(OpenRelayProfile(relayUrl = relayUrl))
            onClickAddition()
        },
        text = relayUrl
    )
}
