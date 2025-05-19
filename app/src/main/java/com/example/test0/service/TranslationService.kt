package com.example.test0.service

import com.example.test0.model.Language
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class TranslationService {
    private val translators = mutableMapOf<Pair<Language, Language>, Translator>()
    
    suspend fun translate(text: String, sourceLanguage: Language, targetLanguage: Language): String {
        if (text.isBlank()) return ""
        
        // If source and target languages are the same, return the original text
        if (sourceLanguage == targetLanguage) return text
        
        val translatorKey = Pair(sourceLanguage, targetLanguage)
        val translator = translators.getOrPut(translatorKey) {
            val options = TranslatorOptions.Builder()
                .setSourceLanguage(sourceLanguage.code)
                .setTargetLanguage(targetLanguage.code)
                .build()
            Translation.getClient(options)
        }
        
        return suspendCancellableCoroutine { continuation ->
            // Download translation model if needed
            translator.downloadModelIfNeeded()
                .addOnSuccessListener {
                    // Perform the translation
                    translator.translate(text)
                        .addOnSuccessListener { translatedText ->
                            continuation.resume(translatedText)
                        }
                        .addOnFailureListener { exception ->
                            continuation.resumeWithException(exception)
                        }
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
                
            continuation.invokeOnCancellation {
                // No need to call translator.close() here as we're caching translators
            }
        }
    }
    
    fun cleanUp() {
        translators.values.forEach { it.close() }
        translators.clear()
    }
} 