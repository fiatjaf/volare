package com.dluvian.voyage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.dluvian.voyage.R
import com.dluvian.voyage.core.ComposableContent
import com.dluvian.voyage.core.Fn
import com.dluvian.voyage.core.OnUpdate
import com.dluvian.voyage.core.model.IPaginator
import com.dluvian.voyage.core.showScrollButton
import com.dluvian.voyage.ui.components.indicator.FullLinearProgressIndicator
import com.dluvian.voyage.ui.theme.ScrollUpIcon
import com.dluvian.voyage.ui.theme.sizing
import com.dluvian.voyage.ui.theme.spacing
import kotlinx.coroutines.launch

@Composable
fun Feed(
    paginator: IPaginator,
    state: LazyListState,
    onRefresh: Fn,
    onAppend: Fn,
    onUpdate: OnUpdate,
    header: ComposableContent = {},
) {
    val isRefreshing by paginator.isRefreshing
    val isAppending by paginator.isAppending
    val posts by paginator.page.value.collectAsState()

    PullRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        if (isAppending) FullLinearProgressIndicator()
        LazyColumn(modifier = Modifier.fillMaxSize(), state = state) {
            item { header() }
            items(items = posts, key = { item -> item.id }) { post ->
                PostRow(
                    post = post,
                    onUpdate = onUpdate
                )
                FullHorizontalDivider()
            }
            if (posts.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            modifier = Modifier.padding(horizontal = spacing.screenEdge),
                            onClick = onAppend
                        ) {
                            Text(text = stringResource(id = R.string.next_page))
                        }
                    }
                }
            }
        }
        if (state.showScrollButton()) {
            val scope = rememberCoroutineScope()
            ScrollUpButton(onScrollUp = { scope.launch { state.animateScrollToItem(0) } })
        }
    }
}

@Composable
private fun ScrollUpButton(onScrollUp: Fn) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = spacing.bigScreenEdge)
            .padding(bottom = spacing.bigScreenEdge)
            .padding(spacing.bigScreenEdge)
            .padding(spacing.bigScreenEdge),
        contentAlignment = Alignment.BottomEnd
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onScrollUp)
                .size(sizing.iconButton),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = ScrollUpIcon,
                tint = Color.Black,
                contentDescription = stringResource(id = R.string.scroll_to_the_page_top)
            )
        }
    }
}
