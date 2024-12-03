package com.dluvian.voyage.ui.components.text

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
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

    val gradientBrush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background))
    var (bigImage, setBigImage) = remember { mutableStateOf(-1) }

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEachIndexed { index, item ->
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
                    if (bigImage != index) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .clickable {
                                    setBigImage(index)
                                }
                                .fillMaxWidth()
                                .height(21.dp)
                                .background(gradientBrush)
                        ) {
                            AsyncImage(model = item.value.text, contentDescription = null)
                            Text(text = item.short, style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp))
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .clickable {
                                    setBigImage(-1)
                                }
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .background(gradientBrush)
                        ) {
                            AsyncImage(model = item.value.text, contentDescription = null)
                            Text(text = item.value, style = TextStyle(fontSize = 16.sp))
                        }
                    }
                is TextItem.VideoURL ->
                    AsyncImage(model = item.value.text, contentDescription = null)
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
