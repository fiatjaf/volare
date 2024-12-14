package com.dluvian.volare.ui.components.bar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow
import com.dluvian.volare.core.HomeViewOpenFilter
import com.dluvian.volare.core.InboxViewOpenFilter
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.core.navigator.DiscoverNavView
import com.dluvian.volare.core.navigator.HomeNavView
import com.dluvian.volare.core.navigator.InboxNavView
import com.dluvian.volare.core.navigator.MainNavView
import com.dluvian.volare.core.navigator.SearchNavView
import com.dluvian.volare.ui.components.button.FilterIconButton
import com.dluvian.volare.ui.components.button.MenuIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(
    currentView: MainNavView,
    onUpdate: OnUpdate = {}
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
private fun MainFilter(currentView: MainNavView, onUpdate: OnUpdate) {
    when (currentView) {
        HomeNavView -> FilterIconButton(onClick = { onUpdate(HomeViewOpenFilter) })

        InboxNavView -> FilterIconButton(onClick = { onUpdate(InboxViewOpenFilter) })

        SearchNavView, DiscoverNavView -> {}
    }
}
