package com.fiatjaf.volare.data.model

import androidx.compose.ui.text.AnnotatedString
import com.fiatjaf.volare.data.room.view.AdvancedProfileView

data class FullProfileUI(
    val inner: AdvancedProfileView = AdvancedProfileView(),
    val about: AnnotatedString? = null,
    val lightning: String? = null,
)
