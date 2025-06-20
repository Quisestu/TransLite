package com.example.test0.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test0.ui.screens.ImageTranslationScreen
import com.example.test0.ui.screens.SpeechTranslationScreen
import com.example.test0.ui.screens.TextTranslationScreen
import com.example.test0.ui.screens.HistoryScreen
import com.example.test0.ui.screens.FirstTimeSetupScreen
import com.example.test0.ui.screens.SettingsScreen
import com.example.test0.AppThemeMode
import com.example.test0.viewmodel.SpeechTranslationViewModel
import com.example.test0.viewmodel.ImageTranslationViewModel
import com.example.test0.model.TranslationType
import com.example.test0.config.ConfigManager
import androidx.navigation.navArgument
import androidx.navigation.NavType
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class Screen(val route: String) {
    data object FirstTimeSetup : Screen("first_time_setup")
    data object TextTranslation : Screen("text_translation")
    data object SpeechRecognition : Screen("speech_recognition")
    data object ImageRecognition : Screen("image_recognition")
    data object History : Screen("history/{filterType}") {
        fun createRoute(filterType: String?) = "history/${filterType ?: "null"}"
    }
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    themeMode: AppThemeMode,
    onThemeSwitch: (AppThemeMode) -> Unit
) {
    val context = LocalContext.current
    val configManager = remember { ConfigManager(context) }
    
    // 配置检查状态
    var isCheckingConfig by remember { mutableStateOf(true) }
    var needsFirstTimeSetup by remember { mutableStateOf(false) }
    
    // 检查是否需要首次配置
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            try {
                needsFirstTimeSetup = configManager.needsFirstTimeSetup()
            } catch (e: Exception) {
                // 如果检查失败，假设需要配置
                needsFirstTimeSetup = true
            } finally {
                isCheckingConfig = false
            }
        }
    }
    
    // 如果还在检查配置，显示加载界面或什么都不显示
    if (isCheckingConfig) {
        return
    }
    
    // 在NavGraph级别创建ViewModel，生命周期与NavGraph相同
    val speechTranslationViewModel: SpeechTranslationViewModel = viewModel()
    val imageTranslationViewModel: ImageTranslationViewModel = viewModel()
    
    val startDestination = if (needsFirstTimeSetup) {
        Screen.FirstTimeSetup.route
    } else {
        Screen.TextTranslation.route
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // 首次配置页面
        composable(Screen.FirstTimeSetup.route) {
            FirstTimeSetupScreen(
                onSetupComplete = {
                    // 配置完成后导航到主页面，并清除返回栈
                    navController.navigate(Screen.TextTranslation.route) {
                        popUpTo(Screen.FirstTimeSetup.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.TextTranslation.route) {
            TextTranslationScreen(
                onNavigateToSpeech = { navController.navigate(Screen.SpeechRecognition.route) },
                onNavigateToImage = { navController.navigate(Screen.ImageRecognition.route) },
                onNavigateToHistory = { navController.navigate(Screen.History.createRoute("text")) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
            )
        }
        
        composable(Screen.SpeechRecognition.route) {
            SpeechTranslationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Screen.History.createRoute("speech")) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                viewModel = speechTranslationViewModel // 传递共享的ViewModel
            )
        }
        
        composable(Screen.ImageRecognition.route) {
            ImageTranslationScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHistory = { navController.navigate(Screen.History.createRoute("image")) },
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                viewModel = imageTranslationViewModel // 传递共享的ViewModel
            )
        }
        
        composable(
            route = Screen.History.route,
            arguments = listOf(navArgument("filterType") { type = NavType.StringType })
        ) { backStackEntry ->
            val filterTypeStr = backStackEntry.arguments?.getString("filterType")
            val filterType = when (filterTypeStr) {
                "text" -> TranslationType.TEXT
                "speech" -> TranslationType.SPEECH
                "image" -> TranslationType.IMAGE
                "null", null -> null
                else -> null
            }
            
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() },
                initialFilterType = filterType
            )
        }
        
        // 设置页面
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                currentTheme = themeMode,
                onThemeChange = onThemeSwitch
            )
        }
    }
} 