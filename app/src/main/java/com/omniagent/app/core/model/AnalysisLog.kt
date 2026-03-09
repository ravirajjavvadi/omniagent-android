package com.omniagent.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an analysis log entry stored in the local Room database.
 */
@Entity(tableName = "analysis_logs")
data class AnalysisLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userInput: String,
    val classifiedModule: String,
    val confidence: Double,
    val confidenceLevel: String,
    val resultJson: String,     // Encrypted JSON result
    val reasoningJson: String,  // Encrypted reasoning chain
    val sessionId: String = "default_session",
    val sessionTitle: String = "New Chat",
    val userRole: String = "user" // "admin" or "user"
)
