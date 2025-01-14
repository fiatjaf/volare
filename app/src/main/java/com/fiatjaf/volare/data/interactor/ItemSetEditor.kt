package com.fiatjaf.volare.data.interactor

import android.util.Log
import com.fiatjaf.volare.core.MAX_KEYS_SQL
import com.fiatjaf.volare.core.model.ItemSetItem
import com.fiatjaf.volare.core.model.ItemSetProfile
import com.fiatjaf.volare.core.model.ItemSetTopic
import com.fiatjaf.volare.core.utils.normalizeDescription
import com.fiatjaf.volare.core.utils.normalizeTitle
import com.fiatjaf.volare.data.event.EventValidator
import com.fiatjaf.volare.data.nostr.NostrService
import com.fiatjaf.volare.data.provider.ItemSetProvider
import com.fiatjaf.volare.data.provider.RelayProvider
import com.fiatjaf.volare.data.room.dao.upsert.ProfileSetUpsertDao
import com.fiatjaf.volare.data.room.dao.upsert.TopicSetUpsertDao
import rust.nostr.sdk.Event
import rust.nostr.sdk.PublicKey

private const val TAG = "ItemSetEditor"

class ItemSetEditor(
    private val nostrService: NostrService,
    private val relayProvider: RelayProvider,
    private val profileSetUpsertDao: ProfileSetUpsertDao,
    private val topicSetUpsertDao: TopicSetUpsertDao,
    private val itemSetProvider: ItemSetProvider,
) {
    suspend fun editProfileSet(
        identifier: String,
        title: String,
        description: String,
        pubkeys: List<String>
    ): Result<Event> {
        return nostrService.publishProfileSet(
            identifier = identifier,
            title = title.normalizeTitle(),
            description = description.normalizeDescription(),
            pubkeys = pubkeys.map { PublicKey.fromHex(it) },
            relayUrls = relayProvider.getWriteRelays()
        ).onFailure {
            Log.w(TAG, "Failed to sign profile set", it)
        }.onSuccess {
            val validated = EventValidator.createValidatedProfileSet(event = it)
            if (validated == null) {
                val err = "Serialized topic profile event differs from input"
                Log.w(TAG, err)
                return Result.failure(IllegalStateException(err))
            }
            profileSetUpsertDao.upsertSet(set = validated)
        }
    }

    suspend fun editTopicSet(
        identifier: String,
        title: String,
        description: String,
        topics: List<String>
    ): Result<Event> {
        return nostrService.publishTopicSet(
            identifier = identifier,
            title = title.normalizeTitle(),
            description = description.normalizeDescription(),
            topics = topics,
            relayUrls = relayProvider.getWriteRelays()
        ).onFailure {
            Log.w(TAG, "Failed to sign topic set", it)
        }.onSuccess {
            val validated = EventValidator.createValidatedTopicSet(event = it)
            if (validated == null) {
                val err = "Serialized topic set event differs from input"
                Log.w(TAG, err)
                return Result.failure(IllegalStateException(err))
            }
            topicSetUpsertDao.upsertSet(set = validated)
        }
    }

    suspend fun addItemToSet(item: ItemSetItem, identifier: String): Result<Event> {
        val currentList = when (item) {
            is ItemSetProfile -> itemSetProvider.getPubkeysFromList(identifier = identifier)
            is ItemSetTopic -> itemSetProvider.getTopicsFromList(identifier = identifier)
        }

        if (currentList.contains(item.value)) {
            return Result.failure(IllegalStateException("Item is already in list"))
        }
        if (currentList.size >= MAX_KEYS_SQL) {
            return Result.failure(IllegalArgumentException("List is already full"))
        }

        val titleAndDescription = itemSetProvider.getTitleAndDescription(identifier = identifier)

        return when (item) {
            is ItemSetProfile -> {
                editProfileSet(
                    identifier = identifier,
                    title = titleAndDescription.title,
                    description = titleAndDescription.description,
                    pubkeys = currentList + item.pubkey,
                )
            }

            is ItemSetTopic -> {
                editTopicSet(
                    identifier = identifier,
                    title = titleAndDescription.title,
                    description = titleAndDescription.description,
                    topics = currentList + item.topic,
                )
            }
        }
    }
}
