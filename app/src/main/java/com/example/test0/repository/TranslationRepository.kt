package com.example.test0.repository

import android.content.Context
import com.example.test0.database.TranslationDao
import com.example.test0.database.TranslationDatabase
import com.example.test0.model.TranslationRecord
import com.example.test0.model.TranslationType
import kotlinx.coroutines.flow.Flow

class TranslationRepository private constructor(private val translationDao: TranslationDao) {
    
    companion object {
        @Volatile
        private var INSTANCE: TranslationRepository? = null
        private const val MAX_RECORDS = 100
        
        fun getInstance(context: Context): TranslationRepository {
            return INSTANCE ?: synchronized(this) {
                val database = TranslationDatabase.getDatabase(context)
                val instance = TranslationRepository(database.translationDao())
                INSTANCE = instance
                instance
            }
        }
    }
    
    fun getAllRecords(): Flow<List<TranslationRecord>> = translationDao.getAllRecords()
    
    fun getRecordsByType(type: TranslationType): Flow<List<TranslationRecord>> = 
        translationDao.getRecordsByType(type.value)
    
    suspend fun saveRecord(
        sourceText: String,
        translatedText: String,
        sourceLanguage: String,
        targetLanguage: String,
        type: TranslationType
    ) {
        // 检查记录数量，如果超过最大限制，删除最旧的记录
        val currentCount = translationDao.getRecordCount()
        if (currentCount >= MAX_RECORDS) {
            translationDao.deleteOldestRecord()
        }
        
        // 插入新记录
        val record = TranslationRecord(
            sourceText = sourceText,
            translatedText = translatedText,
            sourceLanguage = sourceLanguage,
            targetLanguage = targetLanguage,
            type = type.value
        )
        translationDao.insertRecord(record)
    }
    
    suspend fun deleteRecord(record: TranslationRecord) {
        translationDao.deleteRecord(record)
    }
    
    suspend fun deleteAllRecords() {
        translationDao.deleteAllRecords()
    }
} 