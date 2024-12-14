package com.dluvian.volare.ui.components.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.dluvian.volare.core.Fn
import com.dluvian.volare.ui.theme.RemoveIcon

@Composable
fun RemoveIconButton(onRemove: Fn, description: String, color: Color = LocalContentColor.current) {
    IconButton(onClick = onRemove) {
        Icon(
            imageVector = RemoveIcon,
            contentDescription = description,
            tint = color
        )
    }
}
