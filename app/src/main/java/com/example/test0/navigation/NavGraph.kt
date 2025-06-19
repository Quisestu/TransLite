package com.example.test0.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test0.ui.screens.ImageTranslationScreen
import com.example.test0.ui.screens.SpeechTranslationScreen
import com.example.test0.ui.screens.TextTranslationScreen
import com.example.test0.AppThemeMode
import com.example.test0.viewmodel.SpeechTranslationViewModel
import com.example.test0.viewmodel.ImageTranslationViewModel

sealed class Screen(val route: String) {
    data object TextTranslation : Screen("text_translation")
    data object SpeechRecognition : Screen("speech_recognition")
    data object ImageRecognition : Screen("image_recognition")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    themeMode: AppThemeMode,
    onThemeSwitch: (AppThemeMode) -> Unit
) {
    // 在NavGraph级别创建ViewModel，生命周期与NavGraph相同
    val speechTranslationViewModel: SpeechTranslationViewModel = viewModel()
    val imageTranslationViewModel: ImageTranslationViewModel = viewModel()
    
    NavHost(
        navController = navController,
        startDestination = Screen.TextTranslation.route
    ) {
        composable(Screen.TextTranslation.route) {
            TextTranslationScreen(
                onNavigateToSpeech = { navController.navigate(Screen.SpeechRecognition.route) },
                onNavigateToImage = { navController.navigate(Screen.ImageRecognition.route) },
                themeMode = themeMode,
                onThemeSwitch = onThemeSwitch
            )
        }
        
        composable(Screen.SpeechRecognition.route) {
            SpeechTranslationScreen(
                onNavigateBack = { navController.popBackStack() },
                themeMode = themeMode,
                onThemeSwitch = onThemeSwitch,
                viewModel = speechTranslationViewModel // 传递共享的ViewModel
            )
        }
        
        composable(Screen.ImageRecognition.route) {
            ImageTranslationScreen(
                onNavigateBack = { navController.popBackStack() },
                themeMode = themeMode,
                onThemeSwitch = onThemeSwitch,
                viewModel = imageTranslationViewModel // 传递共享的ViewModel
            )
        }
    }
} 