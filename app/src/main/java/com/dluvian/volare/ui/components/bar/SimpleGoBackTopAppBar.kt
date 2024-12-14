package com.dluvian.volare.ui.components.bar

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.dluvian.volare.core.ComposableRowContent
import com.dluvian.volare.core.OnUpdate

@Composable
fun SimpleGoBackTopAppBar(
    title: String? = null,
    actions: ComposableRowContent = {},
    onUpdate: OnUpdate
) {
    GoBackTopAppBar(
        title = { title?.let { Text(text = it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        actions = actions,
        onUpdate = onUpdate
    )
}
