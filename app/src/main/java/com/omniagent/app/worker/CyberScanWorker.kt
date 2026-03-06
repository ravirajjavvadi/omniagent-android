package com.omniagent.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.omniagent.app.OmniAgentApplication
import com.omniagent.app.core.notification.NotificationHelper

/**
 * Background worker that triggers the Python Cyber Security Engine.
 * Performs a heuristic system scan and notifies the user of risks.
 */
class CyberScanWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("CyberScanWorker", "Background scan initiated...")
        
        val container = (applicationContext as OmniAgentApplication).container
        val repository = container.analysisRepository

        return try {
            val result = repository.performSystemScan()
            
            Log.d("CyberScanWorker", "Scan complete. Risk Score: ${result.risk_score}")

            if (result.risk_score > 0) {
                NotificationHelper.showSecurityAlert(
                    applicationContext,
                    "Security Alert",
                    "Risk Level: ${result.structured_analysis["risk_level"] ?: "UNKNOWN"}. Tap to view details."
                )
            } else {
                Log.d("CyberScanWorker", "No immediate risks detected.")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("CyberScanWorker", "Background scan failed", e)
            Result.retry()
        }
    }
}
