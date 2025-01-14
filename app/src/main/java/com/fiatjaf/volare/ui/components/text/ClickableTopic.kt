package com.fiatjaf.volare.ui.components.text

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fiatjaf.volare.core.OpenTopic

@Composable
fun ClickableTopic(topic: String, onUpdate: (UIEvent) -> Unit, onClickAddition: () -> Unit = {}) {
    Text(
        modifier = Modifier.clickable {
            onUpdate(OpenTopic(topic = topic))
            onClickAddition()
        },
        text = "#$topic"
    )
}
