package com.fiatjaf.volare.ui.components.text

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fiatjaf.volare.core.Fn
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.OpenTopic
import com.fiatjaf.volare.core.Topic

@Composable
fun ClickableTopic(topic: Topic, onUpdate: OnUpdate, onClickAddition: Fn = {}) {
    Text(
        modifier = Modifier.clickable {
            onUpdate(OpenTopic(topic = topic))
            onClickAddition()
        },
        text = "#$topic"
    )
}
