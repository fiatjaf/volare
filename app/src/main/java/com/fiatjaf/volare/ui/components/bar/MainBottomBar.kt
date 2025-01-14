package com.fiatjaf.volare.ui.components.bar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.ClickCreate
import com.fiatjaf.volare.core.ClickDiscover
import com.fiatjaf.volare.core.ClickHome
import com.fiatjaf.volare.core.ClickInbox
import com.fiatjaf.volare.core.ClickSearch
import com.fiatjaf.volare.core.UIEvent
import com.fiatjaf.volare.core.navigator.DiscoverNavView
import com.fiatjaf.volare.core.navigator.HomeNavView
import com.fiatjaf.volare.core.navigator.InboxNavView
import com.fiatjaf.volare.core.navigator.MainNavView
import com.fiatjaf.volare.core.navigator.SearchNavView
import com.fiatjaf.volare.ui.theme.AddIcon
import com.fiatjaf.volare.ui.theme.DiscoverIcon
import com.fiatjaf.volare.ui.theme.HomeIcon
import com.fiatjaf.volare.ui.theme.InboxIcon
import com.fiatjaf.volare.ui.theme.SearchIcon
import kotlinx.coroutines.launch

@Composable
fun MainBottomBar(
    currentView: MainNavView,
    homeFeedState: LazyListState,
    inboxFeedState: LazyListState,
    onUpdate: (UIEvent) -> Unit
) {
    NavigationBar(modifier = Modifier
        .navigationBarsPadding()
        .height(52.dp)) {
        val scope = rememberCoroutineScope()
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            MainBottomBarItem(
                selected = currentView is HomeNavView,
                description = stringResource(id = R.string.home),
                icon = HomeIcon,
                onClick = {
                    onUpdate(ClickHome)
                    if (currentView is HomeNavView) {
                        scope.launch { homeFeedState.animateScrollToItem(index = 0) }
                    }
                })
            MainBottomBarItem(
                selected = currentView is DiscoverNavView,
                description = stringResource(id = R.string.discover),
                icon = DiscoverIcon,
                onClick = { onUpdate(ClickDiscover) })
            MainBottomBarItem(
                selected = false,
                description = stringResource(id = R.string.create),
                icon = AddIcon,
                onClick = { onUpdate(ClickCreate) })
            MainBottomBarItem(
                selected = currentView is SearchNavView,
                description = stringResource(id = R.string.search),
                icon = SearchIcon,
                onClick = { onUpdate(ClickSearch) })
            MainBottomBarItem(
                selected = currentView is InboxNavView,
                description = stringResource(id = R.string.inbox),
                icon = InboxIcon,
                onClick = {
                    onUpdate(ClickInbox)
                    if (currentView is InboxNavView) {
                        scope.launch { inboxFeedState.animateScrollToItem(index = 0) }
                    }
                })
        }
    }
}

@Composable
private fun RowScope.MainBottomBarItem(
    selected: Boolean,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = { Icon(imageVector = icon, contentDescription = description) }
    )
}
