package com.fiatjaf.volare.ui.components.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.fiatjaf.volare.ui.theme.HashtagIcon

@Composable
fun TopicList(
    topics: List<String>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    isRemovable: Boolean = false,
    firstRow:  () -> Unit = {},
    lastRow:  () -> Unit = {},
    onRemove: (Int) -> Unit = {},
    onClick: (Int) -> Unit = {},
) {
    val mappedTopics = remember(topics) {
        topics.map { topic ->
            ItemProps(
                first = { Icon(imageVector = HashtagIcon, contentDescription = null) },
                second = topic,
            )
        }
    }
    ItemList(
        modifier = modifier,
        items = mappedTopics,
        state = state,
        isRemovable = isRemovable,
        firstRow = firstRow,
        lastRow = lastRow,
        onRemove = onRemove,
        onClick = onClick,
    )
}
