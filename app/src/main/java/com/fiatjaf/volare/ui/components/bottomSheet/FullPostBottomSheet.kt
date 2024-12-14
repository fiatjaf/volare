package com.fiatjaf.volare.ui.components.bottomSheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.fiatjaf.volare.R
import com.fiatjaf.volare.core.Fn
import com.fiatjaf.volare.ui.components.text.AnnotatedText
import com.fiatjaf.volare.ui.theme.spacing
import com.fiatjaf.volare.data.provider.TextItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPostBottomSheet(content: List<TextItem>, onDismiss: Fn) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        BottomSheetColumn(header = stringResource(id = R.string.original_post)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(state = rememberScrollState())
                    .padding(bottom = spacing.xxl)
            ) {
                AnnotatedText(items = content)
            }
        }
    }
}
