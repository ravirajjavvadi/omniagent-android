package com.omniagent.app.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents the classification result from the AI Kernel.
 */
data class ClassificationResult(
    val status: String = "",
    val module: String? = null,
    val moduleName: String = "",
    val confidence: Double = 0.0,
    val confidenceLevel: String = "",
    val all_scores: Map<String, Double> = emptyMap(),
    val ranking: List<ModuleScore> = emptyList(),
    val reasoning: List<String> = emptyList(),
    val input_features: Int = 0,
    val timestamp: String = ""
)

data class ModuleScore(
    val module: String = "",
    val score: Double = 0.0
)

/**
 * Represents structured resume data for building and exporting.
 */
data class ResumeData(
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val experienceDescription: String = "",
    val education: String = "",
    val skills: String = ""
) {
    fun toMarkdown(): String = """
        # $fullName
        $email | $phone

        ## $jobTitle at $company
        $experienceDescription

        ## Education
        $education

        ## Skills
        $skills
    """.trimIndent()
}

/**
 * Represents a single message in the chat conversation.
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val classification: ClassificationResult? = null,
    val engineResult: EngineResult? = null
)

/**
 * Represents the result from any domain engine.
 */
data class EngineResult(
    val module_name: String = "",
    val confidence_score: Double = 0.0,
    val reasoning: List<String> = emptyList(),
    val structured_analysis: Map<String, Any> = emptyMap(),
    val risk_score: Double = 0.0,
    val timestamp: String = ""
)

/**
 * Represents the full analysis pipeline result.
 */
data class AnalysisPipelineResult(
    val classification: ClassificationResult,
    val engineResult: EngineResult?,
    val totalProcessingTimeMs: Long = 0
)

/**
 * Represents user roles for RBAC.
 */
enum class UserRole(val displayName: String) {
    ADMIN("OmniAgent"),
    USER("Standard User");

    companion object {
        fun fromString(value: String): UserRole {
            return when (value.lowercase()) {
                "admin", "administrator" -> ADMIN
                else -> USER
            }
        }
    }
}

/**
 * Represents a partial update during AI streaming.
 */
data class StreamingUpdate(
    val token: String = "",
    val classification: ClassificationResult? = null,
    val engineResult: EngineResult? = null,
    val isComplete: Boolean = false,
    val error: String? = null
)

/**
 * Simple enum for processing state.
 */
enum class ProcessingState {
    IDLE, PROCESSING, SUCCESS, ERROR
}

/**
 * Represents a group of related messages (a thread).
 */
data class ChatSession(
    val id: String,
    val title: String,
    val lastTimestamp: Long
)
