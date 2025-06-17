package com.example.test0.viewmodel

// TranslationViewModel 负责管理翻译应用的核心功能：
// - 腾讯云机器翻译API（文本、图片翻译）
// - 语音识别（Android SpeechRecognizer）
// - 文本转语音（Android TextToSpeech）
// - 状态管理（StateFlow）

import android.Manifest
import android.app.Application
import android.graphics.Bitmap
import androidx.annotation.RequiresPermission
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.test0.model.Language
import com.example.test0.service.TextToSpeechService
import com.example.test0.service.TencentTranslationService
import com.example.test0.service.TencentStreamingSpeechService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.test0.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request

class TranslationViewModel(application: Application) : AndroidViewModel(application) {

    // Services
    private val translationService = TencentTranslationService()
    private val textToSpeechService = TextToSpeechService(application.applicationContext)

    // State for UI
    private val _uiState = MutableStateFlow<TranslationUiState>(TranslationUiState.Initial)
    val uiState: StateFlow<TranslationUiState> = _uiState.asStateFlow()

    // Language selection
    private val _sourceLanguage = MutableStateFlow(Language.AUTO)
    val sourceLanguage = _sourceLanguage.asStateFlow()

    // 检测到的实际语言，仅在自动检测时有效
    private val _detectedLanguage = MutableStateFlow<Language?>(null)
    val detectedLanguage = _detectedLanguage.asStateFlow()

    private val _targetLanguage = MutableStateFlow(Language.CHINESE)
    val targetLanguage = _targetLanguage.asStateFlow()

    // Auto detection state
    private val _isAutoDetected = MutableStateFlow(false)
    val isAutoDetected = _isAutoDetected.asStateFlow()

    // Available target languages based on source language
    private val _availableTargetLanguages = MutableStateFlow<List<Language>>(Language.getTargetLanguages(Language.AUTO))
    val availableTargetLanguages = _availableTargetLanguages.asStateFlow()

    // Text content
    private val _sourceText = MutableStateFlow("")
    val sourceText = _sourceText.asStateFlow()

    private val _translatedText = MutableStateFlow("")
    val translatedText = _translatedText.asStateFlow()

    // Translation status
    private val _isTranslating = MutableStateFlow(false)
    val isTranslating = _isTranslating.asStateFlow()

    // Can translate flag
    private val _canTranslate = MutableStateFlow(false)
    val canTranslate = _canTranslate.asStateFlow()

    // Text-to-speech state
    val ttsState = textToSpeechService.ttsState

    private var streamingService: TencentStreamingSpeechService? = null

    // 在 TranslationViewModel 类中添加一个 Job 变量
    private var detectLanguageJob: Job? = null

    // 添加 isDetectingLanguage 状态
    private val _isDetectingLanguage = MutableStateFlow(false)
    val isDetectingLanguage = _isDetectingLanguage.asStateFlow()

    // 源文本字符计数
    private val _sourceTextCharCount = MutableStateFlow(0)
    val sourceTextCharCount = _sourceTextCharCount.asStateFlow()

    enum class TtsType { SOURCE, TARGET }
    private val _currentTtsType = MutableStateFlow<TtsType?>(null)
    val currentTtsType = _currentTtsType.asStateFlow()

    init {
        textToSpeechService.initializeTextToSpeech()
    }

    fun updateSourceLanguage(language: Language) {
        _sourceLanguage.value = language
        _isAutoDetected.value = false
        _detectedLanguage.value = null
        val availableTargetLanguages = Language.getTargetLanguages(language)
        _availableTargetLanguages.value = availableTargetLanguages
        if (_targetLanguage.value !in availableTargetLanguages) {
            _targetLanguage.value = availableTargetLanguages.first()
        }
        _canTranslate.value = language != Language.AUTO
        // 自动检测时立即触发语种识别
        if (language == Language.AUTO) {
            val text = _sourceText.value
            if (text.isNotBlank()) {
                detectLanguageJob?.cancel()
                detectLanguageJob = viewModelScope.launch {
                    _isDetectingLanguage.value = true
                    delay(250)
                    try {
                        Log.d(
                            "TranslationVM",
                            "Detecting language for text: $text (triggered by AUTO switch)"
                        )
                        val detected = translationService.detectLanguage(text)
                        Log.d("TranslationVM", "Detected language: $detected")
                        _detectedLanguage.value = detected
                        _isAutoDetected.value = true
                        val detectedTargets = Language.getTargetLanguages(detected)
                        _availableTargetLanguages.value = detectedTargets
                        if (!_availableTargetLanguages.value.contains(_targetLanguage.value)) {
                            _targetLanguage.value = _availableTargetLanguages.value.first()
                        }
                    } catch (e: Exception) {
                        Log.e("TranslationVM", "Language detection failed: ${e.message}", e)
                        _isAutoDetected.value = false
                        _detectedLanguage.value = null
                        _canTranslate.value = false
                        _uiState.value = TranslationUiState.Error("语言检测失败: ${e.message}")
                    } finally {
                        _isDetectingLanguage.value = false
                    }
                }
            }
        }
    }

    fun updateTargetLanguage(language: Language) {
        _targetLanguage.value = language
    }

