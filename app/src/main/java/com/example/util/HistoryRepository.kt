package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.model.SessionHistoryItem
import org.json.JSONArray
import org.json.JSONObject

class HistoryRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("airqr_history_prefs", Context.MODE_PRIVATE)

    fun getHistory(): List<SessionHistoryItem> {
        val jsonString = prefs.getString("session_history_list", null) ?: return emptyList()
        val list = mutableListOf<SessionHistoryItem>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    SessionHistoryItem(
                        id = obj.optString("id"),
                        transferId = obj.optString("transferId"),
                        fileName = obj.optString("fileName"),
                        fileSize = obj.optLong("fileSize"),
                        fileType = obj.optString("fileType"),
                        role = obj.optString("role"),
                        timestamp = obj.optLong("timestamp"),
                        hash = obj.optString("hash"),
                        totalChunks = obj.optInt("totalChunks"),
                        durationSeconds = obj.optDouble("durationSeconds"),
                        averageSpeedKb = obj.optDouble("averageSpeedKb"),
                        status = obj.optString("status")
                    )
                )
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }
        return list
    }

    fun addItem(item: SessionHistoryItem) {
        val current = getHistory().toMutableList()
        current.add(0, item)
        // Keep max 50 items
        val trimmed = if (current.size > 50) current.subList(0, 50) else current

        val array = JSONArray()
        for (it in trimmed) {
            val obj = JSONObject().apply {
                put("id", it.id)
                put("transferId", it.transferId)
                put("fileName", it.fileName)
                put("fileSize", it.fileSize)
                put("fileType", it.fileType)
                put("role", it.role)
                put("timestamp", it.timestamp)
                put("hash", it.hash)
                put("totalChunks", it.totalChunks)
                put("durationSeconds", it.durationSeconds)
                put("averageSpeedKb", it.averageSpeedKb)
                put("status", it.status)
            }
            array.put(obj)
        }
        prefs.edit().putString("session_history_list", array.toString()).apply()
    }

    fun clearHistory() {
        prefs.edit().remove("session_history_list").apply()
    }
}
