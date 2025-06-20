package com.example.test0.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.test0.AppThemeMode
import com.example.test0.config.ConfigManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val testDispatcher = TestCoroutineDispatcher()

    // Mocks
    private lateinit var mockApplication: Application
    private lateinit var mockConfigManager: ConfigManager
    private lateinit var mockOnThemeChange: (AppThemeMode) -> Unit

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
        Dispatchers.setMain(testDispatcher)
        
        mockApplication = mockk(relaxed = true)
        mockConfigManager = mockk(relaxed = true)
        mockOnThemeChange = mockk(relaxed = true)

        // Setup default mock behaviors
        coEvery { mockConfigManager.getConfig() } returns testConfig
        coEvery { mockConfigManager.getUserConfig() } returns flowOf(testConfig)
        coEvery { mockConfigManager.getSupportedRegions() } returns listOf(
            "ap-guangzhou" to "广州（华南）",
            "ap-beijing" to "北京（华北）"
        )
        coEvery { mockConfigManager.importFromDeveloperConfig() } returns false
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    private fun createViewModel(): SettingsViewModel {
        // Note: This is a simplified approach. In real implementation, 
        // you would use dependency injection to provide the mock ConfigManager
        return SettingsViewModel(
            application = mockApplication,
            currentTheme = AppThemeMode.SYSTEM,
            onThemeChange = mockOnThemeChange
        )
    }

    @Test
    fun `initial state loads current configuration correctly`() = runBlockingTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertEquals(testConfig.appId, uiState.appId)
        assertEquals(testConfig.secretId, uiState.secretId)
        assertEquals(testConfig.secretKey, uiState.secretKey)
        assertEquals(testConfig.region, uiState.selectedRegion)
        assertTrue(uiState.isConfigValid)
        assertFalse(uiState.isConfigSkipped)
    }

    @Test
    fun `updateAppId updates state and checks for changes`() {
        val viewModel = createViewModel()
        val newAppId = "new_app_id"
        
        viewModel.updateAppId(newAppId)
        
        val uiState = viewModel.uiState.value
        assertEquals(newAppId, uiState.appId)
        assertNull(uiState.appIdError)
        assertTrue(uiState.hasChanges)
    }

    @Test
    fun `updateSecretId updates state correctly`() {
        val viewModel = createViewModel()
        val newSecretId = "new_secret_id"
        
        viewModel.updateSecretId(newSecretId)
        
        val uiState = viewModel.uiState.value
        assertEquals(newSecretId, uiState.secretId)
        assertNull(uiState.secretIdError)
    }

    @Test
    fun `updateSecretKey updates state correctly`() {
        val viewModel = createViewModel()
        val newSecretKey = "new_secret_key"
        
        viewModel.updateSecretKey(newSecretKey)
        
        val uiState = viewModel.uiState.value
        assertEquals(newSecretKey, uiState.secretKey)
        assertNull(uiState.secretKeyError)
    }

    @Test
    fun `selectRegion updates region and name`() {
        val viewModel = createViewModel()
        val regionCode = "ap-beijing"
        val regionName = "北京（华北）"
        
        viewModel.selectRegion(regionCode, regionName)
        
        val uiState = viewModel.uiState.value
        assertEquals(regionCode, uiState.selectedRegion)
        assertEquals(regionName, uiState.selectedRegionName)
    }

    @Test
    fun `toggleSecretKeyVisibility changes visibility state`() {
        val viewModel = createViewModel()
        val initialVisibility = viewModel.uiState.value.showSecretKey
        
        viewModel.toggleSecretKeyVisibility()
        
        assertEquals(!initialVisibility, viewModel.uiState.value.showSecretKey)
    }

    @Test
    fun `resetToOriginal restores original configuration`() = runBlockingTest {
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Make changes
        viewModel.updateAppId("changed_app_id")
        assertTrue(viewModel.uiState.value.hasChanges)
        
        // Reset
        viewModel.resetToOriginal()
        
        val uiState = viewModel.uiState.value
        assertEquals(testConfig.appId, uiState.appId)
        assertFalse(uiState.hasChanges)
    }

    @Test
    fun `saveConfiguration calls ConfigManager with correct data`() = runBlockingTest {
        coEvery { mockConfigManager.saveUserConfig(any()) } just Runs
        
        val viewModel = createViewModel()
        viewModel.updateAppId("new_app_id")
        viewModel.updateSecretId("new_secret_id") 
        viewModel.updateSecretKey("new_secret_key")
        
        viewModel.saveConfiguration()
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify {
            mockConfigManager.saveUserConfig(
                ConfigManager.TencentConfigData(
                    appId = "new_app_id",
                    secretId = "new_secret_id",
                    secretKey = "new_secret_key",
                    region = testConfig.region
                )
            )
        }
    }

    @Test
    fun `saveConfiguration shows success state after successful save`() = runBlockingTest {
        coEvery { mockConfigManager.saveUserConfig(any()) } just Runs
        
        val viewModel = createViewModel()
        viewModel.updateAppId("new_app_id")
        viewModel.updateSecretId("new_secret_id")
        viewModel.updateSecretKey("new_secret_key")
        
        viewModel.saveConfiguration()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertFalse(uiState.hasChanges)
        assertTrue(uiState.showRestartDialog)
        assertEquals("操作成功！", uiState.successMessage)
    }

    @Test
    fun `saveConfiguration shows error when save fails`() = runBlockingTest {
        val errorMessage = "Save failed"
        coEvery { mockConfigManager.saveUserConfig(any()) } throws Exception(errorMessage)
        
        val viewModel = createViewModel()
        viewModel.updateAppId("new_app_id")
        viewModel.updateSecretId("new_secret_id")
        viewModel.updateSecretKey("new_secret_key")
        
        viewModel.saveConfiguration()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.errorMessage?.contains(errorMessage) == true)
    }

    @Test
    fun `importDeveloperConfig succeeds when developer config available`() = runBlockingTest {
        coEvery { mockConfigManager.importFromDeveloperConfig() } returns true
        
        val viewModel = createViewModel()
        viewModel.importDeveloperConfig()
        testDispatcher.scheduler.advanceUntilIdle()
        
        coVerify { mockConfigManager.importFromDeveloperConfig() }
        
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertTrue(uiState.showRestartDialog)
    }

    @Test
    fun `importDeveloperConfig shows error when no developer config`() = runBlockingTest {
        coEvery { mockConfigManager.importFromDeveloperConfig() } returns false
        
        val viewModel = createViewModel()
        viewModel.importDeveloperConfig()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val uiState = viewModel.uiState.value
        assertFalse(uiState.isLoading)
        assertEquals("开发者配置不可用", uiState.errorMessage)
    }

    @Test
    fun `loads skipped configuration correctly`() = runBlockingTest {
        coEvery { mockConfigManager.getConfig() } returns skipConfig
        coEvery { mockConfigManager.getUserConfig() } returns flowOf(skipConfig)
        
        val viewModel = createViewModel()
        testDispatcher.scheduler.advanceUntilIdle()
        
        val uiState = viewModel.uiState.value
        assertTrue(uiState.isConfigSkipped)
        assertFalse(uiState.isConfigValid)
    }
} 