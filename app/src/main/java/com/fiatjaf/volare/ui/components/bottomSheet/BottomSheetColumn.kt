package com.fiatjaf.volare.ui.components.bottomSheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fiatjaf.volare.ui.theme.OnBgLight
import com.fiatjaf.volare.ui.theme.spacing

@Composable
fun BottomSheetColumn(header: String, content:  () -> Unit) {
    Column(
        // Don't put verticalScroll in Modifier because we use LazyGrid in seed sheet
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.bigScreenEdge)
    ) {
        Text(
            text = header,
            style = MaterialTheme.typography.labelLarge,
            color = OnBgLight
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = spacing.xl)
        )
        content()
    }
}
