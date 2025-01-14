package com.fiatjaf.volare.ui.components.row

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fiatjaf.volare.ui.theme.AddIcon
import com.fiatjaf.volare.ui.theme.spacing

@Composable
fun AddRow(header: String, onClick: () -> Unit) {
    ClickableRow(
        header = header,
        leadingContent = {
            Icon(
                modifier = Modifier.padding(vertical = spacing.large),
                imageVector = AddIcon,
                contentDescription = null
            )
        },
        onClick = onClick
    )
}
