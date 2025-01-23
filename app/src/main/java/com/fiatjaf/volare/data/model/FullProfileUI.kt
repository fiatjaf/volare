package com.fiatjaf.volare.data.model

import androidx.compose.ui.text.AnnotatedString

data class FullProfileUI(
    val inner: backend.Profile,
    val about: AnnotatedString? = null,
    val lightning: String? = null,
)
