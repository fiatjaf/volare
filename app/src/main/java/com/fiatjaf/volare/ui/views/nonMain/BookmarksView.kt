package com.fiatjaf.volare.ui.views.nonMain

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.BookmarksViewAppend
import com.fiatjaf.volare.core.BookmarksViewInit
import com.fiatjaf.volare.core.BookmarksViewRefresh
import com.fiatjaf.volare.core.viewModel.BookmarksViewModel
import com.fiatjaf.volare.ui.components.Feed
import com.fiatjaf.volare.ui.components.scaffold.SimpleGoBackScaffold

@Composable
fun BookmarksView(vm: BookmarksViewModel, snackbar: SnackbarHostState, onUpdate: (UIEvent) -> Unit) {
    LaunchedEffect(key1 = Unit) {
        onUpdate(BookmarksViewInit)
    }
    SimpleGoBackScaffold(
        header = stringResource(id = R.string.bookmarks),
        snackbar = snackbar,
        onUpdate = onUpdate
    ) {
        Feed(
            paginator = vm.paginator,
            postDetails = vm.postDetails,
            state = vm.feedState,
            onRefresh = { onUpdate(BookmarksViewRefresh) },
            onAppend = { onUpdate(BookmarksViewAppend) },
            onUpdate = onUpdate
        )
    }
}
