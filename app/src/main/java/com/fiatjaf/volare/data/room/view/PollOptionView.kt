package com.fiatjaf.volare.data.room.view

import androidx.room.DatabaseView
import com.fiatjaf.volare.core.EventIdHex
import com.fiatjaf.volare.core.Label
import com.fiatjaf.volare.core.OptionId

private const val RESP_TO_OPTION_COND =
    "pollResponse.pollId = pollOption.pollId AND pollResponse.optionId = pollOption.optionId"

@DatabaseView(
    "SELECT pollOption.pollId, " +
            "pollOption.optionId, " +
            "pollOption.label, " +
            "(SELECT COUNT(*) FROM pollResponse WHERE $RESP_TO_OPTION_COND) AS voteCount, " +
            "(SELECT EXISTS(SELECT * FROM pollResponse WHERE pubkey = (SELECT pubkey FROM account) AND $RESP_TO_OPTION_COND)) AS isMyVote " +
            "FROM pollOption "
)
data class PollOptionView(
    val pollId: EventIdHex,
    val optionId: OptionId,
    val label: Label,
    val voteCount: Int,
    val isMyVote: Boolean,
)
