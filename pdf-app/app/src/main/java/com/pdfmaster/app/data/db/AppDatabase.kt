package com.pdfmaster.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DocumentEntity::class, BookmarkEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}
