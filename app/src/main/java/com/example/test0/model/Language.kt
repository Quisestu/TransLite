package com.example.test0.model

import com.google.mlkit.nl.translate.TranslateLanguage

enum class Language(val code: String, val displayName: String) {
    ENGLISH(TranslateLanguage.ENGLISH, "English"),
    CHINESE(TranslateLanguage.CHINESE, "中文"),
    FRENCH(TranslateLanguage.FRENCH, "Français"),
    GERMAN(TranslateLanguage.GERMAN, "Deutsch"),
    JAPANESE(TranslateLanguage.JAPANESE, "日本語"),
    KOREAN(TranslateLanguage.KOREAN, "한국어"),
    SPANISH(TranslateLanguage.SPANISH, "Español"),
    RUSSIAN(TranslateLanguage.RUSSIAN, "Русский"),
    ARABIC(TranslateLanguage.ARABIC, "العربية"),
    ITALIAN(TranslateLanguage.ITALIAN, "Italiano"),
    HINDI(TranslateLanguage.HINDI, "हिन्दी"),
    PORTUGUESE(TranslateLanguage.PORTUGUESE, "Português");

    companion object {
        fun fromCode(code: String): Language {
            return values().find { it.code == code } ?: ENGLISH
        }
        
        fun getAllLanguages(): List<Language> = values().toList()
    }
} 