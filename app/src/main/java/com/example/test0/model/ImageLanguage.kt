package com.example.test0.model

enum class ImageLanguage(
    val code: String,
    val displayName: String,
    val isHidden: Boolean = false
) {
    AUTO("auto", "自动检测"),
    CHINESE("zh", "中文"),
    ENGLISH("en", "英语"),
    JAPANESE("ja", "日语"),
    KOREAN("ko", "韩语"),
    RUSSIAN("ru", "俄语"),
    FRENCH("fr", "法语"),
    GERMAN("de", "德语"),
    ITALIAN("it", "意大利语"),
    SPANISH("es", "西班牙语"),
    PORTUGUESE("pt", "葡萄牙语"),
    MALAY("ms", "马来语"),
    THAI("th", "泰语"),
    VIETNAMESE("vi", "越南语"),
    
    // 隐藏的检测项，用户无法手动选择
    DETECTED_CHINESE("zh-detected", "中文（已检测）", isHidden = true),
    DETECTED_ENGLISH("en-detected", "英语（已检测）", isHidden = true);

    companion object {
        fun fromCode(code: String): ImageLanguage {
            return values().find { it.code == code } ?: AUTO
        }

        fun getAllSourceLanguages(): List<ImageLanguage> {
            return listOf(
                AUTO, CHINESE, ENGLISH, JAPANESE, KOREAN, RUSSIAN,
                FRENCH, GERMAN, ITALIAN, SPANISH, PORTUGUESE, 
                MALAY, THAI, VIETNAMESE
            ).filter { !it.isHidden }
        }

        fun getTargetLanguages(sourceLanguage: ImageLanguage): List<ImageLanguage> {
            return when (sourceLanguage) {
                AUTO -> listOf(CHINESE, ENGLISH) // 限制为中英文
                CHINESE -> listOf(
                    ENGLISH, JAPANESE, KOREAN, RUSSIAN, FRENCH, GERMAN,
                    ITALIAN, SPANISH, PORTUGUESE, MALAY, THAI, VIETNAMESE
                )
                ENGLISH -> listOf(
                    CHINESE, JAPANESE, KOREAN, RUSSIAN, FRENCH, GERMAN,
                    ITALIAN, SPANISH, PORTUGUESE, MALAY, THAI, VIETNAMESE
                )
                // 检测到的中文：目标语言包含中文本身
                DETECTED_CHINESE -> listOf(
                    CHINESE, ENGLISH, JAPANESE, KOREAN, RUSSIAN, FRENCH, GERMAN,
                    ITALIAN, SPANISH, PORTUGUESE, MALAY, THAI, VIETNAMESE
                )
                // 检测到的英文：目标语言包含英文本身
                DETECTED_ENGLISH -> listOf(
                    ENGLISH, CHINESE, JAPANESE, KOREAN, RUSSIAN, FRENCH, GERMAN,
                    ITALIAN, SPANISH, PORTUGUESE, MALAY, THAI, VIETNAMESE
                )
                JAPANESE -> listOf(CHINESE, ENGLISH, KOREAN)
                KOREAN -> listOf(CHINESE, ENGLISH, JAPANESE)
                RUSSIAN, FRENCH, GERMAN, ITALIAN, SPANISH, 
                PORTUGUESE, MALAY, THAI, VIETNAMESE -> listOf(CHINESE, ENGLISH)
            }.filter { !it.isHidden }
        }
        
        // 获取检测对应的隐藏项
        fun getDetectedLanguage(detectedCode: String): ImageLanguage? {
            return when (detectedCode) {
                "zh" -> DETECTED_CHINESE
                "en" -> DETECTED_ENGLISH
                else -> null
            }
        }
    }
}

// 扩展函数用于显示检测到的语言
fun ImageLanguage.getDisplayNameWithDetected(isDetected: Boolean): String {
    return if (isDetected && this != ImageLanguage.AUTO) {
        "$displayName（已检测）"
    } else {
        displayName
    }
}