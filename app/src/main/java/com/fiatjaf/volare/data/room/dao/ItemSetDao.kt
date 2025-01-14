package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.fiatjaf.volare.data.model.ItemSetMeta
import com.fiatjaf.volare.data.room.entity.helper.TitleAndDescription
import kotlinx.coroutines.flow.Flow

private const val LIST_PAIR_NOT_EMPTY = "AND (" +
        "identifier IN (SELECT identifier FROM profileSetItem) " +
        "OR " +
        "identifier IN (SELECT identifier FROM topicSetItem)" +
        ")"

@Dao
interface ItemSetDao {
    @Query(
        "SELECT identifier, title " +
                "FROM profileSet " +
                "WHERE myPubkey = :ourPubkey " +
                "AND deleted = 0 " +
                "AND identifier IN (SELECT identifier FROM profileSetItem)"
    )
    fun getMyProfileSetMetasFlow(ourPubkey: String): Flow<List<ItemSetMeta>>

    @Query(
        "SELECT identifier, title " +
                "FROM topicSet " +
                "WHERE myPubkey = :ourPubkey " +
                "AND deleted = 0 " +
                "AND identifier IN (SELECT identifier FROM topicSetItem)"
    )
    fun getMyTopicSetMetasFlow(ourPubkey: String): Flow<List<ItemSetMeta>>

    @Query(
        "SELECT identifier, title " +
                "FROM profileSet " +
                "WHERE deleted = 0 " +
                "AND identifier NOT IN (SELECT identifier FROM profileSetItem WHERE pubkey = :pubkey) " +
                LIST_PAIR_NOT_EMPTY
    )
    suspend fun getAddableProfileSets(pubkey: String): List<ItemSetMeta>

    @Query(
        "SELECT identifier, title " +
                "FROM profileSet " +
                "WHERE deleted = 0 " +
                "AND identifier IN (SELECT identifier FROM profileSetItem WHERE pubkey = :pubkey) " +
                LIST_PAIR_NOT_EMPTY
    )
    suspend fun getNonAddableProfileSets(pubkey: String): List<ItemSetMeta>

    @Query(
        "SELECT identifier, title " +
                "FROM topicSet " +
                "WHERE deleted = 0 " +
                "AND identifier NOT IN (SELECT identifier FROM topicSetItem WHERE topic = :topic) " +
                LIST_PAIR_NOT_EMPTY
    )
    suspend fun getAddableTopicSets(topic: String): List<ItemSetMeta>

    @Query(
        "SELECT identifier, title " +
                "FROM topicSet " +
                "WHERE deleted = 0 " +
                "AND identifier IN (SELECT identifier FROM topicSetItem WHERE topic = :topic) " +
                LIST_PAIR_NOT_EMPTY
    )
    suspend fun getNonAddableTopicSets(topic: String): List<ItemSetMeta>

    @Query("SELECT title, description FROM profileSet WHERE identifier = :identifier")
    suspend fun getProfileSetTitleAndDescription(identifier: String): TitleAndDescription?

    @Query("SELECT title, description FROM topicSet WHERE identifier = :identifier")
    suspend fun getTopicSetTitleAndDescription(identifier: String): TitleAndDescription?

    @Query("SELECT DISTINCT pubkey FROM profileSetItem WHERE identifier = :identifier LIMIT :limit")
    suspend fun getPubkeys(identifier: String, limit: Int): List<String>

    @Query("SELECT DISTINCT pubkey FROM profileSetItem")
    fun getAllPubkeysFlow(): Flow<List<String>>

    @Query(
        "SELECT pubkey " +
                "FROM profileSetItem " +
                "WHERE identifier = :identifier " +
                "AND pubkey NOT IN (SELECT pubkey FROM nip65) "
    )
    suspend fun getPubkeysWithMissingNip65(identifier: String): List<String>
}
