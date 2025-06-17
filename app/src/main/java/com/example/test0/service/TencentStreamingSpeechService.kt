package com.example.test0.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.*
import okhttp3.*
import okio.ByteString.Companion.toByteString
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class TencentStreamingSpeechService(
    private val appId: String,
    private val secretId: String,
    private val secretKey: String,
    private val sourceLang: String = "en",
    private val targetLang: String = "zh",
    private val onResult: (sourceText: String, targetText: String) -> Unit,
    private val onError: (String) -> Unit
) {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var ws: WebSocket? = null
    private var job: Job? = null

    private val sampleRate = 16000
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
    private val chunkMillis = 400 // 400ms
    private val chunkBytes = sampleRate * 2 * chunkMillis / 1000 // 12800 bytes

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun startStreaming() {
        isRecording = true
        job = CoroutineScope(Dispatchers.IO).launch {
            try {
                val wsUrl = buildWebSocketUrl()
                val timestamp = System.currentTimeMillis() / 1000
                val (authorization, headers) = getAuthorizationHeader(secretId, secretKey, "tmt", "SpeechTranslate", "2018-03-21", timestamp)
                val client = OkHttpClient()
                val requestBuilder = Request.Builder().url(wsUrl)
                headers.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
                ws = client.newWebSocket(requestBuilder.build(), object : WebSocketListener() {
                    override fun onMessage(webSocket: WebSocket, text: String) {
                        val json = JSONObject(text)
                        val sourceText = json.optString("SourceText", "")
                        val targetText = json.optString("TargetText", "")
                        if (sourceText.isNotEmpty() || targetText.isNotEmpty()) {
                            onResult(sourceText, targetText)
                        }
                    }
                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        onError("WebSocket error: ${t.message}")
                    }
                })

                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    channelConfig,
                    audioFormat,
                    bufferSize
                )
                audioRecord?.startRecording()

                val buffer = ByteArray(chunkBytes)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        ws?.send(buffer.toByteString(0, read))
                    }
                    delay(chunkMillis.toLong())
                }

                ws?.close(1000, "User stopped recording")
                audioRecord?.stop()
                audioRecord?.release()
                audioRecord = null
            } catch (e: Exception) {
                onError("Streaming error: ${e.message}")
            }
        }
    }

    fun stopStreaming() {
        isRecording = false
        job?.cancel()
        ws?.close(1000, "User stopped recording")
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun buildWebSocketUrl(): String {
        return "wss://tmt.tencentcloudapi.com/stream?AppId=$appId&Source=$sourceLang&Target=$targetLang"
    }

    // --- TC3-HMAC-SHA256 签名算法实现 ---
    private fun getAuthorizationHeader(
        secretId: String,
        secretKey: String,
        service: String,
        action: String,
        version: String,
        timestamp: Long,
        payload: String = "{}"
    ): Pair<String, Map<String, String>> {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(timestamp * 1000))
        val algorithm = "TC3-HMAC-SHA256"
        val httpRequestMethod = "POST"
        val canonicalUri = "/"
        val canonicalQueryString = ""
        val canonicalHeaders = "content-type:application/json\nhost:tmt.tencentcloudapi.com\nx-tc-action:${action.lowercase(
            Locale.getDefault()
        )}\n"
        val signedHeaders = "content-type;host;x-tc-action"
        val hashedRequestPayload = sha256Hex(payload)
        val canonicalRequest = "$httpRequestMethod\n$canonicalUri\n$canonicalQueryString\n$canonicalHeaders\n$signedHeaders\n$hashedRequestPayload"
        val credentialScope = "$date/$service/tc3_request"
        val stringToSign = "$algorithm\n$timestamp\n$credentialScope\n${sha256Hex(canonicalRequest)}"
        val secretDate = hmacSha256(("TC3$secretKey").toByteArray(), date)
        val secretService = hmacSha256(secretDate, service)
        val secretSigning = hmacSha256(secretService, "tc3_request")
        val signature = bytesToHex(hmacSha256(secretSigning, stringToSign))
        val authorization = "$algorithm Credential=$secretId/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
        val headers = mapOf(
            "Authorization" to authorization,
            "Content-Type" to "application/json",
            "Host" to "tmt.tencentcloudapi.com",
            "X-TC-Action" to action,
            "X-TC-Version" to version,
            "X-TC-Timestamp" to timestamp.toString()
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