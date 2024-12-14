package com.dluvian.volare.ui.views.main

import androidx.compose.runtime.Composable
import com.dluvian.volare.core.Core
import com.dluvian.volare.core.navigator.DiscoverNavView
import com.dluvian.volare.core.navigator.HomeNavView
import com.dluvian.volare.core.navigator.InboxNavView
import com.dluvian.volare.core.navigator.MainNavView
import com.dluvian.volare.core.navigator.SearchNavView
import com.dluvian.volare.ui.components.scaffold.MainScaffold
import com.dluvian.volare.ui.views.main.subViews.DiscoverView
import com.dluvian.volare.ui.views.main.subViews.HomeView
import com.dluvian.volare.ui.views.main.subViews.InboxView
import com.dluvian.volare.ui.views.nonMain.search.SearchView
import kotlinx.coroutines.CoroutineScope

@Composable
fun MainView(
    core: Core,
    scope: CoroutineScope,
    currentView: MainNavView,
) {
    MainDrawer(vm = core.vmContainer.drawerVM, scope = scope, onUpdate = core.onUpdate) {
        MainScaffold(
            currentView = currentView,
            snackbar = core.appContainer.snackbar,
            homeFeedState = core.vmContainer.homeVM.feedState,
            inboxFeedState = core.vmContainer.inboxVM.feedState,
            onUpdate = core.onUpdate
        ) {
            when (currentView) {
                HomeNavView -> HomeView(vm = core.vmContainer.homeVM, onUpdate = core.onUpdate)
                InboxNavView -> InboxView(vm = core.vmContainer.inboxVM, onUpdate = core.onUpdate)
                SearchNavView -> SearchView(
                    vm = core.vmContainer.searchVM,
                    onUpdate = core.onUpdate
                )

                DiscoverNavView -> DiscoverView(
                    vm = core.vmContainer.discoverVM,
                    onUpdate = core.onUpdate
                )
            }
        }
    }
}
