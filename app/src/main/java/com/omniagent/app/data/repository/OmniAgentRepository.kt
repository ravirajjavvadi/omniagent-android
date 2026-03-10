package com.omniagent.app.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omniagent.app.data.local.AnalysisLogDao
import com.omniagent.app.core.model.*
import com.omniagent.app.domain.repository.AnalysisRepository
import com.omniagent.app.kernel.PythonKernelManager
import com.omniagent.app.security.CryptoManager
import com.omniagent.app.security.FileSandbox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Repository — bridges Kotlin UI with Python AI Kernel and Domain Engines.
 * All Python calls are routed through Chaquopy via PythonKernelManager.
 * Results are encrypted before database storage.
 */
class OmniAgentRepository(
    private val analysisLogDao: AnalysisLogDao,
    private val kernelManager: PythonKernelManager,
    private val llamaEngine: com.omniagent.app.engine.LlamaEngine
) : AnalysisRepository {
    private val gson = Gson()

    // === KERNEL OPERATIONS ===

    /**
     * Classify user input through the AI Kernel.
     * Returns which module should handle the task.
     */
    override suspend fun classifyInput(userInput: String): ClassificationResult = withContext(Dispatchers.IO) {
        val sanitizedInput = FileSandbox.sanitizeInput(userInput)
        val resultJson = kernelManager.classify(sanitizedInput)

        parseClassificationResult(resultJson)
    }

    /**
     * Run the full analysis pipeline:
     * 1. Classify input → determine module
     * 2. Route to appropriate engine
     * 3. Get structured result
     * 4. Store encrypted log
     */
    override suspend fun runFullPipeline(
        userInput: String,
        userRole: String,
        sessionId: String,
        sessionTitle: String,
        history: String?
    ): AnalysisPipelineResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        Log.d("OmniAgent", "Pipeline started for input: $userInput")
        val sanitizedInput = FileSandbox.sanitizeInput(userInput)

        // Step 1: Classify
        Log.d("OmniAgent", "Step 1: Classifying input...")
        val classification = classifyInput(sanitizedInput)
        Log.d("OmniAgent", "Classification result: ${classification.module} (${classification.confidence})")

        // Step 2: Route to engine (fallback to general if unknown)
        Log.d("OmniAgent", "Step 2: Routing to engine...")
        val engineResult = runEngine(classification.module ?: "general", sanitizedInput, history)
        Log.d("OmniAgent", "Engine result received from: ${engineResult.module_name}")

        // Step 3: Store encrypted log
        Log.d("OmniAgent", "Step 3: Storing log...")
        val log = AnalysisLog(
            userInput = sanitizedInput,
            classifiedModule = classification.module ?: "none",
            confidence = classification.confidence,
            confidenceLevel = classification.confidenceLevel,
            resultJson = CryptoManager.encrypt(gson.toJson(engineResult)),
            reasoningJson = CryptoManager.encrypt(gson.toJson(classification.reasoning)),
            userRole = userRole,
            sessionId = sessionId,
            sessionTitle = sessionTitle
        )
        analysisLogDao.insertLog(log)

        val totalTime = System.currentTimeMillis() - startTime
        Log.d("OmniAgent", "Pipeline finished in ${totalTime}ms")

        AnalysisPipelineResult(
            classification = classification,
            engineResult = engineResult,
            totalProcessingTimeMs = totalTime
        )
    }

    // === ENGINE OPERATIONS ===

    /**
     * Route to the correct engine based on classification.
     */
    private suspend fun runEngine(moduleName: String, input: String, history: String? = null): EngineResult = withContext(Dispatchers.IO) {
        val resultJson = kernelManager.runEngine(moduleName, input, history)
        
        try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(resultJson, type)

            if (map.containsKey("error")) {
                return@withContext EngineResult(
                    module_name = "Error",
                    reasoning = listOf(map["details"] as? String ?: "Engine execution failed"),
                    timestamp = System.currentTimeMillis().toString()
                )
            }

            @Suppress("UNCHECKED_CAST")
            EngineResult(
                module_name = map["module_name"] as? String ?: "",
                confidence_score = (map["confidence_score"] as? Number)?.toDouble() ?: 0.0,
                reasoning = (map["reasoning"] as? List<String>) ?: emptyList(),
                structured_analysis = (map["structured_analysis"] as? Map<String, Any>) ?: emptyMap(),
                risk_score = (map["risk_score"] as? Number)?.toDouble() ?: 0.0,
                timestamp = map["timestamp"] as? String ?: ""
            )
        } catch (e: Exception) {
            EngineResult(
                module_name = "Parsing Error",
                reasoning = listOf("Failed to parse engine output: ${e.message}"),
                timestamp = System.currentTimeMillis().toString()
            )
        }
    }

    // === LOG OPERATIONS ===

    override fun getAllLogs(): Flow<List<AnalysisLog>> = analysisLogDao.getAllLogs()

    override fun getRecentLogs(limit: Int): Flow<List<AnalysisLog>> = analysisLogDao.getRecentLogs(limit)

    override fun getLogsByModule(module: String): Flow<List<AnalysisLog>> = analysisLogDao.getLogsByModule(module)

    override fun searchLogs(query: String): Flow<List<AnalysisLog>> = analysisLogDao.searchLogs(query)

    override suspend fun clearAllLogs() = analysisLogDao.clearAllLogs()

    override suspend fun getLogCount(): Int = analysisLogDao.getLogCount()

    // === SESSION MANAGEMENT ===
    override fun getAllSessions(): Flow<List<ChatSession>> = analysisLogDao.getAllSessions()

    override fun getLogsBySession(sessionId: String): Flow<List<AnalysisLog>> = 
        analysisLogDao.getLogsBySession(sessionId)

    override suspend fun renameSession(sessionId: String, newTitle: String) = 
        analysisLogDao.renameSession(sessionId, newTitle)

    override suspend fun deleteSession(sessionId: String) = 
        analysisLogDao.deleteSession(sessionId)

    /**
     * Decrypt a log's result JSON (admin only).
     */
    override fun decryptLogResult(encryptedJson: String): String {
        return CryptoManager.decrypt(encryptedJson)
    }

    /**
     * Get reasoning log from Python kernel.
     */
    override suspend fun getKernelReasoningLog(): String = withContext(Dispatchers.IO) {
        kernelManager.getReasoningLog()
    }

    override fun runStreamingPipeline(
        userInput: String,
        userRole: String,
        sessionId: String,
        sessionTitle: String,
        history: String?,
        maxTokens: Int
    ): Flow<StreamingUpdate> = callbackFlow {
        val startTime = System.currentTimeMillis()
        Log.d("OmniAgent", "Streaming Pipeline started for: $userInput with maxTokens: $maxTokens")

        val sanitizedInput = FileSandbox.sanitizeInput(userInput)

        // Step 1: Classify (Fast Path)
        val classification = try {
            classifyInput(sanitizedInput)
        } catch (e: Exception) {
            trySend(StreamingUpdate(error = "Classification failed: ${e.message}"))
            close(e)
            return@callbackFlow
        }
        
        trySend(StreamingUpdate(classification = classification))

        // Step 2: Route
        val moduleName = classification.module ?: "general"
        
        if (moduleName == "general" || moduleName == "coding") {
            // Favor LlamaEngine for most queries for "ChatGPT feel"
            val listener = object : com.omniagent.app.engine.LlamaEngine.StreamingListener {
                override fun onTokenGenerated(token: String) {
                    trySend(StreamingUpdate(token = token))
                }
                override fun onStreamComplete() {
                    trySend(StreamingUpdate(isComplete = true))
                    close()
                }
                override fun onStreamError(error: String) {
                    trySend(StreamingUpdate(error = error))
                    close(Exception(error))
                }
            }
            
            // Note: In an actual app, we'd build a template here. 
            // For now, passing prompt directly as LlamaEngine handles templates internally.
            val success = llamaEngine.generateStream(sanitizedInput, maxTokens, listener)
            if (!success) {
                trySend(StreamingUpdate(error = "Engine failed to start."))
                close()
            }
        } else {
            // For specialized Python engines, we don't have native streaming yet, 
            // so we return the full result as a "single token" update.
            val engineResult = try {
                runEngine(moduleName, sanitizedInput, history)
            } catch (e: Exception) {
                EngineResult(module_name = "Error", reasoning = listOf(e.message ?: "Unknown"))
            }

            val summary = engineResult.structured_analysis["summary"] as? String 
                ?: engineResult.reasoning.firstOrNull() 
                ?: "Analysis complete."

            trySend(StreamingUpdate(token = summary, engineResult = engineResult, isComplete = true))
            close()
        }

        awaitClose {
            llamaEngine.stopInference()
        }
    }

    // === NEW POWER FEATURES ===

    override suspend fun performSystemScan(): EngineResult = withContext(Dispatchers.IO) {
        val resultJson = kernelManager.performSystemScan()
        parseEngineResult(resultJson)
    }

    override suspend fun tailorResume(resumeText: String, jobDescription: String): EngineResult = withContext(Dispatchers.IO) {
        val resultJson = kernelManager.tailorResume(resumeText, jobDescription)
        parseEngineResult(resultJson)
    }

    private fun parseEngineResult(json: String): EngineResult {
        return try {
            val type = object : TypeToken<Map<String, Any>>() {}.type
            val map: Map<String, Any> = gson.fromJson(json, type)
            
            @Suppress("UNCHECKED_CAST")
            EngineResult(
                module_name = map["module_name"] as? String ?: "",
                confidence_score = (map["confidence_score"] as? Number)?.toDouble() ?: 0.0,
                reasoning = (map["reasoning"] as? List<String>) ?: emptyList(),
                structured_analysis = (map["structured_analysis"] as? Map<String, Any>) ?: emptyMap(),
                risk_score = (map["risk_score"] as? Number)?.toDouble() ?: 0.0,
                timestamp = map["timestamp"] as? String ?: ""
            )
        } catch (e: Exception) {
            EngineResult(
                module_name = "Parse Error",
                reasoning = listOf("Failed to parse output: ${e.message}"),
                timestamp = System.currentTimeMillis().toString()
            )
        }
    }

    // === PARSING ===

    private fun parseClassificationResult(json: String): ClassificationResult {
        return try {
            val map: Map<String, Any> = gson.fromJson(
                json, object : TypeToken<Map<String, Any>>() {}.type
            )

            if (map["status"] == "error") {
                return ClassificationResult(status = "error", moduleName = map["message"] as? String ?: "Unknown Error")
            }

            @Suppress("UNCHECKED_CAST")
            ClassificationResult(
                status = map["status"] as? String ?: "",
                module = map["module"] as? String,
                moduleName = map["module_name"] as? String ?: "",
                confidence = (map["confidence"] as? Number)?.toDouble() ?: 0.0,
                confidenceLevel = map["confidence_level"] as? String ?: "",
                all_scores = (map["all_scores"] as? Map<String, Number>)
                    ?.mapValues { it.value.toDouble() } ?: emptyMap(),
                ranking = (map["ranking"] as? List<Map<String, Any>>)?.map { entry ->
                    ModuleScore(
                        module = entry["module"] as? String ?: "",
                        score = (entry["score"] as? Number)?.toDouble() ?: 0.0
                    )
                } ?: emptyList(),
                reasoning = (map["reasoning"] as? List<String>) ?: emptyList(),
                input_features = (map["input_features"] as? Number)?.toInt() ?: 0,
                timestamp = map["timestamp"] as? String ?: ""
            )
        } catch (e: Exception) {
            ClassificationResult(
                status = "error",
                moduleName = "Parse Error: ${e.message}"
            )
        }
    }
}
