package com.example.test0.service

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.test0.model.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TextToSpeechService(private val context: Context) {
    private var textToSpeech: TextToSpeech? = null
    
    // Text-to-speech state
    private val _ttsState = MutableStateFlow<TTSState>(TTSState.Idle)
    val ttsState = _ttsState.asStateFlow()
    
    sealed class TTSState {
        object Idle : TTSState()
        object Speaking : TTSState()
        object Done : TTSState()
        data class Error(val errorMessage: String) : TTSState()
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
    
    fun cleanup() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }
    
    fun stop() {
        textToSpeech?.stop()
        _ttsState.value = TTSState.Done
    }
} 