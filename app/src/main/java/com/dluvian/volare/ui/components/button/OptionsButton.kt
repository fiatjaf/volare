package com.dluvian.volare.ui.components.button

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.model.MainEvent
import com.dluvian.volare.ui.components.button.footer.FooterIconButton
import com.dluvian.volare.ui.components.dropdown.FeedItemDropdown
import com.dluvian.volare.ui.theme.OnBgLight
import com.dluvian.volare.ui.theme.VertMoreIcon

@Composable
fun OptionsButton(
    mainEvent: MainEvent,
    onUpdate: OnUpdate,
) {
    val showMenu = remember { mutableStateOf(false) }
    Box(contentAlignment = Alignment.CenterEnd) {
        FeedItemDropdown(
            isOpen = showMenu.value,
            mainEvent = mainEvent,
            onDismiss = { showMenu.value = false },
            onUpdate = onUpdate,
        )
        FooterIconButton(
            icon = VertMoreIcon,
            description = stringResource(id = R.string.show_options_menu),
            color = OnBgLight,
            onClick = { showMenu.value = !showMenu.value })
    }
}
