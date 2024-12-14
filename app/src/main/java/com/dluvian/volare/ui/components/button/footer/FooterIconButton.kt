package com.dluvian.volare.ui.components.button.footer

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.dluvian.volare.core.Fn
import com.dluvian.volare.ui.theme.sizing

@Composable
fun FooterIconButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary,
    onClick: Fn
) {
    IconButton(
        modifier = modifier.size(sizing.footerIconButton.times(1.5f)),
        colors = IconButtonDefaults.iconButtonColors(contentColor = color),
        onClick = onClick
    ) {
        Icon(
            modifier = Modifier.size(sizing.footerIconButton),
            imageVector = icon,
            contentDescription = description
        )
    }
}
