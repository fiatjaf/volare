package com.dluvian.voyage.data.preferences

import android.content.Context
import androidx.compose.runtime.mutableStateOf

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(APP_FILE, Context.MODE_PRIVATE)
}
