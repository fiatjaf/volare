package com.dluvian.voyage.ui.components.text

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import com.dluvian.voyage.data.provider.TextItem

@Composable
fun AnnotatedText(
    items: List<TextItem>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    maxLines: Int = Int.MAX_VALUE,
) {
    // TODO: handle maxlines somehow

    Column {
        items.forEach { item ->
            when (item) {
                is TextItem.AString ->
                    Text(
                        modifier = modifier,
                        text = item.value,
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis,
                        style = style.copy(color = MaterialTheme.colorScheme.onSurface),
                    )
                is TextItem.ImageURL ->
                    AsyncImage(model = item.value, contentDescription = null)
                is TextItem.VideoURL ->
                    AsyncImage(model = item.value, contentDescription = null)
            }
        }
    }
}

@Composable
fun AnnotatedText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    maxLines: Int = Int.MAX_VALUE,
) {
    Column {
        Text(
            modifier = modifier,
            text = text,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            style = style.copy(color = MaterialTheme.colorScheme.onSurface),
        )
    }
}
