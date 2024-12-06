package com.dluvian.voyage.ui.components.text

import android.util.Log
import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.dluvian.voyage.ui.components.video.VideoPlayer
import com.dluvian.voyage.ui.theme.VideoIcon
import com.dluvian.voyage.data.provider.TextItem
import com.dluvian.voyage.core.utils.BlurHash

val TAG = "AnnotatedText"

@Composable
fun AnnotatedText(
    items: List<TextItem>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    maxLines: Int = Int.MAX_VALUE,
) {
    // TODO: handle maxlines somehow

    val gradientBrush = Brush.linearGradient(colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background))
    val (bigImage, setBigImage) = remember { mutableStateOf(-1) }

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
                    ImageRow(
                        item = item,
                        isSelected = index == bigImage,
                        onSelect = { setBigImage(index) },
                        onUnselect = { setBigImage(-1) },
                        gradientBrush = gradientBrush,
                    )
                is TextItem.VideoURL ->
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
                            Icon(
                                imageVector = VideoIcon,
                                contentDescription = null,
                            )
                            Text(text = item.short, style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp))
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                // .clickable {
                                //     setBigImage(-1)
                                // }
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .background(gradientBrush)
                        ) {
                            VideoPlayer(
                                url = item.value.text,
                                modifier = Modifier
                            )
                            Text(text = item.value, style = TextStyle(fontSize = 16.sp))
                        }
                    }
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

@Composable
fun ImageRow(
    item: TextItem.ImageURL,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUnselect: () -> Unit,
    gradientBrush: Brush,
) {
    fun paintBlurhash (big: Boolean): BitmapPainter {
        val x = if (big) 360 else 36
        val y = ((item.blurhash.dim?.let { (x, y) -> y.toDouble() / x.toDouble() } ?: 0.78) * x).toInt()

        return BitmapPainter(
            ( BlurHash.decode(item.blurhash.blurhash, x, y)
                ?: Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888)
            ).asImageBitmap()
        )
    }
    val (loaded, setLoaded) = remember { mutableStateOf(false) }

    if (!isSelected) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clickable { onSelect() }
                .fillMaxWidth()
                .height(21.dp)
                .background(gradientBrush)
        ) {
            if (loaded) {
                AsyncImage(
                    model = item.value.text,
                    contentDescription = null,
                )
            } else {
                Image(
                    painter = paintBlurhash(false),
                    contentDescription = null,
                    modifier = Modifier.height(36.dp)
                )
            }
            Text(text = item.short, style = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp))
        }
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clickable{ onUnselect() }
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .background(gradientBrush)
        ) {
            AsyncImage(
                model = item.value.text,
                contentDescription = null,
                onSuccess = {
                    setLoaded(true)
                },
                placeholder = paintBlurhash(true),
                contentScale = ContentScale.FillWidth
            )
            Text(text = item.value, style = TextStyle(fontSize = 16.sp))
        }
    }
}
