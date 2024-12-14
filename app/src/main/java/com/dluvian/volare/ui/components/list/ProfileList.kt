package com.dluvian.volare.ui.components.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.dluvian.volare.core.ComposableContent
import com.dluvian.volare.data.room.view.AdvancedProfileView
import com.dluvian.volare.ui.components.icon.TrustIcon

@Composable
fun ProfileList(
    profiles: List<AdvancedProfileView>,
    state: LazyListState,
    isRemovable: Boolean = false,
    firstRow: ComposableContent = {},
    onRemove: (Int) -> Unit = {},
    onClick: (Int) -> Unit = {},
) {
    val mappedProfiles = remember(profiles) {
        profiles.map { profile ->
            ItemProps(
                first = { TrustIcon(profile = profile) },
                second = profile.name,
            )
        }
    }
    ItemList(
        items = mappedProfiles,
        state = state,
        isRemovable = isRemovable,
        firstRow = firstRow,
        onRemove = onRemove,
        onClick = onClick
    )
}