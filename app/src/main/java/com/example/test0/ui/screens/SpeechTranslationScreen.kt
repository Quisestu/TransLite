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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test0.model.Language
import com.example.test0.viewmodel.TranslationViewModel
import com.example.test0.ui.components.LanguageSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeechTranslationScreen(
    onNavigateBack: () -> Unit,
    viewModel: TranslationViewModel = viewModel()
) {
    // 仅支持中英互换
    val sourceLanguage by viewModel.sourceLanguage.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val sourceText by viewModel.sourceText.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val canTranslate by viewModel.canTranslate.collectAsState()
    var isRecording by remember { mutableStateOf(false) }

    // 滚动状态
    val sourceScrollState = rememberScrollState()
    val targetScrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("语音翻译") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIos, contentDescription = "返回")
                    }
                }
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 仅支持中英
                LanguageSelector(
                    selectedLanguage = sourceLanguage,
                    languages = listOf(Language.CHINESE, Language.ENGLISH),
                    onLanguageSelected = {}, // 不可选
                    modifier = Modifier.weight(1f),
                    label = "源语言"
                )
                IconButton(
                    onClick = { viewModel.swapLanguages() },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "交换语言")
                }
                LanguageSelector(
                    selectedLanguage = targetLanguage,
                    languages = listOf(Language.CHINESE, Language.ENGLISH),
                    onLanguageSelected = {}, // 不可选
                    modifier = Modifier.weight(1f),
                    label = "目标语言"
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 上方：原文显示区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 标题栏
                    Text(
                        text = if (sourceLanguage == Language.CHINESE) "中文" else "English",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(8.dp),
                        textAlign = TextAlign.Start
                    )
                    // 可滚动文本
                    Box(modifier = Modifier
                        .weight(1f)
                        .verticalScroll(sourceScrollState)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = sourceText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // 按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = {
                            viewModel.clearSourceText()
                        }) {
                            Icon(Icons.Default.Clear, contentDescription = "清除")
                        }
                        IconButton(onClick = { /* TODO: 复制 */ }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                        }
                        IconButton(onClick = { /* TODO: 朗读 */ }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "朗读")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            // 下方：译文显示区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // 标题栏
                    Text(
                        text = if (targetLanguage == Language.CHINESE) "中文" else "English",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Transparent)
                            .padding(8.dp),
                        textAlign = TextAlign.Start
                    )
                    // 可滚动文本
                    Box(modifier = Modifier
                        .weight(1f)
                        .verticalScroll(targetScrollState)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = translatedText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // 按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { /* TODO: 复制 */ }) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                        }
                        IconButton(onClick = { /* TODO: 朗读 */ }) {
                            Icon(Icons.Default.VolumeUp, contentDescription = "朗读")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // 录音按钮
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = {
                        if (isRecording) {
                            viewModel.stopStreamingRecognition()
                            viewModel.clearSourceText()
                        } else {
                            viewModel.clearSourceText()
                            viewModel.startStreamingRecognition(
                                onResult = { src, tgt ->
                                    viewModel.updateSourceText(src)
                                    // 译文可通过 viewModel.updateTranslatedText(tgt) 实现
                                },
                                onError = { /* TODO: 错误处理 */ }
                            )
                        }
                        isRecording = !isRecording
                    },
                    modifier = Modifier.size(72.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = if (isRecording) "停止录音" else "开始录音",
                        modifier = Modifier.size(48.dp),
                        tint = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
} 