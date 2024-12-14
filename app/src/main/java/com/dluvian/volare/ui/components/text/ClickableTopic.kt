package com.dluvian.volare.ui.components.text

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dluvian.volare.core.Fn
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.OpenTopic
import com.dluvian.volare.core.Topic

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
