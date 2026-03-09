package com.omniagent.app.domain.repository

import com.omniagent.app.core.model.AnalysisLog
import com.omniagent.app.core.model.AnalysisPipelineResult
import com.omniagent.app.core.model.ClassificationResult
import com.omniagent.app.core.model.EngineResult
import kotlinx.coroutines.flow.Flow

/**
 * Domain Interface for Analysis Operations.
 * Isolates UI and Use Cases from concrete implementation details.
 */
interface AnalysisRepository {
    suspend fun classifyInput(userInput: String): ClassificationResult
    suspend fun runFullPipeline(
        userInput: String, 
        userRole: String = "user",
        sessionId: String,
        sessionTitle: String,
        history: String? = null
    ): AnalysisPipelineResult
    
    fun getAllLogs(): Flow<List<AnalysisLog>>
    fun getRecentLogs(limit: Int = 20): Flow<List<AnalysisLog>>
    fun getLogsByModule(module: String): Flow<List<AnalysisLog>>
    fun searchLogs(query: String): Flow<List<AnalysisLog>>
    
    suspend fun clearAllLogs()
    suspend fun getLogCount(): Int
    fun decryptLogResult(encryptedJson: String): String
    suspend fun getKernelReasoningLog(): String

    // === SESSION MANAGEMENT ===
    fun getAllSessions(): Flow<List<com.omniagent.app.core.model.ChatSession>>
    fun getLogsBySession(sessionId: String): Flow<List<AnalysisLog>>
    suspend fun renameSession(sessionId: String, newTitle: String)
    suspend fun deleteSession(sessionId: String)

    // === NEW POWER FEATURES ===
    suspend fun performSystemScan(): EngineResult
    suspend fun tailorResume(resumeText: String, jobDescription: String): EngineResult
}
