package com.example.tipcalculator

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Database(entities = [Tip::class], version = 1)
abstract class TipDatabase : RoomDatabase() {
    abstract fun tipDao(): TipDao

    companion object {
        @Volatile
        private var INSTANCE: TipDatabase? = null

        fun getDatabase(context: Context): TipDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TipDatabase::class.java,
                    "tip_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
