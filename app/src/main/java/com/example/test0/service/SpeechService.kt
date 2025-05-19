package com.example.test0.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.test0.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

class SpeechService(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    
    // Speech recognition state
    private val _recognitionState = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val recognitionState = _recognitionState.asStateFlow()
    
    // Text-to-speech state
    private val _ttsState = MutableStateFlow<TTSState>(TTSState.Idle)
    val ttsState = _ttsState.asStateFlow()
    
    sealed class RecognitionState {
        object Idle : RecognitionState()
        object Listening : RecognitionState()
        data class Result(val text: String) : RecognitionState()
        data class Error(val errorMessage: String) : RecognitionState()
    }
    
    sealed class TTSState {
        object Idle : TTSState()
        object Speaking : TTSState()
        object Done : TTSState()
        data class Error(val errorMessage: String) : TTSState()
    }
    
    fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        }
    }
    
    fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _ttsState.value = TTSState.Speaking
                    }
                    
                    override fun onDone(utteranceId: String?) {
                        _ttsState.value = TTSState.Done
                    }
                    
                    override fun onError(utteranceId: String?) {
                        _ttsState.value = TTSState.Error("TTS error occurred")
                    }
                })
            } else {
                _ttsState.value = TTSState.Error("Failed to initialize TTS")
            }
        }
    }
    
    suspend fun startListening(language: Language): String {
        return suspendCancellableCoroutine { continuation ->
            if (speechRecognizer == null) {
                continuation.resume("")
                _recognitionState.value = RecognitionState.Error("Speech recognition not available")
                return@suspendCancellableCoroutine
            }
            
            _recognitionState.value = RecognitionState.Listening
            
            val recognitionListener = object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.get(0) ?: ""
                    _recognitionState.value = RecognitionState.Result(text)
                    continuation.resume(text)
                }
                
                override fun onError(error: Int) {
                    val errorMessage = getErrorMessage(error)
                    _recognitionState.value = RecognitionState.Error(errorMessage)
                    continuation.resume("")
                }
                
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            }
            
            speechRecognizer?.setRecognitionListener(recognitionListener)
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, mapLanguageToLocale(language))
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            speechRecognizer?.startListening(intent)
            
            continuation.invokeOnCancellation {
                speechRecognizer?.cancel()
                _recognitionState.value = RecognitionState.Idle
            }
        }
    }
    
    fun speak(text: String, language: Language) {
        if (textToSpeech == null) {
            _ttsState.value = TTSState.Error("TTS not initialized")
            return
        }
        
        val locale = mapLanguageToLocale(language)
        val result = textToSpeech?.setLanguage(locale)
        
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            _ttsState.value = TTSState.Error("Language not supported")
            return
        }
        
        val utteranceId = UUID.randomUUID().toString()
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }
    
    private fun mapLanguageToLocale(language: Language): Locale {
        return when (language) {
            Language.ENGLISH -> Locale.ENGLISH
            Language.CHINESE -> Locale.CHINESE
            Language.FRENCH -> Locale.FRENCH
            Language.GERMAN -> Locale.GERMAN
            Language.JAPANESE -> Locale.JAPANESE
            Language.KOREAN -> Locale.KOREAN
            Language.ITALIAN -> Locale.ITALIAN
            else -> {
                // Best effort mapping for other languages
                when (language.code) {
                    "es" -> Locale("es") // Spanish
                    "ru" -> Locale("ru") // Russian
                    "ar" -> Locale("ar") // Arabic
                    "hi" -> Locale("hi") // Hindi
                    "pt" -> Locale("pt") // Portuguese
                    else -> Locale.ENGLISH // Default
                }
            }
        }
    }
    
    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error occurred"
        }
    }
    
    fun cleanup() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
} 