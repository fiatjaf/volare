package com.dluvian.volare.ui.components.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dluvian.volare.R
import com.dluvian.volare.core.ClickSearch
import com.dluvian.volare.core.OnUpdate
import com.dluvian.volare.ui.theme.SearchIcon

@Composable
fun SearchIconButton(onUpdate: OnUpdate) {
    IconButton(onClick = { onUpdate(ClickSearch) }) {
        Icon(imageVector = SearchIcon, contentDescription = stringResource(id = R.string.search))
    }
}
