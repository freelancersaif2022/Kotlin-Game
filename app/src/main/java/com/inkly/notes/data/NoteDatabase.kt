package com.inkly.notes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Note::class], version = 1, exportSchema = false)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        fun build(context: Context): NoteDatabase =
            Room.databaseBuilder(context, NoteDatabase::class.java, "inkly.db").build()
    }
}
