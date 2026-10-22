package com.calorielens.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [MealEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
}
