package com.example.test0.viewmodel

// TranslationViewModel 负责管理翻译应用的核心功能：
// - 腾讯云机器翻译API（文本翻译）
// - 语音识别（Android SpeechRecognizer）
// - 文本转语音（Android TextToSpeech）
// - 状态管理（StateFlow）

import android.Manifest
import android.app.Application
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
import kotlinx.coroutines.CancellationException
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import com.example.test0.repository.TranslationRepository
import com.example.test0.model.TranslationType

class TranslationViewModel(application: Application) : AndroidViewModel(application) {

    // Services
    private val translationService = TencentTranslationService()
    private val textToSpeechService = TextToSpeechService(application.applicationContext)
    private val translationRepository = TranslationRepository.getInstance(application.applicationContext)

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
    private var translateJob: Job? = null

    // 添加 isDetectingLanguage 状态
    private val _isDetectingLanguage = MutableStateFlow(false)
    val isDetectingLanguage = _isDetectingLanguage.asStateFlow()

    // 源文本字符计数
    private val _sourceTextCharCount = MutableStateFlow(0)
    val sourceTextCharCount = _sourceTextCharCount.asStateFlow()

    // 是否忽略翻译结果（清除时使用）
    private var shouldIgnoreTranslation = false

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
                        Log.i("TranslationVM", "Language detection started")
                        val detected = translationService.detectLanguage(text)
                        Log.i("TranslationVM", "Language detected: ${detected.displayName}")
                        
