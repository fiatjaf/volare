package com.fiatjaf.volare.data.preferences

import android.content.Context
import com.fiatjaf.volare.data.model.FriendPubkeys
import com.fiatjaf.volare.data.model.Global
import com.fiatjaf.volare.data.model.InboxFeedSetting
import com.fiatjaf.volare.data.model.NoPubkeys
import com.fiatjaf.volare.data.model.WebOfTrustPubkeys

private const val PUBKEYS = "pubkeys"
private const val FRIENDS = "friends"
private const val WEB_OF_TRUST = "web_of_trust"
private const val GLOBAL = "global"

class InboxPreferences(context: Context) {
    private val preferences = context.getSharedPreferences("inbox", Context.MODE_PRIVATE)

    fun getInboxFeedSetting(): InboxFeedSetting {
        val pubkeys = when (preferences.getString(PUBKEYS, GLOBAL)) {
            FRIENDS -> FriendPubkeys
            WEB_OF_TRUST -> WebOfTrustPubkeys
            GLOBAL -> Global
            else -> Global
        }
        return InboxFeedSetting(pubkeySelection = pubkeys)
    }

    fun setInboxFeedSettings(setting: InboxFeedSetting) {
        val pubkeys = when (setting.pubkeySelection) {
            FriendPubkeys -> FRIENDS
            WebOfTrustPubkeys -> WEB_OF_TRUST
            Global -> GLOBAL
            // For some reason I can't model PubkeySelection like:
            //
            // InboxPubkeySelection(friends, wot, global): PubkeySelection()
            // HomePubkeySelection(noPubkeys): InboxPubkeySelection()
            //
            // Can't mix those in a when() switch in HomePreferences. So we default to GLOBAL
            NoPubkeys -> GLOBAL
        }
        preferences.edit()
            .putString(PUBKEYS, pubkeys)
            .apply()
    }
}
