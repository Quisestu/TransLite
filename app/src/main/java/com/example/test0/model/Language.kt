package com.example.test0.model

enum class Language(val code: String, val displayName: String) {
    AUTO("auto", "自动检测"),
    CHINESE("zh", "中文"),
    ENGLISH("en", "英语"),
    JAPANESE("ja", "日语"),
    KOREAN("ko", "韩语"),
    FRENCH("fr", "法语"),
    GERMAN("de", "德语"),
    RUSSIAN("ru", "俄语"),
    SPANISH("es", "西班牙语"),
    ITALIAN("it", "意大利语"),
    PORTUGUESE("pt", "葡萄牙语"),
    TURKISH("tr", "土耳其语"),
    ARABIC("ar", "阿拉伯语"),
    THAI("th", "泰语"),
    VIETNAMESE("vi", "越南语"),
    INDONESIAN("id", "印尼语"),
    MALAY("ms", "马来语");
    
    companion object {
        private val codeMapping = mapOf(
            "jp" to "ja",
            "kr" to "ko"
        )

        fun fromCode(code: String): Language {
            val normalizedCode = codeMapping[code] ?: code
            return values().find { it.code == normalizedCode } ?: AUTO
        }
        
        fun getAllLanguages(): List<Language> = values().toList()
        
        // 获取支持的目标语言列表
        fun getTargetLanguages(sourceLanguage: Language): List<Language> {
            return when (sourceLanguage) {
                AUTO -> getAllLanguages().filter { it != AUTO }
                CHINESE -> listOf(ENGLISH, JAPANESE, KOREAN, FRENCH, SPANISH, ITALIAN, RUSSIAN, PORTUGUESE, GERMAN, THAI, VIETNAMESE, INDONESIAN, MALAY, ARABIC, TURKISH)
                ENGLISH -> listOf(CHINESE, JAPANESE, KOREAN, FRENCH, SPANISH, ITALIAN, RUSSIAN, PORTUGUESE, GERMAN, THAI, VIETNAMESE, INDONESIAN, MALAY, ARABIC, TURKISH)
                JAPANESE -> listOf(CHINESE, ENGLISH, KOREAN)
                KOREAN -> listOf(CHINESE, ENGLISH, JAPANESE)
                FRENCH -> listOf(CHINESE, ENGLISH, SPANISH, ITALIAN, GERMAN, TURKISH, RUSSIAN, PORTUGUESE)
                SPANISH -> listOf(CHINESE, ENGLISH, FRENCH, ITALIAN, GERMAN, TURKISH, RUSSIAN, PORTUGUESE)
                ITALIAN -> listOf(CHINESE, ENGLISH, FRENCH, SPANISH, GERMAN, TURKISH, RUSSIAN, PORTUGUESE)
                GERMAN -> listOf(CHINESE, ENGLISH, FRENCH, SPANISH, ITALIAN, TURKISH, RUSSIAN, PORTUGUESE)
                TURKISH -> listOf(CHINESE, ENGLISH, FRENCH, SPANISH, ITALIAN, GERMAN, RUSSIAN, PORTUGUESE)
                RUSSIAN -> listOf(CHINESE, ENGLISH, FRENCH, SPANISH, ITALIAN, GERMAN, TURKISH, PORTUGUESE)
                PORTUGUESE -> listOf(CHINESE, ENGLISH, FRENCH, SPANISH, ITALIAN, GERMAN, TURKISH, RUSSIAN)
                THAI -> listOf(CHINESE, ENGLISH)
                VIETNAMESE -> listOf(CHINESE, ENGLISH)
                INDONESIAN -> listOf(CHINESE, ENGLISH)
                MALAY -> listOf(CHINESE, ENGLISH)
                ARABIC -> listOf(CHINESE, ENGLISH)
            

              
            }
        }

        // 获取带自动识别状态的显示名称
        fun getDisplayNameWithAutoDetect(language: Language, isAutoDetected: Boolean): String {
            return if (isAutoDetected) "${language.displayName}（自动识别）" else language.displayName
        }
    }
}

fun Language.getDisplayNameWithDetected(isDetected: Boolean): String {
    return if (isDetected) "${this.displayName}(已检测)" else this.displayName
} 