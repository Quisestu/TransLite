package com.example.test0.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.test0.model.TranslationRecord

@Database(
    entities = [TranslationRecord::class],
    version = 1,
    exportSchema = false
)
abstract class TranslationDatabase : RoomDatabase() {
    
    abstract fun translationDao(): TranslationDao
    
    companion object {
        @Volatile
        private var INSTANCE: TranslationDatabase? = null
        
        fun getDatabase(context: Context): TranslationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TranslationDatabase::class.java,
                    "translation_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 