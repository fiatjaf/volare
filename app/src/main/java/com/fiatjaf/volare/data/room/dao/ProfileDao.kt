package com.fiatjaf.volare.data.room.dao

import androidx.room.Dao
import androidx.room.Query
import com.fiatjaf.volare.core.PubkeyHex
import com.fiatjaf.volare.data.room.entity.ProfileEntity
import com.fiatjaf.volare.data.room.view.AdvancedProfileView
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM AdvancedProfileView WHERE pubkey = :pubkey")
    fun getAdvancedProfileFlow(pubkey: PubkeyHex): Flow<AdvancedProfileView?>

    @Query(
        "SELECT * " +
                "FROM AdvancedProfileView " +
                "WHERE pubkey = (SELECT friendPubkey FROM weboftrust WHERE webOfTrustPubkey = :pubkey)"
    )
    fun getAdvancedProfileTrustedByFlow(pubkey: PubkeyHex): Flow<AdvancedProfileView?>

    @Query(
        "SELECT pubkey, IFNULL(name, '') name, IFNULL(createdAt, 0) createdAt " +
                "FROM profile " +
                "WHERE pubkey = :ourPubkey " +
                "LIMIT 1"
    )
    suspend fun getPersonalProfile(ourPubkey: String): ProfileEntity?

    @Query("SELECT * FROM AdvancedProfileView WHERE pubkey IN (:pubkeys)")
    fun getAdvancedProfilesFlow(pubkeys: Collection<PubkeyHex>): Flow<List<AdvancedProfileView>>

    @Query("SELECT * FROM AdvancedProfileView WHERE pubkey IN (SELECT friendPubkey FROM friend)")
    suspend fun getAdvancedProfilesOfFriends(): List<AdvancedProfileView>

    @Query("SELECT * FROM AdvancedProfileView WHERE pubkey IN (SELECT mutedItem FROM mute WHERE tag = 'p')")
    suspend fun getAdvancedProfilesOfMutes(): List<AdvancedProfileView>

    @Query(
        "SELECT * " +
                "FROM AdvancedProfileView " +
                "WHERE pubkey IN (SELECT pubkey FROM profileSetItem WHERE identifier = :identifier)"
    )
    suspend fun getAdvancedProfilesOfList(identifier: String): List<AdvancedProfileView>

    @Query(
        "SELECT pubkey " +
                "FROM profileSetItem " +
                "WHERE identifier = :identifier " +
                "AND pubkey NOT IN (SELECT pubkey FROM profile)"
    )
    suspend fun getUnknownPubkeysFromList(identifier: String): List<PubkeyHex>

    @Query("SELECT name FROM profile WHERE pubkey = :pubkey")
    suspend fun getName(pubkey: PubkeyHex): String?

    @Query("SELECT createdAt FROM profile WHERE pubkey = :pubkey")
    suspend fun getMaxCreatedAt(pubkey: PubkeyHex): Long?

    suspend fun getProfilesByName(name: String, limit: Int): List<AdvancedProfileView> {
        if (limit <= 0) return emptyList()

        return internalGetProfilesWithNameLike(name = name, somewhere = "%$name%", limit = limit)
    }

    @Query(
        "SELECT DISTINCT pubkey AS pk " +
                "FROM mainEvent " +
                "WHERE pk NOT IN (SELECT friendPubkey FROM friend) " +
                "AND pk != :ourPubkey " +
                "AND pk IN (SELECT webOfTrustPubkey FROM weboftrust) " +
                "AND pk NOT IN (SELECT mutedItem FROM mute WHERE tag = 'p') " +
                "AND pk NOT IN (SELECT pubkey FROM profileSetItem) " +
                "GROUP BY pk " +
                "ORDER BY COUNT(pk) DESC " +
                "LIMIT :limit"
    )
    suspend fun getPopularUnfollowedPubkeys(ourPubkey: String, limit: Int): List<PubkeyHex>

    @Query(
        "SELECT DISTINCT pubkey " +
                "FROM profile " +
                "WHERE pubkey IN (:pubkeys)"
    )
    suspend fun filterKnownProfiles(pubkeys: Collection<PubkeyHex>): List<PubkeyHex>

    @Query(
        "SELECT friendPubkey " +
                "FROM friend " +
                "WHERE friendPubkey NOT IN (SELECT pubkey FROM profile)"
    )
    suspend fun getUnknownFriends(): List<PubkeyHex>

    @Query(
        "SELECT mutedItem " +
                "FROM mute " +
                "WHERE mutedItem NOT IN (SELECT pubkey FROM profile) " +
                "AND tag = 'p'"
    )
    suspend fun getUnknownMutes(): List<PubkeyHex>

    @Query(
        "SELECT * " +
                "FROM AdvancedProfileView " +
                "WHERE (name = :name OR name LIKE :somewhere) " +
                "AND name != '' " +
                "AND pubkey NOT IN (SELECT mutedItem FROM mute WHERE tag = 'p') " +
                "ORDER BY length(name) ASC " +
                "LIMIT :limit"
    )
    suspend fun internalGetProfilesWithNameLike(
        name: String,
        somewhere: String,
        limit: Int
    ): List<AdvancedProfileView>
}
