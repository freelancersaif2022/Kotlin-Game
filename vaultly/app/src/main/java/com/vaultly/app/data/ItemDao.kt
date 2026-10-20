package com.vaultly.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY purchaseDate DESC, id DESC")
    fun observeAll(): Flow<List<Item>>

    @Query("SELECT * FROM items WHERE id = :id")
    fun observe(id: Long): Flow<Item?>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun get(id: Long): Item?

    @Query("SELECT * FROM items WHERE status = 'ACTIVE'")
    suspend fun active(): List<Item>

    @Query("SELECT COUNT(*) FROM items WHERE status = 'ACTIVE'")
    suspend fun activeCount(): Int

    @Upsert
    suspend fun upsert(item: Item): Long

    @Delete
    suspend fun delete(item: Item)
}
