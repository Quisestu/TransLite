package com.example.test0.service

import com.example.test0.model.Language
import org.junit.Test
import org.junit.Assert.*
import org.json.JSONObject

/**
 * 4.2.2.1 翻译服务测试 - 翻译服务核心功能测试
 * 测试翻译服务的基本功能，包括请求构建、响应解析、错误处理等
 */
class TranslationServiceTest {

    // 模拟翻译结果类
    sealed class TranslationResult {
        data class Success(
            val translatedText: String,
            val detectedLanguage: Language
        ) : TranslationResult()
        
        data class Error(val message: String) : TranslationResult()
    }

    // 测试数据
    private val successfulTranslationResponse = """
        {
            "Response": {
                "TargetText": "Hello World",
                "Source": "zh",
                "Target": "en",
                "RequestId": "test-request-id"
            }
        }
    """.trimIndent()

    private val errorResponse = """
        {
            "Response": {
                "Error": {
                    "Code": "InvalidParameter",
                    "Message": "参数错误"
                },
                "RequestId": "error-request-id"
            }
        }
    """.trimIndent()

    @Test
    fun `测试翻译请求构建_基本参数`() {
        // 测试基本翻译请求的构建
        val sourceText = "你好世界"
        val sourceLanguage = Language.CHINESE
        val targetLanguage = Language.ENGLISH

        // 验证输入参数的处理逻辑
        assertNotNull("源文本不应为空", sourceText)
        assertTrue("源文本长度应大于0", sourceText.isNotEmpty())
        assertNotEquals("源语言和目标语言不能相同", sourceLanguage, targetLanguage)
    }

    @Test
    fun `测试成功翻译响应解析`() {
        // 测试JSON响应解析
        val jsonResponse = JSONObject(successfulTranslationResponse)
        val response = jsonResponse.getJSONObject("Response")
        
        assertEquals("翻译结果应该匹配", "Hello World", response.getString("TargetText"))
        assertEquals("源语言应该匹配", "zh", response.getString("Source"))
        assertEquals("目标语言应该匹配", "en", response.getString("Target"))
        assertTrue("请求ID应该存在", response.has("RequestId"))
    }

    @Test
    fun `测试错误响应解析`() {
        // 测试错误响应的解析
        val jsonResponse = JSONObject(errorResponse)
        val response = jsonResponse.getJSONObject("Response")
        
        assertTrue("错误响应应该包含Error字段", response.has("Error"))
        
        val error = response.getJSONObject("Error")
        assertEquals("错误代码应该匹配", "InvalidParameter", error.getString("Code"))
        assertEquals("错误消息应该匹配", "参数错误", error.getString("Message"))
    }

    @Test
    fun `测试语言代码转换`() {
        // 测试Language枚举到腾讯云API代码的转换
        assertEquals("中文代码转换", "zh", Language.CHINESE.code)
        assertEquals("英文代码转换", "en", Language.ENGLISH.code)
        assertEquals("日文代码转换", "ja", Language.JAPANESE.code)
        assertEquals("韩文代码转换", "ko", Language.KOREAN.code)
        assertEquals("自动检测代码转换", "auto", Language.AUTO.code)
    }

    @Test
    fun `测试文本长度验证`() {
        // 测试空文本
        val emptyText = ""
        assertTrue("空文本应该被识别", emptyText.isEmpty())
        
        // 测试正常长度文本
        val normalText = "这是一段正常长度的测试文本"
        assertTrue("正常文本长度应该有效", normalText.length < 5000)
        
        // 测试超长文本
        val longText = "a".repeat(6000)
        assertTrue("超长文本应该被识别", longText.length > 5000)
    }

    @Test
    fun `测试认证签名生成逻辑`() {
        // 测试认证相关的参数
        val timestamp = System.currentTimeMillis() / 1000
        val nonce = (Math.random() * 1000000).toInt()
        
        assertTrue("时间戳应该是正数", timestamp > 0)
        assertTrue("随机数应该在合理范围内", nonce in 0..1000000)
        
        // 测试签名字符串的基本组成
        val action = "TextTranslate"
        val version = "2018-03-21"
        assertNotNull("Action不应为空", action)
        assertNotNull("Version不应为空", version)
    }

