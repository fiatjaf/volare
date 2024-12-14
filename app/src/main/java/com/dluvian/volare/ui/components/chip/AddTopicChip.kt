package com.dluvian.volare.ui.components.chip

import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.Fn
import com.dluvian.volare.ui.theme.AddIcon

@Composable
fun AddTopicChip(onOpenTopicSelection: Fn, modifier: Modifier = Modifier) {
    SmallFilterChip(
        modifier = modifier,
        onClick = onOpenTopicSelection,
        label = { Text(text = stringResource(id = R.string.topics)) },
        leadingIcon = {
            Icon(
                modifier = Modifier.size(AssistChipDefaults.IconSize),
                imageVector = AddIcon,
                contentDescription = stringResource(id = R.string.topics),
            )
        },
    )
}
