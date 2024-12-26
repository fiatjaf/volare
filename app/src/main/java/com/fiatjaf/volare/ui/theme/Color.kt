package com.fiatjaf.volare.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.fiatjaf.volare.core.model.FriendTrust
import com.fiatjaf.volare.core.model.IsInListTrust
import com.fiatjaf.volare.core.model.Muted
import com.fiatjaf.volare.core.model.NoTrust
import com.fiatjaf.volare.core.model.Oneself
import com.fiatjaf.volare.core.model.TrustType
import com.fiatjaf.volare.core.model.WebTrust

val md_theme_light_primary = Color(0xFF3B4DD8)
val md_theme_light_onPrimary = Color(0xFFFFFFFF)
val md_theme_light_primaryContainer = Color(0xFFDFE0FF)
val md_theme_light_onPrimaryContainer = Color(0xFF000B62)
val md_theme_light_secondary = Color(0xFFAB351F)
val md_theme_light_onSecondary = Color(0xFFFFFFFF)
val md_theme_light_secondaryContainer = Color(0xFFFFDAD3)
val md_theme_light_onSecondaryContainer = Color(0xFF3E0400)
val md_theme_light_tertiary = Color(0xFF4756B4)
val md_theme_light_onTertiary = Color(0xFFFFFFFF)
val md_theme_light_tertiaryContainer = Color(0xFFDEE0FF)
val md_theme_light_onTertiaryContainer = Color(0xFF000E5E)
val md_theme_light_error = Color(0xFFBA1A1A)
val md_theme_light_errorContainer = Color(0xFFFFDAD6)
val md_theme_light_onError = Color(0xFFFFFFFF)
val md_theme_light_onErrorContainer = Color(0xFF410002)
val md_theme_light_background = Color(0xFFFFFBFF)
val md_theme_light_onBackground = Color(0xFF1B1B1F)
val md_theme_light_surface = Color(0xFFFFFBFF)
val md_theme_light_onSurface = Color(0xFF1B1B1F)
val md_theme_light_surfaceVariant = Color(0xFFE3E1EC)
val md_theme_light_onSurfaceVariant = Color(0xFF46464F)
val md_theme_light_outline = Color(0xFF777680)
val md_theme_light_inverseOnSurface = Color(0xFFF3F0F4)
val md_theme_light_inverseSurface = Color(0xFF303034)
val md_theme_light_inversePrimary = Color(0xFFBCC2FF)
val md_theme_light_surfaceTint = Color(0xFF3B4DD8)
val md_theme_light_outlineVariant = Color(0xFFC7C5D0)
val md_theme_light_scrim = Color(0xFF000000)

val md_theme_dark_primary = Color(0xFFBCC2FF)
val md_theme_dark_onPrimary = Color(0xFF00179B)
val md_theme_dark_primaryContainer = Color(0xFF1C31C0)
val md_theme_dark_onPrimaryContainer = Color(0xFFDFE0FF)
val md_theme_dark_secondary = Color(0xFFFFB4A5)
val md_theme_dark_onSecondary = Color(0xFF650B00)
val md_theme_dark_secondaryContainer = Color(0xFF891D09)
val md_theme_dark_onSecondaryContainer = Color(0xFFFFDAD3)
val md_theme_dark_tertiary = Color(0xFFBBC3FF)
val md_theme_dark_onTertiary = Color(0xFF112384)
val md_theme_dark_tertiaryContainer = Color(0xFF2D3D9B)
val md_theme_dark_onTertiaryContainer = Color(0xFFDEE0FF)
val md_theme_dark_error = Color(0xFFFFB4AB)
val md_theme_dark_errorContainer = Color(0xFF93000A)
val md_theme_dark_onError = Color(0xFF690005)
val md_theme_dark_onErrorContainer = Color(0xFFFFDAD6)
val md_theme_dark_background = Color(0xFF1B1B1F)
val md_theme_dark_onBackground = Color(0xFFE4E1E6)
val md_theme_dark_surface = Color(0xFF1B1B1F)
val md_theme_dark_onSurface = Color(0xFFE4E1E6)
val md_theme_dark_surfaceVariant = Color(0xFF46464F)
val md_theme_dark_onSurfaceVariant = Color(0xFFC7C5D0)
val md_theme_dark_outline = Color(0xFF91909A)
val md_theme_dark_inverseOnSurface = Color(0xFF1B1B1F)
val md_theme_dark_inverseSurface = Color(0xFFE4E1E6)
val md_theme_dark_inversePrimary = Color(0xFF3B4DD8)
val md_theme_dark_surfaceTint = Color(0xFFBCC2FF)
val md_theme_dark_outlineVariant = Color(0xFF46464F)
val md_theme_dark_scrim = Color(0xFF000000)

fun Color.light(factor: Float = 0.5f) = this.copy(alpha = this.alpha * factor)

val Neutral100 = Color(0XFFF5F5F5)
val Neutral300 = Color(0XFFD4D4D4)
val Neutral400 = Color(0XFFA3A3A3)
val Neutral500 = Color(0XFF737373)
val Neutral800 = Color(0XFF262626)
val Red400 = Color(0xFFF87171)
val Red600 = Color(0xFFDC2626)
val Amber200 = Color(0XFFFDE68A)
val Stone200 = Color(0XFFE7E5E4)
val Stone400 = Color(0XFFA3A3A3)
val Sky300 = Color(0XFF7DD3FC)
val Sky400 = Color(0XFF38BDF8)
val Sky500 = Color(0XFF0EA5E9)
val Sky600 = Color(0XFF0284C7)
val Pink300 = Color(0XFFF9A8D4)
val Pink400 = Color(0XFFF472B6)
val Pink500 = Color(0XFFEC4899)
val Lime500 = Color(0XFF84CC16)
val Lime600 = Color(0XFF65A30D)

val OnBgLight: Color
    @Composable
    get() = MaterialTheme.colorScheme.onBackground.light()

@Stable
@Composable
fun getTrustColor(trustType: TrustType): Color {
    return when (trustType) {
        Oneself -> Stone400
        FriendTrust, IsInListTrust -> Stone400
        WebTrust -> Amber200
        Muted -> Red400
        NoTrust -> Stone200
    }
}
