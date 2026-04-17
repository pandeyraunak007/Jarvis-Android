package com.voxn.ai.data.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: Role,
    var content: String = "",
    var status: Status = Status.COMPLETE,
    var toolCalls: List<ToolCallRecord> = emptyList(),
    val toolCallId: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
) {
    enum class Role { USER, ASSISTANT, TOOL }
    enum class Status { STREAMING, COMPLETE, ERROR }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("role", role.name)
        put("content", content)
        put("status", status.name)
        put("toolCallId", toolCallId ?: JSONObject.NULL)
        put("timestamp", timestamp)
        put("toolCalls", JSONArray().apply {
            toolCalls.forEach { tc ->
                put(JSONObject().apply {
                    put("id", tc.id)
                    put("name", tc.name)
                    put("arguments", tc.arguments)
                })
            }
        })
    }

    companion object {
        fun fromJson(obj: JSONObject): ChatMessage = ChatMessage(
            id = obj.optString("id", UUID.randomUUID().toString()),
            role = Role.valueOf(obj.optString("role", "USER")),
            content = obj.optString("content", ""),
            status = Status.valueOf(obj.optString("status", "COMPLETE")),
            toolCallId = obj.optString("toolCallId").takeIf { it != "null" && it.isNotEmpty() },
            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
            toolCalls = buildList {
                val arr = obj.optJSONArray("toolCalls") ?: return@buildList
                for (i in 0 until arr.length()) {
                    val tc = arr.getJSONObject(i)
                    add(ToolCallRecord(tc.getString("id"), tc.getString("name"), tc.optString("arguments", "")))
                }
            },
        )
    }
}

data class ToolCallRecord(
    val id: String,
    val name: String,
    var arguments: String = "",
)
