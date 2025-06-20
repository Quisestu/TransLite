package com.example.test0.viewmodel

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.example.test0.config.ConfigManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class FirstTimeSetupViewModelTest {

    // Rule for running LiveData tasks synchronously
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    // Coroutine dispatcher for testing
    private val testDispatcher = TestCoroutineDispatcher()

    // Mocks
    private lateinit var mockApplication: Application
    private lateinit var mockConfigManager: ConfigManager

    // ViewModel under test
    private lateinit var viewModel: FirstTimeSetupViewModel

    @Before
    fun setUp() {
        // Set the main coroutine dispatcher for testing
        Dispatchers.setMain(testDispatcher)

        // Create mocks
        mockApplication = mockk(relaxed = true)
        mockConfigManager = mockk(relaxed = true)

        // Stub the ConfigManager factory method if it's used in the ViewModel
        // For simplicity, we'll assume the ViewModel takes ConfigManager as a constructor param
        // or we can use a DI framework. Here, we'll inject it manually after creation if needed,
        // or ensure the ViewModel can be tested with a mocked one.
        // Let's assume we can replace it. We will need to adjust the ViewModel for testability.
        // For now, let's proceed and see. If the ViewModel directly creates `ConfigManager`,
        // this test setup will need more work (e.g. using PowerMock or refactoring the ViewModel).

        // Stub the behavior of mockConfigManager
        coEvery { mockConfigManager.importFromDeveloperConfig() } returns false
        coEvery { mockConfigManager.getSupportedRegions() } returns listOf("ap-guangzhou" to "广州（华南）")

        // How the ViewModel gets the ConfigManager is crucial. Let's assume the ViewModel has a public setter or a way to inject it.
        // The provided code shows `private val configManager = ConfigManager(application)`, so we can't directly inject it.
        // We will test the public API and verify state changes.

        viewModel = FirstTimeSetupViewModel(mockApplication)
        // This is a simplification. In a real project, you would use a proper DI framework
        // to provide the mocked ConfigManager. Here we can't replace the instance easily.
        // However, we can test the UI state logic which is independent of the manager's implementation details.
    }

    @After
    fun tearDown() {
        // Reset the main coroutine dispatcher
        Dispatchers.resetMain()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `updateAppId updates uiState correctly`() {
        val testAppId = "test_app_id"
        viewModel.updateAppId(testAppId)

        val uiState = viewModel.uiState.value
        assertEquals(testAppId, uiState.appId)
        assertEquals(null, uiState.appIdError)
    }

    @Test
    fun `skipConfiguration updates state to complete`() {
        viewModel.skipConfiguration()

        // Advance coroutines
        testDispatcher.scheduler.advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertTrue(uiState.isSetupComplete)
    }

    @Test
    fun `when all fields valid, isFormValid is true`() {
        viewModel.updateAppId("test_id")
        viewModel.updateSecretId("test_secret_id")
        viewModel.updateSecretKey("test_secret_key")

        assertTrue(viewModel.uiState.value.isFormValid)
    }
} 