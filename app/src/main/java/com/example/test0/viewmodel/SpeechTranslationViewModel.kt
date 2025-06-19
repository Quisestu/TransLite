package com.example.test0.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.test0.model.Language
import com.example.test0.service.TextToSpeechService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.example.test0.service.TencentStreamingSpeechService
import com.example.test0.BuildConfig
import android.Manifest
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.delay
import com.example.test0.repository.TranslationRepository
import com.example.test0.model.TranslationType
import android.util.Log

class SpeechTranslationViewModel(application: Application) : AndroidViewModel(application) {
    private val supportedLanguages = listOf(Language.CHINESE, Language.ENGLISH)
    private val textToSpeechService = TextToSpeechService(application.applicationContext)
    private val translationRepository = TranslationRepository.getInstance(application.applicationContext)

    private val _sourceLanguage = MutableStateFlow(Language.CHINESE)
    val sourceLanguage: StateFlow<Language> = _sourceLanguage.asStateFlow()

    private val _targetLanguage = MutableStateFlow(Language.ENGLISH)
    val targetLanguage: StateFlow<Language> = _targetLanguage.asStateFlow()

    private val _sourceText = MutableStateFlow("")
    val sourceText: StateFlow<String> = _sourceText.asStateFlow()

    private val _translatedText = MutableStateFlow("")
    val translatedText: StateFlow<String> = _translatedText.asStateFlow()

    private val _ttsState = textToSpeechService.ttsState
    val ttsState: StateFlow<TextToSpeechService.TTSState> = _ttsState

    enum class TtsType { SOURCE, TARGET }
    private val _currentTtsType = MutableStateFlow<TtsType?>(null)
    val currentTtsType = _currentTtsType.asStateFlow()

    private var streamingService: TencentStreamingSpeechService? = null

    private val _volume = MutableStateFlow(0)
    val volume: StateFlow<Int> = _volume.asStateFlow()

    // 倒计时相关
    private val _remainingTimeSeconds = MutableStateFlow(60)
    val remainingTimeSeconds: StateFlow<Int> = _remainingTimeSeconds.asStateFlow()
    
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _isProcessingQueue = MutableStateFlow(false)
    val isProcessingQueue: StateFlow<Boolean> = _isProcessingQueue.asStateFlow()
    
    private var countdownJob: Job? = null
    private var shouldIgnoreTranslation = false // 新增：是否忽略翻译结果（清除时使用）

    init {
        textToSpeechService.initializeTextToSpeech()
    }

    fun swapLanguages() {
        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp
    }

    fun clearSourceText() {
        // 设置忽略翻译标志
        shouldIgnoreTranslation = true
        
        // 强制停止服务并清空队列
        streamingService?.forceStop()
        
        // 立即清空文本和重置状态
        _sourceText.value = ""
        _translatedText.value = ""
        _isRecording.value = false
        _isProcessingQueue.value = false
        _volume.value = 0
        _remainingTimeSeconds.value = 60
        countdownJob?.cancel()
        stopSpeaking()
        
        // 延迟重置忽略标志，确保清除操作完成
        viewModelScope.launch {
            delay(500)
            shouldIgnoreTranslation = false
        }
    }

    fun updateSourceText(text: String) {
        android.util.Log.d("SpeechViewModel", "更新源文本: '$text'")
        // 如果正在清除操作，忽略源文本更新
        if (!shouldIgnoreTranslation) {
            _sourceText.value = text
        }
    }

    fun updateTranslatedText(text: String) {
        android.util.Log.d("SpeechViewModel", "更新译文: '$text'")
        // 如果正在清除操作，忽略翻译结果
        if (!shouldIgnoreTranslation) {
            _translatedText.value = text
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

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    fun startStreamingRecognition(onResult: (String, String) -> Unit, onError: (String) -> Unit) {
        if (_isRecording.value || _isProcessingQueue.value) return
        
        // 开始录音时停止当前朗读
        stopSpeaking()
        
        // 立即重置文本显示区
        _sourceText.value = ""
        _translatedText.value = ""
        
        shouldIgnoreTranslation = false // 重置忽略标志
        _isRecording.value = true
        startCountdown()
        
        streamingService = TencentStreamingSpeechService(
            appId = BuildConfig.TENCENT_APP_ID,
            secretId = BuildConfig.TENCENT_SECRET_ID,
            secretKey = BuildConfig.TENCENT_SECRET_KEY,
            sourceLang = _sourceLanguage.value.code,
            targetLang = _targetLanguage.value.code,
            onResult = onResult,
            onError = { errorMsg ->
                // 只显示错误，不停止录音
                _errorMessage.value = errorMsg
                onError(errorMsg)
            },
            onVolume = { amp -> _volume.value = amp },
            onQueueStatusChanged = { isProcessing ->
                _isProcessingQueue.value = isProcessing
            }
        )
        streamingService?.startStreaming()
    }

    fun stopStreamingRecognition() {
        stopRecordingAndReset()
        streamingService?.stopStreaming()
        
        // 在录音结束时保存历史记录（一次录音整体保存）
        viewModelScope.launch {
            // 等待队列处理完成
            while (_isProcessingQueue.value) {
                delay(100)
            }
            
            // 检查是否应该保存记录
            val sourceText = _sourceText.value
            val translatedText = _translatedText.value
            
            if (!shouldIgnoreTranslation && sourceText.isNotBlank() && translatedText.isNotBlank()) {
                try {
                    translationRepository.saveRecord(
                        sourceText = sourceText,
                        translatedText = translatedText,
                        sourceLanguage = _sourceLanguage.value.displayName,
                        targetLanguage = _targetLanguage.value.displayName,
                        type = TranslationType.SPEECH
                    )
                    Log.i("SpeechTranslationVM", "Speech translation record saved to history")
                } catch (e: Exception) {
                    Log.e("SpeechTranslationVM", "Failed to save speech translation record: ${e.message}", e)
                }
            }
        }
    }
    
    private fun startCountdown() {
        countdownJob?.cancel()
        _remainingTimeSeconds.value = 60
        
        countdownJob = viewModelScope.launch {
            while (_remainingTimeSeconds.value > 0 && _isRecording.value) {
                delay(1000)
                _remainingTimeSeconds.value -= 1
                
                if (_remainingTimeSeconds.value <= 0) {
                    // 倒计时结束，自动停止录音
                    stopStreamingRecognition()
                    break
                }
            }
        }
    }
    
    private fun stopRecordingAndReset() {
        _isRecording.value = false
        countdownJob?.cancel()
        
        // 等待队列处理完毕后再重置倒计时和音量
        viewModelScope.launch {
            // 等待队列处理完成
            while (_isProcessingQueue.value) {
                delay(100)
            }
            
            // 队列处理完成后重置
            if (!_isRecording.value) { // 确保没有新的录音开始
                _remainingTimeSeconds.value = 60
                _volume.value = 0
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    override fun onCleared() {
        stopSpeaking()
        textToSpeechService.cleanup()
        super.onCleared()
    }
} 