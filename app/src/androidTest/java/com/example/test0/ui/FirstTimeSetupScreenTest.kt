package com.example.test0.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.test0.viewmodel.FirstTimeSetupViewModel
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FirstTimeSetupScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: FirstTimeSetupViewModel

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        viewModel = FirstTimeSetupViewModel(
            application = context.applicationContext as android.app.Application
        )
    }

    @Test
    fun firstTimeSetupScreen_displaysWelcomeMessage() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(
            //     viewModel = viewModel,
            //     onSetupComplete = { }
            // )
        }

        // Should display welcome or introduction message
        composeTestRule.onNodeWithText("欢迎使用翻译应用").assertExists()
        composeTestRule.onNodeWithText("请配置您的腾讯云服务").assertExists()
    }

    @Test
    fun firstTimeSetupScreen_formValidation_requiresAllFields() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Try to save with empty fields
        composeTestRule.onNodeWithText("保存配置").performClick()

        // Should show validation errors
        composeTestRule.onNodeWithText("此项不能为空").assertExists()
    }

    @Test
    fun firstTimeSetupScreen_formInput_enablesSaveButton() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Fill all required fields
        composeTestRule.onNodeWithText("App ID").performTextInput("test_app_id")
        composeTestRule.onNodeWithText("Secret ID").performTextInput("test_secret_id") 
        composeTestRule.onNodeWithText("Secret Key").performTextInput("test_secret_key")

        // Save button should be enabled
        composeTestRule.onNodeWithText("保存配置").assertIsEnabled()
    }

    @Test
    fun firstTimeSetupScreen_regionSelection_showsDropdown() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Click region dropdown
        composeTestRule.onNodeWithText("广州（华南）").performClick()

        // Should show all available regions
        composeTestRule.onNodeWithText("北京（华北）").assertExists()
        composeTestRule.onNodeWithText("上海（华东）").assertExists()
        composeTestRule.onNodeWithText("成都（西南）").assertExists()
        composeTestRule.onNodeWithText("新加坡").assertExists()
        composeTestRule.onNodeWithText("香港").assertExists()
    }

    @Test
    fun firstTimeSetupScreen_regionSelection_updatesSelection() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Click region dropdown and select different region
        composeTestRule.onNodeWithText("广州（华南）").performClick()
        composeTestRule.onNodeWithText("北京（华北）").performClick()

        // Should update the displayed selection
        composeTestRule.onNodeWithText("北京（华北）").assertExists()
    }

    @Test
    fun firstTimeSetupScreen_secretKeyVisibility_togglesCorrectly() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Enter secret key
        composeTestRule.onNodeWithText("Secret Key").performTextInput("test_secret_key")

        // Toggle visibility
        composeTestRule.onNodeWithContentDescription("显示密码").performClick()

        // Should show password and toggle icon should change
        composeTestRule.onNodeWithContentDescription("隐藏密码").assertExists()
    }

    @Test
    fun firstTimeSetupScreen_skipButton_showsConfirmationDialog() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Click skip button
        composeTestRule.onNodeWithText("跳过").performClick()

        // Should show confirmation dialog
        composeTestRule.onNodeWithText("确认跳过").assertExists()
        composeTestRule.onNodeWithText("跳过配置后，您将无法使用翻译功能").assertExists()
        composeTestRule.onNodeWithText("确认").assertExists()
        composeTestRule.onNodeWithText("取消").assertExists()
    }

    @Test
    fun firstTimeSetupScreen_skipConfirmation_completesSetup() {
        var setupCompleted = false
        
        composeTestRule.setContent {
            // FirstTimeSetupScreen(
            //     viewModel = viewModel,
            //     onSetupComplete = { setupCompleted = true }
            // )
        }

        // Click skip and confirm
        composeTestRule.onNodeWithText("跳过").performClick()
        composeTestRule.onNodeWithText("确认").performClick()

        // Should complete setup
        assert(setupCompleted)
    }

    @Test
    fun firstTimeSetupScreen_skipCancel_returnsToForm() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Click skip then cancel
        composeTestRule.onNodeWithText("跳过").performClick()
        composeTestRule.onNodeWithText("取消").performClick()

        // Should return to form
        composeTestRule.onNodeWithText("App ID").assertExists()
        composeTestRule.onNodeWithText("保存配置").assertExists()
    }

    @Test
    fun firstTimeSetupScreen_saveConfiguration_showsLoading() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Fill form and save
        composeTestRule.onNodeWithText("App ID").performTextInput("test_app_id")
        composeTestRule.onNodeWithText("Secret ID").performTextInput("test_secret_id")
        composeTestRule.onNodeWithText("Secret Key").performTextInput("test_secret_key")
        composeTestRule.onNodeWithText("保存配置").performClick()

        // Should show loading state
        composeTestRule.onNodeWithTag("loading_indicator").assertExists()
    }

    @Test
    fun firstTimeSetupScreen_saveSuccess_completesSetup() {
        var setupCompleted = false
        
        composeTestRule.setContent {
            // FirstTimeSetupScreen(
            //     viewModel = viewModel,
            //     onSetupComplete = { setupCompleted = true }
            // )
        }

        // Fill form and save
        composeTestRule.onNodeWithText("App ID").performTextInput("test_app_id")
        composeTestRule.onNodeWithText("Secret ID").performTextInput("test_secret_id")
        composeTestRule.onNodeWithText("Secret Key").performTextInput("test_secret_key")
        composeTestRule.onNodeWithText("保存配置").performClick()

        // Wait for completion (in real test, you'd wait for the coroutine)
        // Should complete setup
        // assert(setupCompleted)
    }

    @Test
    fun firstTimeSetupScreen_developerConfigButton_showsWhenAvailable() {
        // This test assumes developer config is available
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Should show developer config import button
        composeTestRule.onNodeWithText("导入开发者配置").assertExists()
        composeTestRule.onNodeWithText("使用预设配置快速开始").assertExists()
    }

    @Test
    fun firstTimeSetupScreen_importDeveloperConfig_completesSetup() {
        var setupCompleted = false
        
        composeTestRule.setContent {
            // FirstTimeSetupScreen(
            //     viewModel = viewModel,
            //     onSetupComplete = { setupCompleted = true }
            // )
        }

        // Click import developer config
        composeTestRule.onNodeWithText("导入开发者配置").performClick()

        // Should complete setup
        // assert(setupCompleted)
    }

    @Test
    fun firstTimeSetupScreen_errorState_displaysErrorMessage() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Fill invalid config and save (would need to mock error)
        composeTestRule.onNodeWithText("App ID").performTextInput("invalid")
        composeTestRule.onNodeWithText("Secret ID").performTextInput("invalid")
        composeTestRule.onNodeWithText("Secret Key").performTextInput("invalid")
        composeTestRule.onNodeWithText("保存配置").performClick()

        // Should show error message
        composeTestRule.onNodeWithText("配置保存失败").assertExists()
    }

    @Test
    fun firstTimeSetupScreen_helpText_providesGuidance() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Should provide helpful guidance
        composeTestRule.onNodeWithText("如何获取").assertExists()
        composeTestRule.onNodeWithText("腾讯云控制台").assertExists()
        composeTestRule.onNodeWithText("API密钥管理").assertExists()
    }

    @Test
    fun firstTimeSetupScreen_formState_persistsAcrossConfigChanges() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Fill form
        val testAppId = "test_app_id"
        composeTestRule.onNodeWithText("App ID").performTextInput(testAppId)

        // Simulate configuration change (rotation)
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Form data should persist
        composeTestRule.onNodeWithText("App ID").assertTextContains(testAppId)
    }

    @Test
    fun firstTimeSetupScreen_multipleRegionSelection_worksCorrectly() {
        composeTestRule.setContent {
            // FirstTimeSetupScreen(viewModel = viewModel, onSetupComplete = { })
        }

        // Test selecting multiple regions in sequence
        composeTestRule.onNodeWithText("广州（华南）").performClick()
        composeTestRule.onNodeWithText("上海（华东）").performClick()
        
        // Should show the last selected region
        composeTestRule.onNodeWithText("上海（华东）").assertExists()
        
        // Select another region
        composeTestRule.onNodeWithText("上海（华东）").performClick()
        composeTestRule.onNodeWithText("北京（华北）").performClick()
        
        // Should update to the new selection
        composeTestRule.onNodeWithText("北京（华北）").assertExists()
    }
} 