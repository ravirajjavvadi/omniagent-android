package com.omniagent.app.engine

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

data class DownloadState(
    val modelId: String = "",
    val status: String = "NOT_STARTED", // NOT_STARTED, DOWNLOADING, SUCCESS, FAILED
    val progress: Int = 0,
    val totalBytes: Long = 0,
    val downloadedBytes: Long = 0,
    val localFilePath: String? = null
)

class ModelDownloadManager(private val context: Context) {
    
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val _downloadState = MutableStateFlow(DownloadState())
    val downloadState: StateFlow<DownloadState> = _downloadState
    
    private var downloadId: Long = -1

    // Predefined reliable mirrors (usually HuggingFace)
    val availableModels = listOf(
        ModelInfo(
            id = "qwen2.5_0_5b",
            name = "Model A (Lightweight): Qwen2.5 0.5B",
            url = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            sizeMb = 450
        ),
        ModelInfo(
            id = "qwen2.5_1_5b",
            name = "Model B (Balanced): Qwen2.5 1.5B",
            url = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            sizeMb = 1100
        ),
        ModelInfo(
            id = "gemma_2_2b",
            name = "Model C (Advanced Pro): Gemma-2-2B",
            url = "https://huggingface.co/bartowski/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it-Q4_K_M.gguf",
            sizeMb = 1800
        )
    )

    fun isModelFullyDownloaded(modelId: String): Boolean {
        val model = availableModels.find { it.id == modelId } ?: return false
        val file = File(context.getExternalFilesDir(null), "$modelId.gguf")
        
        // Expected size in bytes
        val expectedBytes = model.sizeMb * 1024L * 1024L
        
        // Relax check to 80% to account for compression or different unit reportings (MiB vs MB)
        val isFull = file.exists() && file.length() >= (expectedBytes * 0.8)
        Log.d("ModelDownload", "Checking $modelId: exists=${file.exists()}, size=${file.length()}, expected >= ${expectedBytes*0.8}, result=$isFull")
        return isFull
    }

    fun isAnyModelDownloaded(): Boolean {
        return availableModels.any { isModelFullyDownloaded(it.id) }
    }

    fun getDownloadedModelPath(): String? {
        availableModels.forEach { model ->
            if (isModelFullyDownloaded(model.id)) {
                val path = File(context.getExternalFilesDir(null), "${model.id}.gguf").absolutePath
                Log.i("ModelDownload", "Detected model: ${model.name} at $path")
                return path
            }
        }
        Log.w("ModelDownload", "No fully downloaded model detected.")
        return null
    }

    fun startDownload(modelId: String) {
        val model = availableModels.find { it.id == modelId } ?: return
        
        // Define where to save (EXTERNAL files dir to allow DownloadManager access)
        val targetFile = File(context.getExternalFilesDir(null), "${model.id}.gguf")
        
        // Cleanup if a failed/partial/small file exists to ensure fresh start
        if (targetFile.exists() && !isModelFullyDownloaded(model.id)) {
            targetFile.delete()
        }
        
        if (isModelFullyDownloaded(model.id)) {
            _downloadState.value = DownloadState(
                modelId = modelId,
                status = "SUCCESS",
                progress = 100,
                localFilePath = targetFile.absolutePath
            )
            return
        }

        val request = DownloadManager.Request(Uri.parse(model.url))
            .setTitle("Downloading OmniAgent AI Brain")
            .setDescription("Fetching ${model.name}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, null, "${model.id}.gguf")
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
            .setAllowedOverMetered(true) 
            .setAllowedOverRoaming(true)
            .setRequiresCharging(false)
            .setRequiresDeviceIdle(false)

        try {
            downloadId = downloadManager.enqueue(request)
            _downloadState.value = DownloadState(modelId = modelId, status = "DOWNLOADING")
            trackProgress(targetFile.absolutePath)
        } catch (e: Exception) {
            Log.e("ModelDownload", "Failed to enqueue download", e)
            _downloadState.value = DownloadState(modelId = modelId, status = "FAILED")
        }
    }

    private fun trackProgress(targetPath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            var downloading = true
            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    val status = cursor.getInt(statusIndex)
                    val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                    val bytesTotal = cursor.getLong(bytesTotalIndex)

                    val progress = if (bytesTotal > 0) ((bytesDownloaded * 100L) / bytesTotal).toInt() else 0

                    _downloadState.value = _downloadState.value.copy(
                        progress = progress,
                        downloadedBytes = bytesDownloaded,
                        totalBytes = bytesTotal
                    )

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            _downloadState.value = _downloadState.value.copy(
                                status = "SUCCESS",
                                localFilePath = targetPath
                            )
                            downloading = false
                        }
                        DownloadManager.STATUS_FAILED -> {
                            _downloadState.value = _downloadState.value.copy(status = "FAILED")
                            downloading = false
                            // Cleanup partial file
                            File(targetPath).delete()
                        }
                    }
                }
                cursor.close()
                delay(1000) // Poll every second
            }
        }
    }
}

data class ModelInfo(
    val id: String,
    val name: String,
    val url: String,
    val sizeMb: Int
)
