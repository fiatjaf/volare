package com.fiatjaf.volare.ui.components.scaffold

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.fiatjaf.volare.core.ComposableContent
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.navigator.DiscoverNavView
import com.fiatjaf.volare.core.navigator.HomeNavView
import com.fiatjaf.volare.core.navigator.InboxNavView
import com.fiatjaf.volare.core.navigator.MainNavView
import com.fiatjaf.volare.core.navigator.SearchNavView
import com.fiatjaf.volare.ui.components.bar.MainBottomBar
import com.fiatjaf.volare.ui.components.bar.MainTopAppBar
import com.fiatjaf.volare.ui.views.nonMain.search.SearchScaffold

@Composable
fun MainScaffold(
    currentView: MainNavView,
    snackbar: SnackbarHostState,
    homeFeedState: LazyListState,
    inboxFeedState: LazyListState,
    onUpdate: OnUpdate,
    content: ComposableContent
) {
    when (currentView) {
        HomeNavView, InboxNavView, DiscoverNavView -> {
            VolareScaffold(
                snackbar = snackbar,
                topBar = {
                    MainTopAppBar(currentView = currentView, onUpdate = onUpdate)
                },
                bottomBar = {
                    MainBottomBar(
                        currentView = currentView,
                        homeFeedState = homeFeedState,
                        inboxFeedState = inboxFeedState,
                        onUpdate = onUpdate
                    )
                }
            ) {
                content()
            }
        }

        SearchNavView -> SearchScaffold(
            snackbar = snackbar,
            bottomBar = {
                MainBottomBar(
                    currentView = currentView,
                    homeFeedState = homeFeedState,
                    inboxFeedState = inboxFeedState,
                    onUpdate = onUpdate
                )
            },
            onUpdate = onUpdate
        ) {
            content()
        }
    }

}