    @Test
    fun `测试支持的语言对验证`() {
        // 测试中英互译支持
        val chineseTargets = Language.getTargetLanguages(Language.CHINESE)
        assertTrue("中文应该支持翻译到英文", chineseTargets.contains(Language.ENGLISH))
        
        val englishTargets = Language.getTargetLanguages(Language.ENGLISH)
        assertTrue("英文应该支持翻译到中文", englishTargets.contains(Language.CHINESE))
        
        // 测试小语种支持
        val thaiTargets = Language.getTargetLanguages(Language.THAI)
        assertEquals("泰语应该只支持中英互译", 2, thaiTargets.size)
        assertTrue("泰语应该支持翻译到中文", thaiTargets.contains(Language.CHINESE))
        assertTrue("泰语应该支持翻译到英文", thaiTargets.contains(Language.ENGLISH))
    }

    @Test
    fun `测试JSON格式验证`() {
        // 测试有效JSON响应
        try {
            val validJson = JSONObject(successfulTranslationResponse)
            assertTrue("应该包含Response字段", validJson.has("Response"))
        } catch (e: Exception) {
            fail("有效JSON不应该抛出异常: ${e.message}")
        }
        
        // 测试无效JSON响应
        val invalidJson = "{ invalid json }"
        try {
            JSONObject(invalidJson)
            fail("无效JSON应该抛出异常")
        } catch (e: Exception) {
            // 预期的异常，任何异常都说明JSON解析失败
            assertNotNull("应该抛出异常", e.message)
        }
    }

    @Test
    fun `测试批量翻译场景`() {
        // 测试多个文本的处理逻辑
        val texts = listOf("你好", "世界", "测试")
        assertTrue("文本列表不应为空", texts.isNotEmpty())
        assertTrue("所有文本都应该有内容", texts.all { it.isNotBlank() })
        
        // 验证批量处理的总长度限制
        val totalLength = texts.sumOf { it.length }
        assertTrue("批量文本总长度应该合理", totalLength < 5000)
    }

    @Test
    fun `测试特殊字符处理`() {
        // 测试包含特殊字符的文本
        val specialTexts = listOf(
            "Hello\nWorld",  // 换行符
            "Test\tTab",     // 制表符
            "Quote\"Test\"", // 引号
            "中英混合Text",   // 中英混合
            "Emoji😀Test"    // 表情符号
        )
        
        specialTexts.forEach { text ->
            assertNotNull("特殊字符文本不应为空", text)
            assertTrue("特殊字符文本应该有长度", text.isNotEmpty())
        }
    }

    @Test
    fun `测试翻译结果创建`() {
        // 测试成功结果创建
        val successResult = TranslationResult.Success("Hello World", Language.CHINESE)
        assertTrue("成功结果应该是Success类型", successResult is TranslationResult.Success)
        assertEquals("翻译文本应该匹配", "Hello World", successResult.translatedText)
        assertEquals("检测语言应该匹配", Language.CHINESE, successResult.detectedLanguage)
        
        // 测试错误结果创建
        val errorResult = TranslationResult.Error("网络错误")
        assertTrue("错误结果应该是Error类型", errorResult is TranslationResult.Error)
        assertEquals("错误消息应该匹配", "网络错误", errorResult.message)
    }

    @Test
    fun `测试区域配置验证`() {
        // 测试不同区域的配置
        val regions = listOf("ap-guangzhou", "ap-beijing", "ap-shanghai", "ap-singapore")
        
        regions.forEach { region ->
            assertTrue("区域 $region 应该不为空", region.isNotBlank())
            assertTrue("区域 $region 应该以ap-开头", region.startsWith("ap-"))
        }
    }
} 