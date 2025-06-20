package com.example.test0.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.test0.R
import com.example.test0.model.Language
import com.example.test0.model.getDisplayNameWithDetected
import com.example.test0.ui.components.LanguageSelector
import com.example.test0.viewmodel.TranslationViewModel
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextTranslationScreen(
    onNavigateToSpeech: () -> Unit,
    onNavigateToImage: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: TranslationViewModel = viewModel()
) {
    val sourceLanguage by viewModel.sourceLanguage.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val availableTargetLanguages by viewModel.availableTargetLanguages.collectAsState()
    val isAutoDetected by viewModel.isAutoDetected.collectAsState()
    val sourceText by viewModel.sourceText.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val canTranslate by viewModel.canTranslate.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val isDetectingLanguage by viewModel.isDetectingLanguage.collectAsState()
    val detectedLanguage by viewModel.detectedLanguage.collectAsState()
    val currentTtsType by viewModel.currentTtsType.collectAsState()

    // 新增：滚动状态
    val sourceScrollState = rememberScrollState()
    val targetScrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 源文本输入时自动滚动到底部
    LaunchedEffect(sourceText) {
        if (sourceText.isNotEmpty()) {
            coroutineScope.launch {
                // 稍微延迟以确保TextField布局完成
                kotlinx.coroutines.delay(50)
                // 只有当前滚动位置接近底部时（或没有滚动空间时）才自动滚动
                val currentPosition = sourceScrollState.value
                val maxPosition = sourceScrollState.maxValue
                if (maxPosition == 0 || currentPosition >= maxPosition - 100) {
                    sourceScrollState.animateScrollTo(maxPosition)
                }
            }
        }
    }

    // 翻译完成后源文本滚动回顶部，便于对比查看
    LaunchedEffect(isTranslating, translatedText) {
        // 当翻译完成（不再翻译中）且有翻译结果时，滚动到顶部
        if (!isTranslating && translatedText.isNotEmpty()) {
            coroutineScope.launch {
                kotlinx.coroutines.delay(100) // 稍长延迟确保翻译结果完全显示
                sourceScrollState.animateScrollTo(0)
            }
        }
    }

    val shadowsFont = FontFamily(Font(R.font.shadows_into_light_two_regular))

    val clipboardManager = LocalClipboardManager.current
    val ttsState by viewModel.ttsState.collectAsState()
    val isSpeaking = ttsState is com.example.test0.service.TextToSpeechService.TTSState.Speaking

    // 离开界面时自动停止朗读
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopSpeaking()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val configuration = LocalConfiguration.current
                    val density = LocalDensity.current
                    val screenWidth = configuration.screenWidthDp
                    val titleFontSizeDp = when {
                        screenWidth < 360 -> 28.dp
                        screenWidth < 400 -> 34.dp
                        else -> 40.dp
                    }
                    val titleFontSize = with(density) { titleFontSizeDp.toSp() }
                    Text(
                        text = "TransLite",
                        fontSize = titleFontSize,
                        fontFamily = shadowsFont,
                        modifier = Modifier
                            .padding(16.dp)
                            .offset(x = (-16).dp)
                    )
                },
                actions = {
                    // 历史记录按钮
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            Icons.Default.History, 
                            contentDescription = "历史记录",
                            tint = MaterialTheme.colorScheme.onPrimary
                            )
                    }
                    // 设置按钮
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "设置",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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
                .padding(16.dp)
        ) {
            // 语言选择器
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 源语言选择器
                LanguageSelector(
                    selectedLanguage = (if (isAutoDetected) detectedLanguage else sourceLanguage) ?: Language.AUTO,
                    languages = Language.getAllLanguages(),
                    onLanguageSelected = { viewModel.updateSourceLanguage(it) },
                    modifier = Modifier.weight(1f),
                    label = "源语言",
                    isDetected = isAutoDetected && detectedLanguage != null,
                    enabled = !isTranslating && !isDetectingLanguage
                )

                // 交换按钮
                IconButton(
                    onClick = { viewModel.swapLanguages() },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    enabled = !isTranslating && !isDetectingLanguage
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "交换语言")
                }

                // 目标语言选择器
                LanguageSelector(
                    selectedLanguage = targetLanguage,
                    languages = availableTargetLanguages,
                    onLanguageSelected = { viewModel.updateTargetLanguage(it) },
                    modifier = Modifier.weight(1f),
                    label = "目标语言",
                    enabled = !isTranslating && !isDetectingLanguage
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
                    // 标题栏
                    val sourceTitle = if (isAutoDetected && detectedLanguage != null) {
                        detectedLanguage!!.getDisplayNameWithDetected(true)
                    } else {
                        sourceLanguage.getDisplayNameWithDetected(false)
                    }
                    Text(
                        text = sourceTitle,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                        textAlign = TextAlign.Start
                    )
                    // 输入框
                    TextField(
                        value = sourceText,
                        onValueChange = { viewModel.updateSourceText(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(sourceScrollState)
                            .padding(horizontal = 8.dp),
                        placeholder = { Text(text="输入文本") },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                    // 底部按钮行
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row {
                            IconButton(onClick = {
                                if (isSpeaking && currentTtsType == com.example.test0.viewmodel.TranslationViewModel.TtsType.SOURCE) {
                                    viewModel.stopSpeaking()
                                } else {
                                    viewModel.speakSourceText()
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.TranslationViewModel.TtsType.SOURCE) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.TranslationViewModel.TtsType.SOURCE) "停止朗读" else "朗读"
                                )
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(sourceText))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val charCount = sourceText.length
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = if (charCount > 2000) Color.Red else Color.Gray)) {
                                        append("$charCount")
                                    }
                                    withStyle(SpanStyle(color = Color.Gray)) {
                                        append("/2000")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            IconButton(
                                onClick = { viewModel.translate() },
                                enabled = charCount in 1..2000 && canTranslate && !isTranslating && !(sourceLanguage == Language.AUTO && isDetectingLanguage)
                            ) {
                                if (isTranslating) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = "发送")
                                }
                            }
                        }
                    }
                }
                // 右上角清除按钮
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
                    // 标题栏
                    val targetTitle = targetLanguage.displayName
                    Text(
                        text = targetTitle,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier
                            .padding(start = 8.dp, top = 16.dp, bottom = 4.dp),
                        textAlign = TextAlign.Start
                    )
                    // 译文显示区
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
                    // 底部按钮行
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            if (isSpeaking && currentTtsType == com.example.test0.viewmodel.TranslationViewModel.TtsType.TARGET) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.speakTranslatedText()
                            }
                        }) {
                            Icon(
                                imageVector = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.TranslationViewModel.TtsType.TARGET) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                                contentDescription = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.TranslationViewModel.TtsType.TARGET) "停止朗读" else "朗读"
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

            // 底部图片翻译、语音翻译按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onNavigateToImage,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 16.dp)
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    val configuration = LocalConfiguration.current
                    val density = LocalDensity.current
                    val screenWidth = configuration.screenWidthDp
                    val iconSizeDp = when {
                        screenWidth < 360 -> 20.dp
                        screenWidth < 400 -> 22.dp
                        else -> 24.dp
                    }
                    val textFontSizeDp = when {
                        screenWidth < 360 -> 16.dp
                        screenWidth < 400 -> 17.dp
                        else -> 18.dp
                    }
                    val textFontSize = with(density) { textFontSizeDp.toSp() }
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(iconSizeDp), tint = Color.White)
                    
                    Spacer(Modifier.width(8.dp))
                    Text("图片翻译", fontSize = textFontSize, color = Color.White)
                }
                Button(
                    onClick = onNavigateToSpeech,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                        .height(60.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    val configuration = LocalConfiguration.current
                    val density = LocalDensity.current
                    val screenWidth = configuration.screenWidthDp
                    val iconSizeDp = when {
                        screenWidth < 360 -> 20.dp
                        screenWidth < 400 -> 22.dp
                        else -> 24.dp
                    }
                    val textFontSizeDp = when {
                        screenWidth < 360 -> 16.dp
                        screenWidth < 400 -> 17.dp
                        else -> 18.dp
                    }
                    val textFontSize = with(density) { textFontSizeDp.toSp() }
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(iconSizeDp), tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("语音翻译", fontSize = textFontSize, color = Color.White)
                }
            }
        }
    }
} 