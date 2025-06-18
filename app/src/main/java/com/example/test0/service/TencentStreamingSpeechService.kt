package com.example.test0.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.concurrent.ConcurrentLinkedQueue

class TencentStreamingSpeechService(
    private val appId: String,
    private val secretId: String,
    private val secretKey: String,
    private val sourceLang: String = "zh",
    private val targetLang: String = "en",
    private val onResult: (sourceText: String, targetText: String) -> Unit,
    private val onError: (String) -> Unit,
    private val onVolume: ((Int) -> Unit)? = null
) {
    companion object {
        private const val TAG = "TencentSpeech"
    }
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var job: Job? = null
    private val client = OkHttpClient()

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val chunkMillis = 250 // 250ms分片
    private val chunkBytes = sampleRate * 2 * chunkMillis / 1000 // 每片字节数
    private val maxRecordingTimeMs = 60 * 1000 // 最大录音时长60秒

    private var sessionUuid: String = ""
    private var seqNumber = 0
    private var lastRequestTime = 0L
    private var recordingStartTime = 0L
    private val audioBuffer = ConcurrentLinkedQueue<Pair<ByteArray, Int>>()
    private var sendingJob: Job? = null

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startStreaming() {
        if (isRecording) return
        
        isRecording = true
        sessionUuid = "sid-${System.currentTimeMillis()}"
        seqNumber = 0
        recordingStartTime = System.currentTimeMillis()
        
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                audioRecord?.startRecording()

                // 启动发送协程
                sendingJob = launch {
                    while (isRecording || audioBuffer.isNotEmpty()) {
                        val audioChunk = audioBuffer.poll()
                        if (audioChunk != null) {
                            val (audioData, volume) = audioChunk
                            sendAudioChunk(audioData, seqNumber, false)
                            seqNumber++
                        } else {
                            delay(50) // 缓冲队列为空时短暂等待
                        }
                    }
                    // 发送结束标志
                    sendAudioChunk(ByteArray(0), seqNumber, true)
                }

                val buffer = ByteArray(chunkBytes)
                while (isRecording) {
                    val startTime = System.currentTimeMillis()
                    
                    // 检查录音总时长
                    val totalRecordingTime = startTime - recordingStartTime
                    if (totalRecordingTime >= maxRecordingTimeMs) {
                        onError("录音时长已达上限(${maxRecordingTimeMs / 1000}秒)，自动停止")
                        break
                    }
                    
                    val read = audioRecord?.read(buffer, 0, minOf(buffer.size, chunkBytes)) ?: 0
                    if (read > 0) {
                        // 计算音量用于波形动画
                        var maxAmp = 0
                        var i = 0
                        while (i < read - 1) {
                            val low = buffer[i].toInt() and 0xFF
                            val high = buffer[i + 1].toInt()
                            val sample = (high shl 8) or low
                            maxAmp = maxOf(maxAmp, Math.abs(sample))
                            i += 2
                        }
                        onVolume?.invoke(maxAmp)

                        // 将音频数据加入缓冲队列
                        val audioData = buffer.copyOf(read)
                        audioBuffer.offer(Pair(audioData, maxAmp))
                    }
                    
                    // 确保录音间隔300ms
                    val elapsed = System.currentTimeMillis() - startTime
                    val remainingDelay = maxOf(0, chunkMillis - elapsed)
                    delay(remainingDelay)
                }

                // 等待发送完成
                sendingJob?.join()
                
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
            } catch (e: Exception) {
                // 只有严重错误才停止录音
                if (e.message?.contains("permission", true) == true || 
                    e.message?.contains("AudioRecord", true) == true) {
                    onError("录音权限或硬件错误: ${e.message}")
                } else {
                    Log.e(TAG, "录音过程中的一般错误: ${e.message}", e)
                }
            }
        }
    }

    fun stopStreaming() {
        isRecording = false
        sendingJob?.cancel()
        job?.cancel()
        audioBuffer.clear()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private suspend fun sendAudioChunk(audioData: ByteArray, seq: Int, isEnd: Boolean) {
        try {
            // 验证音频时长
            if (!isEnd && audioData.isNotEmpty()) {
                val durationMs = (audioData.size * 1000) / (sampleRate * 2)
                if (durationMs > 500) {
                    // 只记录警告，不停止录音
                    Log.w(TAG, "音频分片时长超限: ${durationMs}ms，跳过分片序号$seq")
                    return
                }
            }
            
            val timestamp = System.currentTimeMillis() / 1000
            val audioBase64 = if (audioData.isNotEmpty()) {
                Base64.encodeToString(audioData, Base64.NO_WRAP)
            } else ""

            val payload = JSONObject().apply {
                put("SessionUuid", sessionUuid)
                put("Source", sourceLang)
                put("Target", targetLang)
                put("AudioFormat", 146) // PCM格式 16kHz
                put("Seq", seq)
                put("IsEnd", if (isEnd) 1 else 0)
                put("Data", audioBase64)
                put("ProjectId", 0)
                // 尝试添加更多参数
                if (audioData.isNotEmpty()) {
                    put("VoiceId", sessionUuid) // 添加VoiceId
                }
            }.toString()

            Log.d(TAG, "发送请求 (序号$seq): Source=$sourceLang, Target=$targetLang, IsEnd=${if (isEnd) 1 else 0}")
            Log.d(TAG, "音频数据长度: ${audioData.size} bytes, Base64长度: ${audioBase64.length}")
            Log.d(TAG, "完整请求payload (序号$seq): $payload")
            
            // 验证参数
            if (sourceLang.isBlank() || targetLang.isBlank()) {
                Log.e(TAG, "语言参数错误: Source='$sourceLang', Target='$targetLang'")
                return
            }
            
            if (sourceLang == targetLang) {
                Log.w(TAG, "源语言和目标语言相同: $sourceLang")
            }

            val (authorization, headers) = getAuthorizationHeader(
                secretId, secretKey, "tmt", "SpeechTranslate", 
                "2018-03-21", timestamp, payload
            )

            val requestBuilder = Request.Builder()
                .url("https://tmt.tencentcloudapi.com/")
                .post(payload.toRequestBody("application/json".toMediaType()))

            headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

            Log.d(TAG, "请求头: $headers")
            
            val response = client.newCall(requestBuilder.build()).execute()
            val responseBody = response.body?.string() ?: ""

            Log.d(TAG, "响应状态码: ${response.code}")
            Log.d(TAG, "响应头: ${response.headers}")

            if (response.isSuccessful) {
                parseResponse(responseBody, seq)
            } else {
                // 网络错误只记录，不停止录音
                Log.e(TAG, "网络错误 (序号$seq): ${response.code} - $responseBody")
            }
        } catch (e: Exception) {
            // 请求错误只记录，不停止录音
            Log.e(TAG, "请求错误 (序号$seq): ${e.message}", e)
        }
    }

    private fun parseResponse(responseBody: String, seq: Int) {
        try {
            Log.d(TAG, "API响应 (序号$seq): $responseBody")
            
            val json = JSONObject(responseBody)
            val response = json.optJSONObject("Response")
            
            if (response != null) {
                val error = response.optJSONObject("Error")
                if (error != null) {
                    val errorCode = error.optString("Code", "")
                    val errorMessage = error.optString("Message", "")
                    // API错误只记录，不停止录音
                    Log.e(TAG, "API错误 (序号$seq): $errorCode - $errorMessage")
                    return
                }

                val sourceText = response.optString("SourceText", "")
                val targetText = response.optString("TargetText", "")
                
                Log.d(TAG, "解析结果 (序号$seq): sourceText='$sourceText', targetText='$targetText'")
                
                if (sourceText.isNotEmpty()) {
                    if (targetText.isNotEmpty()) {
                        // 正常情况：有源文本和译文
                        Log.d(TAG, "调用onResult回调 (序号$seq): source='$sourceText', target='$targetText'")
                        onResult(sourceText, targetText)
                    } else {
                        // 只有源文本，没有译文 - 可能是API限制或配置问题
                        Log.w(TAG, "只有源文本，没有译文 (序号$seq): '$sourceText'")
                        
                        // 尝试调用文本翻译API作为备用方案
                        if (sourceText.isNotBlank() && sourceLang != targetLang) {
                            Log.d(TAG, "尝试使用文本翻译API作为备用方案")
                            translateText(sourceText, sourceLang, targetLang) { translatedText ->
                                if (translatedText.isNotEmpty()) {
                                    Log.d(TAG, "文本翻译成功: '$translatedText'")
                                    onResult(sourceText, translatedText)
                                } else {
                                    Log.w(TAG, "文本翻译也失败，只返回源文本")
                                    onResult(sourceText, "")
                                }
                            }
                        } else {
                            // 暂时只传递源文本，译文为空
                            onResult(sourceText, "")
                        }
                        
                        // 如果是最后一个分片，提示用户
                        if (response.optInt("IsEnd", 0) == 1 || response.optInt("RecognizeStatus", 1) == 0) {
                            Log.w(TAG, "语音识别完成，但翻译功能可能未启用或受限")
                        }
                    }
                } else if (targetText.isNotEmpty()) {
                    // 只有译文，没有源文本（不太可能，但以防万一）
                    Log.w(TAG, "只有译文，没有源文本 (序号$seq): '$targetText'")
                    onResult("", targetText)
                } else {
                    Log.w(TAG, "源文本和译文都为空 (序号$seq)")
                }
            } else {
                Log.e(TAG, "响应中没有Response对象 (序号$seq)")
            }
        } catch (e: Exception) {
            // 解析错误只记录，不停止录音
            Log.e(TAG, "解析响应错误 (序号$seq): ${e.message}", e)
        }
    }

    private fun getAuthorizationHeader(
        secretId: String,
        secretKey: String,
        service: String,
        action: String,
        version: String,
        timestamp: Long,
        payload: String = "{}"
    ): Pair<String, Map<String, String>> {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { 
            timeZone = TimeZone.getTimeZone("UTC") 
        }.format(Date(timestamp * 1000))
        
        val algorithm = "TC3-HMAC-SHA256"
        val httpRequestMethod = "POST"
        val canonicalUri = "/"
        val canonicalQueryString = ""
        val canonicalHeaders = "content-type:application/json; charset=utf-8\n" +
                              "host:tmt.tencentcloudapi.com\n" +
                              "x-tc-action:${action.lowercase(Locale.getDefault())}\n" +
                              "x-tc-region:ap-guangzhou\n"
        val signedHeaders = "content-type;host;x-tc-action;x-tc-region"
        val hashedRequestPayload = sha256Hex(payload)
        
        val canonicalRequest = "$httpRequestMethod\n$canonicalUri\n$canonicalQueryString\n" +
                              "$canonicalHeaders\n$signedHeaders\n$hashedRequestPayload"
        
        val credentialScope = "$date/$service/tc3_request"
        val stringToSign = "$algorithm\n$timestamp\n$credentialScope\n${sha256Hex(canonicalRequest)}"
        
        val secretDate = hmacSha256(("TC3$secretKey").toByteArray(), date)
        val secretService = hmacSha256(secretDate, service)
        val secretSigning = hmacSha256(secretService, "tc3_request")
        val signature = bytesToHex(hmacSha256(secretSigning, stringToSign))
        
        val authorization = "$algorithm Credential=$secretId/$credentialScope, " +
                           "SignedHeaders=$signedHeaders, Signature=$signature"
        
        val headers = mapOf(
            "Authorization" to authorization,
            "Content-Type" to "application/json; charset=utf-8",
            "Host" to "tmt.tencentcloudapi.com",
            "X-TC-Action" to action,
            "X-TC-Version" to version,
            "X-TC-Timestamp" to timestamp.toString(),
            "X-TC-Region" to "ap-guangzhou"
        )
        
        return Pair(authorization, headers)
    }

    private fun sha256Hex(s: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val d = md.digest(s.toByteArray())
        return d.joinToString("") { "%02x".format(it) }
    }

    private fun hmacSha256(key: ByteArray, msg: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(msg.toByteArray())
    }

    private fun bytesToHex(bytes: ByteArray): String {
        return bytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun translateText(text: String, source: String, target: String, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val timestamp = System.currentTimeMillis() / 1000
                val payload = JSONObject().apply {
                    put("SourceText", text)
                    put("Source", source)
                    put("Target", target)
                    put("ProjectId", 0)
                }.toString()

                val (authorization, headers) = getAuthorizationHeader(
                    secretId, secretKey, "tmt", "TextTranslate", 
                    "2018-03-21", timestamp, payload
                )

                val requestBuilder = Request.Builder()
                    .url("https://tmt.tencentcloudapi.com/")
                    .post(payload.toRequestBody("application/json".toMediaType()))

                headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }

                val response = client.newCall(requestBuilder.build()).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val json = JSONObject(responseBody)
                    val responseObj = json.optJSONObject("Response")
                    if (responseObj != null) {
                        val error = responseObj.optJSONObject("Error")
                        if (error != null) {
                            Log.e(TAG, "文本翻译API错误: ${error.optString("Code")} - ${error.optString("Message")}")
                            callback("")
                        } else {
                            val targetText = responseObj.optString("TargetText", "")
                            Log.d(TAG, "文本翻译API响应: '$targetText'")
                            callback(targetText)
                        }
                    } else {
                        Log.e(TAG, "文本翻译API响应格式错误")
                        callback("")
                    }
                } else {
                    Log.e(TAG, "文本翻译API网络错误: ${response.code}")
                    callback("")
                }
            } catch (e: Exception) {
                Log.e(TAG, "文本翻译API异常: ${e.message}", e)
                callback("")
            }
        }
    }
} 