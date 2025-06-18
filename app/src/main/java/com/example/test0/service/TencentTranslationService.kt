package com.example.test0.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.test0.config.TencentConfig
import com.example.test0.model.Language
import com.tencentcloudapi.common.Credential
import com.tencentcloudapi.common.profile.ClientProfile
import com.tencentcloudapi.common.profile.HttpProfile
import com.tencentcloudapi.tmt.v20180321.TmtClient
import com.tencentcloudapi.tmt.v20180321.models.ImageTranslateRequest
import com.tencentcloudapi.tmt.v20180321.models.ImageTranslateResponse
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateRequest
import com.tencentcloudapi.tmt.v20180321.models.TextTranslateResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

class TencentTranslationService {
    private val credential = Credential(TencentConfig.SECRET_ID, TencentConfig.SECRET_KEY)
    private val clientProfile = ClientProfile().apply {
        httpProfile = HttpProfile().apply {
            endpoint = TencentConfig.ENDPOINT
        }
    }
    private val client = TmtClient(credential, TencentConfig.REGION, clientProfile)

    suspend fun translateText(text: String, sourceLanguage: Language, targetLanguage: Language): String {
        return withContext(Dispatchers.IO) {
            try {
                val req = TextTranslateRequest().apply {
                    this.sourceText = text
                    this.source = sourceLanguage.code
                    this.target = targetLanguage.code
                    this.projectId = 0
                }
                val resp = client.TextTranslate(req) as TextTranslateResponse
                resp.targetText ?: ""
            } catch (e: Exception) {
                Log.e("TencentTranslation", "Translation API failed: ${e.message}", e)
                throw TranslationException("翻译失败: ${e.message ?: e.toString()}")
            }
        }
    }

    suspend fun detectLanguage(text: String): Language = withContext(Dispatchers.IO) {
        try {
            val req = TextTranslateRequest().apply {
                this.sourceText = text
                this.source = "auto"
                this.target = "zh" // 使用中文作为目标语言来检测
                this.projectId = 0
            }
            val resp = client.TextTranslate(req) as TextTranslateResponse
            Language.fromCode(resp.source ?: "auto")
        } catch (e: Exception) {
            throw TranslationException("语言检测失败: ${e.message}")
        }
    }

    suspend fun translateImage(bitmap: Bitmap, sourceLanguage: Language, targetLanguage: Language): ImageTranslationResult {
        return withContext(Dispatchers.IO) {
            try {
                // 将Bitmap转换为Base64编码
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                val imageBytes = byteArrayOutputStream.toByteArray()
                val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

                val req = ImageTranslateRequest().apply {
                    this.data = base64Image
                    this.source = sourceLanguage.code
                    this.target = targetLanguage.code
                    this.projectId = 0
                }
                val resp = client.ImageTranslate(req) as ImageTranslateResponse
                
                ImageTranslationResult(
                    sourceText = resp.source ?: "",
                    translatedText = resp.target ?: "",
                    detectedLanguage = if (sourceLanguage == Language.AUTO) {
                        Language.fromCode(resp.source ?: "auto")
                    } else {
                        sourceLanguage
                    }
                )
            } catch (e: Exception) {
                Log.e("TencentTranslation", "Error translating image", e)
                throw TranslationException("图片翻译失败: ${e.message}")
            }
        }
    }
}

data class ImageTranslationResult(
    val sourceText: String,
    val translatedText: String,
    val detectedLanguage: Language
)

class TranslationException(message: String) : Exception(message)