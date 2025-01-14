package com.fiatjaf.volare.ui.components.bar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.fiatjaf.volare.core.HomeViewOpenFilter
import com.fiatjaf.volare.core.InboxViewOpenFilter
import com.fiatjaf.volare.core.navigator.DiscoverNavView
import com.fiatjaf.volare.core.navigator.HomeNavView
import com.fiatjaf.volare.core.navigator.InboxNavView
import com.fiatjaf.volare.core.navigator.MainNavView
import com.fiatjaf.volare.core.navigator.SearchNavView
import com.fiatjaf.volare.ui.components.button.FilterIconButton
import com.fiatjaf.volare.ui.components.button.MenuIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    currentView: MainNavView,
    onUpdate: (UIEvent) -> Unit = {}
) {
    TopAppBar(
        title = {
            Text(
                text = currentView.getTitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            MenuIconButton(onUpdate = onUpdate)
        },
        actions = {
            MainFilter(currentView = currentView, onUpdate = onUpdate)
        }
    )
}

@Composable
private fun MainFilter(currentView: MainNavView, onUpdate: (UIEvent) -> Unit) {
    when (currentView) {
        HomeNavView -> FilterIconButton(onClick = { onUpdate(HomeViewOpenFilter) })

        InboxNavView -> FilterIconButton(onClick = { onUpdate(InboxViewOpenFilter) })

        SearchNavView, DiscoverNavView -> {}
    }
}
