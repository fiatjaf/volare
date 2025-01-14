package com.fiatjaf.volare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.FEED_PAGE_SIZE
import com.fiatjaf.volare.core.model.IPaginator
import com.fiatjaf.volare.core.utils.debounce
import com.fiatjaf.volare.core.utils.showScrollButton
import com.fiatjaf.volare.data.model.PostDetails
import com.fiatjaf.volare.ui.components.bottomSheet.PostDetailsBottomSheet
import com.fiatjaf.volare.ui.components.indicator.BaseHint
import com.fiatjaf.volare.ui.components.indicator.FullLinearProgressIndicator
import com.fiatjaf.volare.ui.components.row.mainEvent.MainEventCtx
import com.fiatjaf.volare.ui.components.row.mainEvent.MainEventRow
import com.fiatjaf.volare.ui.theme.ScrollUpIcon
import com.fiatjaf.volare.ui.theme.sizing
import com.fiatjaf.volare.ui.theme.spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "Feed"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Feed(
    paginator: IPaginator,
    postDetails: State<PostDetails?>,
    state: LazyListState,
    onRefresh: () -> Unit,
    onAppend: () -> Unit,
    onUpdate: (UIEvent) -> Unit,
) {
    val isRefreshing by paginator.isRefreshing
    val isAppending by paginator.isAppending
    val hasMoreRecentItems by paginator.hasMoreRecentItems
    val hasPage by paginator.hasPage.value.collectAsState()
    val pageTimestamps by paginator.pageTimestamps
    val filteredPage by paginator.filteredPage.value.collectAsState()
    val scope = rememberCoroutineScope()
    val showProgressIndicator by remember {
        derivedStateOf { isAppending || (hasPage && pageTimestamps.isEmpty()) }
    }

    // a complicated scheme to enable preloading images from notes that are likely to be in focus
    // by the user -- i.e. when the user stops scrolling and the note seems to be more or less
    // in the top or middle of the screen, it works pretty well actually
    val coroutineScope = rememberCoroutineScope()
    val (focused, setFocused) = remember { mutableStateOf<MainEventCtx?>(null) }

    fun guessWhatIndexIsActuallyFocused(state: LazyListState): Int {
        return when (state.firstVisibleItemScrollOffset) {
            in 0..45 -> state.firstVisibleItemIndex
            in 45 .. 200 -> -1
            in 200 .. 1600 -> state.firstVisibleItemIndex + 1
            else -> -1
        }
    }

    val debouncedScrollingStopped = remember {
        debounce(1000, coroutineScope) {
            val index = guessWhatIndexIsActuallyFocused(state)
            if (state.firstVisibleItemIndex >= 0) {
                coroutineScope.launch {
                    delay(1000)
                    if (guessWhatIndexIsActuallyFocused(state) == index) {
                        setFocused(filteredPage.getOrNull(index))
                    }
                }
            }
        }
    }
    LaunchedEffect (state) {
        snapshotFlow { state.isScrollInProgress }
            .collect {
                if (it) return@collect
                debouncedScrollingStopped()
            }
    }

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = onRefresh) {
        if (showProgressIndicator) FullLinearProgressIndicator()
        if (!hasPage && pageTimestamps.isEmpty()) BaseHint(stringResource(id = R.string.no_posts_found))
        postDetails.value?.let { details ->
            PostDetailsBottomSheet(postDetails = details, onUpdate = onUpdate)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state
        ) {
            if (hasMoreRecentItems) item {
                MostRecentPostsTextButton(onClick = {
                    onRefresh()
                    state.requestScrollToItem(index = 0)
                })
            }

            items(items = filteredPage) { mainEventCtx ->
                MainEventRow(
                    ctx = mainEventCtx,
                    onUpdate = onUpdate,
                    isFocused = focused == mainEventCtx
                )
                FullHorizontalDivider()
            }

            if (pageTimestamps.size >= FEED_PAGE_SIZE) item {
                NextPageButton(onAppend = {
                    onAppend()
                    // 1 = Skip TextButton
                    state.requestScrollToItem(index = 1)
                })
            }
        }
        if (state.showScrollButton()) {
            ScrollUpButton(onScrollToTop = { scope.launch { state.animateScrollToItem(index = 0) } })
        }
    }
}

@Composable
private fun MostRecentPostsTextButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        TextButton(onClick = onClick) {
            Text(text = stringResource(id = R.string.click_to_load_most_recent_posts))
        }
    }
}

@Composable
private fun NextPageButton(onAppend: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            modifier = Modifier.padding(horizontal = spacing.screenEdge),
            onClick = onAppend
        ) {
            Text(text = stringResource(id = R.string.next_page))
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ScrollUpButton(onScrollToTop: () -> Unit) {
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
                .clickable(onClick = onScrollToTop)
                .size(sizing.scrollUpButton),
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
