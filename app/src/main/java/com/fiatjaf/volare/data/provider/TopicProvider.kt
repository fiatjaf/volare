package com.fiatjaf.volare.data.provider

import com.fiatjaf.volare.core.VOLARE
import com.fiatjaf.volare.core.model.TopicFollowState
import com.fiatjaf.volare.core.model.TopicMuteState
import com.fiatjaf.volare.core.utils.takeRandom
import com.fiatjaf.volare.data.room.dao.MuteDao
import com.fiatjaf.volare.data.room.dao.TopicDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class TopicProvider(
    val forcedFollowStates: Flow<Map<String, Boolean>>,
    val forcedMuteStates: Flow<Map<String, Boolean>>,
    private val topicDao: TopicDao,
    private val muteDao: MuteDao,
) {
    suspend fun getAllTopics(): List<String> {
        return topicDao.getAllTopics()
    }

    suspend fun getPopularUnfollowedTopics(limit: Int): List<String> {
        return topicDao.getUnfollowedTopics(limit = limit)
            .ifEmpty { (defaultTopics - topicDao.getMyTopics().toSet()).shuffled() }
    }

    fun getIsFollowedFlow(topic: String): Flow<Boolean> {
        return combine(
            topicDao.getIsFollowedFlow(topic = topic),
            forcedFollowStates
        ) { db, forced ->
            forced[topic] ?: db
        }
    }

    fun getIsMutedFlow(topic: String): Flow<Boolean> {
        return combine(
            muteDao.getTopicIsMutedFlow(topic = topic),
            forcedMuteStates
        ) { db, forced ->
            forced[topic] ?: db
        }
    }

    // Not named "getMaxCreatedAt" bc there should only be one createdAt available
    suspend fun getCreatedAt() = topicDao.getMaxCreatedAt()

    private val defaultTopics = listOf(
        VOLARE,
        "nostr",
        "asknostr",
        "introductions",
        "grownostr",
        "newstr",
        "bitcoin",
        "runstr",
        "bookstr",
        "devstr",
        "releastr"
    )
}
