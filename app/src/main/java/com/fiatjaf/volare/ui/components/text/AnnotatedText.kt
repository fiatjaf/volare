package com.fiatjaf.volare.ui.components.text

import kotlin.math.abs
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import coil3.compose.asPainter
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import com.fiatjaf.volare.ui.components.video.VideoPlayer
import com.fiatjaf.volare.ui.theme.VideoIcon
import com.fiatjaf.volare.data.provider.TextItem
import com.fiatjaf.volare.core.utils.BlurHash
import com.fiatjaf.volare.ui.theme.extendedColors

private const val TAG = "AnnotatedText"
private const val previewRowHeight = 34

@Composable
fun AnnotatedText(
    items: List<TextItem>,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    preload: Boolean = false,
    maxLines: Int = Int.MAX_VALUE,
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.background)
    )
    val textStyle = style.copy(color = MaterialTheme.extendedColors.text)
    val addedHeight = remember { mutableStateOf(0) }
    var lastWasSpacer = remember { mutableStateOf(true) } // Start with true, so if a media is the first item, skip the spacer
    var realHeight by remember { mutableStateOf(0) }
    var maxHeight = (addedHeight.value + (maxLines * style.lineHeight.value)).dp
    val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

    Box(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
            .onGloballyPositioned { layoutCoordinates ->
                realHeight = layoutCoordinates.size.height // Warning: height is calculated after the cropping done by the following command
            }
            .heightIn(max = maxHeight)
            .padding(top = 0.dp, bottom = 0.dp)
        ) {
            items.forEach { item ->
                when (item) {
                    is TextItem.AString -> {
                        Text(
                            modifier = modifier,
                            text = item.value,
                            overflow = TextOverflow.Ellipsis,
                            style = textStyle,
                        )
                        lastWasSpacer.value = false
                    }
                    is TextItem.ImageURL -> {
                        if (!lastWasSpacer.value) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        ImageRow(
                            item = item,
                            gradientBrush = gradientBrush,
                            textStyle = textStyle,
                            preload = preload,
                            addedHeight = addedHeight,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        lastWasSpacer.value = true
                    }
                    is TextItem.VideoURL -> {
                        if (!lastWasSpacer.value) {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        VideoRow(
                            item = item,
                            gradientBrush = gradientBrush,
                            textStyle = textStyle
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        lastWasSpacer.value = true
                    }
                }
            }
            lastWasSpacer.value = true
        }

        // Ugly hack: since realHeight is the eventually cropped height,
        // compare it with max height to check if the box have been cropped
        // and add a +/-30px rounding, since the truncation is not px precise
        if (abs(realHeight.toFloat() - maxHeightPx) < 30) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Transparent,
                                0.3f to MaterialTheme.colorScheme.background.copy(alpha = 0.7f),
                                1.0f to MaterialTheme.colorScheme.background
                            ),
                            startY = 0f,
                            endY = 100f
                        )
                    )
            )
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
    Column(
        modifier = Modifier.heightIn(max = (maxLines * style.lineHeight.value).dp)
    ) {
        Text(
            modifier = modifier,
            text = text,
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
    preload: Boolean,
    addedHeight: MutableState<Int>,
) {
    val (isSelected, setSelected) = remember { mutableStateOf(false) }
    val (bigBlurhashPainter, setBigBlurhashPainter) = remember {
        mutableStateOf(
            BitmapPainter(
                Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).asImageBitmap()
            )
        )
    }
    val (coilImage, setCoilImage) = remember { mutableStateOf<coil3.Image?>(null) }
    val (imageFailed, setImageFailed) = remember { mutableStateOf(false) }
    val (onPlacedCalled, setOnPlacedCalled) = remember { mutableStateOf(false) }

    val context = LocalContext.current
    val imageLoader = context.imageLoader

    fun paintBlurhash (x: Int, y: Int): BitmapPainter {
        return BitmapPainter(
            ( BlurHash.decode(item.blurhash.blurhash, x, y)
                ?: Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888)
            ).asImageBitmap()
        )
    }

    val smallBlurhashPainter = remember { paintBlurhash(previewRowHeight, previewRowHeight) }

    LaunchedEffect(preload) {
        // on startup only load image from cache, if it's not available we will fall back to
        // blurhash (this is done dynamically when after rendering because we must know the
        // width/height first)
        // well -- now we preload from the network too if `preload` is true
        if (coilImage == null) {
            imageLoader.enqueue(
                ImageRequest.Builder(context)
                    .data(item.value.text)
                    .networkCachePolicy(if (preload) CachePolicy.ENABLED else CachePolicy.DISABLED)
                    .target(onSuccess = { setCoilImage(it) })
                    .build()
            )
        }
    }

    // only when the user clicks we actually try to load from network
    fun actuallyLoadImage() {
        // if this image was already loaded or failed we don't try again
        if (coilImage != null || imageFailed) return

        imageLoader.enqueue(
            ImageRequest.Builder(context)
                .data(item.value.text)
                .memoryCachePolicy(CachePolicy.DISABLED)
                .diskCachePolicy(CachePolicy.DISABLED)
                .target(onSuccess = { setCoilImage(it) }, onError = { setImageFailed(true) })
                .build()
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

                    if (coilImage != null) {
                        addedHeight.value += coilImage.height
                    } else if (onPlacedCalled) {
                        addedHeight.value += bigBlurhashPainter.intrinsicSize.height.toInt()
                    }
                }
                .fillMaxWidth()
                .height(previewRowHeight.dp)
                .background(gradientBrush, shape = RoundedCornerShape(4.dp))
                .padding(top = 0.dp, bottom = 0.dp)
        ) {
            Image(
                contentDescription = null,
                painter = coilImage?.asPainter(context) ?: smallBlurhashPainter,
                modifier = Modifier
                    .height(previewRowHeight.dp)
                    .width(previewRowHeight.dp)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, topStart = 4.dp)),

                contentScale = ContentScale.Crop
            )
            Text(text = item.short, style = textStyle)
        }
    } else {
        Column(
            modifier = Modifier
                .clickable {
                    setSelected(false)
                    addedHeight.value -= (coilImage?.height ?: bigBlurhashPainter.intrinsicSize.height.toInt())
                }
                .fillMaxWidth()
                .background(gradientBrush, shape = RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .onPlaced { coords ->
                        if (onPlacedCalled) return@onPlaced
                        setOnPlacedCalled(true)
                    }
            ) {
                Image(
                    contentDescription = null,
                    painter = coilImage?.asPainter(context) ?: bigBlurhashPainter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { modifier ->
                            if (coilImage != null) {
                                // If image is available, calculate aspect ratio using width and height
                                modifier.aspectRatio(coilImage.width.toFloat() / coilImage.height.toFloat())
                            } else {
                                // Aspect ratio will be calculated after the layout
                                modifier
                            }
                        }
                        .onPlaced { coords ->
                            // Only when the Box is placed, we can use coords to calculate the aspect ratio
                            if (coilImage == null) {
                                val width = coords.size.width
                                val impliedHeight =
                                    ((item.blurhash.dim?.let { (x, y) -> y.toDouble() / x.toDouble() }
                                        ?: 0.78) * width).toInt()
                                setBigBlurhashPainter(paintBlurhash(width, impliedHeight))
                                addedHeight.value += impliedHeight
                            }
                        }
                        .clip(RoundedCornerShape(4.dp))
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
                .height(previewRowHeight.dp)
                .background(gradientBrush, shape = RoundedCornerShape(4.dp))
                .padding(top = 0.dp, bottom = 0.dp)
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
