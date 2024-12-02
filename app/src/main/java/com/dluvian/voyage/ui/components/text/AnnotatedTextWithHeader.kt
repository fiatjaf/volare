package com.dluvian.voyage.ui.components.text

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import com.dluvian.voyage.data.provider.TextItem

@Composable
fun AnnotatedTextWithHeader(
    items: List<TextItem>,
    modifier: Modifier = Modifier,
    header: String,
) {
    Column(modifier = modifier) {
        SmallHeader(header = header)
        AnnotatedText(items = items)
    }
}

@Composable
fun AnnotatedTextWithHeader(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    header: String,
) {
    Column(modifier = modifier) {
        SmallHeader(header = header)
        AnnotatedText(text = text)
    }
}
