package com.omniagent.app.kernel

import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Isolates all Chaquopy interactions from the rest of the application.
 */
class PythonKernelManager {

    private val python by lazy { Python.getInstance() }

    suspend fun classify(sanitizedInput: String): String = withContext(Dispatchers.IO) {
        try {
            val classifier = python.getModule("intent_classifier")
            classifier.callAttr("classify_input", sanitizedInput).toString()
        } catch (e: Exception) {
            "{\"status\": \"error\", \"message\": \"Classification failed: ${e.message}\"}"
        }
    }

    suspend fun runEngine(moduleName: String, methodName: String, input: String): String = withContext(Dispatchers.IO) {
        val module = python.getModule(moduleName)
        return@withContext module.callAttr(methodName, input).toString()
    }

    /**
     * Specialized call for background system scan.
     */
    suspend fun performSystemScan(): String = withContext(Dispatchers.IO) {
        val module = python.getModule("cyber_engine")
        return@withContext module.callAttr("perform_system_scan").toString()
    }

    /**
     * Specialized call for resume tailoring.
     */
    suspend fun tailorResume(resumeText: String, jobDescription: String): String = withContext(Dispatchers.IO) {
        val module = python.getModule("resume_engine")
        return@withContext module.callAttr("tailor_resume", resumeText, jobDescription).toString()
    }

    suspend fun getReasoningLog(): String = withContext(Dispatchers.IO) {
        val classifier = python.getModule("intent_classifier")
        classifier.callAttr("get_reasoning_log").toString()
    }

    suspend fun runEngine(moduleName: String, sanitizedInput: String, history: String? = null): String = withContext(Dispatchers.IO) {
        try {
            when (moduleName) {
                "coding" -> python.getModule("coding_engine").callAttr("analyze_code", sanitizedInput, history).toString()
                "cybersecurity" -> python.getModule("cyber_engine").callAttr("analyze_security", sanitizedInput, history).toString()
                "resume" -> python.getModule("resume_engine").callAttr("analyze_resume", sanitizedInput, history).toString()
                "startup" -> python.getModule("startup_engine").callAttr("analyze_startup", sanitizedInput, history).toString()
                "general" -> python.getModule("general_engine").callAttr("analyze_general", sanitizedInput, history).toString()
                else -> "{\"error\": \"Routing failed: Unknown module '$moduleName'\"}"
            }
        } catch (e: Exception) {
            "{\"error\": \"AI Engine Error\", \"details\": \"${e.message}\", \"module\": \"$moduleName\"}"
        }
    }
}
