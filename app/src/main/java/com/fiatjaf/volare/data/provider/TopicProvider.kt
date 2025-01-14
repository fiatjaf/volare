package com.fiatjaf.volare.data.provider

import com.fiatjaf.volare.core.VOLARE
import com.fiatjaf.volare.core.model.TopicFollowState
import com.fiatjaf.volare.core.model.TopicMuteState
import com.fiatjaf.volare.core.utils.takeRandom
import com.fiatjaf.volare.data.model.ListTopics
import com.fiatjaf.volare.data.model.MyTopics
import com.fiatjaf.volare.data.model.NoTopics
import com.fiatjaf.volare.data.model.TopicSelection
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
    private val itemSetProvider: ItemSetProvider,
) {
    suspend fun getMyTopics(limit: Int = Int.MAX_VALUE): List<String> {
        return topicDao.getMyTopics().takeRandom(limit)
    }

    suspend fun getTopicSelection(
        topicSelection: TopicSelection,
        limit: Int = Int.MAX_VALUE
    ): List<String> {
        return when (topicSelection) {
            NoTopics -> emptyList()
            MyTopics -> getMyTopics(limit = limit)
            is ListTopics -> itemSetProvider.getTopicsFromList(
                identifier = topicSelection.identifier,
                limit = limit
            )
        }
    }

    suspend fun getAllTopics(): List<String> {
        return topicDao.getAllTopics()
    }

    suspend fun getPopularUnfollowedTopics(limit: Int): List<String> {
        return topicDao.getUnfollowedTopics(limit = limit)
            .ifEmpty { (defaultTopics - topicDao.getMyTopics().toSet()).shuffled() }
    }

    suspend fun getMyTopicsFlow(): Flow<List<StringFollowState>> {
        // We want to be able to unfollow on the same list
        val myTopics = getMyTopics()

        return forcedFollowStates.map { forcedFollows ->
            myTopics.map { topic ->
                TopicFollowState(
                    topic = topic,
                    isFollowed = forcedFollows[topic] ?: true
                )
            }
        }
    }

    suspend fun getMutedTopicsFlow(): Flow<List<StringMuteState>> {
        // We want to be able to unmute on the same list
        val mutedTopics = muteDao.getMyTopicMutes()

        return forcedMuteStates.map { forcedMutes ->
            mutedTopics.map { topic ->
                TopicMuteState(
                    topic = topic,
                    isMuted = forcedMutes[topic] ?: true
                )
            }
        }
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
