package com.voxn.ai.service

import android.content.Context
import com.voxn.ai.manager.KeystoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

sealed class GroqStreamEvent {
    data class ContentDelta(val text: String) : GroqStreamEvent()
    data class ToolCallStart(val id: String, val name: String, val index: Int) : GroqStreamEvent()
    data class ToolCallArgs(val index: Int, val args: String) : GroqStreamEvent()
    data class Finished(val reason: String?) : GroqStreamEvent()
}

class GroqService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private val endpoint = "https://api.groq.com/openai/v1/chat/completions"

    fun hasApiKey(): Boolean = KeystoreManager.hasApiKey(context)

    fun streamChat(
        messages: JSONArray,
        tools: JSONArray?,
        model: String = "llama-3.3-70b-versatile",
        temperature: Double = 0.4,
    ): Flow<GroqStreamEvent> = callbackFlow {
        val apiKey = KeystoreManager.loadApiKey(context)
            ?: throw IllegalStateException("Groq API key not set. Add it in Settings.")

        val body = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            if (tools != null && tools.length() > 0) put("tools", tools)
            put("stream", true)
            put("temperature", temperature)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val call = client.newCall(request)
        val response = withContext(Dispatchers.IO) { call.execute() }

        if (!response.isSuccessful) {
            val errBody = response.body?.string() ?: ""
            response.close()
            throw Exception("Groq API error ${response.code}: ${errBody.take(300)}")
        }

        val reader = BufferedReader(InputStreamReader(response.body!!.byteStream()))
        try {
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val l = line ?: continue
                if (!l.startsWith("data: ")) continue
                val payload = l.removePrefix("data: ").trim()
                if (payload == "[DONE]") {
                    trySend(GroqStreamEvent.Finished(null))
                    break
                }

                val chunk = try { JSONObject(payload) } catch (_: Exception) { continue }
                val choices = chunk.optJSONArray("choices") ?: continue
                if (choices.length() == 0) continue
                val choice = choices.getJSONObject(0)
                val delta = choice.optJSONObject("delta") ?: continue

                val content = delta.optString("content", "")
                if (content.isNotEmpty()) trySend(GroqStreamEvent.ContentDelta(content))

                val toolCalls = delta.optJSONArray("tool_calls")
                if (toolCalls != null) {
                    for (i in 0 until toolCalls.length()) {
                        val tc = toolCalls.getJSONObject(i)
                        val idx = tc.getInt("index")
                        val id = tc.optString("id", "")
                        val fn = tc.optJSONObject("function")
                        val name = fn?.optString("name", "") ?: ""
                        val args = fn?.optString("arguments", "") ?: ""

                        if (id.isNotEmpty() && name.isNotEmpty()) {
                            trySend(GroqStreamEvent.ToolCallStart(id, name, idx))
                        }
                        if (args.isNotEmpty()) {
                            trySend(GroqStreamEvent.ToolCallArgs(idx, args))
                        }
                    }
                }

                val finishReason = choice.optString("finish_reason", "")
                if (finishReason.isNotEmpty() && finishReason != "null") {
                    trySend(GroqStreamEvent.Finished(finishReason))
                }
            }
        } finally {
            reader.close()
            response.close()
        }
        close()
        awaitClose { call.cancel() }
    }

    suspend fun complete(
        messages: JSONArray,
        model: String = "llama-3.3-70b-versatile",
        temperature: Double = 0.3,
    ): String {
        val apiKey = KeystoreManager.loadApiKey(context)
            ?: throw IllegalStateException("Groq API key not set.")

        val body = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("stream", false)
            put("temperature", temperature)
        }

        val request = Request.Builder()
            .url(endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            response.close()
            if (!response.isSuccessful) throw Exception("Groq API error ${response.code}")
            val json = JSONObject(responseBody)
            json.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "")
        }
    }
}
