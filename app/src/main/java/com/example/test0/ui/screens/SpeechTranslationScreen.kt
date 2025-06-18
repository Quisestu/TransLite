package com.example.test0.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test0.model.Language
import com.example.test0.viewmodel.SpeechTranslationViewModel
import com.example.test0.ui.components.LanguageSelector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.DisposableHandle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import com.google.accompanist.permissions.PermissionStatus
import android.annotation.SuppressLint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@SuppressLint("MissingPermission")
@Composable
fun SpeechTranslationScreen(
    onNavigateBack: () -> Unit,
    viewModel: SpeechTranslationViewModel = viewModel()
) {
    // 仅支持中英互换
    val sourceLanguage by viewModel.sourceLanguage.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val sourceText by viewModel.sourceText.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val remainingTime by viewModel.remainingTimeSeconds.collectAsState()
    val isRecording by viewModel.isRecording.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val clipboardManager = LocalClipboardManager.current
    val ttsState by viewModel.ttsState.collectAsState()
    val isSpeaking = ttsState is com.example.test0.service.TextToSpeechService.TTSState.Speaking
    val currentTtsType by viewModel.currentTtsType.collectAsState()
    val volume by viewModel.volume.collectAsState()

    val supportedLanguages = listOf(Language.CHINESE, Language.ENGLISH)

    val coroutineScope = rememberCoroutineScope()
    val recordAudioPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    var sourceScrollState = rememberScrollState()
    var targetScrollState = rememberScrollState()

    // 离开界面时自动停止朗读
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
        }
    }

    // 错误对话框
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearErrorMessage() },
            title = { Text("提示") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearErrorMessage() }) {
                    Text("确定")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                    text = "语音翻译",
                    style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIos, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 语言选择器和交换按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LanguageSelector(
                    selectedLanguage = sourceLanguage,
                    languages = supportedLanguages,
                    onLanguageSelected = {}, // 不可选
                    modifier = Modifier.weight(1f),
                    label = "源语言",
                    enabled = false
                )
                IconButton(
                    onClick = { viewModel.swapLanguages() },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "交换语言")
                }
                LanguageSelector(
                    selectedLanguage = targetLanguage,
                    languages = supportedLanguages,
                    onLanguageSelected = {}, // 不可选
                    modifier = Modifier.weight(1f),
                    label = "目标语言",
                    enabled = false
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 上方：原文显示区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = sourceLanguage.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                        textAlign = TextAlign.Start
                    )
                    TextField(
                        value = sourceText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(sourceScrollState)
                            .padding(horizontal = 8.dp),
                        placeholder = { Text("点击底部按钮说话") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            IconButton(onClick = {
                                if (isSpeaking && currentTtsType == com.example.test0.viewmodel.SpeechTranslationViewModel.TtsType.SOURCE) {
                                    viewModel.stopSpeaking()
                                } else {
                                    viewModel.speakSourceText()
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.SpeechTranslationViewModel.TtsType.SOURCE) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.SpeechTranslationViewModel.TtsType.SOURCE) "停止朗读" else "朗读"
                                )
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(sourceText))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                            }
                        }
                        
                        // 倒计时显示
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = buildAnnotatedString {
                                    val minutes = remainingTime / 60
                                    val seconds = remainingTime % 60
                                    withStyle(SpanStyle(color = if (remainingTime <= 10) Color.Red else Color.Gray)) {
                                        append("$minutes:${seconds.toString().padStart(2, '0')}")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }
                }
                // 清除按钮放右上角
                IconButton(
                    onClick = { viewModel.clearSourceText() },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "清除")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // 下方：译文显示区
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = targetLanguage.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                        textAlign = TextAlign.Start
                    )
                    TextField(
                        value = translatedText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(targetScrollState)
                            .padding(horizontal = 8.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (isSpeaking && currentTtsType == com.example.test0.viewmodel.SpeechTranslationViewModel.TtsType.TARGET) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.speakTranslatedText()
                            }
                        }) {
                            Icon(
                                imageVector = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.SpeechTranslationViewModel.TtsType.TARGET) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                                contentDescription = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.SpeechTranslationViewModel.TtsType.TARGET) "停止朗读" else "朗读"
                            )
                        }
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(translatedText))
                        }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.weight(0.1f))

            // 底部Mic按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧音量指示器
                if (isRecording) {
                    VolumeIndicator(
                        amplitude = volume,
                        modifier = Modifier.weight(1f),
                        isLeft = true
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                // 中间Mic按钮
                Button(
                    onClick = {
                        if (isRecording) {
                            // 停止录音
                            viewModel.stopStreamingRecognition()
                        } else {
                            // 开始录音
                            if (recordAudioPermission.status == PermissionStatus.Granted) {
                                viewModel.startStreamingRecognition(
                                    onResult = { source, translated ->
                                        viewModel.updateSourceText(source)
                                        viewModel.updateTranslatedText(translated)
                                    },
                                    onError = { errorMsg ->
                                        // 错误会通过viewModel.errorMessage显示
                                    }
                                )
                            } else {
                                recordAudioPermission.launchPermissionRequest()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(60.dp), // 使用size确保圆形
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (isRecording) "停止录音" else "开始录音",
                        modifier = Modifier.size(30.dp),
                        tint = if (isRecording) Color(0xFFBB271A) else Color.White
                    )
                }
                
                // 右侧音量指示器
                if (isRecording) {
                    VolumeIndicator(
                        amplitude = volume,
                        modifier = Modifier.weight(1f),
                        isLeft = false
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// QQ风格音量指示器
@Composable
fun VolumeIndicator(
    amplitude: Int,
    modifier: Modifier = Modifier,
    isLeft: Boolean = true
) {
    val normalized = (amplitude / 32767f).coerceIn(0f, 1f)
    val activeBarCount = (normalized * 10).toInt().coerceIn(0, 10)
    
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = if (isLeft) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val barIndices = if (isLeft) (9 downTo 0) else (0..9)
        
        barIndices.forEach { index ->
            val isActive = index < activeBarCount
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(32.dp)
                    .padding(horizontal = 2.dp)
                    .background(
                        color = if (isActive) Color.White else Color.White.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        }
    }
}