package com.example.test0.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIos
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.test0.model.ImageLanguage
import com.example.test0.model.getDisplayNameWithDetected
import com.example.test0.ui.components.ImageLanguageSelector
import com.example.test0.viewmodel.ImageTranslationViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageTranslationScreen(
    onNavigateBack: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: ImageTranslationViewModel = viewModel()
) {
    val sourceLanguage by viewModel.sourceLanguage.collectAsState()
    val targetLanguage by viewModel.targetLanguage.collectAsState()
    val availableTargetLanguages by viewModel.availableTargetLanguages.collectAsState()
    val isAutoDetected by viewModel.isAutoDetected.collectAsState()
    val sourceText by viewModel.sourceText.collectAsState()
    val translatedText by viewModel.translatedText.collectAsState()
    val isTranslating by viewModel.isTranslating.collectAsState()
    val isDetectingLanguage by viewModel.isDetectingLanguage.collectAsState()
    val detectedLanguage by viewModel.detectedLanguage.collectAsState()
    val currentTtsType by viewModel.currentTtsType.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageViewer by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 滚动状态
    val sourceScrollState = rememberScrollState()
    val targetScrollState = rememberScrollState()

    val clipboardManager = LocalClipboardManager.current
    val ttsState by viewModel.ttsState.collectAsState()
    val isSpeaking = ttsState is com.example.test0.service.TextToSpeechService.TTSState.Speaking

    // 创建临时文件用于相机拍照
    val tempImageFile = remember {
        File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
    }
    
    // 使用FileProvider创建安全的URI
    val tempImageUri = remember(tempImageFile) {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempImageFile
        )
    }

    // 图片转Bitmap的辅助函数
    fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            Log.e("ImageTranslation", "Failed to convert URI to Bitmap: ${e.message}", e)
            null
        }
    }

    // 相机启动器
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            selectedImageUri = tempImageUri
            // 转换图片并调用翻译API（新选择图片，重置检测状态）
            coroutineScope.launch {
                val bitmap = uriToBitmap(tempImageUri)
                if (bitmap != null) {
                    viewModel.translateImage(bitmap, shouldResetDetection = true)
                }
            }
        }
    }

    // 权限请求启动器
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // 权限已授予，启动相机
            cameraLauncher.launch(tempImageUri)
        } else {
            // 权限被拒绝，显示提示
            viewModel.showError("需要相机权限才能拍照，请在设置中授予权限")
        }
    }

    // 检查并请求相机权限的函数
    fun requestCameraPermissionAndTakePhoto() {
        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
            PackageManager.PERMISSION_GRANTED -> {
                // 权限已授予，直接启动相机
                cameraLauncher.launch(tempImageUri)
            }
            else -> {
                // 请求权限
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    // 图库启动器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            // 转换图片并调用翻译API（新选择图片，重置检测状态）
            coroutineScope.launch {
                val bitmap = uriToBitmap(it)
                if (bitmap != null) {
                    viewModel.translateImage(bitmap, shouldResetDetection = true)
                }
            }
        }
    }

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
                    Text(
                        text = "确定",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "图片翻译",
                        style = MaterialTheme.typography.titleMedium
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBackIos, contentDescription = "返回")
                    }
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
                ImageLanguageSelector(
                    selectedLanguage = (if (isAutoDetected) detectedLanguage else sourceLanguage) ?: ImageLanguage.AUTO,
                    languages = ImageLanguage.getAllSourceLanguages(),
                    onLanguageSelected = { viewModel.updateSourceLanguage(it) },
                    modifier = Modifier.weight(1f),
                    label = "源语言",
                    isDetected = isAutoDetected && detectedLanguage != null,
                    enabled = !isTranslating
                )

                // 交换按钮
                IconButton(
                    onClick = { viewModel.swapLanguages() },
                    modifier = Modifier.padding(horizontal = 8.dp),
                    enabled = !isTranslating
                ) {
                    Icon(Icons.Default.SwapHoriz, contentDescription = "交换语言")
                }

                // 目标语言选择器
                ImageLanguageSelector(
                    selectedLanguage = targetLanguage,
                    languages = availableTargetLanguages,
                    onLanguageSelected = { viewModel.updateTargetLanguage(it) },
                    modifier = Modifier.weight(1f),
                    label = "目标语言",
                    enabled = !isTranslating
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
                    // 只读文本框
                    TextField(
                        value = sourceText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(sourceScrollState)
                            .padding(horizontal = 8.dp),
                        placeholder = { Text("拍照或从图库选择") },
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
                        // 左侧：常规按钮
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {
                                if (isSpeaking && currentTtsType == com.example.test0.viewmodel.ImageTranslationViewModel.TtsType.SOURCE) {
                                    viewModel.stopSpeaking()
                                } else {
                                    viewModel.speakSourceText()
                                }
                            }) {
                                Icon(
                                    imageVector = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.ImageTranslationViewModel.TtsType.SOURCE) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                                    contentDescription = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.ImageTranslationViewModel.TtsType.SOURCE) "停止朗读" else "朗读"
                                )
                            }
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(sourceText))
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "复制")
                            }
                        }
                        
                        // 右侧：图片相关按钮
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 图片缩略图
                            selectedImageUri?.let { uri ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        .clickable { showImageViewer = true }
                                ) {
                                    Image(
                                        painter = rememberAsyncImagePainter(uri),
                                        contentDescription = "查看图片",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            
                            // 动态发送/加载按钮
                            selectedImageUri?.let {
                                if (isTranslating) {
                                    // 加载指示器
                                    Box(
                                        modifier = Modifier.size(40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                } else {
                                    // 发送按钮
                                    IconButton(
                                        onClick = {
                                            coroutineScope.launch {
                                                val bitmap = uriToBitmap(it)
                                                if (bitmap != null) {
                                                    viewModel.translateImage(bitmap, shouldResetDetection = false)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Default.Send, contentDescription = "重新翻译")
                                    }
                                }
                            }
                        }
                    }
                }
                // 右上角清除按钮
                IconButton(
                    onClick = { 
                        viewModel.clearSourceText()
                        selectedImageUri = null
                    },
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
                            if (isSpeaking && currentTtsType == com.example.test0.viewmodel.ImageTranslationViewModel.TtsType.TARGET) {
                                viewModel.stopSpeaking()
                            } else {
                                viewModel.speakTranslatedText()
                            }
                        }) {
                            Icon(
                                imageVector = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.ImageTranslationViewModel.TtsType.TARGET) Icons.Default.StopCircle else Icons.Default.VolumeUp,
                                contentDescription = if (isSpeaking && currentTtsType == com.example.test0.viewmodel.ImageTranslationViewModel.TtsType.TARGET) "停止朗读" else "朗读"
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

            // 底部按钮栏 - 两个圆形按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp, top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 拍照按钮
                Button(
                    onClick = {
                        requestCameraPermissionAndTakePhoto()
                    },
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(0.dp),
                    enabled = !isTranslating
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "拍照",
                        modifier = Modifier.size(30.dp),
                        tint = Color.White
                    )
                }

                // 图库选择按钮
                Button(
                    onClick = {
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(30.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    contentPadding = PaddingValues(0.dp),
                    enabled = !isTranslating
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "从图库选择",
                        modifier = Modifier.size(30.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
    
    // 图片查看对话框
    if (showImageViewer && selectedImageUri != null) {
        Dialog(
            onDismissRequest = { showImageViewer = false }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showImageViewer = false },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = rememberAsyncImagePainter(selectedImageUri),
                    contentDescription = "查看原图",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
                
                // 关闭按钮
                IconButton(
                    onClick = { showImageViewer = false },
                modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "关闭",
                        tint = Color.White
                    )
                }
            }
        }
    }
} 