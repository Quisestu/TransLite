package com.example.test0.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_records")
data class TranslationRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val sourceText: String,
    val translatedText: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val type: String, // "text", "speech", "image"
    val timestamp: Long = System.currentTimeMillis()
)

enum class TranslationType(val value: String, val displayName: String) {
    TEXT("text", "文本翻译"),
    SPEECH("speech", "语音翻译"),
    IMAGE("image", "图片翻译");
    
    companion object {
        fun fromValue(value: String): TranslationType? {
            return values().find { it.value == value }
        }
    }
} 