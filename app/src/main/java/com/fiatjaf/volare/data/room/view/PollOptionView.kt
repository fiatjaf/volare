package com.fiatjaf.volare.data.room.view

import androidx.room.DatabaseView

private const val RESP_TO_OPTION_COND =
    "pollResponse.pollId = pollOption.pollId AND pollResponse.optionId = pollOption.optionId"

@DatabaseView(
    "SELECT pollOption.pollId, " +
            "pollOption.optionId, " +
            "pollOption.label, " +
            "(SELECT COUNT(*) FROM pollResponse WHERE $RESP_TO_OPTION_COND) AS voteCount " +
            "FROM pollOption "
)
data class PollOptionView(
    val pollId: String,
    val optionId: String,
    val label: String,
    val voteCount: Int
)
