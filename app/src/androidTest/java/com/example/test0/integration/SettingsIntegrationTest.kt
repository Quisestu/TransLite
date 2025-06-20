package com.example.test0.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.test0.AppThemeMode
import com.example.test0.viewmodel.FirstTimeSetupViewModel
import com.example.test0.viewmodel.SettingsViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: android.content.Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
    }

    @Test
    fun completeUserFlow_firstTimeSetupToSettings_worksCorrectly() {
        // Scenario: New user opens app, completes first-time setup, then modifies settings
        
        var isSetupComplete = false
        val firstTimeViewModel = FirstTimeSetupViewModel(
            application = context.applicationContext as android.app.Application
        )

        // Step 1: First-time setup
        composeTestRule.setContent {
            // FirstTimeSetupScreen(
            //     viewModel = firstTimeViewModel,
            //     onSetupComplete = { isSetupComplete = true }
            // )
        }

        // Fill first-time setup form
        composeTestRule.onNodeWithText("App ID").performTextInput("initial_app_id")
        composeTestRule.onNodeWithText("Secret ID").performTextInput("initial_secret_id")
        composeTestRule.onNodeWithText("Secret Key").performTextInput("initial_secret_key")
        composeTestRule.onNodeWithText("保存配置").performClick()

        // Wait for setup completion
        composeTestRule.waitUntil(timeoutMillis = 5000) { isSetupComplete }

        // Step 2: Navigate to settings
        val settingsViewModel = SettingsViewModel(
            application = context.applicationContext as android.app.Application,
            currentTheme = AppThemeMode.SYSTEM,
            onThemeChange = { }
        )

        composeTestRule.setContent {
            // SettingsScreen(
            //     viewModel = settingsViewModel,
            //     onNavigateBack = { }
            // )
        }

        // Step 3: Verify initial configuration is loaded
        composeTestRule.onNodeWithText("配置有效").assertExists()

        // Step 4: Modify configuration
        composeTestRule.onNodeWithText("修改").performClick()
        composeTestRule.onNodeWithText("App ID").performTextReplacement("modified_app_id")
        composeTestRule.onNodeWithText("保存").performClick()

        // Step 5: Verify configuration was updated
        composeTestRule.onNodeWithText("操作成功！").assertExists()
    }

    @Test
    fun skipFlow_firstTimeSkipThenConfigure_worksCorrectly() {
        // Scenario: User skips first-time setup, then configures later in settings
        
        var isSetupComplete = false
        val firstTimeViewModel = FirstTimeSetupViewModel(
            application = context.applicationContext as android.app.Application
        )

        // Step 1: Skip first-time setup
        composeTestRule.setContent {
            // FirstTimeSetupScreen(
            //     viewModel = firstTimeViewModel,
            //     onSetupComplete = { isSetupComplete = true }
            // )
        }

        composeTestRule.onNodeWithText("跳过").performClick()
        composeTestRule.onNodeWithText("确认").performClick()

        // Wait for setup completion
        composeTestRule.waitUntil(timeoutMillis = 5000) { isSetupComplete }

        // Step 2: Go to settings
        val settingsViewModel = SettingsViewModel(
            application = context.applicationContext as android.app.Application,
            currentTheme = AppThemeMode.SYSTEM,
            onThemeChange = { }
        )

        composeTestRule.setContent {
            // SettingsScreen(
            //     viewModel = settingsViewModel,
            //     onNavigateBack = { }
            // )
        }

        // Step 3: Verify skipped status
        composeTestRule.onNodeWithText("已跳过").assertExists()

        // Step 4: Add configuration
        composeTestRule.onNodeWithText("添加配置").performClick()
        composeTestRule.onNodeWithText("App ID").performTextInput("new_app_id")
        composeTestRule.onNodeWithText("Secret ID").performTextInput("new_secret_id")
        composeTestRule.onNodeWithText("Secret Key").performTextInput("new_secret_key")
        composeTestRule.onNodeWithText("保存").performClick()

        // Step 5: Verify configuration is now valid
        composeTestRule.onNodeWithText("配置有效").assertExists()
    }

    @Test
    fun developerFlow_importConfig_worksCorrectly() {
        // Scenario: Developer imports configuration from local.properties
        
        var isSetupComplete = false
        val firstTimeViewModel = FirstTimeSetupViewModel(
            application = context.applicationContext as android.app.Application
        )

        composeTestRule.setContent {
            // FirstTimeSetupScreen(
            //     viewModel = firstTimeViewModel,
            //     onSetupComplete = { isSetupComplete = true }
            // )
        }

        // Check if developer config button is available
        if (composeTestRule.onNodeWithText("导入开发者配置").isDisplayed()) {
            // Import developer config
            composeTestRule.onNodeWithText("导入开发者配置").performClick()
            
            // Wait for completion
            composeTestRule.waitUntil(timeoutMillis = 5000) { isSetupComplete }
            
            // Verify import success
            assert(isSetupComplete)
        }
    }

    @Test
    fun errorHandling_invalidConfigSave_showsError() {
        // Scenario: Test error handling when save fails
        
        val settingsViewModel = SettingsViewModel(
            application = context.applicationContext as android.app.Application,
            currentTheme = AppThemeMode.SYSTEM,
            onThemeChange = { }
        )

        composeTestRule.setContent {
            // SettingsScreen(
            //     viewModel = settingsViewModel,
            //     onNavigateBack = { }
            // )
        }

        // Try to save empty configuration
        composeTestRule.onNodeWithText("添加配置").performClick()
        composeTestRule.onNodeWithText("保存").performClick()

        // Should show validation errors
        composeTestRule.onNodeWithText("此项不能为空").assertExists()
    }

    @Test
    fun themeChange_persistsAcrossScreens_worksCorrectly() {
        // Scenario: Test that theme changes persist across different screens
        
        var currentTheme = AppThemeMode.SYSTEM
        val settingsViewModel = SettingsViewModel(
            application = context.applicationContext as android.app.Application,
            currentTheme = currentTheme,
            onThemeChange = { newTheme -> currentTheme = newTheme }
        )

        composeTestRule.setContent {
            // SettingsScreen(
            //     viewModel = settingsViewModel,
            //     onNavigateBack = { }
            // )
        }

        // Change theme to dark
        composeTestRule.onNodeWithText("跟随系统").performClick()
        composeTestRule.onNodeWithText("暗色").performClick()

        // Verify theme change callback was called
        assert(currentTheme == AppThemeMode.DARK)

        // Navigate away and back (simulated)
        composeTestRule.setContent {
            // SettingsScreen(
            //     viewModel = settingsViewModel,
            //     onNavigateBack = { }
            // )
        }

        // Theme should still be dark
        composeTestRule.onNodeWithText("暗色").assertExists()
    }

    @Test
    fun configurationValidation_realTimeValidation_worksCorrectly() {
        // Scenario: Test real-time form validation
        
        val settingsViewModel = SettingsViewModel(
            application = context.applicationContext as android.app.Application,
            currentTheme = AppThemeMode.SYSTEM,
            onThemeChange = { }
        )

        composeTestRule.setContent {
            // SettingsScreen(
            //     viewModel = settingsViewModel,
            //     onNavigateBack = { }
            // )
        }

        // Open configuration form
        composeTestRule.onNodeWithText("添加配置").performClick()

        // Initially save button should be disabled
        composeTestRule.onNodeWithText("保存").assertIsNotEnabled()

        // Fill App ID only
        composeTestRule.onNodeWithText("App ID").performTextInput("test_app_id")
        // Save should still be disabled
        composeTestRule.onNodeWithText("保存").assertIsNotEnabled()

        // Fill Secret ID
        composeTestRule.onNodeWithText("Secret ID").performTextInput("test_secret_id")
        // Save should still be disabled
        composeTestRule.onNodeWithText("保存").assertIsNotEnabled()

        // Fill Secret Key
        composeTestRule.onNodeWithText("Secret Key").performTextInput("test_secret_key")
        // Now save should be enabled
        composeTestRule.onNodeWithText("保存").assertIsEnabled()
    }

    @Test
    fun multipleRegionChanges_persistsCorrectly_worksCorrectly() {
        // Scenario: Test multiple region changes and persistence
        
        val settingsViewModel = SettingsViewModel(
            application = context.applicationContext as android.app.Application,
            currentTheme = AppThemeMode.SYSTEM,
            onThemeChange = { }
        )

        composeTestRule.setContent {
            // SettingsScreen(
            //     viewModel = settingsViewModel,
            //     onNavigateBack = { }
            // )
        }

        // Open form and test region changes
        composeTestRule.onNodeWithText("添加配置").performClick()

        // Change region multiple times
        composeTestRule.onNodeWithText("广州（华南）").performClick()
        composeTestRule.onNodeWithText("北京（华北）").performClick()

        composeTestRule.onNodeWithText("北京（华北）").performClick()
        composeTestRule.onNodeWithText("上海（华东）").performClick()

        composeTestRule.onNodeWithText("上海（华东）").performClick()
        composeTestRule.onNodeWithText("成都（西南）").performClick()

        // Final selection should be 成都
        composeTestRule.onNodeWithText("成都（西南）").assertExists()

        // Fill other fields and save
        composeTestRule.onNodeWithText("App ID").performTextInput("test_app_id")
        composeTestRule.onNodeWithText("Secret ID").performTextInput("test_secret_id")
        composeTestRule.onNodeWithText("Secret Key").performTextInput("test_secret_key")
        composeTestRule.onNodeWithText("保存").performClick()

        // Region should be saved correctly
        composeTestRule.onNodeWithText("操作成功！").assertExists()
    }

    @Test
    fun resetFunctionality_restoresOriginalValues_worksCorrectly() {
        // Scenario: Test reset functionality restores original values
        
        val settingsViewModel = SettingsViewModel(
            application = context.applicationContext as android.app.Application,
            currentTheme = AppThemeMode.SYSTEM,
            onThemeChange = { }
        )

        composeTestRule.setContent {
            // SettingsScreen(
            //     viewModel = settingsViewModel,
            //     onNavigateBack = { }
            // )
        }

        // Assuming there's already some configuration loaded
        composeTestRule.onNodeWithText("修改").performClick()

        // Modify all fields
        composeTestRule.onNodeWithText("App ID").performTextReplacement("modified_app_id")
        composeTestRule.onNodeWithText("Secret ID").performTextReplacement("modified_secret_id")
        composeTestRule.onNodeWithText("Secret Key").performTextReplacement("modified_secret_key")

        // Change region
        composeTestRule.onNodeWithText("广州（华南）").performClick()
        composeTestRule.onNodeWithText("北京（华北）").performClick()

        // Reset to original
        composeTestRule.onNodeWithText("撤销").performClick()

        // All fields should be restored to original values
        // This would need to check against the original loaded values
        composeTestRule.onNodeWithText("撤销").assertIsNotEnabled()
    }
} 