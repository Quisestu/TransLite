package com.example.test0.business

import com.example.test0.model.Language
import org.junit.Test
import org.junit.Assert.*

/**
 * 4.2.3 业务逻辑测试 - 翻译业务流程测试
 * 测试完整的翻译业务流程，包括文本处理、语言检测、翻译执行、结果处理等
 */
class TranslationBusinessTest {

    // 模拟翻译记录类
    data class TranslationRecord(
        val id: Long = 0,
        val sourceText: String,
        val translatedText: String,
        val sourceLanguage: Language,
        val targetLanguage: Language,
        val timestamp: Long = System.currentTimeMillis(),
        val isFavorite: Boolean = false
    )

    // 模拟翻译业务逻辑类
    class TranslationBusiness {
        
        fun validateTranslationInput(
            sourceText: String,
            sourceLanguage: Language,
            targetLanguage: Language
        ): ValidationResult {
            // 验证源文本
            if (sourceText.isBlank()) {
                return ValidationResult.Error("源文本不能为空")
            }
            
            if (sourceText.length > 5000) {
                return ValidationResult.Error("文本长度不能超过5000字符")
            }
            
            // 验证语言组合
            if (sourceLanguage == targetLanguage && sourceLanguage != Language.AUTO) {
                return ValidationResult.Error("源语言和目标语言不能相同")
            }
            
            if (targetLanguage == Language.AUTO) {
                return ValidationResult.Error("目标语言不能是自动检测")
            }
            
            // 验证语言支持
            val supportedTargets = Language.getTargetLanguages(sourceLanguage)
            if (!supportedTargets.contains(targetLanguage)) {
                return ValidationResult.Error("不支持的语言组合")
            }
            
            return ValidationResult.Success
        }
        
        fun processTranslationText(text: String): String {
            return text.trim()
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\t", " ")
                .replace(Regex("\\s+"), " ")
        }
        
        fun detectLanguage(text: String): Language {
            // 简单的语言检测逻辑
            val chinesePattern = Regex("[\\u4e00-\\u9fff]")
            val englishPattern = Regex("[a-zA-Z]")
            val japanesePattern = Regex("[\\u3040-\\u309f\\u30a0-\\u30ff]")
            val koreanPattern = Regex("[\\uac00-\\ud7af]")
            
            return when {
                chinesePattern.containsMatchIn(text) -> Language.CHINESE
                japanesePattern.containsMatchIn(text) -> Language.JAPANESE
                koreanPattern.containsMatchIn(text) -> Language.KOREAN
                englishPattern.containsMatchIn(text) -> Language.ENGLISH
                else -> Language.AUTO
            }
        }
        
        fun createTranslationRecord(
            sourceText: String,
            translatedText: String,
            sourceLanguage: Language,
            targetLanguage: Language
        ): TranslationRecord {
            return TranslationRecord(
                sourceText = sourceText,
                translatedText = translatedText,
                sourceLanguage = sourceLanguage,
                targetLanguage = targetLanguage,
                timestamp = System.currentTimeMillis()
            )
        }
        
        fun shouldSaveToHistory(
            sourceText: String,
            translatedText: String
        ): Boolean {
            return sourceText.isNotBlank() && 
                   translatedText.isNotBlank() && 
                   sourceText != translatedText
        }
    }

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    private val translationBusiness = TranslationBusiness()

    @Test
    fun `测试翻译输入验证_有效输入`() {
        val result = translationBusiness.validateTranslationInput(
            sourceText = "Hello World",
            sourceLanguage = Language.ENGLISH,
            targetLanguage = Language.CHINESE
        )
        
        assertTrue("有效输入应该通过验证", result is ValidationResult.Success)
    }

    @Test
    fun `测试翻译输入验证_空文本`() {
        val result = translationBusiness.validateTranslationInput(
            sourceText = "",
            sourceLanguage = Language.ENGLISH,
            targetLanguage = Language.CHINESE
        )
        
        assertTrue("空文本应该验证失败", result is ValidationResult.Error)
        if (result is ValidationResult.Error) {
            assertEquals("源文本不能为空", result.message)
        }
    }

