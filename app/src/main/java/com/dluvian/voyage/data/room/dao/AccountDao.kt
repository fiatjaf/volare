package com.dluvian.voyage.data.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.dluvian.voyage.core.PubkeyHex
import com.dluvian.voyage.data.room.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM account LIMIT 1")
    suspend fun getAccount(): AccountEntity?

    @Query("SELECT pubkey FROM account LIMIT 1")
    fun getMyPubkeyFlow(): Flow<PubkeyHex?>

    @Query("SELECT packageName FROM account LIMIT 1")
    suspend fun getPackageName(): String

    @Transaction
    suspend fun updateAccount(account: AccountEntity) {
        internalDeleteAllAccounts()
        internalInsertAccount(accountEntity = account)
    }

    @Query("DELETE FROM account")
    suspend fun internalDeleteAllAccounts()

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun internalInsertAccount(accountEntity: AccountEntity)
}
