package com.example.test0.viewmodel

/**
 * TranslationViewModel 负责管理翻译应用的核心功能：
 * 
 * 1. 翻译服务 (TranslationService)
 * - 使用 Google ML Kit Translation API
 * - 支持实时翻译
 * - 处理语言切换和文本更新
 * 
 * 2. OCR服务 (OCRService)
 * - 使用 Google ML Kit Text Recognition API
 * - 处理图片文字识别
 * - 支持从相机拍摄的图片中提取文字
 * 
 * 3. 语音服务 (SpeechService)
 * - 语音识别：使用 Android SpeechRecognizer API
 * - 文本转语音：使用 Android TextToSpeech API
 * - 管理语音识别和语音合成的生命周期
 * 
 * 状态管理：
 * - 使用 StateFlow 管理 UI 状态
 * - 处理加载、成功、错误等状态
 * - 管理源语言、目标语言、源文本、翻译文本等状态
 */

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.test0.model.Language
import com.example.test0.service.OCRService
import com.example.test0.service.SpeechService
import com.example.test0.service.TranslationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TranslationViewModel(application: Application) : AndroidViewModel(application) {
    
    // Services
    private val translationService = TranslationService()
    private val ocrService = OCRService()
    private val speechService = SpeechService(application.applicationContext)
    
    // State for UI
    private val _uiState = MutableStateFlow<TranslationUiState>(TranslationUiState.Initial)
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()
    
    // Language selection
    private val _sourceLanguage = MutableStateFlow(Language.ENGLISH)
    val sourceLanguage = _sourceLanguage.asStateFlow()
    
    private val _targetLanguage = MutableStateFlow(Language.CHINESE)
    val targetLanguage = _targetLanguage.asStateFlow()
    
    // Text content
    private val _sourceText = MutableStateFlow("")
    val sourceText = _sourceText.asStateFlow()
    
    private val _translatedText = MutableStateFlow("")
    val translatedText = _translatedText.asStateFlow()
    
    // Translation status
    private val _isTranslating = MutableStateFlow(false)
    val isTranslating = _isTranslating.asStateFlow()
    
    // Speech Recognition state
    val recognitionState = speechService.recognitionState
    
    // Text-to-speech state
    val ttsState = speechService.ttsState
    
    init {
        speechService.initializeSpeechRecognizer()
        speechService.initializeTextToSpeech()
    }
    
    fun updateSourceLanguage(language: Language) {
        _sourceLanguage.value = language
        translateIfPossible()
    }
    
    fun updateTargetLanguage(language: Language) {
        _targetLanguage.value = language
        translateIfPossible()
    }
    
    fun updateSourceText(text: String) {
        _sourceText.value = text
        translateIfPossible()
    }
    
    private fun translateIfPossible() {
        val sourceText = _sourceText.value
        if (sourceText.isNotBlank()) {
            translate(sourceText)
        }
    }
    
    fun translate(text: String) {
        _isTranslating.value = true
        viewModelScope.launch {
            try {
                val result = translationService.translate(
                    text,
                    _sourceLanguage.value,
                    _targetLanguage.value
                )
                _translatedText.value = result
                _uiState.value = TranslationUiState.Success
            } catch (e: Exception) {
                _uiState.value = TranslationUiState.Error("Translation failed: ${e.message}")
            } finally {
                _isTranslating.value = false
            }
        }
    }
    
    fun recognizeTextFromImage(bitmap: Bitmap) {
        _uiState.value = TranslationUiState.Loading
        viewModelScope.launch {
            try {
                val recognizedText = ocrService.recognizeText(bitmap)
                _sourceText.value = recognizedText
                translate(recognizedText)
            } catch (e: Exception) {
                _uiState.value = TranslationUiState.Error("OCR failed: ${e.message}")
            }
        }
    }
    
    fun startSpeechRecognition() {
        viewModelScope.launch {
            try {
                val recognizedText = speechService.startListening(_sourceLanguage.value)
                if (recognizedText.isNotBlank()) {
                    _sourceText.value = recognizedText
                    translate(recognizedText)
                }
            } catch (e: Exception) {
                _uiState.value = TranslationUiState.Error("Speech recognition failed: ${e.message}")
            }
        }
    }
    
    fun speakTranslatedText() {
        val text = _translatedText.value
        if (text.isNotBlank()) {
            speechService.speak(text, _targetLanguage.value)
        }
    }
    
    fun swapLanguages() {
        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp
        
        // Swap text as well
        val tempText = _sourceText.value
        _sourceText.value = _translatedText.value
        
        // Translate back
        if (_sourceText.value.isNotBlank()) {
            translate(_sourceText.value)
        }
    }
    
    override fun onCleared() {
        translationService.cleanUp()
        ocrService.cleanUp()
        speechService.cleanup()
        super.onCleared()
    }
    
    sealed class TranslationUiState {
        object Initial : TranslationUiState()
        object Loading : TranslationUiState()
        object Success : TranslationUiState()
        data class Error(val message: String) : TranslationUiState()
    }
} 