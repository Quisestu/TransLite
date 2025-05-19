package com.example.test0.ui.screens

import android.Manifest
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
    
    // Camera state
    var showCamera by remember { mutableStateOf(false) }
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    // Audio recording state
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    
    // Handle errors
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
                // Language selectors
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
                        label = "From"
                    )
                    
                    IconButton(
                        onClick = { viewModel.swapLanguages() },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Swap languages"
                        )
                    }
                    
                    LanguageSelector(
                        selectedLanguage = targetLanguage,
                        languages = Language.getAllLanguages(),
                        onLanguageSelected = { viewModel.updateTargetLanguage(it) },
                        modifier = Modifier.weight(1f),
                        label = "To"
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Translation card
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
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Feature description
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Features",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "• Multi-language Translation - Support for 12 languages",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "• OCR Text Recognition - Extract text from images",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "• Speech Recognition - Convert speech to text",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        Text(
                            text = "• Text-to-Speech - Listen to translations",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
} 