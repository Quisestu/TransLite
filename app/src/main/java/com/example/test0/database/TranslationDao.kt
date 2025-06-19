package com.example.test0.database

import androidx.room.*
import com.example.test0.model.TranslationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    
    @Query("SELECT * FROM translation_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<TranslationRecord>>
    
    @Query("SELECT * FROM translation_records WHERE type = :type ORDER BY timestamp DESC")
    fun getRecordsByType(type: String): Flow<List<TranslationRecord>>
    
    @Insert
    suspend fun insertRecord(record: TranslationRecord)
    
    @Delete
    suspend fun deleteRecord(record: TranslationRecord)
    
    @Query("DELETE FROM translation_records")
    suspend fun deleteAllRecords()
    
    @Query("SELECT COUNT(*) FROM translation_records")
    suspend fun getRecordCount(): Int
    
    @Query("DELETE FROM translation_records WHERE id IN (SELECT id FROM translation_records ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestRecords(count: Int)
    
    @Query("DELETE FROM translation_records WHERE id = (SELECT id FROM translation_records ORDER BY timestamp ASC LIMIT 1)")
    suspend fun deleteOldestRecord()
} 