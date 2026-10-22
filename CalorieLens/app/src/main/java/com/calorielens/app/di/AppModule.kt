package com.calorielens.app.di

import android.content.Context
import androidx.room.Room
import com.calorielens.app.data.db.AppDatabase
import com.calorielens.app.data.db.MealDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "calorielens.db").build()

    @Provides
    fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()
}
