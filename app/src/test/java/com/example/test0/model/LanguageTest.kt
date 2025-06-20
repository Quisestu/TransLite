package com.example.test0.model

import org.junit.Test
import org.junit.Assert.*

/**
 * 4.2.1.1 数据模型测试 - Language枚举类测试
 * 测试语言数据模型的各项功能，包括代码映射、显示名称、目标语言获取等
 */
class LanguageTest {

    @Test
    fun `测试语言枚举基本属性`() {
        // 测试中文语言属性
        assertEquals("zh", Language.CHINESE.code)
        assertEquals("中文", Language.CHINESE.displayName)
        
        // 测试英文语言属性
        assertEquals("en", Language.ENGLISH.code)
        assertEquals("英语", Language.ENGLISH.displayName)
        
        // 测试自动检测属性
        assertEquals("auto", Language.AUTO.code)
        assertEquals("自动检测", Language.AUTO.displayName)
    }

    @Test
    fun `测试fromCode方法正常代码映射`() {
        // 测试正常代码映射
        assertEquals(Language.CHINESE, Language.fromCode("zh"))
        assertEquals(Language.ENGLISH, Language.fromCode("en"))
        assertEquals(Language.JAPANESE, Language.fromCode("ja"))
        assertEquals(Language.KOREAN, Language.fromCode("ko"))
    }

    @Test
    fun `测试fromCode方法特殊代码映射`() {
        // 测试特殊代码映射 jp -> ja, kr -> ko
        assertEquals(Language.JAPANESE, Language.fromCode("jp"))
        assertEquals(Language.KOREAN, Language.fromCode("kr"))
    }

    @Test
    fun `测试fromCode方法无效代码返回AUTO`() {
        // 测试无效代码返回AUTO
        assertEquals(Language.AUTO, Language.fromCode("invalid"))
        assertEquals(Language.AUTO, Language.fromCode(""))
        assertEquals(Language.AUTO, Language.fromCode("xyz"))
    }

    @Test
    fun `测试getAllLanguages方法`() {
        val allLanguages = Language.getAllLanguages()
        
        // 验证包含所有预期语言
        assertTrue(allLanguages.contains(Language.AUTO))
        assertTrue(allLanguages.contains(Language.CHINESE))
        assertTrue(allLanguages.contains(Language.ENGLISH))
        assertTrue(allLanguages.contains(Language.JAPANESE))
        
        // 验证语言数量
        assertEquals(17, allLanguages.size)
    }

    @Test
    fun `测试getTargetLanguages方法_自动检测源语言`() {
        val targetLanguages = Language.getTargetLanguages(Language.AUTO)
        
        // 自动检测不应包含在目标语言中
        assertFalse(targetLanguages.contains(Language.AUTO))
        
        // 应该包含其他所有语言
        assertTrue(targetLanguages.contains(Language.CHINESE))
        assertTrue(targetLanguages.contains(Language.ENGLISH))
        assertTrue(targetLanguages.contains(Language.JAPANESE))
        
        // 验证数量 = 总数 - 1（排除AUTO）
        assertEquals(16, targetLanguages.size)
    }

    @Test
    fun `测试getTargetLanguages方法_中文源语言`() {
        val targetLanguages = Language.getTargetLanguages(Language.CHINESE)
        
        // 应该包含主要目标语言
        assertTrue(targetLanguages.contains(Language.ENGLISH))
        assertTrue(targetLanguages.contains(Language.JAPANESE))
        assertTrue(targetLanguages.contains(Language.KOREAN))
        assertTrue(targetLanguages.contains(Language.FRENCH))
        
        // 不应该包含自己
        assertFalse(targetLanguages.contains(Language.CHINESE))
        assertFalse(targetLanguages.contains(Language.AUTO))
    }

    @Test
    fun `测试getTargetLanguages方法_英文源语言`() {
        val targetLanguages = Language.getTargetLanguages(Language.ENGLISH)
        
        // 应该包含主要目标语言
        assertTrue(targetLanguages.contains(Language.CHINESE))
        assertTrue(targetLanguages.contains(Language.JAPANESE))
        assertTrue(targetLanguages.contains(Language.KOREAN))
        
        // 不应该包含自己
        assertFalse(targetLanguages.contains(Language.ENGLISH))
    }

    @Test
    fun `测试getTargetLanguages方法_小语种支持`() {
        // 测试泰语只支持中英互译
        val thaiTargets = Language.getTargetLanguages(Language.THAI)
        assertEquals(2, thaiTargets.size)
        assertTrue(thaiTargets.contains(Language.CHINESE))
        assertTrue(thaiTargets.contains(Language.ENGLISH))
        
        // 测试越南语只支持中英互译
        val vietnameseTargets = Language.getTargetLanguages(Language.VIETNAMESE)
        assertEquals(2, vietnameseTargets.size)
        assertTrue(vietnameseTargets.contains(Language.CHINESE))
        assertTrue(vietnameseTargets.contains(Language.ENGLISH))
    }

    @Test
    fun `测试getDisplayNameWithAutoDetect方法`() {
        // 测试自动检测状态显示
        val withAutoDetect = Language.getDisplayNameWithAutoDetect(Language.CHINESE, true)
        assertEquals("中文（自动识别）", withAutoDetect)
        
        // 测试非自动检测状态显示
        val withoutAutoDetect = Language.getDisplayNameWithAutoDetect(Language.CHINESE, false)
        assertEquals("中文", withoutAutoDetect)
        
        // 测试英语自动检测
        val englishAutoDetect = Language.getDisplayNameWithAutoDetect(Language.ENGLISH, true)
        assertEquals("英语（自动识别）", englishAutoDetect)
    }

    @Test
    fun `测试扩展函数getDisplayNameWithDetected`() {
        // 测试已检测状态显示
        val detectedName = Language.CHINESE.getDisplayNameWithDetected(true)
        assertEquals("中文(已检测)", detectedName)
        
        // 测试未检测状态显示
        val normalName = Language.CHINESE.getDisplayNameWithDetected(false)
        assertEquals("中文", normalName)
        
        // 测试其他语言
        val japaneseDetected = Language.JAPANESE.getDisplayNameWithDetected(true)
        assertEquals("日语(已检测)", japaneseDetected)
    }

    @Test
    fun `测试语言代码的唯一性`() {
        val allLanguages = Language.getAllLanguages()
        val codes = allLanguages.map { it.code }
        
        // 验证代码无重复
        assertEquals(codes.size, codes.distinct().size)
    }

    @Test
    fun `测试语言显示名称的唯一性`() {
        val allLanguages = Language.getAllLanguages()
        val displayNames = allLanguages.map { it.displayName }
        
        // 验证显示名称无重复
        assertEquals(displayNames.size, displayNames.distinct().size)
    }

    @Test
    fun `测试双向翻译支持`() {
        // 验证中英互译支持
        val chineseTargets = Language.getTargetLanguages(Language.CHINESE)
        val englishTargets = Language.getTargetLanguages(Language.ENGLISH)
        
        assertTrue(chineseTargets.contains(Language.ENGLISH))
        assertTrue(englishTargets.contains(Language.CHINESE))
        
        // 验证中日互译支持
        val japaneseTargets = Language.getTargetLanguages(Language.JAPANESE)
        assertTrue(chineseTargets.contains(Language.JAPANESE))
        assertTrue(japaneseTargets.contains(Language.CHINESE))
    }
} 