    @Test
    fun `测试翻译输入验证_超长文本`() {
        val longText = "a".repeat(6000)
        val result = translationBusiness.validateTranslationInput(
            sourceText = longText,
            sourceLanguage = Language.ENGLISH,
            targetLanguage = Language.CHINESE
        )
        
        assertTrue("超长文本应该验证失败", result is ValidationResult.Error)
        if (result is ValidationResult.Error) {
            assertEquals("文本长度不能超过5000字符", result.message)
        }
    }

    @Test
    fun `测试翻译输入验证_相同语言`() {
        val result = translationBusiness.validateTranslationInput(
            sourceText = "Hello",
            sourceLanguage = Language.ENGLISH,
            targetLanguage = Language.ENGLISH
        )
        
        assertTrue("相同语言应该验证失败", result is ValidationResult.Error)
        if (result is ValidationResult.Error) {
            assertEquals("源语言和目标语言不能相同", result.message)
        }
    }

    @Test
    fun `测试翻译输入验证_目标语言为自动检测`() {
        val result = translationBusiness.validateTranslationInput(
            sourceText = "Hello",
            sourceLanguage = Language.ENGLISH,
            targetLanguage = Language.AUTO
        )
        
        assertTrue("目标语言为AUTO应该验证失败", result is ValidationResult.Error)
        if (result is ValidationResult.Error) {
            assertEquals("目标语言不能是自动检测", result.message)
        }
    }

    @Test
    fun `测试文本预处理功能`() {
        // 测试基本文本处理
        val basicText = translationBusiness.processTranslationText("Hello World")
        assertEquals("Hello World", basicText)
        
        // 测试去除首尾空格
        val trimmedText = translationBusiness.processTranslationText("  Hello  ")
        assertEquals("Hello", trimmedText)
        
        // 测试空文本处理
        val emptyProcessed = translationBusiness.processTranslationText("")
        assertEquals("", emptyProcessed)
        
        // 测试非空文本不为null
        val nonEmptyText = translationBusiness.processTranslationText("Test")
        assertNotNull(nonEmptyText)
    }

    @Test
    fun `测试语言自动检测_中文文本`() {
        val chineseTexts = listOf("你好世界", "这是中文测试", "汉字识别")
        
        chineseTexts.forEach { text ->
            val detected = translationBusiness.detectLanguage(text)
            assertEquals("应该检测为中文", Language.CHINESE, detected)
        }
    }

    @Test
    fun `测试语言自动检测_英文文本`() {
        val englishTexts = listOf("Hello World", "This is English", "Language Detection")
        
        englishTexts.forEach { text ->
            val detected = translationBusiness.detectLanguage(text)
            assertEquals("应该检测为英文", Language.ENGLISH, detected)
        }
    }

    @Test
    fun `测试语言自动检测_日文文本`() {
        val japaneseTexts = listOf("こんにちは", "ひらがな", "カタカナ")
        
        japaneseTexts.forEach { text ->
            val detected = translationBusiness.detectLanguage(text)
            assertEquals("应该检测为日文", Language.JAPANESE, detected)
        }
    }

    @Test
    fun `测试语言自动检测_韩文文本`() {
        val koreanTexts = listOf("안녕하세요", "한국어", "테스트")
        
        koreanTexts.forEach { text ->
            val detected = translationBusiness.detectLanguage(text)
            assertEquals("应该检测为韩文", Language.KOREAN, detected)
        }
    }

    @Test
    fun `测试语言自动检测_混合文本`() {
        // 中英混合，应该检测为中文（因为包含中文字符）
        val mixedText = "Hello 世界"
        val detected = translationBusiness.detectLanguage(mixedText)
        assertEquals("中英混合应该检测为中文", Language.CHINESE, detected)
    }

