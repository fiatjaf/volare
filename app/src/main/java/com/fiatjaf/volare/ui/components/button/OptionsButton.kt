package com.fiatjaf.volare.ui.components.button

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.model.MainEvent
import com.fiatjaf.volare.ui.components.button.footer.FooterIconButton
import com.fiatjaf.volare.ui.components.dropdown.FeedItemDropdown
import com.fiatjaf.volare.ui.theme.OnBgLight
import com.fiatjaf.volare.ui.theme.VertMoreIcon

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
