package com.example.test0.service

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.test0.config.TencentConfig
import com.example.test0.model.ImageLanguage
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

class ImageTranslationService {
    private val credential = Credential(TencentConfig.SECRET_ID, TencentConfig.SECRET_KEY)
    private val clientProfile = ClientProfile().apply {
        httpProfile = HttpProfile().apply {
            endpoint = TencentConfig.ENDPOINT
        }
    }
    private val client = TmtClient(credential, TencentConfig.REGION, clientProfile)

    suspend fun detectLanguage(text: String): ImageLanguage = withContext(Dispatchers.IO) {
        try {
            val req = TextTranslateRequest().apply {
                this.sourceText = text
                this.source = "auto"
                this.target = "zh" // 使用中文作为目标语言来检测
                this.projectId = 0
            }
            val resp = client.TextTranslate(req) as TextTranslateResponse
            ImageLanguage.fromCode(resp.source ?: "auto")
        } catch (e: Exception) {
            Log.e("ImageTranslationService", "Language detection failed: ${e.message}", e)
            throw ImageTranslationException("语言检测失败: ${e.message}")
        }
    }

    suspend fun translateImage(
        bitmap: Bitmap, 
        sourceLanguage: ImageLanguage, 
        targetLanguage: ImageLanguage
    ): ImageTranslationResult {
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
                    this.scene = "doc" // 设置翻译场景，doc表示文档场景
                    this.sessionUuid = "session-${UUID.randomUUID()}" // 生成唯一会话ID
                }
                val resp = client.ImageTranslate(req) as ImageTranslateResponse
                
                // 从 ImageRecord.Value 数组中提取文本内容
                var sourceText = ""
                var translatedText = ""
                
                try {
                    val imageRecord = resp.imageRecord
                    if (imageRecord != null) {
                        Log.d("ImageTranslationService", "========== ImageRecord 分析 ==========")
                        
                        // 找到 Value 字段（ItemValue 数组）
                        val valueField = imageRecord.javaClass.declaredFields.find { it.name.equals("value", ignoreCase = true) }
                        if (valueField != null) {
                            valueField.isAccessible = true
                            val itemValues = valueField.get(imageRecord) as? Array<*>
                            
                            if (itemValues != null && itemValues.isNotEmpty()) {
                                Log.d("ImageTranslationService", "ItemValue 数组长度: ${itemValues.size}")
                                
                                val sourceTexts = mutableListOf<String>()
                                val targetTexts = mutableListOf<String>()
                                
                                itemValues.forEachIndexed { index, itemValue ->
                                    if (itemValue != null) {
                                        Log.d("ImageTranslationService", "--- ItemValue[$index] ---")
                                        
                                        // 分析每个 ItemValue 的字段
                                        val itemFields = itemValue.javaClass.declaredFields
                                        itemFields.forEach { field ->
                                            field.isAccessible = true
                                            val value = field.get(itemValue)
                                            Log.d("ImageTranslationService", "  ${field.name}: $value")
                                            
                                            // 提取文本字段
                                            when (field.name.lowercase()) {
                                                "sourcetext", "source_text", "detectedtext", "detected_text" -> {
                                                    val text = value?.toString()?.trim()
                                                    if (!text.isNullOrBlank()) {
                                                        sourceTexts.add(text)
                                                    }
                                                }
                                                "targettext", "target_text", "translatedtext", "translated_text" -> {
                                                    val text = value?.toString()?.trim()
                                                    if (!text.isNullOrBlank()) {
                                                        targetTexts.add(text)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                sourceText = sourceTexts.joinToString("\n")
                                translatedText = targetTexts.joinToString("\n")
                                
                                Log.d("ImageTranslationService", "合并结果 - 源文本: '$sourceText'")
                                Log.d("ImageTranslationService", "合并结果 - 翻译文本: '$translatedText'")
                            } else {
                                Log.w("ImageTranslationService", "ItemValue 数组为空")
                            }
                        } else {
                            Log.w("ImageTranslationService", "未找到 Value 字段")
                        }
                        
                        Log.d("ImageTranslationService", "=====================================")
                    } else {
                        Log.w("ImageTranslationService", "ImageRecord 为 null")
                    }
                } catch (e: Exception) {
                    Log.e("ImageTranslationService", "提取文本失败: ${e.message}", e)
                }
                
                // 如果没有提取到文本内容，使用语言代码作为临时回退
                if (sourceText.isEmpty() && translatedText.isEmpty()) {
                    Log.w("ImageTranslationService", "未能提取到文本内容，暂时使用语言代码")
                    sourceText = "语言: ${resp.source ?: "unknown"}"
                    translatedText = "语言: ${resp.target ?: "unknown"}"
                }
                
                ImageTranslationResult(
                    sourceText = sourceText,
                    translatedText = translatedText,
                    detectedLanguage = if (sourceLanguage == ImageLanguage.AUTO) {
                        ImageLanguage.fromCode(resp.source ?: "auto")
                    } else {
                        sourceLanguage
                    }
                )
            } catch (e: Exception) {
                Log.e("ImageTranslationService", "Image translation failed: ${e.message}", e)
                throw ImageTranslationException("图片翻译失败: ${e.message}")
            }
        }
    }
}

data class ImageTranslationResult(
    val sourceText: String,
    val translatedText: String,
    val detectedLanguage: ImageLanguage
)

class ImageTranslationException(message: String) : Exception(message)