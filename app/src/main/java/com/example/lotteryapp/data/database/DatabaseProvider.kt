package com.example.lotteryapp.data.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {

    @Volatile
    private var instance: AppDatabase? = null

    fun getDatabase(context: Context): AppDatabase {
        return instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "lotteryapp_database"
            ).addMigrations(AppDatabase.MIGRATION_1_2).build().also { instance = it }
        }
    }
}