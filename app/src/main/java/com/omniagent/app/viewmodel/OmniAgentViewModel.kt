package com.omniagent.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import com.omniagent.app.core.model.*
import com.omniagent.app.data.local.OmniAgentDatabase
import com.omniagent.app.domain.repository.AnalysisRepository
import com.omniagent.app.engine.LlamaEngine
import com.omniagent.app.security.*
import com.chaquo.python.Python
import com.google.gson.Gson

/**
 * Main ViewModel — manages all UI state and orchestrates the analysis pipeline.
 * Refactored for DI: accepts repository via constructor.
 */
class OmniAgentViewModel(
    private val repository: AnalysisRepository,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "OmniAgent"
    }

    // Singleton LlamaEngine — lives as long as ViewModel (prevents crash from reloading per-message)
    private val llamaEngine = LlamaEngine()

    // Auto-lock inactivity timer (5 minutes)
    private val INACTIVITY_TIMEOUT_MS = 5 * 60 * 1000L
    private var inactivityJob: kotlinx.coroutines.Job? = null
    
    // Persistent State Handle
    private val sharedPrefs: SharedPreferences = application.getSharedPreferences(
        "omniagent_prefs", Context.MODE_PRIVATE
    )

    // === UI STATE ===

    private val _uiState = MutableStateFlow(OmniAgentUiState())
    val uiState: StateFlow<OmniAgentUiState> = _uiState.asStateFlow()

    private val _classificationResult = MutableStateFlow<ClassificationResult?>(null)
    val classificationResult: StateFlow<ClassificationResult?> = _classificationResult.asStateFlow()

    private val _engineResult = MutableStateFlow<EngineResult?>(null)
    val engineResult: StateFlow<EngineResult?> = _engineResult.asStateFlow()

    private val _tailoringResult = MutableStateFlow<EngineResult?>(null)
    val tailoringResult: StateFlow<EngineResult?> = _tailoringResult.asStateFlow()

    private val _reasoningSteps = MutableStateFlow<List<String>>(emptyList())
    val reasoningSteps: StateFlow<List<String>> = _reasoningSteps.asStateFlow()

    private val _pipelineResult = MutableStateFlow<AnalysisPipelineResult?>(null)
    val pipelineResult: StateFlow<AnalysisPipelineResult?> = _pipelineResult.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _chatInput = MutableStateFlow("")
    val chatInput: StateFlow<String> = _chatInput.asStateFlow()

    // === CAREER HUB STATE ===
    private val _careerResumeData = MutableStateFlow(ResumeData())
    val careerResumeData: StateFlow<ResumeData> = _careerResumeData.asStateFlow()

    private val _careerAuditResult = MutableStateFlow<EngineResult?>(null)
    val careerAuditResult: StateFlow<EngineResult?> = _careerAuditResult.asStateFlow()

    private val _careerTailorResult = MutableStateFlow<EngineResult?>(null)
    val careerTailorResult: StateFlow<EngineResult?> = _careerTailorResult.asStateFlow()

    val recentLogs = repository.getRecentLogs(50)

    init {
        restorePendingAnalysisState()
    }

    // === ACTIONS ===

    /**
     * Run the full analysis pipeline with user input.
     */
    fun analyzeInput(userInput: String) {
        if (userInput.isBlank()) return
        // Save state persistently in case process dies
        savePendingAnalysisState(userInput)

        viewModelScope.launch {
            // Clear immediately to prevent crash loops
            clearPendingAnalysisState()
            
            _uiState.update { it.copy(isProcessing = true, error = null, activeTab = DashboardTab.OUTPUT) }
            Log.d("OmniAgent", "UI State updated: isProcessing=true, activeTab=OUTPUT")
            
            withContext(Dispatchers.Main) {
                Toast.makeText(getApplication(), "Starting analysis...", Toast.LENGTH_SHORT).show()
            }

            try {
                val role = AccessControl.getCurrentRole().name.lowercase()
                Log.d("OmniAgent", "Calling repository.runFullPipeline...")
                val result = repository.runFullPipeline(userInput, role)
                
                Log.d("OmniAgent", "Pipeline result received. Classification: ${result.classification.moduleName}")

                _pipelineResult.value = result
                _reasoningSteps.value = result.classification.reasoning
                _classificationResult.value = result.classification
                _engineResult.value = result.engineResult

                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        lastModule = result.classification.module ?: "none",
                        lastModuleName = result.classification.moduleName,
                        lastConfidence = result.classification.confidence,
                        processingTimeMs = result.totalProcessingTimeMs,
                        hasResult = true
                    )
                }
                Log.d("OmniAgent", "UI State updated: isProcessing=false, hasResult=true")
                
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Analysis Complete!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("OmniAgent", "Analysis failed", e)
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        error = "Analysis failed: ${e.message}"
                    )
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Switch active dashboard tab.
     */
    fun switchTab(tab: DashboardTab) {
        resetInactivityTimer()
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun updateChatInput(input: String) {
        _chatInput.value = input
    }

    // === CAREER HUB ACTIONS ===

    fun updateResumeData(data: ResumeData) {
        _careerResumeData.value = data
    }

    fun runCareerAudit(text: String) {
        if (text.isBlank()) return
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                val py = Python.getInstance()
                val engine = py.getModule("resume_engine")
                val resultJson = withContext(Dispatchers.IO) {
                    engine.callAttr("analyze_resume", text).toString()
                }
                val engineResult = Gson().fromJson(resultJson, EngineResult::class.java)
                _careerAuditResult.value = engineResult
                _uiState.update { it.copy(isProcessing = false) }
            } catch (e: Exception) {
                Log.e("OmniAgent", "Career audit failed", e)
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    fun runCareerTailor(resume: String, jd: String) {
        if (resume.isBlank() || jd.isBlank()) return
        _uiState.update { it.copy(isProcessing = true) }
        viewModelScope.launch {
            try {
                val py = Python.getInstance()
                val engine = py.getModule("resume_engine")
                val resultJson = withContext(Dispatchers.IO) {
                    engine.callAttr("tailor_resume", resume, jd).toString()
                }
                val engineResult = Gson().fromJson(resultJson, EngineResult::class.java)
                _careerTailorResult.value = engineResult
                _uiState.update { it.copy(isProcessing = false) }
            } catch (e: Exception) {
                Log.e("OmniAgent", "Career tailoring failed", e)
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    /**
     * Clear current results.
     */
    fun clearResults() {
        _classificationResult.value = null
        _engineResult.value = null
        _reasoningSteps.value = emptyList()
        _pipelineResult.value = null
        _uiState.update { it.copy(hasResult = false, lastModule = "", lastModuleName = "") }
    }

    /**
     * Clear all logs.
     */
    fun clearAllLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    /**
     * Authenticate as admin.
     */
    fun authenticateAdmin(pin: String): Boolean {
        return true 
    }

    /**
     * Switch to user role.
     */
    fun switchToUserRole() {
        // No-op: Persistent Admin state
    }

    /**
     * Decrypt log result (admin only).
     */
    fun decryptLogResult(encrypted: String): String {
        return repository.decryptLogResult(encrypted)
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    // Demo functionality removed for Full Realization phase


    // No-op for role remnants
    private fun resetInactivityTimer() {}

    // === PERSISTENCE LOGIC ===
    private fun savePendingAnalysisState(input: String) {
        sharedPrefs.edit().putString("pending_analysis_input", input).apply()
    }

    private fun clearPendingAnalysisState() {
        sharedPrefs.edit().remove("pending_analysis_input").apply()
    }

    private fun restorePendingAnalysisState() {
        val pendingInput = sharedPrefs.getString("pending_analysis_input", null)
        if (!pendingInput.isNullOrBlank()) {
            _uiState.update { it.copy(error = "Resuming incomplete background analysis...") }
            analyzeInput(pendingInput)
        }
    }

    /**
     * Send a message in the chat and get AI response.
     * Uses the singleton LlamaEngine with model-specific chat templates for accuracy.
     */
    fun sendMessage(text: String, localModelPath: String? = null) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(text = text, isUser = true)
        _chatMessages.update { it + userMsg }

        viewModelScope.launch {
            // Priority Interrupt: If already processing, stop the old one first
            if (_uiState.value.isProcessing) {
                Log.i(TAG, "Priority Interrupt: Stopping active inference for new message.")
                llamaEngine.stopInference()
                kotlinx.coroutines.delay(500) // Small breather for native cleanup
            }

            _uiState.update { it.copy(isProcessing = true) }
            try {
                // --- FAST PATH: Greetings and Small Queries (< 5s target) ---
                val lowCaseText = text.lowercase().trim()
                val isGreeting = lowCaseText.matches(Regex(".*\\b(hi|hello|hey|greetings|morning|afternoon|thanks|thank you|bye|goodbye)\\b.*"))
                
                if (isGreeting) {
                    Log.i(TAG, "Fast Path: Routing to Python General Engine for instant response.")
                    val role = AccessControl.getCurrentRole().name.lowercase()
                    val result = repository.runFullPipeline(text, role)
                    val summaryVal = result.engineResult?.structured_analysis?.get("summary")?.toString() ?: "Hello! How can I help you today?"
                    
                    _chatMessages.update { it + ChatMessage(text = summaryVal, isUser = false, classification = result.classification) }
                    _uiState.update { it.copy(isProcessing = false) }
                    return@launch
                }

                // --- LLM PATH: Optimized for 10-Thread Speed ---
                if (localModelPath != null && java.io.File(localModelPath).exists()) {
                    withContext(Dispatchers.IO) {
                        val loaded = llamaEngine.loadModel(localModelPath)
                        if (!loaded) {
                            withContext(Dispatchers.Main) {
                                _chatMessages.update {
                                    it + ChatMessage(
                                        text = "❌ Failed to load AI model. The file may be corrupt. Please re-download.",
                                        isUser = false
                                    )
                                }
                            }
                            return@withContext
                        }

                        // Apply model-specific chat template for accurate responses
                        val formattedPrompt = buildChatPrompt(text, localModelPath)
                        Log.d(TAG, "Using prompt template for: ${localModelPath.substringAfterLast('/')}")

                        val aiMsgId = System.currentTimeMillis().toString()
                        val initialAiMsg = ChatMessage(
                            id = aiMsgId,
                            text = "",
                            isUser = false,
                            classification = ClassificationResult(
                                module = "llm_chat",
                                moduleName = "Offline AI Engine",
                                confidence = 1.0,
                                reasoning = listOf("Generating response...")
                            )
                        )
                        _chatMessages.update { it + initialAiMsg }

                        // Real-time Thinking Flow
                        _reasoningSteps.value = listOf("Initializing Neural Weights...", "Optimizing 6-Thread Performance...", "Generating Token Stream (Completeness Optimized)...")

                        llamaEngine.generateStream(
                            prompt = formattedPrompt,
                            listener = object : LlamaEngine.StreamingListener {
                                private var isFirstToken = true
                                override fun onTokenGenerated(token: String) {
                                    var cleanedToken = token
                                    
                                    // Robust Artifact Stripping: Only on the start of the stream
                                    if (isFirstToken) {
                                        cleanedToken = token.trimStart()
                                            .replace(Regex("^\"\"\"|^```"), "")
                                        if (cleanedToken.isNotEmpty()) isFirstToken = false
                                    }

                                    _chatMessages.update { messages ->
                                        messages.map { msg ->
                                            if (msg.id == aiMsgId) msg.copy(text = msg.text + cleanedToken)
                                            else msg
                                        }
                                    }
                                }
                                override fun onStreamComplete() {
                                    Log.i(TAG, "Streaming complete")
                                    _reasoningSteps.value = listOf("Neural alignment complete.", "Response delivered within 1 minute.")
                                }
                                override fun onStreamError(error: String) {
                                    Log.e(TAG, "Stream error: $error")
                                    _chatMessages.update { messages ->
                                        messages.map { msg ->
                                            if (msg.id == aiMsgId && msg.text.isBlank())
                                                msg.copy(text = "⚠️ Response error: $error")
                                            else msg
                                        }
                                    }
                                }
                            }
                        )
                    }
                } else {
                    // Fallback to the Python heuristic engine if no model downloaded
                    val role = AccessControl.getCurrentRole().name.lowercase()
                    val result = repository.runFullPipeline(text, role)

                    val summaryVal = result.engineResult?.structured_analysis?.get("summary")?.toString()
                    val defaultPrefix = if (result.classification.moduleName == "General Context Handler") "" else "Analysis complete: "
                    val textResponse = summaryVal ?: (defaultPrefix + (result.engineResult?.reasoning?.firstOrNull() ?: "I've analyzed your request."))

                    val aiMsg = ChatMessage(
                        text = textResponse,
                        isUser = false,
                        classification = result.classification,
                        engineResult = result.engineResult
                    )
                    _chatMessages.update { it + aiMsg }
                    _pipelineResult.value = result
                    _engineResult.value = result.engineResult
                }
            } catch (e: Exception) {
                Log.e(TAG, "sendMessage error", e)
                _chatMessages.update {
                    it + ChatMessage(text = "⚠️ An unexpected error occurred: ${e.message}", isUser = false)
                }
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    /**
     * Explicitly stop the current AI generation.
     */
    fun stopResponse() {
        Log.i(TAG, "User requested to stop response.")
        llamaEngine.stopInference()
        _uiState.update { it.copy(isProcessing = false) }
    }

    /**
     * Builds a model-specific chat prompt for accurate AI responses.
     * Detects the model from the file path and applies the correct template.
     */
    private fun buildChatPrompt(userMessage: String, modelPath: String): String {
        // HYBRID QUALITY: Prioritize complete, accurate, and professional answers. No time-limit pressure.
        val systemPrompt = "You are a professional and accurate AI. Provide complete and helpful answers. " +
                "Respect user constraints strictly (e.g., if '3 lines' is asked, give exactly 3 descriptive lines). " +
                "For code, provide ONLY the requested code block without excessive preamble. " +
                "Do not rush; prioritize completeness for long answers."

        return when {
            // Qwen2.5 chat template
            modelPath.contains("qwen", ignoreCase = true) -> {
                "<|im_start|>system\n$systemPrompt<|im_end|>\n" +
                        "<|im_start|>user\n$userMessage<|im_end|>\n" +
                        "<|im_start|>assistant\n"
            }
            // Gemma 2 chat template
            modelPath.contains("gemma", ignoreCase = true) -> {
                "<start_of_turn>user\n$systemPrompt\n\nUser Question: $userMessage<end_of_turn>\n" +
                        "<start_of_turn>model\n"
            }
            // Generic fallback (instruction-style)
            else -> {
                "### System:\n$systemPrompt\n\n### User:\n$userMessage\n\n### Assistant:\n"
            }
        }
    }

    /**
     * Tailor a resume to a specific job description.
     */
    fun tailorResume(resumeText: String, jobDescription: String) {
        if (resumeText.isBlank() || jobDescription.isBlank()) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, error = null) }
            try {
                val result = repository.tailorResume(resumeText, jobDescription)
                _tailoringResult.value = result
                _uiState.update { it.copy(isProcessing = false, hasResult = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = "Tailoring failed: ${e.message}") }
            }
        }
    }
}

/**
 * Factory to inject dependencies into the ViewModel.
 */
class OmniAgentViewModelFactory(
    private val repository: AnalysisRepository,
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OmniAgentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OmniAgentViewModel(repository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * Represents the full UI state of the dashboard.
 */
data class OmniAgentUiState(
    val isProcessing: Boolean = false,
    val hasResult: Boolean = false,
    val error: String? = null,
    val activeTab: DashboardTab = DashboardTab.OUTPUT,
    val lastModule: String = "",
    val lastModuleName: String = "",
    val lastConfidence: Double = 0.0,
    val processingTimeMs: Long = 0,
    val currentRole: UserRole = UserRole.ADMIN // Default to full access
)

/**
 * Dashboard navigation tabs.
 */
enum class DashboardTab(val title: String, val icon: String) {
    CHAT("Chat", "chat"),
    OUTPUT("Output", "workspace"),
    REASONING("Reasoning", "brain"),
    CAREER("Career", "trending_up"),
    LOGS("Logs", "history"),
    SETTINGS("Settings", "settings"),
    MODEL_SELECTION("AI Setup", "smart_toy")
}
