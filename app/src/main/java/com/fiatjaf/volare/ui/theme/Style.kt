package com.fiatjaf.volare.ui.theme

import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration

// Global style functions (no @Composable needed)
val MentionStyle: SpanStyle
    get() = SpanStyle(color = AppTheme.extendedColors.mention)

val HashtagStyle: SpanStyle
    get() = SpanStyle(color = AppTheme.extendedColors.hashtag)

val UrlStyle: SpanStyle
    get() = SpanStyle(color = AppTheme.extendedColors.link, textDecoration = TextDecoration.Underline)
