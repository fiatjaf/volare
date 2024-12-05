package com.dluvian.voyage.data.preferences

import android.content.Context
import com.dluvian.voyage.core.DEFAULT_AUTOPILOT_RELAYS
import com.dluvian.voyage.core.MAX_AUTOPILOT_RELAYS
import com.dluvian.voyage.core.MIN_AUTOPILOT_RELAYS

private const val SEND_AUTH = "send_auth"
private const val AUTOPILOT_RELAYS = "autopilot_relays"

class RelayPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(RELAY_FILE, Context.MODE_PRIVATE)

    fun getSendAuth(): Boolean {
        return preferences.getBoolean(SEND_AUTH, false)
    }

    fun setSendAuth(sendAuth: Boolean) {
        preferences.edit()
            .putBoolean(SEND_AUTH, sendAuth)
            .apply()
    }

    fun getAutopilotRelays(): Int {
        val num = preferences.getInt(AUTOPILOT_RELAYS, DEFAULT_AUTOPILOT_RELAYS)
        return if (num in MIN_AUTOPILOT_RELAYS..MAX_AUTOPILOT_RELAYS) num else DEFAULT_AUTOPILOT_RELAYS
    }

    fun setAutopilotRelays(newNumber: Int) {
        val newNum = if (newNumber in MIN_AUTOPILOT_RELAYS..MAX_AUTOPILOT_RELAYS) newNumber
        else DEFAULT_AUTOPILOT_RELAYS

        preferences.edit()
            .putInt(AUTOPILOT_RELAYS, newNum)
            .apply()
    }
}
