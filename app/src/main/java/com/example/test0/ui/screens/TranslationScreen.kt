package com.example.test0.ui.screens

/**
 * 本应用使用的API说明：
 * 
 * 1. 翻译功能 (Translation)
 * - 使用 Google ML Kit Translation API
 * - 依赖: com.google.mlkit:translate:17.0.2
 * - 支持12种语言之间的互译
 * 
 * 2. OCR文字识别 (Optical Character Recognition)
 * - 使用 Google ML Kit Text Recognition API
 * - 依赖: 
 *   - com.google.mlkit:text-recognition:16.0.0
 *   - com.google.android.gms:play-services-mlkit-text-recognition:19.0.0
 * - 支持从图片中提取文字
 * 
 * 3. 语音识别 (Speech Recognition)
 * - 使用 Android 原生 Speech Recognition API
 * - 通过 Intent 调用系统语音识别服务
 * - 需要 RECORD_AUDIO 权限
 * 
 * 4. 文本转语音 (Text-to-Speech)
 * - 使用 Android 原生 TextToSpeech API
 * - 支持多种语言的语音合成
 * 
 * 5. 相机功能
 * - 使用 CameraX API
 * - 依赖:
 *   - androidx.camera:camera-camera2:1.3.2
 *   - androidx.camera:camera-lifecycle:1.3.2
 *   - androidx.camera:camera-view:1.3.2
 * - 用于拍摄图片进行OCR识别
 */

import android.Manifest
import android.app.Application
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test0.model.Language
import com.example.test0.ui.components.CameraView
import com.example.test0.ui.components.LanguageSelector
import com.example.test0.ui.components.TranslationCard
import com.example.test0.viewmodel.TranslationViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TranslationScreen(
    viewModel: TranslationViewModel = viewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    val sourceLanguage by viewModel.sourceLanguage.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val sourceText by viewModel.sourceText.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    
    // 相机状态
    var showCamera by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // 录音状态
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    // 处理错误
    LaunchedEffect(uiState) {
        if (uiState is TranslationViewModel.TranslationUiState.Error) {
            val errorMessage = (uiState as TranslationViewModel.TranslationUiState.Error).message
            snackbarHostState.showSnackbar(errorMessage)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("TransLite") }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        if (showCamera) {
            CameraView(
                onImageCaptured = { bitmap ->
                    viewModel.recognizeTextFromImage(bitmap)
                    showCamera = false
                },
                onDismiss = { showCamera = false }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // 语言选择器
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LanguageSelector(
                        selectedLanguage = sourceLanguage,
                        languages = Language.getAllLanguages(),
                        onLanguageSelected = { viewModel.updateSourceLanguage(it) },
                        modifier = Modifier.weight(1f),
                        label = "从"
                    )
                    
                    IconButton(
                        onClick = { viewModel.swapLanguages() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "交换语言"
                        )
                    }
                    
                    LanguageSelector(
                        selectedLanguage = targetLanguage,
                        languages = Language.getAllLanguages(),
                        onLanguageSelected = { viewModel.updateTargetLanguage(it) },
                        modifier = Modifier.weight(1f),
                        label = "到"
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 翻译卡片
                TranslationCard(
                    sourceText = sourceText,
                    translatedText = translatedText,
                    sourceLanguage = sourceLanguage,
                    targetLanguage = targetLanguage,
                    isTranslating = isTranslating,
                    onSourceTextChanged = { viewModel.updateSourceText(it) },
                    onSwapLanguages = { viewModel.swapLanguages() },
                    onClearText = { viewModel.updateSourceText("") },
                    onCameraClicked = {
                        if (cameraPermissionState.status.isGranted) {
                            showCamera = true
                        } else {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    },
                    onMicrophoneClicked = {
                        if (audioPermissionState.status.isGranted) {
                            viewModel.startSpeechRecognition()
                        } else {
                            audioPermissionState.launchPermissionRequest()
                        }
                    },
                    onSpeakClicked = {
                        viewModel.speakTranslatedText()
                    }
                )
            }
        }
    }
}

// 预览参数提供器
class TranslationViewModelPreviewParameterProvider : PreviewParameterProvider<TranslationViewModel> {
    override val values = sequenceOf(
        TranslationViewModel(Application())
    )
}

// 预览函数
@Preview(
    name = "翻译界面预览",
    showBackground = true,
    showSystemUi = true,
    widthDp = 360,
    heightDp = 640
)
@Composable
fun TranslationScreenPreview() {
    MaterialTheme {
        TranslationScreen()
    }
} 