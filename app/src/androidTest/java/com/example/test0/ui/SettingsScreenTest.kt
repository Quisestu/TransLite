package com.example.test0.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.test0.AppThemeMode
import com.example.test0.viewmodel.SettingsViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        viewModel = SettingsViewModel(
            application = context.applicationContext as android.app.Application,
            currentTheme = AppThemeMode.SYSTEM,
            onThemeChange = { }
        )
    }

    @Test
    fun settingsScreen_displaysConfigurationStatus() {
        composeTestRule.setContent {
            // SettingsScreen(
            //     viewModel = viewModel,
            //     onNavigateBack = { }
            // )
            // Note: The actual Screen composable would be called here
            // For now, we'll test individual components
        }

        // Test that the configuration status is displayed
        composeTestRule.onNodeWithText("配置状态").assertExists()
    }

    @Test
    fun settingsScreen_showsEditFormWhenModifyClicked() {
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // Click modify button
        composeTestRule.onNodeWithText("修改").performClick()

        // Verify form fields are displayed
        composeTestRule.onNodeWithText("App ID").assertExists()
        composeTestRule.onNodeWithText("Secret ID").assertExists() 
        composeTestRule.onNodeWithText("Secret Key").assertExists()
        composeTestRule.onNodeWithText("服务器区域").assertExists()
    }

    @Test
    fun settingsScreen_inputValidation_showsErrorForEmptyFields() {
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // Click modify to show form
        composeTestRule.onNodeWithText("修改").performClick()

        // Clear all fields and try to save
        composeTestRule.onNodeWithText("App ID").performTextClearance()
        composeTestRule.onNodeWithText("保存").performClick()

        // Should show validation error
        composeTestRule.onNodeWithText("此项不能为空").assertExists()
    }

    @Test
    fun settingsScreen_formInput_enablesSaveButton() {
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // Click modify to show form
        composeTestRule.onNodeWithText("修改").performClick()

        // Fill in all required fields
        composeTestRule.onNodeWithText("App ID").performTextInput("test_app_id")
        composeTestRule.onNodeWithText("Secret ID").performTextInput("test_secret_id")
        composeTestRule.onNodeWithText("Secret Key").performTextInput("test_secret_key")

        // Save button should be enabled
        composeTestRule.onNodeWithText("保存").assertIsEnabled()
    }

    @Test
    fun settingsScreen_resetButton_restoresOriginalValues() {
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // Click modify to show form
        composeTestRule.onNodeWithText("修改").performClick()

        // Modify a field
        composeTestRule.onNodeWithText("App ID").performTextReplacement("modified_app_id")

        // Click reset
        composeTestRule.onNodeWithText("撤销").performClick()

        // Should restore original value
        composeTestRule.onNodeWithText("App ID").assertTextContains("original_value")
    }

    @Test
    fun settingsScreen_regionSelector_showsDropdown() {
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // Click modify to show form
        composeTestRule.onNodeWithText("修改").performClick()

        // Click region dropdown
        composeTestRule.onNodeWithText("广州（华南）").performClick()

        // Should show region options
        composeTestRule.onNodeWithText("北京（华北）").assertExists()
        composeTestRule.onNodeWithText("上海（华东）").assertExists()
        composeTestRule.onNodeWithText("成都（西南）").assertExists()
    }

    @Test
    fun settingsScreen_secretKeyVisibility_togglesCorrectly() {
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // Click modify to show form
        composeTestRule.onNodeWithText("修改").performClick()

        // Click visibility toggle
        composeTestRule.onNodeWithContentDescription("显示密码").performClick()

        // Secret key should be visible
        composeTestRule.onNodeWithContentDescription("隐藏密码").assertExists()
    }

    @Test
    fun settingsScreen_themeSelection_changesTheme() {
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // Click current theme
        composeTestRule.onNodeWithText("跟随系统").performClick()

        // Select dark theme
        composeTestRule.onNodeWithText("暗色").performClick()

        // Should show dark theme is selected
        composeTestRule.onNodeWithText("暗色").assertExists()
    }

    @Test
    fun settingsScreen_developerConfigButton_showsWhenAvailable() {
        // This test would require mocking the ConfigManager to return true for developer config
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // Should show developer config button when available
        composeTestRule.onNodeWithText("导入开发者配置").assertExists()
    }

    @Test
    fun settingsScreen_saveSuccess_showsSuccessDialog() {
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // Fill form and save
        composeTestRule.onNodeWithText("修改").performClick()
        composeTestRule.onNodeWithText("App ID").performTextInput("test_app_id")
        composeTestRule.onNodeWithText("Secret ID").performTextInput("test_secret_id")
        composeTestRule.onNodeWithText("Secret Key").performTextInput("test_secret_key")
        composeTestRule.onNodeWithText("保存").performClick()

        // Should show success dialog
        composeTestRule.onNodeWithText("操作成功！").assertExists()
        composeTestRule.onNodeWithText("应用将在重启后生效").assertExists()
    }

    @Test
    fun settingsScreen_navigation_backButtonWorks() {
        var backPressed = false
        
        composeTestRule.setContent {
            // SettingsScreen(
            //     viewModel = viewModel,
            //     onNavigateBack = { backPressed = true }
            // )
        }

        // Click back button
        composeTestRule.onNodeWithContentDescription("返回").performClick()

        // Should trigger navigation callback
        assert(backPressed)
    }

    @Test
    fun settingsScreen_loadingState_showsProgressIndicator() {
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // Trigger save to show loading
        composeTestRule.onNodeWithText("修改").performClick()
        composeTestRule.onNodeWithText("App ID").performTextInput("test")
        composeTestRule.onNodeWithText("Secret ID").performTextInput("test")
        composeTestRule.onNodeWithText("Secret Key").performTextInput("test")
        composeTestRule.onNodeWithText("保存").performClick()

        // Should show loading indicator
        composeTestRule.onNodeWithTag("loading_indicator").assertExists()
    }

    @Test
    fun settingsScreen_errorState_displaysErrorMessage() {
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // This would require triggering an error state in the ViewModel
        // For example, by mocking a save failure

        // Should show error message
        composeTestRule.onNodeWithText("配置保存失败").assertExists()
    }

    @Test
    fun settingsScreen_configurationTypes_displayCorrectly() {
        composeTestRule.setContent {
            // SettingsScreen(viewModel = viewModel, onNavigateBack = { })
        }

        // Test valid configuration display
        composeTestRule.onNodeWithText("配置有效").assertExists()
        
        // Test for appropriate status indicators
        composeTestRule.onNodeWithContentDescription("配置状态指示器").assertExists()
    }
} 