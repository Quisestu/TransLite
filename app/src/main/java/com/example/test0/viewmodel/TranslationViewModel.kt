package com.example.test0.viewmodel

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