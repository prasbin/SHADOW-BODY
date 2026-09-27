package com.shadowbody.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface XpTransactionDao {
    @Query("SELECT * FROM xp_transaction ORDER BY loggedAt DESC, id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<XpTransaction>>

    @Query("SELECT * FROM xp_transaction ORDER BY loggedAt DESC, id DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<XpTransaction>

    @Query("SELECT * FROM xp_transaction WHERE dayKey = :dayKey ORDER BY loggedAt DESC")
    fun observeForDay(dayKey: String): Flow<List<XpTransaction>>

    @Query("SELECT * FROM xp_transaction WHERE dayKey = :dayKey ORDER BY loggedAt DESC")
    suspend fun getForDay(dayKey: String): List<XpTransaction>

    @Query("SELECT * FROM xp_transaction WHERE source = :source AND sourceRef = :sourceRef")
    abstract suspend fun findBySourceRef(source: String, sourceRef: String): XpTransaction?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insert(tx: XpTransaction): Long?

    @Query("SELECT SUM(xpAmount) FROM xp_transaction")
    fun observeTotalXp(): Flow<Int?>

    @Query("SELECT SUM(xpAmount) FROM xp_transaction")
    suspend fun getTotalXpSync(): Int?
}