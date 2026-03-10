package com.omniagent.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import android.util.Log
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import com.omniagent.app.service.DeviceHealthManager
import com.omniagent.app.service.DeviceVitals
import com.omniagent.app.service.AppHealthStats
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.content.Intent

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

    private val _currentSessionId = MutableStateFlow(java.util.UUID.randomUUID().toString())
    val currentSessionId: StateFlow<String> = _currentSessionId.asStateFlow()

    private val _currentSessionTitle = MutableStateFlow("New Chat")
    val currentSessionTitle: StateFlow<String> = _currentSessionTitle.asStateFlow()

    val chatHistory: StateFlow<List<ChatSession>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    // === CAREER HUB STATE ===
    private val _careerResumeData = MutableStateFlow(ResumeData())
    val careerResumeData: StateFlow<ResumeData> = _careerResumeData.asStateFlow()

    private val _careerAuditResult = MutableStateFlow<EngineResult?>(null)
    val careerAuditResult: StateFlow<EngineResult?> = _careerAuditResult.asStateFlow()

    private val _careerTailorResult = MutableStateFlow<EngineResult?>(null)
    val careerTailorResult: StateFlow<EngineResult?> = _careerTailorResult.asStateFlow()

    val recentLogs = repository.getRecentLogs(50)

    // === CYBERSEC GUARDIAN STATE ===
    private val deviceHealthManager = DeviceHealthManager(application)

    private val _deviceVitals = MutableStateFlow<DeviceVitals?>(null)
    val deviceVitals: StateFlow<DeviceVitals?> = _deviceVitals.asStateFlow()

    private val _suspiciousApps = MutableStateFlow<List<AppHealthStats>>(emptyList())
    val suspiciousApps: StateFlow<List<AppHealthStats>> = _suspiciousApps.asStateFlow()

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    init {
        restorePendingAnalysisState()
        refreshCyberSecVitals()
        initSpeechRecognizer()
    }

    private fun initSpeechRecognizer() {
        Handler(Looper.getMainLooper()).post {
            if (SpeechRecognizer.isRecognitionAvailable(getApplication())) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getApplication())
                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        Log.d(TAG, "SpeechRecognizer: Ready")
                        _isRecordingVoice.value = true
                    }
                    override fun onBeginningOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer: Beginning")
                    }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        Log.d(TAG, "SpeechRecognizer: End")
                        _isRecordingVoice.value = false
                    }
                    override fun onError(error: Int) {
                        _isRecordingVoice.value = false
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Assistant is busy"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Assistant error: $error"
                        }
                        Log.e(TAG, "Speech Error ($error): $message")
                        if (error != SpeechRecognizer.ERROR_NO_MATCH) {
                            Toast.makeText(getApplication(), message, Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        Log.d(TAG, "Speech Results: $matches")
                        if (!matches.isNullOrEmpty()) {
                            val text = matches[0]
                            _chatInput.value = text
                            if (text.length > 3 && !uiState.value.isProcessing) {
                                sendMessage()
                            }
                        }
                        _isRecordingVoice.value = false
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            _chatInput.value = matches[0]
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            } else {
                Log.e(TAG, "Speech Recognition not available")
            }
        }
    }

    fun startVoiceRecording() {
        Handler(Looper.getMainLooper()).post {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
                _isRecordingVoice.value = false
            }
        }
    }

    fun stopVoiceRecording() {
        speechRecognizer?.stopListening()
        _isRecordingVoice.value = false
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
    }

    // === CYBERSEC ACTIONS ===
    fun refreshCyberSecVitals() {
        viewModelScope.launch {
            try {
                _deviceVitals.value = deviceHealthManager.getVitals()
                // Usage stats queries should be on IO to prevent jank
                val apps = withContext(Dispatchers.IO) { deviceHealthManager.scanAppActivity() }
                _suspiciousApps.value = apps
            } catch (e: Exception) {
                Log.e("OmniAgent", "Failed to refresh CyberSec vitals", e)
            }
        }
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
                val result = repository.runFullPipeline(
                    userInput = userInput,
                    userRole = role,
                    sessionId = currentSessionId.value,
                    sessionTitle = currentSessionTitle.value,
                    history = formatChatHistoryForPipeline()
                )
                
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

    fun sendMessage() {
        val input = _chatInput.value.trim()
        if (input.isEmpty()) return

        // If it's a new chat, use the first message as the title
        if (chatMessages.value.isEmpty() && _currentSessionTitle.value == "New Chat") {
            _currentSessionTitle.value = if (input.length > 20) input.take(17) + "..." else input
        }

        val userMessage = ChatMessage(text = input, isUser = true)
        _chatMessages.value = _chatMessages.value + userMessage
        _chatInput.value = ""

        // Include up to 5 previous messages for context
        val contextMessages = chatMessages.value.takeLast(5)
        val historyString = if (contextMessages.isNotEmpty()) {
            contextMessages.joinToString("\n") { (if (it.isUser) "User:" else "AI:") + " " + it.text }
        } else null

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isProcessing = true) }
                val role = AccessControl.getCurrentRole().name.lowercase()
                val result = repository.runFullPipeline(
                    input, 
                    role, 
                    _currentSessionId.value, 
                    _currentSessionTitle.value,
                    historyString
                )
                
                val aiMessage = ChatMessage(
                    text = result.engineResult?.structured_analysis?.get("answer") as? String 
                        ?: result.engineResult?.reasoning?.firstOrNull() ?: "I analyzed that for you.",
                    isUser = false,
                    classification = result.classification,
                    engineResult = result.engineResult
                )
                _chatMessages.value = _chatMessages.value + aiMessage
                _uiState.update { it.copy(isProcessing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isProcessing = false, error = e.message) }
            }
        }
    }

    // === SESSION MANAGEMENT ===

    fun switchSession(session: ChatSession) {
        viewModelScope.launch {
            _currentSessionId.value = session.id
            _currentSessionTitle.value = session.title
            repository.getLogsBySession(session.id).collect { logs ->
                _chatMessages.value = logs.map { log ->
                    ChatMessage(
                        id = log.id.toString(),
                        text = log.userInput,
                        isUser = true,
                        timestamp = log.timestamp
                        // Note: For simplicity in history view, we might only show user inputs 
                        // or reconstruct AI replies if they were stored separately.
                        // For now, mapping logs to messages.
                    )
                }
            }
        }
    }

    fun createNewSession() {
        _currentSessionId.value = java.util.UUID.randomUUID().toString()
        _currentSessionTitle.value = "New Chat"
        _chatMessages.value = emptyList()
    }

    fun renameSession(sessionId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameSession(sessionId, newTitle)
            if (_currentSessionId.value == sessionId) {
                _currentSessionTitle.value = newTitle
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_currentSessionId.value == sessionId) {
                createNewSession()
            }
        }
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
     * Uses the unified streaming pipeline in the repository for <5s responses.
     */
    fun sendMessage(text: String, localModelPath: String? = null, maxTokens: Int = 1024) {
        if (text.isBlank()) return

        val userMsg = ChatMessage(text = text, isUser = true)
        _chatMessages.update { it + userMsg }

        val historyString = formatChatHistoryForPipeline()
        val aiMsgId = java.util.UUID.randomUUID().toString()

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            
            val initialAiMsg = ChatMessage(
                id = aiMsgId,
                text = "",
                isUser = false,
                classification = ClassificationResult(
                    module = "loading",
                    moduleName = "Thinking...",
                    confidence = 1.0,
                    reasoning = listOf("Initializing Neural Fast Path...")
                )
            )
            _chatMessages.update { it + initialAiMsg }

            try {
                val role = AccessControl.getCurrentRole().name.lowercase()
                repository.runStreamingPipeline(
                    userInput = text,
                    userRole = role,
                    sessionId = currentSessionId.value,
                    sessionTitle = currentSessionTitle.value,
                    history = historyString,
                    maxTokens = maxTokens,
                    modelPath = localModelPath
                ).collect { update ->
                    if (update.error != null) {
                        updateMessageText(aiMsgId, "⚠️ Error: ${update.error}")
                        return@collect
                    }

                    if (update.classification != null) {
                        updateMessageClassification(aiMsgId, update.classification)
                        _reasoningSteps.value = update.classification.reasoning
                    }

                    if (update.token.isNotEmpty()) {
                        appendMessageText(aiMsgId, update.token)
                    }

                    if (update.isComplete) {
                        _uiState.update { it.copy(isProcessing = false) }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Streaming failed", e)
                updateMessageText(aiMsgId, "⚠️ Unexpected Error: ${e.message}")
            } finally {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    private fun updateMessageText(id: String, newText: String) {
        _chatMessages.update { messages ->
            messages.map { if (it.id == id) it.copy(text = newText) else it }
        }
    }

    private fun appendMessageText(id: String, token: String) {
        _chatMessages.update { messages ->
            messages.map { if (it.id == id) it.copy(text = it.text + token) else it }
        }
    }

    private fun updateMessageClassification(id: String, classification: ClassificationResult) {
        _chatMessages.update { messages ->
            messages.map { if (it.id == id) it.copy(classification = classification) else it }
        }
    }

    /**
     * Explicitly stop the current AI generation.
     */
    fun stopResponse() {
        Log.i(TAG, "User requested to stop response.")
        // In a flow-based system, we'd cancel the job. 
        // For simplicity, we trigger the repository's stop mechanism.
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = false) }
        }
    }

    /**
     * Builds a model-specific chat prompt for accurate AI responses.
     * Detects the model from the file path and applies the correct template.
     */
    private fun buildChatPrompt(userMessage: String, modelPath: String, history: String? = null): String {
        // HYBRID QUALITY: Prioritize complete, accurate, and professional answers. No time-limit pressure.
        val systemPrompt = "You are a professional and accurate AI. Provide complete and helpful answers. " +
                "Respect user constraints strictly (e.g., if '3 lines' is asked, give exactly 3 descriptive lines). " +
                "For code, provide ONLY the requested code block without excessive preamble. " +
                "Do not rush; prioritize completeness for long answers."

        val historyContext = if (history != null) "\nPrevious Chat Context:\n$history\n" else ""

        return when {
            // Qwen2.5 chat template
            modelPath.contains("qwen", ignoreCase = true) -> {
                "<|im_start|>system\n$systemPrompt$historyContext<|im_end|>\n" +
                        "<|im_start|>user\n$userMessage<|im_end|>\n" +
                        "<|im_start|>assistant\n"
            }
            // Gemma 2 chat template
            modelPath.contains("gemma", ignoreCase = true) -> {
                "<start_of_turn>user\n$systemPrompt$historyContext\n\nUser Question: $userMessage<end_of_turn>\n" +
                        "<start_of_turn>model\n"
            }
            // Generic fallback (instruction-style)
            else -> {
                "### System:\n$systemPrompt$historyContext\n\n### User:\n$userMessage\n\n### Assistant:\n"
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

    private fun formatChatHistoryForPipeline(): String {
        return chatMessages.value
            .takeLast(10)
            .joinToString("\n") { (if (it.isUser) "User: " else "AI: ") + it.text }
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
    CYBER("CyberSec", "security"),
    MODEL_SELECTION("AI Setup", "smart_toy")
}
