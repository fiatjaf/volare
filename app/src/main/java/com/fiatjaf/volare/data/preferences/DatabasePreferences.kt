package com.fiatjaf.volare.data.preferences

import android.content.Context
import com.fiatjaf.volare.core.DEFAULT_RETAIN_ROOT
import com.fiatjaf.volare.core.MAX_RETAIN_ROOT
import com.fiatjaf.volare.core.MIN_RETAIN_ROOT

private const val SWEEP_THRESHOLD = "sweep_threshold"

class DatabasePreferences(context: Context) {
    private val preferences = context.getSharedPreferences("database", Context.MODE_PRIVATE)

    fun getSweepThreshold(): Int {
        val lowerBoundCheck = maxOf(
            preferences.getInt(SWEEP_THRESHOLD, DEFAULT_RETAIN_ROOT),
            MIN_RETAIN_ROOT.toInt()
        )

        return minOf(lowerBoundCheck, MAX_RETAIN_ROOT.toInt())
    }

    fun setSweepThreshold(newThreshold: Int) {
        val lowerBoundCheck = maxOf(newThreshold, MIN_RETAIN_ROOT.toInt())
        preferences.edit()
            .putInt(SWEEP_THRESHOLD, minOf(lowerBoundCheck, MAX_RETAIN_ROOT.toInt()))
            .apply()
    }
}
