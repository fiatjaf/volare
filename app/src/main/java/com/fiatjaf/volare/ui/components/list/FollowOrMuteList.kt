package com.fiatjaf.volare.ui.components.list

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.ui.components.indicator.BaseHint
import com.fiatjaf.volare.ui.components.row.ClickableRow
import com.fiatjaf.volare.ui.model.FollowableOrMutableItem


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowOrMuteList(
    rows: List<FollowableOrMutableItem>,
    isRefreshing: Boolean,
    state: LazyListState,
    onRefresh: () -> Unit,
) {
    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        if (rows.isEmpty()) BaseHint(text = stringResource(id = R.string.list_is_empty))
        LazyColumn(modifier = Modifier.fillMaxSize(), state = state) {
            items(rows) {
                ClickableRow(
                    header = it.label,
                    leadingContent = it.icon,
                    trailingContent = it.button,
                    onClick = it.onOpen
                )
            }
        }
    }
}
