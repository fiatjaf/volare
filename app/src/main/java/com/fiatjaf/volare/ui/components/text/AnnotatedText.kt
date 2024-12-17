package com.fiatjaf.volare.ui.components.text

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.asPainter
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.fiatjaf.volare.ui.components.video.VideoPlayer
import com.fiatjaf.volare.ui.theme.VideoIcon
import com.fiatjaf.volare.data.provider.TextItem
import com.fiatjaf.volare.core.utils.BlurHash

val TAG = "AnnotatedText"

@Composable
fun AnnotatedText(
    items: List<TextItem>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    maxLines: Int = Int.MAX_VALUE,
) {
    // TODO: handle maxlines somehow

    val gradientBrush = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background)
    )
    val textStyle = style.copy(color = MaterialTheme.colorScheme.onSurface)

    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { item ->
            when (item) {
                is TextItem.Paragraph ->
                    Spacer(modifier = Modifier.height(5.dp))
                is TextItem.AString ->
                    Text(
                        modifier = modifier,
                        text = item.value,
                        maxLines = maxLines,
                        overflow = TextOverflow.Ellipsis,
                        style = textStyle,
                    )
                is TextItem.ImageURL ->
                    ImageRow(
                        item = item,
                        gradientBrush = gradientBrush,
                        textStyle = textStyle
                    )
                is TextItem.VideoURL ->
                    VideoRow(
                        item = item,
                        gradientBrush = gradientBrush,
                        textStyle = textStyle
                    )
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
    gradientBrush: Brush,
    textStyle: TextStyle,
) {
    val (isSelected, setSelected) = remember { mutableStateOf(false) }
    val (smallBlurhashPainter, setSmallBlurhashPainter) = remember {
        mutableStateOf(
            BitmapPainter(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
            )
        )
    }
    val (bigBlurhashPainter, setBigBlurhashPainter) = remember {
        mutableStateOf(
            BitmapPainter(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
            )
        )
    }
    val (coilImage, setCoilImage) = remember { mutableStateOf<coil3.Image?>(null) }

    val context = LocalContext.current
    val imageLoader = context.imageLoader

    LaunchedEffect(Unit) {
        // on startup only load image from cache, if it's not available we will fall back to
        // blurhash (this is done dynamically when after rendering because we must know the
        // width/height first)
        imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(item.value.text)
                .networkCachePolicy(CachePolicy.DISABLED)
                .target(onSuccess = { setCoilImage(it) })
                .build()
        )
    }

    // only when the user clicks we actually try to load from network
    fun actuallyLoadImage() {
        imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(item.value.text)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .target(onSuccess = { setCoilImage(it) })
                .build()
        )
    }

    fun paintBlurhash (x: Int, y: Int): BitmapPainter {
        return BitmapPainter(
            ( BlurHash.decode(item.blurhash.blurhash, x, y)
                ?: Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888)
            ).asImageBitmap()
        )
    }

    if (!isSelected) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clickable {
                    actuallyLoadImage()
                    setSelected(true)
                }
                .fillMaxWidth()
                .height(21.dp)
                .background(gradientBrush, shape = RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, topStart = 4.dp))
            ) {
                Image(
                    contentDescription = null,
                    painter = coilImage?.asPainter(context) ?: smallBlurhashPainter,
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .onPlaced { coords ->
                            if (coilImage == null) {
                                setSmallBlurhashPainter(paintBlurhash(coords.size.width, coords.size.height))
                            }
                        },
                    contentScale = ContentScale.Crop
                )
            }
            Text(text = item.short, style = textStyle)
        }
    } else {
        Column(
            modifier = Modifier
                .clickable{ setSelected(false) }
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .background(gradientBrush),
        ) {
            Box(
                modifier = Modifier
                    .heightIn(max = (LocalConfiguration.current.screenHeightDp * 2).dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
            ) {
                Image(
                    contentDescription = null,
                    painter = coilImage?.asPainter(context) ?: bigBlurhashPainter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPlaced { coords ->
                            if (coilImage == null) {
                                val width = coords.size.width
                                val impliedHeight = ((item.blurhash.dim?.let { (x, y) -> y.toDouble() / x.toDouble() } ?: 0.78) * width).toInt()
                                setBigBlurhashPainter(paintBlurhash(width, impliedHeight))
                            }
                        }
                )
            }
            Text(
                text = item.value,
                style = textStyle.copy(fontSize = 12.sp /* font is 20% smaller */),
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}

@Composable
fun VideoRow(
    item: TextItem.VideoURL,
    gradientBrush: Brush,
    textStyle: TextStyle,
) {
    val (isSelected, setSelected) = remember { mutableStateOf(false) }

    if (!isSelected) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .clickable { setSelected(true) }
                .fillMaxWidth()
                .height(21.dp)
                .background(gradientBrush, shape = RoundedCornerShape(4.dp))
        ) {
            Icon(
                imageVector = VideoIcon,
                contentDescription = null,
            )
            Text(text = item.short, style = textStyle)
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .background(gradientBrush)
        ) {
            VideoPlayer(
                url = item.value.text,
                modifier = Modifier
            )
            Text(
                text = item.value,
                style = textStyle.copy(fontSize = 12.sp /* font is 20% smaller */),
                modifier = Modifier.padding(10.dp),
            )
        }
    }
}
