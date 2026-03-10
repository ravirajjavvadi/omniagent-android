package com.omniagent.app.engine

import android.util.Log

/**
 * LlamaEngine — Singleton wrapper for the native Llama.cpp library.
 * The native model is loaded ONCE and reused across queries, preventing
 * the SIGSEGV crash that occurred when creating a new instance per message.
 */
class LlamaEngine {

    companion object {
        private const val TAG = "OmniAgent-Llama"

        init {
            try {
                System.loadLibrary("omnilogic")
                Log.i(TAG, "Native Llama library loaded successfully.")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "CRITICAL: Failed to load native library 'omnilogic'. Ensure CMake build is correct.", e)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load Native Llama library.", e)
            }
        }
    }

    interface StreamingListener {
        fun onTokenGenerated(token: String)
        fun onStreamComplete()
        fun onStreamError(error: String)
    }

    private var streamingListener: StreamingListener? = null
    private var loadedModelPath: String? = null

    // -- JNI Declarations --
    external fun loadModelJNI(path: String): Boolean
    external fun generateResponseJNI(prompt: String): String
    external fun generateStreamingResponseJNI(prompt: String, maxTokens: Int): Boolean
    external fun freeModelJNI()
    external fun stopInferenceJNI()
    external fun forceReleaseJNI()

    /**
     * Loads the model from the given file path.
     * Skips loading if the same file is already loaded (singleton behaviour).
     */
    fun loadModel(filePath: String): Boolean {
        if (loadedModelPath == filePath) {
            Log.i(TAG, "Model already loaded at this path. Skipping reload.")
            return true
        }
        Log.i(TAG, "Loading model: $filePath")
        val success = loadModelJNI(filePath)
        if (success) {
            loadedModelPath = filePath
            Log.i(TAG, "Model loaded successfully.")
        } else {
            loadedModelPath = null
            Log.e(TAG, "Model load FAILED for: $filePath")
        }
        return success
    }

    fun isModelLoaded(): Boolean = loadedModelPath != null

    fun generate(prompt: String): String {
        if (!isModelLoaded()) return "Error: No model loaded."
        return generateResponseJNI(prompt)
    }

    fun generateStream(prompt: String, maxTokens: Int = 1024, listener: StreamingListener): Boolean {
        if (!isModelLoaded()) {
            listener.onStreamError("No model loaded.")
            return false
        }
        this.streamingListener = listener
        val result = generateStreamingResponseJNI(prompt, maxTokens)
        if (result) {
            listener.onStreamComplete()
        } else {
            listener.onStreamError("Streaming generation failed.")
        }
        this.streamingListener = null
        return result
    }

    /** Called by JNI for each generated token (on a background thread) */
    fun onNativeToken(token: String) {
        streamingListener?.onTokenGenerated(token)
    }

    fun release() {
        // Model stays loaded in native memory (singleton).
        // Only called when ViewModel is destroyed.
        streamingListener = null
    }

    fun stopInference() {
        stopInferenceJNI()
        Log.i(TAG, "LlamaEngine stop requested.")
    }

    fun forceRelease() {
        streamingListener = null
        loadedModelPath = null
        forceReleaseJNI()
        Log.i(TAG, "LlamaEngine force-released all resources.")
    }
}
