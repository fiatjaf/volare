package com.fiatjaf.volare.ui.components.row

import androidx.compose.runtime.Composable
import com.fiatjaf.volare.core.ComposableContent
import com.fiatjaf.volare.core.Fn
import com.fiatjaf.volare.core.model.TrustType
import com.fiatjaf.volare.ui.components.icon.TrustIcon

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
