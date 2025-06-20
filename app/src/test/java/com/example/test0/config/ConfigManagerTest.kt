package com.example.test0.config

import android.content.Context
import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class ConfigManagerTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Mocks
    private lateinit var mockContext: Context
    private lateinit var mockSharedPreferences: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    private lateinit var configManager: ConfigManager

    // Test data
    private val testConfig = ConfigManager.TencentConfigData(
        appId = "test_app_id",
        secretId = "test_secret_id",
        secretKey = "test_secret_key",
        region = "ap-guangzhou"
    )

    private val skipConfig = ConfigManager.TencentConfigData(
        appId = "SKIPPED",
        secretId = "SKIPPED", 
        secretKey = "SKIPPED",
        region = "ap-guangzhou"
    )

    @Before
    fun setUp() {
        mockContext = mockk(relaxed = true)
        mockSharedPreferences = mockk(relaxed = true)
        mockEditor = mockk(relaxed = true)

        // Setup SharedPreferences mocks
        every { mockContext.getSharedPreferences(any(), any()) } returns mockSharedPreferences
        every { mockSharedPreferences.edit() } returns mockEditor
        every { mockEditor.putString(any(), any()) } returns mockEditor
        every { mockEditor.apply() } just Runs
        every { mockEditor.clear() } returns mockEditor

        configManager = ConfigManager(mockContext)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `TencentConfigData isValid returns true for valid config`() {
        assertTrue(testConfig.isValid())
    }

    @Test
    fun `TencentConfigData isValid returns false for invalid config`() {
        val invalidConfig = ConfigManager.TencentConfigData("", "", "", "ap-guangzhou")
        assertFalse(invalidConfig.isValid())
    }

    @Test
    fun `TencentConfigData isSkipped returns true for skip config`() {
        assertTrue(skipConfig.isSkipped())
    }

    @Test
    fun `TencentConfigData isSkipped returns false for normal config`() {
        assertFalse(testConfig.isSkipped())
    }

    @Test
    fun `saveUserConfig saves data to SharedPreferences`() {
        configManager.saveUserConfig(testConfig)

        verify {
            mockEditor.putString("app_id", testConfig.appId)
            mockEditor.putString("secret_id", testConfig.secretId)
            mockEditor.putString("secret_key", testConfig.secretKey)
            mockEditor.putString("region", testConfig.region)
            mockEditor.apply()
        }
    }

    @Test
    fun `getUserConfig returns saved configuration`() = runBlockingTest {
        // Setup mock to return test data
        every { mockSharedPreferences.getString("app_id", "") } returns testConfig.appId
        every { mockSharedPreferences.getString("secret_id", "") } returns testConfig.secretId
        every { mockSharedPreferences.getString("secret_key", "") } returns testConfig.secretKey
        every { mockSharedPreferences.getString("region", "ap-guangzhou") } returns testConfig.region

        val result = configManager.getUserConfig().first()

        assertEquals(testConfig, result)
    }

    @Test
    fun `getUserConfig returns empty config when no data saved`() = runBlockingTest {
        // Setup mock to return empty strings
        every { mockSharedPreferences.getString(any(), any()) } returns ""
        every { mockSharedPreferences.getString("region", "ap-guangzhou") } returns "ap-guangzhou"

        val result = configManager.getUserConfig().first()

        assertEquals("", result.appId)
        assertEquals("", result.secretId)
        assertEquals("", result.secretKey)
        assertEquals("ap-guangzhou", result.region)
    }

    @Test
    fun `clearUserConfig clears SharedPreferences`() {
        configManager.clearUserConfig()

        verify {
            mockEditor.clear()
            mockEditor.apply()
        }
    }

    @Test
    fun `getSupportedRegions returns expected regions`() {
        val regions = configManager.getSupportedRegions()

        assertTrue(regions.isNotEmpty())
        assertTrue(regions.any { it.first == "ap-guangzhou" && it.second == "广州（华南）" })
        assertTrue(regions.any { it.first == "ap-beijing" && it.second == "北京（华北）" })
        assertTrue(regions.any { it.first == "ap-shanghai" && it.second == "上海（华东）" })
    }

    @Test
    fun `getConfig returns user config when valid`() = runBlockingTest {
        // Setup mock to return valid user config
        every { mockSharedPreferences.getString("app_id", "") } returns testConfig.appId
        every { mockSharedPreferences.getString("secret_id", "") } returns testConfig.secretId
        every { mockSharedPreferences.getString("secret_key", "") } returns testConfig.secretKey
        every { mockSharedPreferences.getString("region", "ap-guangzhou") } returns testConfig.region

        val result = configManager.getConfig()

        assertEquals(testConfig, result)
    }

    @Test
    fun `validation works correctly for different config states`() {
        // Test valid config
        assertTrue(testConfig.isValid())
        assertFalse(testConfig.isSkipped())

        // Test skip config
        assertTrue(skipConfig.isSkipped())
        assertFalse(skipConfig.isValid())

        // Test empty config
        val emptyConfig = ConfigManager.TencentConfigData("", "", "", "ap-guangzhou")
        assertFalse(emptyConfig.isValid())
        assertFalse(emptyConfig.isSkipped())

        // Test partial config
        val partialConfig = ConfigManager.TencentConfigData("app_id", "", "", "ap-guangzhou")
        assertFalse(partialConfig.isValid())
        assertFalse(partialConfig.isSkipped())
    }

    @Test
    fun `region validation works correctly`() {
        val supportedRegions = configManager.getSupportedRegions()
        val supportedCodes = supportedRegions.map { it.first }

        // Test all supported regions
        supportedCodes.forEach { regionCode ->
            val config = testConfig.copy(region = regionCode)
            assertTrue("Region $regionCode should be valid", config.isValid())
        }
    }

    @Test
    fun `config equality works correctly`() {
        val config1 = ConfigManager.TencentConfigData("app1", "secret1", "key1", "region1")
        val config2 = ConfigManager.TencentConfigData("app1", "secret1", "key1", "region1")
        val config3 = ConfigManager.TencentConfigData("app2", "secret1", "key1", "region1")

        assertEquals(config1, config2)
        assertNotEquals(config1, config3)
    }
} 