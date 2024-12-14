package com.dluvian.volare.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dluvian.volare.core.Core
import com.dluvian.volare.core.SystemBackPress
import com.dluvian.volare.ui.theme.VolareTheme

@Composable
fun VolareApp(core: Core) {
    BackHandler { core.onUpdate(SystemBackPress) }

    VolareTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            VolareAppContent(core = core)
        }
    }
}
