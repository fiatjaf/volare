package com.fiatjaf.volare.ui.views.nonMain.topic

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import com.fiatjaf.volare.core.OnUpdate
import com.fiatjaf.volare.core.TopicViewAppend
import com.fiatjaf.volare.core.TopicViewRefresh
import com.fiatjaf.volare.core.viewModel.TopicViewModel
import com.fiatjaf.volare.ui.components.Feed

@Composable
fun TopicView(vm: TopicViewModel, snackbar: SnackbarHostState, onUpdate: OnUpdate) {
    TopicScaffold(vm = vm, snackbar = snackbar, onUpdate = onUpdate) {
        Feed(
            paginator = vm.paginator,
            postDetails = vm.postDetails,
            state = vm.feedState,
            onRefresh = { onUpdate(TopicViewRefresh) },
            onAppend = { onUpdate(TopicViewAppend) },
            onUpdate = onUpdate
        )
    }
}
