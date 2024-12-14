package com.dluvian.volare.ui.components.row

import androidx.compose.runtime.Composable
import com.dluvian.volare.core.ComposableContent
import com.dluvian.volare.core.Fn
import com.dluvian.volare.core.model.TrustType
import com.dluvian.volare.ui.components.icon.TrustIcon

@Composable
fun ClickableTrustIconRow(
    trustType: TrustType,
    header: String? = null,
    content: String? = null,
    trailingContent: ComposableContent = {},
    onClick: Fn,
) {
    ClickableRow(
        header = header.orEmpty(),
        text = content,
        leadingContent = { TrustIcon(trustType = trustType) },
        trailingContent = trailingContent,
        onClick = onClick
    )
}