    fun updateSourceText(text: String) {
        _sourceText.value = text
        _sourceTextCharCount.value = text.length
        _canTranslate.value = text.isNotBlank()

        if (text.isEmpty()) {
            // 确保取消当前的检测任务
            detectLanguageJob?.cancel()
            // 文本为空时重置检测状态
            _isAutoDetected.value = false
            _detectedLanguage.value = null
            _translatedText.value = ""
        } else if (_sourceLanguage.value == Language.AUTO) {
            detectLanguageJob?.cancel()
            detectLanguageJob = viewModelScope.launch {
                _isDetectingLanguage.value = true
                delay(250)
                try {
                    // log: 开始调用语种检测API
                    Log.d("TranslationVM", "Detecting language for text: $text")
                    val detected = translationService.detectLanguage(text)
                    // log: 语种检测API返回的结果
                    Log.d("TranslationVM", "Detected language: $detected")
                    _detectedLanguage.value = detected
                    _isAutoDetected.value = true
                    val availableTargetLanguages = Language.getTargetLanguages(detected)
                    _availableTargetLanguages.value = availableTargetLanguages
                    if (!_availableTargetLanguages.value.contains(_targetLanguage.value)) {
                        _targetLanguage.value = availableTargetLanguages.first()
                    }
                } catch (e: Exception) {
                    // log: 语种检测API调用出错
                    Log.e("TranslationVM", "Language detection failed: ${e.message}", e)
                    _isAutoDetected.value = false
                    _detectedLanguage.value = null
                    _canTranslate.value = false
                    _uiState.value = TranslationUiState.Error("语言检测失败: ${e.message}")
                } finally {
                    _isDetectingLanguage.value = false
                }
            }
        }
    }

    fun translate() {
        viewModelScope.launch {
            _isTranslating.value = true
            try {
                // log: 开始调用翻译API
                Log.d(
                    "TranslationVM",
                    "Translating from ${_sourceLanguage.value} to ${_targetLanguage.value}, text: ${_sourceText.value}"
                )
                val result = translationService.translateText(
                    _sourceText.value,
                    _sourceLanguage.value,
                    _targetLanguage.value
                )
                // log: 翻译API返回的结果
                Log.d("TranslationVM", "Translation result: $result")
                _translatedText.value = result
                _uiState.value = TranslationUiState.Success
            } catch (e: Exception) {
                // log: 翻译API调用出错
                Log.e("TranslationVM", "Translation failed: ${e.message}", e)
                _uiState.value = TranslationUiState.Error("翻译失败: ${e.message}")
            } finally {
                _isTranslating.value = false
            }
        }
    }

    fun translateImage(bitmap: Bitmap) {
        _isTranslating.value = true
        viewModelScope.launch {
            try {
                val result = translationService.translateImage(
                    bitmap,
                    _sourceLanguage.value,
                    _targetLanguage.value
                )
                _sourceText.value = result.sourceText
                _translatedText.value = result.translatedText
                if (_sourceLanguage.value == Language.AUTO) {
                    _sourceLanguage.value = result.detectedLanguage
                    _isAutoDetected.value = true
                    _availableTargetLanguages.value =
                        Language.getTargetLanguages(result.detectedLanguage)
                }
                _canTranslate.value = true
                _uiState.value = TranslationUiState.Success
            } catch (e: Exception) {
                _uiState.value = TranslationUiState.Error("图片翻译失败: ${e.message}")
            } finally {
                _isTranslating.value = false
            }
        }
    }

    fun speakSourceText() {
        val text = _sourceText.value
        if (text.isNotBlank()) {
            stopSpeaking()
            textToSpeechService.speak(text, _sourceLanguage.value)
            _currentTtsType.value = TtsType.SOURCE
        }
    }

    fun speakTranslatedText() {
        val text = _translatedText.value
        if (text.isNotBlank()) {
            stopSpeaking()
            textToSpeechService.speak(text, _targetLanguage.value)
            _currentTtsType.value = TtsType.TARGET
        }
    }

    fun stopSpeaking() {
        textToSpeechService.stop()
        _currentTtsType.value = null
    }

    fun swapLanguages() {
        if (_sourceLanguage.value == Language.AUTO || _isAutoDetected.value) {
            // 如果源语言是自动检测或已自动检测，不允许交换
            return
        }

        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp

        // 更新可用目标语言列表
        _availableTargetLanguages.value = Language.getTargetLanguages(_sourceLanguage.value)
    }

    fun clearSourceText() {
        _sourceText.value = ""
        _translatedText.value = ""
        _sourceTextCharCount.value = 0
        _canTranslate.value = false
        // 清除时也重置检测状态
        _isAutoDetected.value = false
        _detectedLanguage.value = null
        stopSpeaking()
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startStreamingRecognition(onResult: (String, String) -> Unit, onError: (String) -> Unit) {
        streamingService = TencentStreamingSpeechService(
            appId = BuildConfig.TENCENT_APP_ID,
            secretId = BuildConfig.TENCENT_SECRET_ID,
            secretKey = BuildConfig.TENCENT_SECRET_KEY,
            sourceLang = _sourceLanguage.value.code,
            targetLang = _targetLanguage.value.code,
            onResult = onResult,
            onError = onError
        )
        streamingService?.startStreaming()
    }

    fun stopStreamingRecognition() {
        streamingService?.stopStreaming()
    }

    override fun onCleared() {
        textToSpeechService.cleanup()
        super.onCleared()
    }

    sealed class TranslationUiState {
        data object Initial : TranslationUiState()
        data object Loading : TranslationUiState()
        data object Success : TranslationUiState()
        data class Error(val message: String) : TranslationUiState()
    }
}