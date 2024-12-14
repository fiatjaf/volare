package com.dluvian.volare.ui.components.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SmallHeader(header: String, modifier: Modifier = Modifier) {
    Text(modifier = modifier, text = header, style = MaterialTheme.typography.titleMedium)
}