                        // 检查是否应该忽略检测结果
                        if (!shouldIgnoreTranslation) {
                            _detectedLanguage.value = detected
                            _isAutoDetected.value = true
                            val detectedTargets = Language.getTargetLanguages(detected)
                            _availableTargetLanguages.value = detectedTargets
                            if (!_availableTargetLanguages.value.contains(_targetLanguage.value)) {
                                _targetLanguage.value = detectedTargets.first()
                            }
                        } else {
                            Log.i("TranslationVM", "Language detection result ignored due to user clear action")
                        }
                    } catch (e: CancellationException) {
                        // 协程取消是正常操作（用户点击清除），不显示错误
                        Log.d("TranslationVM", "Language detection cancelled by user")
                        throw e // 重新抛出CancellationException以正确处理协程取消
                    } catch (e: Exception) {
                        // 只有在没有被忽略时才显示错误
                        if (!shouldIgnoreTranslation) {
                            Log.e("TranslationVM", "Language detection failed: ${e.message}", e)
                            _isAutoDetected.value = false
                            _detectedLanguage.value = null
                            _canTranslate.value = false
                            _uiState.value = TranslationUiState.Error("语言检测失败: ${e.message}")
                        }
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
            // 文本为空时重置检测状态到初始状态
            _isAutoDetected.value = false
            _detectedLanguage.value = null
            _translatedText.value = ""
            
            // 如果当前是自动检测模式，重置目标语言列表为AUTO对应的列表
            if (_sourceLanguage.value == Language.AUTO) {
                val autoTargetLanguages = Language.getTargetLanguages(Language.AUTO)
                _availableTargetLanguages.value = autoTargetLanguages
                // 如果当前目标语言不在AUTO的列表中，重置为第一个
                if (_targetLanguage.value !in autoTargetLanguages) {
                    _targetLanguage.value = autoTargetLanguages.first()
                }
            }
        } else if (_sourceLanguage.value == Language.AUTO) {
            detectLanguageJob?.cancel()
            detectLanguageJob = viewModelScope.launch {
                _isDetectingLanguage.value = true
                delay(250)
                                    try {
                        Log.i("TranslationVM", "Language detection started")
                        val detected = translationService.detectLanguage(text)
                        Log.i("TranslationVM", "Language detected: ${detected.displayName}")
                        
                        // 检查是否应该忽略检测结果
                        if (!shouldIgnoreTranslation) {
                            _detectedLanguage.value = detected
                            _isAutoDetected.value = true
                            val availableTargetLanguages = Language.getTargetLanguages(detected)
                            _availableTargetLanguages.value = availableTargetLanguages
                            if (!_availableTargetLanguages.value.contains(_targetLanguage.value)) {
                                _targetLanguage.value = availableTargetLanguages.first()
                            }
                        } else {
                            Log.i("TranslationVM", "Language detection result ignored due to user clear action")
                        }
                    } catch (e: CancellationException) {
                        // 协程取消是正常操作（用户点击清除），不显示错误
                        Log.d("TranslationVM", "Language detection cancelled by user")
                        throw e // 重新抛出CancellationException以正确处理协程取消
                    } catch (e: Exception) {
                        // 只有在没有被忽略时才显示错误
                        if (!shouldIgnoreTranslation) {
                            Log.e("TranslationVM", "Language detection failed: ${e.message}", e)
                            _isAutoDetected.value = false
                            _detectedLanguage.value = null
                            _canTranslate.value = false
                            _uiState.value = TranslationUiState.Error("语言检测失败: ${e.message}")
                        }
                    } finally {
                    _isDetectingLanguage.value = false
                }
            }
        }
    }

    fun translate() {
        shouldIgnoreTranslation = false // 重置忽略标志
        translateJob = viewModelScope.launch {
            _isTranslating.value = true
            try {
                // 开始翻译
                Log.i("TranslationVM", "Translation started: ${_sourceLanguage.value.displayName} -> ${_targetLanguage.value.displayName}")
                
                val result = translationService.translateText(
                    _sourceText.value,
                    _sourceLanguage.value,
                    _targetLanguage.value
                )
                
                Log.i("TranslationVM", "Translation completed successfully")
                
                // 检查是否应该忽略翻译结果（用户可能已经点击了清除）
                if (!shouldIgnoreTranslation) {
                    _translatedText.value = result
                    
                    // 保存历史记录（使用实际的源语言，不使用AUTO）
                    val actualSourceLanguage = if (_isAutoDetected.value && _detectedLanguage.value != null) {
                        _detectedLanguage.value!!
                    } else {
                        _sourceLanguage.value
                    }
                    
                    // 只有在不是AUTO的情况下才保存
                    if (actualSourceLanguage != Language.AUTO) {
                        viewModelScope.launch {
                            try {
                                translationRepository.saveRecord(
                                    sourceText = _sourceText.value,
                                    translatedText = result,
                                    sourceLanguage = actualSourceLanguage.displayName,
                                    targetLanguage = _targetLanguage.value.displayName,
                                    type = TranslationType.TEXT
                                )
                                Log.i("TranslationVM", "Translation record saved to history")
                            } catch (e: Exception) {
                                Log.e("TranslationVM", "Failed to save translation record: ${e.message}", e)
                            }
                        }
                    }
                } else {
                    Log.i("TranslationVM", "Translation result ignored due to user clear action")
                }
            } catch (e: CancellationException) {
                // 协程取消是正常操作（用户点击清除），不显示错误
                Log.d("TranslationVM", "Translation cancelled by user")
                throw e // 重新抛出CancellationException以正确处理协程取消
            } catch (e: Exception) {
                // 只有在没有被忽略时才显示错误
                if (!shouldIgnoreTranslation) {
                    Log.e("TranslationVM", "Translation failed: ${e.message}", e)
                    _uiState.value = TranslationUiState.Error("翻译失败: ${e.message}")
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
        // 设置忽略翻译标志，类似语音翻译和图片翻译的逻辑
        shouldIgnoreTranslation = true
        
        // 取消正在进行的语言检测任务和翻译任务
        detectLanguageJob?.cancel()
        translateJob?.cancel()
        
        // 立即清空文本和重置状态
        _sourceText.value = ""
        _translatedText.value = ""
        _sourceTextCharCount.value = 0
        _canTranslate.value = false
        _isTranslating.value = false // 立即恢复按钮状态
        _isDetectingLanguage.value = false // 重置检测状态
        // 清除时也重置检测状态到初始状态
        _isAutoDetected.value = false
        _detectedLanguage.value = null
        
        // 如果当前是自动检测模式，重置目标语言列表为AUTO对应的列表
        if (_sourceLanguage.value == Language.AUTO) {
            val autoTargetLanguages = Language.getTargetLanguages(Language.AUTO)
            _availableTargetLanguages.value = autoTargetLanguages
            // 如果当前目标语言不在AUTO的列表中，重置为第一个
            if (_targetLanguage.value !in autoTargetLanguages) {
                _targetLanguage.value = autoTargetLanguages.first()
            }
        }
        
        stopSpeaking()
        
        // 延迟重置忽略标志，确保清除操作完成
        viewModelScope.launch {
            delay(500)
            shouldIgnoreTranslation = false
        }
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
        stopSpeaking()
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