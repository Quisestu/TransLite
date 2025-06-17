package com.example.test0.ui.screens

// // 原有内容全部注释，便于后续恢复
// import android.net.Uri
// import androidx.activity.compose.rememberLauncherForActivityResult
// import androidx.activity.result.contract.ActivityResultContracts
// import androidx.compose.foundation.Image
// import androidx.compose.foundation.layout.*
// import androidx.compose.material.icons.Icons
// import androidx.compose.material.icons.filled.ArrowBackIos
// import androidx.compose.material.icons.filled.Camera
// import androidx.compose.material.icons.filled.Clear
// import androidx.compose.material.icons.filled.ContentCopy
// import androidx.compose.material.icons.filled.Image
// import androidx.compose.material.icons.filled.Send
// import androidx.compose.material.icons.filled.VolumeUp
// import androidx.compose.material3.*
// import androidx.compose.runtime.*
// import androidx.compose.ui.Alignment
// import androidx.compose.ui.Modifier
// import androidx.compose.ui.layout.ContentScale
// import androidx.compose.ui.platform.LocalContext
// import androidx.compose.ui.text.style.TextAlign
// import androidx.compose.ui.unit.dp
// import androidx.lifecycle.viewmodel.compose.viewModel
// import coil.compose.rememberAsyncImagePainter
// import com.example.test0.viewmodel.TranslationViewModel
//
// @OptIn(ExperimentalMaterial3Api::class)
// @Composable
// fun ImageTranslationScreen(
// onNavigateBack: () -> Unit,
// viewModel: TranslationViewModel = viewModel()
// ) {
// var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
// val sourceText by viewModel.sourceText.collectAsState()
// val translatedText by viewModel.translatedText.collectAsState()
// val canTranslate by viewModel.canTranslate.collectAsState()
// val isTranslating by viewModel.isTranslating.collectAsState()
// val context = LocalContext.current
//
// // 相机启动器
// val cameraLauncher = rememberLauncherForActivityResult(
// contract = ActivityResultContracts.TakePicture()
// ) { success ->
// if (success) {
// // TODO: 处理拍摄的照片
// }
// }
//
// // 图库启动器
// val galleryLauncher = rememberLauncherForActivityResult(
// contract = ActivityResultContracts.GetContent()
// ) { uri ->
// uri?.let {
// selectedImageUri = it
// // TODO: 处理选择的图片
// }
// }
//
// Scaffold(
// topBar = {
// TopAppBar(
// title = { Text("图片翻译") },
// navigationIcon = {
// IconButton(onClick = onNavigateBack) {
// Icon(Icons.Default.ArrowBackIos, contentDescription = "返回")
// }
// }
// )
// }
// ) { paddingValues ->
// Column(
// modifier = Modifier
// .fillMaxSize()
// .padding(paddingValues)
// .padding(16.dp),
// horizontalAlignment = Alignment.CenterHorizontally
// ) {
// // 图片选择按钮
// Row(
// modifier = Modifier
// .fillMaxWidth()
// .padding(bottom = 16.dp),
// horizontalArrangement = Arrangement.SpaceEvenly
// ) {
// Button(
// onClick = { galleryLauncher.launch("image/*") },
// modifier = Modifier.weight(1f).padding(end = 8.dp)
// ) {
// Icon(Icons.Default.Image, contentDescription = "从图库选择")
// Spacer(modifier = Modifier.width(8.dp))
// Text("从图库选择")
// }
//
// Button(
// onClick = { /* TODO: 启动相机 */ },
// modifier = Modifier.weight(1f).padding(start = 8.dp)
// ) {
// Icon(Icons.Default.Camera, contentDescription = "拍照")
// Spacer(modifier = Modifier.width(8.dp))
// Text("拍照")
// }
// }
//
// // 预览图片
// selectedImageUri?.let { uri ->
// Image(
// painter = rememberAsyncImagePainter(uri),
// contentDescription = "预览图片",
// modifier = Modifier
// .fillMaxWidth()
// .height(200.dp)
// .padding(vertical = 16.dp),
// contentScale = ContentScale.Fit
// )
// }
//
// // 原文显示区
// Box(
// modifier = Modifier
// .weight(1f)
// .fillMaxWidth()
// .background(MaterialTheme.colorScheme.surfaceVariant)
// ) {
// Column(modifier = Modifier.fillMaxSize()) {
// // 标题栏
// Text(
// text = if (sourceLanguage == Language.CHINESE) "中文" else "English",
// style = MaterialTheme.typography.titleMedium,
// modifier = Modifier
// .fillMaxWidth()
// .background(Color.Transparent)
// .padding(8.dp),
// textAlign = TextAlign.Start
// )
// // 可滚动文本
// Box(modifier = Modifier
// .weight(1f)
// .verticalScroll(sourceScrollState)
// .padding(horizontal = 8.dp, vertical = 4.dp)
// ) {
// Text(
// text = sourceText,
// style = MaterialTheme.typography.bodyLarge,
// color = MaterialTheme.colorScheme.onSurface
// )
// }
// // 按钮行
// Row(
// modifier = Modifier.fillMaxWidth(),
// horizontalArrangement = Arrangement.End
// ) {
// IconButton(onClick = {
// viewModel.clearSourceText()
// }) {
// Icon(Icons.Default.Clear, contentDescription = "清除")
// }
// IconButton(onClick = { /* TODO: 复制 */ }) {
// Icon(Icons.Default.ContentCopy, contentDescription = "复制")
// }
// IconButton(onClick = { /* TODO: 朗读 */ }) {
// Icon(Icons.Default.VolumeUp, contentDescription = "朗读")
// }
// }
// }
// }
// Spacer(modifier = Modifier.height(8.dp))
//
// // 译文显示区
// Box(
// modifier = Modifier
// .weight(1f)
// .fillMaxWidth()
// .background(MaterialTheme.colorScheme.surfaceVariant)
// ) {
// Column(modifier = Modifier.fillMaxSize()) {
// // 标题栏
// Text(
// text = if (targetLanguage == Language.CHINESE) "中文" else "English",
// style = MaterialTheme.typography.titleMedium,
// modifier = Modifier
// .fillMaxWidth()
// .background(Color.Transparent)
// .padding(8.dp),
// textAlign = TextAlign.Start
// )
// // 可滚动文本
// Box(modifier = Modifier
// .weight(1f)
// .verticalScroll(targetScrollState)
// .padding(horizontal = 8.dp, vertical = 4.dp)
// ) {
// Text(
// text = translatedText,
// style = MaterialTheme.typography.bodyLarge,
// color = MaterialTheme.colorScheme.onSurface
// )
// }
// // 按钮行
// Row(
// modifier = Modifier.fillMaxWidth(),
// horizontalArrangement = Arrangement.End
// ) {
// IconButton(onClick = { /* TODO: 复制 */ }) {
// Icon(Icons.Default.ContentCopy, contentDescription = "复制")
// }
// IconButton(onClick = { /* TODO: 朗读 */ }) {
// Icon(Icons.Default.VolumeUp, contentDescription = "朗读")
// }
// }
// }
// }
//
// // 翻译按钮
// Button(
// onClick = { viewModel.translateImage(bitmap) },
// enabled = canTranslate && !isTranslating,
// modifier = Modifier.fillMaxWidth()
// ) {
// Icon(Icons.Default.Send, contentDescription = "发送")
// }
// }
// }
// }