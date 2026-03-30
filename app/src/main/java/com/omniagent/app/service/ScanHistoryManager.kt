package com.omniagent.app.service

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

data class ScanEvent(
    val id: String,
    val type: EventType,
    val title: String,
    val description: String,
    val timestamp: Long,
    val packageName: String? = null,
    val riskLevel: RiskLevel = RiskLevel.LOW
)

enum class EventType {
    URL_SCAN, APP_SCAN, SENSOR_ACCESS, THREAT_BLOCKED, PERMISSION_CHANGED
}

enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

class ScanHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("scan_history", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun addEvent(event: ScanEvent) {
        val history = getHistory().toMutableList()
        history.add(0, event) // Add to top
        
        // Keep only last 24h
        val cutOff = Calendar.getInstance().apply { add(Calendar.HOUR, -24) }.timeInMillis
        val recentHistory = history.filter { it.timestamp > cutOff }
        
        saveHistory(recentHistory)
    }

    fun getHistory(): List<ScanEvent> {
        val json = prefs.getString("events", "[]")
        val type = object : TypeToken<List<ScanEvent>>() {}.type
        return try {
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun clearHistory() {
        prefs.edit().putString("events", "[]").apply()
    }

    private fun saveHistory(history: List<ScanEvent>) {
        val json = gson.toJson(history)
        prefs.edit().putString("events", json).apply()
    }
}
