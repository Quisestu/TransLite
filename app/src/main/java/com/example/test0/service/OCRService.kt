package com.example.test0.service

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OCRService {
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    suspend fun recognizeText(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return recognizeText(image)
    }
    
    private suspend fun recognizeText(image: InputImage): String {
        return suspendCancellableCoroutine { continuation ->
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(extractText(visionText))
                }
                .addOnFailureListener { exception ->
                    continuation.resumeWithException(exception)
                }
                
            continuation.invokeOnCancellation {
                // No need to call close as TextRecognizer doesn't have a close method
            }
        }
    }
    
    private fun extractText(visionText: Text): String {
        return visionText.text
    }
    
    fun cleanUp() {
        textRecognizer.close()
    }
} 