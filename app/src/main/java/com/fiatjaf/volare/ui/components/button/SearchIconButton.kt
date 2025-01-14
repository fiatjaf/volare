package com.fiatjaf.volare.ui.components.button

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.ClickSearch
import com.fiatjaf.volare.ui.theme.SearchIcon

@Composable
fun SearchIconButton(onUpdate: (UIEvent) -> Unit) {
    IconButton(onClick = { onUpdate(ClickSearch) }) {
        Icon(imageVector = SearchIcon, contentDescription = stringResource(id = R.string.search))
    }
}