    @Test
    fun `测试翻译记录创建`() {
        val record = translationBusiness.createTranslationRecord(
            sourceText = "Hello",
            translatedText = "你好",
            sourceLanguage = Language.ENGLISH,
            targetLanguage = Language.CHINESE
        )
        
        assertEquals("Hello", record.sourceText)
        assertEquals("你好", record.translatedText)
        assertEquals(Language.ENGLISH, record.sourceLanguage)
        assertEquals(Language.CHINESE, record.targetLanguage)
        assertFalse(record.isFavorite)
        assertTrue("时间戳应该大于0", record.timestamp > 0)
    }

    @Test
    fun `测试历史记录保存判断_有效翻译`() {
        val shouldSave = translationBusiness.shouldSaveToHistory(
            sourceText = "Hello",
            translatedText = "你好"
        )
        
        assertTrue("有效翻译应该保存到历史", shouldSave)
    }

    @Test
    fun `测试历史记录保存判断_空文本`() {
        val shouldSave1 = translationBusiness.shouldSaveToHistory(
            sourceText = "",
            translatedText = "你好"
        )
        assertFalse("空源文本不应该保存", shouldSave1)
        
        val shouldSave2 = translationBusiness.shouldSaveToHistory(
            sourceText = "Hello",
            translatedText = ""
        )
        assertFalse("空翻译结果不应该保存", shouldSave2)
    }

    @Test
    fun `测试历史记录保存判断_相同文本`() {
        val shouldSave = translationBusiness.shouldSaveToHistory(
            sourceText = "Hello",
            translatedText = "Hello"
        )
        
        assertFalse("相同文本不应该保存", shouldSave)
    }

    @Test
    fun `测试完整翻译业务流程`() {
        val sourceText = "  Hello World  "
        val targetLanguage = Language.CHINESE
        
        // 1. 预处理文本
        val processedText = translationBusiness.processTranslationText(sourceText)
        assertEquals("Hello World", processedText)
        
        // 2. 检测语言
        val detectedLanguage = translationBusiness.detectLanguage(processedText)
        assertEquals(Language.ENGLISH, detectedLanguage)
        
        // 3. 验证输入
        val validationResult = translationBusiness.validateTranslationInput(
            processedText, detectedLanguage, targetLanguage
        )
        assertTrue("验证应该通过", validationResult is ValidationResult.Success)
        
        // 4. 模拟翻译结果
        val translatedText = "你好世界"
        
        // 5. 创建记录
        val record = translationBusiness.createTranslationRecord(
            processedText, translatedText, detectedLanguage, targetLanguage
        )
        
        assertEquals("Hello World", record.sourceText)
        assertEquals("你好世界", record.translatedText)
        assertEquals(Language.ENGLISH, record.sourceLanguage)
        assertEquals(Language.CHINESE, record.targetLanguage)
        
        // 6. 判断是否保存历史
        val shouldSave = translationBusiness.shouldSaveToHistory(
            record.sourceText, record.translatedText
        )
        assertTrue("应该保存到历史记录", shouldSave)
    }

    @Test
    fun `测试边界条件处理`() {
        // 测试单字符文本
        val singleChar = "A"
        val result1 = translationBusiness.validateTranslationInput(
            singleChar, Language.ENGLISH, Language.CHINESE
        )
        assertTrue("单字符应该有效", result1 is ValidationResult.Success)
        
        // 测试最大长度文本
        val maxLengthText = "a".repeat(5000)
        val result2 = translationBusiness.validateTranslationInput(
            maxLengthText, Language.ENGLISH, Language.CHINESE
        )
        assertTrue("最大长度文本应该有效", result2 is ValidationResult.Success)
        
        // 测试超出最大长度
        val overLengthText = "a".repeat(5001)
        val result3 = translationBusiness.validateTranslationInput(
            overLengthText, Language.ENGLISH, Language.CHINESE
        )
        assertTrue("超长文本应该无效", result3 is ValidationResult.Error)
    }
} 