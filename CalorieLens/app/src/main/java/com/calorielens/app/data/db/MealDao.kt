package com.calorielens.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id")
    fun observe(id: Long): Flow<MealEntity?>

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun get(id: Long): MealEntity?

    @Query("SELECT imagePath FROM meals")
    suspend fun allImagePaths(): List<String>

    @Query("SELECT COUNT(*) FROM meals")
    fun observeCount(): Flow<Int>

    @Insert
    suspend fun insert(meal: MealEntity): Long

    @Update
    suspend fun update(meal: MealEntity)

    @Delete
    suspend fun delete(meal: MealEntity)

    @Query("DELETE FROM meals")
    suspend fun deleteAll()
}
