package com.example.test0.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.test0.model.ImageLanguage
import com.example.test0.model.getDisplayNameWithDetected
import com.example.test0.service.ImageTranslationService
import com.example.test0.service.TextToSpeechService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImageTranslationViewModel(application: Application) : AndroidViewModel(application) {

    // Services
    private val imageTranslationService = ImageTranslationService()
    private val textToSpeechService = TextToSpeechService(application.applicationContext)

    // Language selection
    private val _sourceLanguage = MutableStateFlow(ImageLanguage.AUTO)
    val sourceLanguage = _sourceLanguage.asStateFlow()

    private val _detectedLanguage = MutableStateFlow<ImageLanguage?>(null)
    val detectedLanguage = _detectedLanguage.asStateFlow()

    private val _targetLanguage = MutableStateFlow(ImageLanguage.CHINESE)
    val targetLanguage = _targetLanguage.asStateFlow()

    // Auto detection state
    private val _isAutoDetected = MutableStateFlow(false)
    val isAutoDetected = _isAutoDetected.asStateFlow()

    // Available target languages based on source language
    private val _availableTargetLanguages = MutableStateFlow<List<ImageLanguage>>(
        ImageLanguage.getTargetLanguages(ImageLanguage.AUTO)
    )
    val availableTargetLanguages = _availableTargetLanguages.asStateFlow()

    // Text content
    private val _sourceText = MutableStateFlow("")
    val sourceText = _sourceText.asStateFlow()

    private val _translatedText = MutableStateFlow("")
    val translatedText = _translatedText.asStateFlow()

    // Translation status
    private val _isTranslating = MutableStateFlow(false)
    val isTranslating = _isTranslating.asStateFlow()

    private val _isDetectingLanguage = MutableStateFlow(false)
    val isDetectingLanguage = _isDetectingLanguage.asStateFlow()

    // Text-to-speech state
    val ttsState = textToSpeechService.ttsState

    enum class TtsType { SOURCE, TARGET }
    private val _currentTtsType = MutableStateFlow<TtsType?>(null)
    val currentTtsType = _currentTtsType.asStateFlow()

    // Error handling
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var detectLanguageJob: Job? = null
    private var shouldIgnoreTranslation = false // 是否忽略翻译结果（清除时使用）

    init {
        textToSpeechService.initializeTextToSpeech()
    }

    fun updateSourceLanguage(language: ImageLanguage) {
        _sourceLanguage.value = language
        _isAutoDetected.value = false
        _detectedLanguage.value = null
        
        val availableTargetLanguages = ImageLanguage.getTargetLanguages(language)
        _availableTargetLanguages.value = availableTargetLanguages
        
        if (_targetLanguage.value !in availableTargetLanguages) {
            _targetLanguage.value = availableTargetLanguages.first()
        }
    }

    fun updateTargetLanguage(language: ImageLanguage) {
        _targetLanguage.value = language
    }

    fun translateImage(bitmap: Bitmap) {
        shouldIgnoreTranslation = false // 重置忽略标志
        _isTranslating.value = true
        viewModelScope.launch {
            try {
                Log.i("ImageTranslationVM", "Image translation started: ${_sourceLanguage.value.displayName} -> ${_targetLanguage.value.displayName}")
                
                val result = imageTranslationService.translateImage(
                    bitmap,
                    _sourceLanguage.value,
                    _targetLanguage.value
                )
                
                Log.i("ImageTranslationVM", "Image translation completed successfully")
                
                // 检查是否应该忽略翻译结果（用户可能已经点击了清除）
                if (!shouldIgnoreTranslation) {
                    _sourceText.value = result.sourceText
                    _translatedText.value = result.translatedText
                    
                    if (_sourceLanguage.value == ImageLanguage.AUTO) {
                        _detectedLanguage.value = result.detectedLanguage
                        _isAutoDetected.value = true
                        val availableTargetLanguages = ImageLanguage.getTargetLanguages(result.detectedLanguage)
                        _availableTargetLanguages.value = availableTargetLanguages
                        if (_targetLanguage.value !in availableTargetLanguages) {
                            _targetLanguage.value = availableTargetLanguages.first()
                        }
                    }
                } else {
                    Log.i("ImageTranslationVM", "Translation result ignored due to user clear action")
                }
                
            } catch (e: Exception) {
                // 只有在没有被忽略时才显示错误
                if (!shouldIgnoreTranslation) {
                    Log.e("ImageTranslationVM", "Image translation failed: ${e.message}", e)
                    _errorMessage.value = "图片翻译失败: ${e.message}"
                }
            } finally {
                _isTranslating.value = false
            }
        }
    }

    fun speakSourceText() {
        val text = _sourceText.value
        if (text.isNotBlank()) {
            stopSpeaking()
            // 使用检测到的语言或选择的源语言
            val languageToSpeak = if (_isAutoDetected.value && _detectedLanguage.value != null) {
                _detectedLanguage.value!!
            } else {
                _sourceLanguage.value
            }
            
            // 转换为原始Language枚举（用于TTS）
            val originalLanguage = convertToOriginalLanguage(languageToSpeak)
            textToSpeechService.speak(text, originalLanguage)
            _currentTtsType.value = TtsType.SOURCE
        }
    }

    fun speakTranslatedText() {
        val text = _translatedText.value
        if (text.isNotBlank()) {
            stopSpeaking()
            val originalLanguage = convertToOriginalLanguage(_targetLanguage.value)
            textToSpeechService.speak(text, originalLanguage)
            _currentTtsType.value = TtsType.TARGET
        }
    }

    fun stopSpeaking() {
        textToSpeechService.stop()
        _currentTtsType.value = null
    }

    fun swapLanguages() {
        if (_sourceLanguage.value == ImageLanguage.AUTO || _isAutoDetected.value) {
            // 如果源语言是自动检测或已自动检测，不允许交换
            return
        }

        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp

        // 更新可用目标语言列表
        _availableTargetLanguages.value = ImageLanguage.getTargetLanguages(_sourceLanguage.value)
    }

    fun clearSourceText() {
        // 设置忽略翻译标志，类似语音翻译的逻辑
        shouldIgnoreTranslation = true
        
        // 立即清空文本和重置状态
        _sourceText.value = ""
        _translatedText.value = ""
        _isAutoDetected.value = false
        _detectedLanguage.value = null
        _isTranslating.value = false // 立即恢复按钮状态
        stopSpeaking()
        
        // 延迟重置忽略标志，确保清除操作完成
        viewModelScope.launch {
            delay(500)
            shouldIgnoreTranslation = false
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun showError(message: String) {
        _errorMessage.value = message
    }

    // 转换ImageLanguage到原始Language枚举（用于TTS）
    private fun convertToOriginalLanguage(imageLanguage: ImageLanguage): com.example.test0.model.Language {
        return when (imageLanguage) {
            ImageLanguage.CHINESE -> com.example.test0.model.Language.CHINESE
            ImageLanguage.ENGLISH -> com.example.test0.model.Language.ENGLISH
            ImageLanguage.JAPANESE -> com.example.test0.model.Language.JAPANESE
            ImageLanguage.KOREAN -> com.example.test0.model.Language.KOREAN
            ImageLanguage.RUSSIAN -> com.example.test0.model.Language.RUSSIAN
            ImageLanguage.FRENCH -> com.example.test0.model.Language.FRENCH
            ImageLanguage.GERMAN -> com.example.test0.model.Language.GERMAN
            ImageLanguage.ITALIAN -> com.example.test0.model.Language.ITALIAN
            ImageLanguage.SPANISH -> com.example.test0.model.Language.SPANISH
            ImageLanguage.PORTUGUESE -> com.example.test0.model.Language.PORTUGUESE
            else -> com.example.test0.model.Language.CHINESE // 默认中文
        }
    }

    override fun onCleared() {
        stopSpeaking()
        textToSpeechService.cleanup()
        super.onCleared()
    }
}