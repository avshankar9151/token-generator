package com.example.tokengenerator.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TokenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(token: Token): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tokens: List<Token>): List<Long>

    @Update
    suspend fun update(token: Token)

    @Delete
    suspend fun delete(token: Token)

    @Query("SELECT * FROM tokens ORDER BY id DESC")
    fun getAllTokens(): LiveData<List<Token>>

    @Query("SELECT * FROM tokens WHERE id IN (:tokenIds)")
    suspend fun getTokensByIds(tokenIds: List<Long>): List<Token>
}
