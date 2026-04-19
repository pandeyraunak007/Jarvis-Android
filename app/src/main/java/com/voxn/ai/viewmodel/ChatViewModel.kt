package com.voxn.ai.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.voxn.ai.data.model.ChatMessage
import com.voxn.ai.data.model.ToolCallRecord
import com.voxn.ai.manager.JarvisMemoryManager
import com.voxn.ai.manager.KeystoreManager
import com.voxn.ai.manager.UserProfileManager
import com.voxn.ai.service.ChatToolCatalog
import com.voxn.ai.service.ChatToolDispatcher
import com.voxn.ai.service.GroqService
import com.voxn.ai.service.GroqStreamEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val ctx: Context get() = getApplication()
    private val groq = GroqService(ctx)
    private val dispatcher = ChatToolDispatcher(ctx)
    private val memoryManager = JarvisMemoryManager(ctx)

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private var streamJob: Job? = null

    private val sessionFile: File get() = File(ctx.filesDir, "jarvis_chat_session.json")

    init { loadSession() }

    private val systemPrompt: String get() {
        val name = UserProfileManager(ctx).userName.value
        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", java.util.Locale.getDefault()).format(java.util.Date())
        var prompt = """
            You are Voxn, ${name}'s personal AI assistant inside the Voxn AI Android app — in the spirit of Iron Man's JARVIS. Speak with concise, confident, slightly dry professionalism. No filler, no hedging, no emoji.

            You have tools to read the user's real data (spending, habits, health, notes, calendar), log expenses, and create notes/reminders/tasks. ALWAYS call a tool before stating any specific number. Never guess. Amounts are INR — format as ₹ with Indian grouping (e.g., ₹1,240).

            Current local time: $nowIso. When the user asks you to "remind me to X at/in Y", "add a task", or "create a note", call create_note. For reminders/tasks, compute reminder_iso as an absolute local ISO timestamp (yyyy-MM-dd'T'HH:mm:ss) based on current time — never ask the user to format it. Plain notes omit reminder_iso.

            Keep replies short and specific. 1–4 tight sentences unless the user asks for detail. Lead with the answer, then one insight or actionable nudge if relevant. After any create/log tool, confirm briefly with what was saved.
        """.trimIndent()
        val memBlock = memoryManager.promptBlock()
        if (memBlock != null) prompt += "\n\n$memBlock"
        return prompt
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isStreaming.value) return
        if (!KeystoreManager.hasApiKey(ctx)) {
            _errorMessage.value = "Add your Groq API key in Settings first."
            return
        }
        _errorMessage.value = null
        _messages.value = _messages.value + ChatMessage(role = ChatMessage.Role.USER, content = trimmed)
        saveSession()
        startStream()
    }

    fun cancel() {
        streamJob?.cancel()
        streamJob = null
        _isStreaming.value = false
        val msgs = _messages.value.toMutableList()
        if (msgs.isNotEmpty() && msgs.last().status == ChatMessage.Status.STREAMING) {
            msgs[msgs.lastIndex] = msgs.last().copy(status = ChatMessage.Status.COMPLETE)
            _messages.value = msgs
            saveSession()
        }
    }

    fun clear() {
        cancel()
        _messages.value = emptyList()
        _errorMessage.value = null
        sessionFile.delete()
    }

    fun onDismiss() {
        saveSession()
        val userCount = _messages.value.count { it.role == ChatMessage.Role.USER }
        if (userCount >= 5) {
            viewModelScope.launch {
                memoryManager.extractMemories(_messages.value)
            }
        }
    }

    // MARK: Session persistence

    private fun saveSession() {
        val saveable = _messages.value.filter { it.status != ChatMessage.Status.STREAMING }
        if (saveable.isEmpty()) { sessionFile.delete(); return }
        try {
            val arr = JSONArray().apply { saveable.forEach { put(it.toJson()) } }
            sessionFile.writeText(arr.toString())
        } catch (_: Exception) { }
    }

    private fun loadSession() {
        if (!sessionFile.exists()) return
        try {
            val arr = JSONArray(sessionFile.readText())
            val loaded = (0 until arr.length()).map { ChatMessage.fromJson(arr.getJSONObject(it)) }
            _messages.value = loaded
        } catch (_: Exception) { sessionFile.delete() }
    }

    // MARK: Streaming loop

    private fun startStream() {
        _isStreaming.value = true
        streamJob = viewModelScope.launch { runLoop() }
    }

    private suspend fun runLoop() {
        try {
            for (iteration in 0 until 5) {
                val msgs = _messages.value.toMutableList()
                val assistant = ChatMessage(role = ChatMessage.Role.ASSISTANT, status = ChatMessage.Status.STREAMING)
                msgs.add(assistant)
                _messages.value = msgs
                val index = msgs.lastIndex

                val toolBuffers = mutableMapOf<Int, ToolCallRecord>()
                var finishReason: String? = null

                try {
                    groq.streamChat(buildGroqHistory(), ChatToolCatalog.tools()).collect { event ->
                        val current = _messages.value.toMutableList()
                        when (event) {
                            is GroqStreamEvent.ContentDelta -> {
                                val updated = current[index].copy(content = current[index].content + event.text)
                                current[index] = updated
                                _messages.value = current
                            }
                            is GroqStreamEvent.ToolCallStart -> {
                                toolBuffers[event.index] = ToolCallRecord(event.id, event.name, "")
                            }
                            is GroqStreamEvent.ToolCallArgs -> {
                                toolBuffers[event.index]?.let { it.arguments += event.args }
                            }
                            is GroqStreamEvent.Finished -> {
                                finishReason = event.reason
                            }
                        }
                    }
                } catch (e: Exception) {
                    val current = _messages.value.toMutableList()
                    val errMsg = "⚠ ${e.message ?: "Unknown error"}"
                    val content = if (current[index].content.isEmpty()) errMsg else current[index].content + "\n\n$errMsg"
                    current[index] = current[index].copy(content = content, status = ChatMessage.Status.ERROR)
                    _messages.value = current
                    return
                }

                val calls = toolBuffers.keys.sorted().mapNotNull { toolBuffers[it] }
                val current = _messages.value.toMutableList()
                current[index] = current[index].copy(toolCalls = calls, status = ChatMessage.Status.COMPLETE)
                _messages.value = current

                if (calls.isEmpty()) return

                val withToolResults = _messages.value.toMutableList()
                for (call in calls) {
                    val result = dispatcher.execute(call.name, call.arguments)
                    withToolResults.add(ChatMessage(role = ChatMessage.Role.TOOL, content = result, toolCallId = call.id))
                }
                _messages.value = withToolResults

                if (finishReason == "stop") return
            }
        } finally {
            _isStreaming.value = false
            saveSession()
        }
    }

    private fun buildGroqHistory(): JSONArray = JSONArray().apply {
        put(JSONObject().put("role", "system").put("content", systemPrompt))
        _messages.value.forEach { m ->
            when (m.role) {
                ChatMessage.Role.USER -> put(JSONObject().put("role", "user").put("content", m.content))
                ChatMessage.Role.ASSISTANT -> {
                    if (m.content.isEmpty() && m.toolCalls.isEmpty()) return@forEach
                    val obj = JSONObject().put("role", "assistant")
                    if (m.content.isNotEmpty()) obj.put("content", m.content)
                    if (m.toolCalls.isNotEmpty()) {
                        val tc = JSONArray()
                        m.toolCalls.forEach { call ->
                            tc.put(JSONObject().apply {
                                put("id", call.id)
                                put("type", "function")
                                put("function", JSONObject().put("name", call.name).put("arguments", call.arguments))
                            })
                        }
                        obj.put("tool_calls", tc)
                    }
                    put(obj)
                }
                ChatMessage.Role.TOOL -> {
                    put(JSONObject().put("role", "tool").put("content", m.content).put("tool_call_id", m.toolCallId))
                }
            }
        }
    }
}
