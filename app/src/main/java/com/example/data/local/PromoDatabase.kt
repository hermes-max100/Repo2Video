package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.PromoProject

@Database(entities = [PromoProject::class], version = 1, exportSchema = false)
abstract class PromoDatabase : RoomDatabase() {

    abstract fun promoDao(): PromoDao

    companion object {
        @Volatile
        private var INSTANCE: PromoDatabase? = null

        fun getDatabase(context: Context): PromoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PromoDatabase::class.java,
                    "promo_video_database"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
