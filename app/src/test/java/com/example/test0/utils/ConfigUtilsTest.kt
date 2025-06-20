package com.example.test0.utils

import org.junit.Test
import org.junit.Assert.*

/**
 * 4.2.1.2 基础函数测试 - 配置工具类测试
 * 测试配置相关的基础功能，包括配置验证、数据处理等核心函数
 */
class ConfigUtilsTest {

    // 模拟配置数据类
    data class TestConfig(
        val appId: String,
        val secretId: String,
        val secretKey: String,
        val region: String
    ) {
        fun isValid(): Boolean {
            return appId.isNotBlank() && 
                   secretId.isNotBlank() && 
                   secretKey.isNotBlank() && 
                   region.isNotBlank()
        }
        
        fun isSkipped(): Boolean {
            return appId == "SKIPPED" && 
                   secretId == "SKIPPED" && 
                   secretKey == "SKIPPED"
        }
    }

    // 测试数据
    private val validConfig = TestConfig(
        appId = "test_app_id_12345",
        secretId = "test_secret_id_67890",
        secretKey = "test_secret_key_abcdef",
        region = "ap-guangzhou"
    )

    private val skipConfig = TestConfig(
        appId = "SKIPPED",
        secretId = "SKIPPED",
        secretKey = "SKIPPED",
        region = "ap-guangzhou"
    )

    @Test
    fun `测试配置数据验证_有效配置`() {
        // 测试有效配置返回true
        assertTrue("有效配置应该通过验证", validConfig.isValid())
        
        // 测试各个字段都不为空
        assertNotEquals("", validConfig.appId)
        assertNotEquals("", validConfig.secretId)
        assertNotEquals("", validConfig.secretKey)
        assertNotEquals("", validConfig.region)
    }

    @Test
    fun `测试配置数据验证_无效配置`() {
        // 测试空字段配置
        val emptyAppIdConfig = TestConfig("", "secret", "key", "region")
        assertFalse("空AppId配置应该验证失败", emptyAppIdConfig.isValid())
        
        val emptySecretIdConfig = TestConfig("app", "", "key", "region")
        assertFalse("空SecretId配置应该验证失败", emptySecretIdConfig.isValid())
        
        val emptySecretKeyConfig = TestConfig("app", "secret", "", "region")
        assertFalse("空SecretKey配置应该验证失败", emptySecretKeyConfig.isValid())
        
        // 测试全空配置
        val allEmptyConfig = TestConfig("", "", "", "")
        assertFalse("全空配置应该验证失败", allEmptyConfig.isValid())
    }

    @Test
    fun `测试跳过配置识别`() {
        // 测试跳过配置返回true
        assertTrue("跳过配置应该被正确识别", skipConfig.isSkipped())
        
        // 测试正常配置不是跳过配置
        assertFalse("正常配置不应该被识别为跳过配置", validConfig.isSkipped())
        
        // 测试部分SKIPPED的配置
        val partialSkipConfig = TestConfig("SKIPPED", "real_secret", "real_key", "ap-guangzhou")
        assertFalse("部分跳过配置不应该被识别为完全跳过", partialSkipConfig.isSkipped())
    }

    @Test
    fun `测试支持的区域列表`() {
        val regions = getSupportedRegions()

        // 验证包含主要区域
        assertTrue("应该包含广州区域", regions.any { it.first == "ap-guangzhou" && it.second == "广州（华南）" })
        assertTrue("应该包含北京区域", regions.any { it.first == "ap-beijing" && it.second == "北京（华北）" })
        assertTrue("应该包含上海区域", regions.any { it.first == "ap-shanghai" && it.second == "上海（华东）" })
        
        // 验证区域数量合理
        assertTrue("区域数量应该大于5", regions.size > 5)
    }

    @Test
    fun `测试配置对象相等性`() {
        // 测试相同配置相等
        val config1 = TestConfig("app1", "secret1", "key1", "region1")
        val config2 = TestConfig("app1", "secret1", "key1", "region1")
        assertEquals("相同配置应该相等", config1, config2)

        // 测试不同配置不相等
        val config3 = TestConfig("app2", "secret1", "key1", "region1")
        assertNotEquals("不同配置不应该相等", config1, config3)
    }

    @Test
    fun `测试区域代码验证`() {
        val supportedRegions = getSupportedRegions()
        val supportedCodes = supportedRegions.map { it.first }

        // 测试所有支持的区域代码都能创建有效配置
        supportedCodes.forEach { regionCode ->
            val config = TestConfig("test_app", "test_secret", "test_key", regionCode)
            assertTrue("区域代码 $regionCode 应该能创建有效配置", config.isValid())
        }
    }

    @Test
    fun `测试配置数据复制功能`() {
        // 测试配置数据可以正确复制
        val originalConfig = validConfig
        val copiedConfig = originalConfig.copy(appId = "new_app_id")
        
        // 验证复制后的变化
        assertEquals("new_app_id", copiedConfig.appId)
        assertEquals(originalConfig.secretId, copiedConfig.secretId)
        assertEquals(originalConfig.secretKey, copiedConfig.secretKey)
        assertEquals(originalConfig.region, copiedConfig.region)
        
        // 验证原配置未被修改
        assertNotEquals(originalConfig.appId, copiedConfig.appId)
    }

    @Test
    fun `测试边界条件_长字符串配置`() {
        // 测试长字符串配置
        val longString = "a".repeat(1000)
        val longConfig = TestConfig(longString, longString, longString, "ap-guangzhou")
        
        // 长字符串配置应该仍然有效（只要不为空）
        assertTrue("长字符串配置应该有效", longConfig.isValid())
    }

    @Test
    fun `测试边界条件_特殊字符配置`() {
        // 测试包含特殊字符的配置
        val specialConfig = TestConfig(
            "app-id_123", 
            "secret.id@test", 
            "key#with\$special%chars", 
            "ap-guangzhou"
        )
        
        assertTrue("特殊字符配置应该有效", specialConfig.isValid())
    }

    // 辅助函数：获取支持的区域列表
    private fun getSupportedRegions(): List<Pair<String, String>> {
        return listOf(
            "ap-guangzhou" to "广州（华南）",
            "ap-beijing" to "北京（华北）",
            "ap-shanghai" to "上海（华东）",
            "ap-chengdu" to "成都（西南）",
            "ap-singapore" to "新加坡",
            "ap-hongkong" to "香港",
            "ap-tokyo" to "东京",
            "ap-seoul" to "首尔"
        )
    }
} 