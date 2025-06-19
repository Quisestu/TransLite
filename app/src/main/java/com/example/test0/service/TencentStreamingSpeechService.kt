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
    private val onVolume: ((Int) -> Unit)? = null,
    private val onQueueStatusChanged: ((Boolean) -> Unit)? = null
) {
    companion object {
        private const val TAG = "TencentSpeech"
        private const val DELAY_TAG = "TencentDelay"
    }
    
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var isProcessingQueue = false
    private var job: Job? = null
    private val client = OkHttpClient()
    private var isForceStopped = false // 新增：强制停止标志

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val chunkMillis = 350 // 分片长度（ms） 
    private val chunkBytes = sampleRate * 2 * chunkMillis / 1000 // 每片字节数
    private val maxRecordingTimeMs = 60 * 1000 // 最大录音时长60秒

    private var sessionUuid: String = ""
    private var seqNumber = 0
    private var lastRequestTime = 0L
    private var recordingStartTime = 0L
    private val audioBuffer = ConcurrentLinkedQueue<Pair<ByteArray, Int>>()
    private var sendingJob: Job? = null
    private var shouldStopRecording = false

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startStreaming() {
        if (isRecording) return
        
        isRecording = true
        shouldStopRecording = false // 重置停止标志
        isForceStopped = false // 重置强制停止标志
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

                // 启动发送协程 - 确保串行发送保持顺序
                sendingJob = launch {
                    isProcessingQueue = true
                    onQueueStatusChanged?.invoke(true) // 通知开始处理队列
                    
                    while (isRecording || audioBuffer.isNotEmpty()) {
                        val audioChunk = audioBuffer.poll()
                        if (audioChunk != null) {
                            val (audioData, volume) = audioChunk
                            // 检查是否是最后一个分片
                            val isLastChunk = !isRecording && audioBuffer.isEmpty()
                            
                            // 串行发送，失败直接跳过，确保顺序和时序
                            sendAudioChunk(audioData, seqNumber, isLastChunk)
                            seqNumber++
                        } else {
                            delay(50) // 缓冲队列为空时短暂等待
                        }
                    }
                    
                    // 如果录音已停止但队列不为空，发送结束标志
                    if (!isRecording && audioBuffer.isEmpty()) {
                        sendAudioChunk(ByteArray(0), seqNumber, true)
                    }
                    
                    isProcessingQueue = false
                    onQueueStatusChanged?.invoke(false) // 通知队列处理完成
                }

                val buffer = ByteArray(chunkBytes)
                while (isRecording && !shouldStopRecording) {
                    val startTime = System.currentTimeMillis()
                    
                    // 检查录音总时长
                    val totalRecordingTime = startTime - recordingStartTime
                    if (totalRecordingTime >= maxRecordingTimeMs) {
                        Log.d(TAG, "录音时长已达上限(${maxRecordingTimeMs / 1000}秒)，停止录音但继续处理队列")
                        // 只停止录音，不调用onError，让队列继续处理
                        isRecording = false
                        break
                    }
                    
                    val read = audioRecord?.read(buffer, 0, chunkBytes) ?: 0
                    if (read > 0) {
                        // 严格限制分片长度，防止异常超长分片
                        val actualRead = minOf(read, chunkBytes)
                        if (read > chunkBytes) {
                            Log.w(TAG, "AudioRecord返回超长数据: ${read}字节, 截断为${actualRead}字节")
                        }
                        // 计算音量用于波形动画（使用实际读取长度）
                        var maxAmp = 0
                        var i = 0
                        while (i < actualRead - 1) {
                            val low = buffer[i].toInt() and 0xFF
                            val high = buffer[i + 1].toInt()
                            val sample = (high shl 8) or low
                            maxAmp = maxOf(maxAmp, Math.abs(sample))
                            i += 2
                        }
                        onVolume?.invoke(maxAmp)

                        // 将音频数据加入缓冲队列（严格限制长度）
                        val audioData = buffer.copyOf(actualRead)
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
        // 只停止录音，但继续处理队列中的分片
        shouldStopRecording = true
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }
    
    fun forceStop() {
        // 强制停止所有处理，清空队列
        isForceStopped = true // 设置强制停止标志
        shouldStopRecording = true
        isRecording = false
        sendingJob?.cancel()
        job?.cancel()
        audioBuffer.clear()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        isProcessingQueue = false
        onQueueStatusChanged?.invoke(false)
    }
    
    fun isQueueProcessing(): Boolean {
        return isProcessingQueue || audioBuffer.isNotEmpty()
    }

    // 简化的发送方法，失败直接跳过，保持时序连续性
    private suspend fun sendAudioChunk(audioData: ByteArray, seq: Int, isEnd: Boolean) {
        val requestStartTime = System.currentTimeMillis()
        
        try {
            // 验证音频时长
            if (!isEnd && audioData.isNotEmpty()) {
                val durationMs = (audioData.size * 1000) / (sampleRate * 2)
                if (durationMs > 700) {
                    // 只记录警告，不停止录音 (350ms * 2 = 700ms容错)
                    Log.w(TAG, "音频分片时长超限: ${durationMs}ms，跳过分片序号$seq")
                    return // 直接跳过
                }
            }
            
            val timestamp = System.currentTimeMillis() / 1000
            val audioBase64 = if (audioData.isNotEmpty()) {
                Base64.encodeToString(audioData, Base64.NO_WRAP)
            } else if (isEnd) {
                // 结束标志时发送一个最小的有效音频数据（静音）
                // 创建一个短的静音音频数据 (16字节 = 8个16位样本 = 0.5ms的静音)
                val silenceData = ByteArray(16) { 0 }
                Base64.encodeToString(silenceData, Base64.NO_WRAP)
            } else {
                ""
            }

            val payload = JSONObject().apply {
                put("SessionUuid", sessionUuid)
                put("Source", sourceLang)
                put("Target", targetLang)
                put("AudioFormat", 146) // PCM格式 16kHz，根据文档应该是146
                put("Seq", seq)
                put("IsEnd", if (isEnd) 1 else 0)
                // 即使是结束标志也要发送Data字段
                put("Data", audioBase64)
                put("ProjectId", 0)
                // 根据官方文档，可能需要VoiceId参数
                if (seq == 0) {
                    put("VoiceId", sessionUuid) // 使用sessionUuid作为VoiceId
                }
            }.toString()

            Log.d(TAG, "发送请求 (序号$seq): Source=$sourceLang, Target=$targetLang, IsEnd=${if (isEnd) 1 else 0}")
            Log.d(TAG, "音频数据长度: ${audioData.size} bytes, Base64长度: ${audioBase64.length}")
            if (seq <= 2) { // 只显示前几次的完整payload以避免日志过多
                Log.d(TAG, "完整请求payload (序号$seq): $payload")
            }
            
            // 验证参数
            if (sourceLang.isBlank() || targetLang.isBlank()) {
                Log.e(TAG, "语言参数错误: Source='$sourceLang', Target='$targetLang'")
                return // 参数错误直接跳过
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
            val requestEndTime = System.currentTimeMillis()
            val totalLatency = requestEndTime - requestStartTime
            
            val responseBody = response.body?.string() ?: ""

            Log.d(TAG, "响应状态码: ${response.code}")
            Log.d(TAG, "响应头: ${response.headers}")
            Log.i(DELAY_TAG, "分片 $seq 网络延迟: ${totalLatency}ms (${if (isEnd) "结束分片" else "普通分片"})")

            if (response.isSuccessful) {
                parseResponse(responseBody, seq)
                
                // 估算各部分延迟
                val (uploadTime, processingTime, downloadTime) = estimateLatencyBreakdown(totalLatency, audioData.size)
                
                Log.i(DELAY_TAG, "分片 $seq 成功处理，总耗时: ${totalLatency}ms")
                Log.i(DELAY_TAG, "分片 $seq 延迟分解 - 上传: ${uploadTime}ms, 处理: ${processingTime}ms, 下载: ${downloadTime}ms")
            } else {
                // 记录错误并直接跳过，保持时序连续性
                Log.w(TAG, "分片 $seq 发送失败: ${response.code} - $responseBody，跳过继续")
                Log.w(DELAY_TAG, "分片 $seq 失败，耗时: ${totalLatency}ms，错误码: ${response.code}")
            }
        } catch (e: Exception) {
            val requestEndTime = System.currentTimeMillis()
            val totalLatency = requestEndTime - requestStartTime
            // 记录异常并直接跳过，保持时序连续性
            Log.w(TAG, "分片 $seq 请求异常: ${e.message}，跳过继续")
            Log.w(DELAY_TAG, "分片 $seq 异常，耗时: ${totalLatency}ms，异常: ${e.message}")
        }
    }

    // 估算延迟分解：返回 (上传时间, 处理时间, 下载时间)
    private fun estimateLatencyBreakdown(totalLatency: Long, dataSize: Int): Triple<Long, Long, Long> {
        // 基于数据大小估算上传时间 (假设网速)
        val estimatedUploadSpeed = 1024 * 1024 / 8 // 1Mbps = 128KB/s
        val baseUploadTime = (dataSize * 1000L) / estimatedUploadSpeed
        
        // 服务器处理时间估算 (基于经验)
        val estimatedProcessingTime = when {
            totalLatency < 80 -> 15L   // 很快：本地缓存或简单处理
            totalLatency < 150 -> 30L  // 较快：正常语音识别
            totalLatency < 300 -> 60L  // 中等：复杂语音识别
            totalLatency < 600 -> 120L // 较慢：服务器负载高
            else -> 200L               // 很慢：服务器很忙
        }
        
        // 剩余时间分配给网络传输 (上传+下载)
        val remainingTime = maxOf(0L, totalLatency - estimatedProcessingTime)
        val networkLatency = remainingTime / 2 // 假设上传下载时间相等
        
        // 实际上传时间 = 基础传输时间 + 网络延迟
        val actualUploadTime = maxOf(baseUploadTime, networkLatency)
        val actualDownloadTime = remainingTime - actualUploadTime
        
        return Triple(actualUploadTime, estimatedProcessingTime, maxOf(0L, actualDownloadTime))
    }

    private fun parseResponse(responseBody: String, seq: Int) {
        // 如果已经强制停止，不再处理响应
        if (isForceStopped) {
            Log.d(TAG, "服务已强制停止，忽略响应 (序号$seq)")
            return
        }
        
        try {
            Log.d(TAG, "API响应 (序号$seq): $responseBody")
            
            val json = JSONObject(responseBody)
            val response = json.optJSONObject("Response")
            
            if (response != null) {
                val error = response.optJSONObject("Error")
                if (error != null) {
                    val errorCode = error.optString("Code", "")
                    val errorMessage = error.optString("Message", "")
                    Log.e(TAG, "API错误 (序号$seq): $errorCode - $errorMessage")
                    return
                }

                // 详细记录响应中的所有字段
                Log.d(TAG, "响应对象所有键 (序号$seq): ${response.keys().asSequence().toList()}")
                
                val sourceText = response.optString("SourceText", "")
                val targetText = response.optString("TargetText", "")
                
                // 检查是否有其他可能的字段名
                val alternativeTargetFields = listOf("TranslatedText", "Translation", "Target", "Result")
                alternativeTargetFields.forEach { field ->
                    val value = response.optString(field, "")
                    if (value.isNotEmpty()) {
                        Log.d(TAG, "发现可能的翻译字段 '$field' (序号$seq): '$value'")
                    }
                }
                
                Log.d(TAG, "解析结果 (序号$seq): sourceText='$sourceText', targetText='$targetText'")
                
                // 检查其他状态字段
                val isEnd = response.optInt("IsEnd", -1)
                val recognizeStatus = response.optInt("RecognizeStatus", -1)
                val message = response.optString("Message", "")
                
                Log.d(TAG, "状态信息 (序号$seq): IsEnd=$isEnd, RecognizeStatus=$recognizeStatus, Message='$message'")
                
                if (sourceText.isNotEmpty()) {
                    // 检查识别状态
                    when (recognizeStatus) {
                        0 -> {
                            // 识别完成，应该有翻译结果
                            if (targetText.isNotEmpty()) {
                                Log.d(TAG, "识别完成，获得翻译结果 (序号$seq): source='$sourceText', target='$targetText'")
                                onResult(sourceText, targetText)
                            } else {
                                Log.w(TAG, "识别完成但翻译结果为空 (序号$seq): source='$sourceText'")
                                onResult(sourceText, "")
                            }
                        }
                        1 -> {
                            // 识别进行中，可能没有翻译结果
                            Log.d(TAG, "识别进行中 (序号$seq): source='$sourceText'")
                            onResult(sourceText, targetText) // 传递当前结果，即使翻译为空
                        }
                        else -> {
                            // 其他状态
                            Log.d(TAG, "未知识别状态 $recognizeStatus (序号$seq): source='$sourceText', target='$targetText'")
                            onResult(sourceText, targetText)
                        }
                    }
                } else {
                    Log.w(TAG, "源文本为空 (序号$seq)")
                }
            } else {
                Log.e(TAG, "响应中没有Response对象 (序号$seq)")
                Log.e(TAG, "完整响应结构: $responseBody")
            }
        } catch (e: Exception) {
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
} 