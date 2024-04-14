package com.dluvian.voyage.ui.components.bottomSheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dluvian.voyage.R
import com.dluvian.voyage.core.ComposableContent
import com.dluvian.voyage.core.Fn
import com.dluvian.voyage.core.MAX_TOPICS
import com.dluvian.voyage.core.Topic
import com.dluvian.voyage.ui.components.chip.TopicChip
import com.dluvian.voyage.ui.theme.sizing
import com.dluvian.voyage.ui.theme.spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicSelectionBottomSheet(
    selectedTopics: MutableState<List<Topic>>,
    myTopics: List<Topic>,
    onDismiss: Fn
) {
    ModalBottomSheet(modifier = Modifier.fillMaxSize(), onDismissRequest = onDismiss) {
        TopicSelection(myTopics = myTopics, selectedTopics = selectedTopics)
    }
}

@Composable
private fun TopicSelection(myTopics: List<Topic>, selectedTopics: MutableState<List<Topic>>) {
    ContentRow(
        header = stringResource(
            id = R.string.selected_n_of_m,
            selectedTopics.value.size,
            MAX_TOPICS
        )
    ) {
        if (myTopics.isEmpty()) Text(
            text = stringResource(id = R.string.you_dont_follow_any_topics_yet),
            style = MaterialTheme.typography.titleMedium
        )
        else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(minSize = sizing.topicChipMinSize)
            ) {
                items(myTopics) {
                    val isSelected = selectedTopics.value.contains(it)
                    TopicChip(
                        modifier = Modifier.padding(spacing.small),
                        isSelected = isSelected,
                        topic = it,
                        onClick = {
                            if (isSelected) selectedTopics.value -= it
                            else if (selectedTopics.value.size < MAX_TOPICS) selectedTopics.value += it
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentRow(header: String, content: ComposableContent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenEdge)
    ) {
        Text(
            text = header,
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(spacing.medium))
        content()
    }
}