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

class SpeechTranslationViewModel(application: Application) : AndroidViewModel(application) {
    private val supportedLanguages = listOf(Language.CHINESE, Language.ENGLISH)
    private val textToSpeechService = TextToSpeechService(application.applicationContext)

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
    
    private var countdownJob: Job? = null

    fun swapLanguages() {
        val temp = _sourceLanguage.value
        _sourceLanguage.value = _targetLanguage.value
        _targetLanguage.value = temp
    }

    fun clearSourceText() {
        _sourceText.value = ""
        _translatedText.value = ""
        stopSpeaking()
    }

    fun updateSourceText(text: String) {
        android.util.Log.d("SpeechViewModel", "更新源文本: '$text'")
        _sourceText.value = text
    }

    fun updateTranslatedText(text: String) {
        android.util.Log.d("SpeechViewModel", "更新译文: '$text'")
        _translatedText.value = text
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
        if (_isRecording.value) return
        
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
            onVolume = { amp -> _volume.value = amp }
        )
        streamingService?.startStreaming()
    }

    fun stopStreamingRecognition() {
        stopRecordingAndReset()
        streamingService?.stopStreaming()
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
        
        // 等待所有请求处理完毕后重置倒计时
        viewModelScope.launch {
            delay(1000) // 给一点时间让请求完成
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