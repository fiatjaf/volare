package com.fiatjaf.volare.ui.components.bar

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun SimpleGoBackTopAppBar(
    title: String? = null,
    actions:  RowScope.() -> Unit = {},
    onUpdate: (UIEvent) -> Unit
) {
    GoBackTopAppBar(
        title = { title?.let { Text(text = it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
        actions = actions,
        onUpdate = onUpdate
    )
}
