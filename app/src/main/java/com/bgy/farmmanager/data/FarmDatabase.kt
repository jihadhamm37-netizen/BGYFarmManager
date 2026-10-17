package com.bgy.farmmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [FarmEntity::class], version = 1, exportSchema = false)
abstract class FarmDatabase : RoomDatabase() {
    abstract fun farmDao(): FarmDao

    companion object {
        @Volatile private var INSTANCE: FarmDatabase? = null

        fun get(context: Context): FarmDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FarmDatabase::class.java,
                    "bgy_farms.db"
                ).build().also { INSTANCE = it }
            }
    }
}
