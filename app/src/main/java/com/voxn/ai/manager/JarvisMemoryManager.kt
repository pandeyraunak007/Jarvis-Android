package com.voxn.ai.manager

import android.content.Context
import android.content.SharedPreferences
import com.voxn.ai.data.model.ChatMessage
import com.voxn.ai.service.GroqService
import org.json.JSONArray
import org.json.JSONObject

class JarvisMemoryManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_memories", Context.MODE_PRIVATE)

    companion object {
        private const val KEY = "long_term_memories"
        private const val MAX = 50
    }

    fun getMemories(): List<String> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun add(facts: List<String>) {
        val cleaned = facts.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleaned.isEmpty()) return
        val merged = getMemories().toMutableList()
        cleaned.forEach { fact ->
            if (merged.none { it.equals(fact, ignoreCase = true) }) merged.add(fact)
        }
        val trimmed = if (merged.size > MAX) merged.takeLast(MAX) else merged
        save(trimmed)
    }

    fun remove(index: Int) {
        val list = getMemories().toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            save(list)
        }
    }

    fun removeAll() = save(emptyList())

    fun promptBlock(): String? {
        val memories = getMemories()
        if (memories.isEmpty()) return null
        val list = memories.joinToString("\n") { "- $it" }
        return "Long-term memory about this user (reference naturally, don't list them):\n$list"
    }

    suspend fun extractMemories(messages: List<ChatMessage>) {
        val userMessages = messages.filter { it.role == ChatMessage.Role.USER }
        if (userMessages.size < 2) return
        if (!KeystoreManager.hasApiKey(context)) return

        val transcript = messages
            .filter { it.role == ChatMessage.Role.USER || (it.role == ChatMessage.Role.ASSISTANT && it.content.isNotEmpty()) }
            .joinToString("\n") { "${if (it.role == ChatMessage.Role.USER) "User" else "JARVIS"}: ${it.content}" }

        val existing = getMemories()
        val existingBlock = if (existing.isEmpty()) "None yet." else existing.joinToString("\n") { "- $it" }

        val extractionPrompt = """
            Analyze this conversation between a user and their personal AI assistant. Extract durable facts worth remembering for future conversations. Focus on: preferences, goals, recurring patterns, personal details, likes/dislikes, lifestyle context.

            DO NOT extract:
            - Transient data (today's step count, current spending totals)
            - Things already in the existing memory below
            - Generic observations

            Existing memories (do not duplicate):
            $existingBlock

            Respond with ONLY a JSON array of short strings. If nothing new, respond with [].
            Example: ["Prefers morning workouts", "Trying to cut food delivery spending"]
        """.trimIndent()

        try {
            val groq = GroqService(context)
            val result = groq.complete(JSONArray().apply {
                put(JSONObject().put("role", "system").put("content", extractionPrompt))
                put(JSONObject().put("role", "user").put("content", transcript))
            }, temperature = 0.2)

            val jsonStart = result.indexOf('[')
            val jsonEnd = result.lastIndexOf(']')
            if (jsonStart < 0 || jsonEnd <= jsonStart) return
            val arr = JSONArray(result.substring(jsonStart, jsonEnd + 1))
            val facts = (0 until arr.length()).map { arr.getString(it) }
            add(facts)
        } catch (_: Exception) { }
    }

    private fun save(list: List<String>) {
        prefs.edit().putString(KEY, JSONArray(list).toString()).apply()
    }
}
