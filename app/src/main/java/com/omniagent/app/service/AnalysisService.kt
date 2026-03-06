package com.omniagent.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.omniagent.app.MainActivity
import com.omniagent.app.OmniAgentApplication
import kotlinx.coroutines.*

/**
 * Foreground Service for running long Python Kernel operations in the background.
 * Ensures the process persists even if the user minimizes the app.
 */
class AnalysisService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val userInput = intent?.getStringExtra(EXTRA_USER_INPUT)
        val userRole = intent?.getStringExtra(EXTRA_USER_ROLE) ?: "user"

        if (userInput == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, buildNotification("Initializing analysis..."))

        // Run the analysis pipeline in the background service scope
        serviceScope.launch {
            try {
                updateNotification("AI Kernel actively analyzing input...")
                
                // Get the application-level DI container
                val container = (application as OmniAgentApplication).container
                
                // Run full rigorous pipeline via the Repository
                container.analysisRepository.runFullPipeline(userInput, userRole)
                
                updateNotification("Analysis complete. Check the OmniAgent dashboard.")
                
            } catch (e: Exception) {
                updateNotification("Analysis failed: ${e.message}")
            } finally {
                delay(3000) // Keep the complete/failed notification visible briefly
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf(startId)
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    private fun buildNotification(contentText: String): android.app.Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OmniAgent Processing")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun updateNotification(text: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "OmniAgent Background Processing",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the AI Kernel running in the background"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "omniagent_analysis_channel"
        private const val NOTIFICATION_ID = 1001
        
        const val EXTRA_USER_INPUT = "extra_user_input"
        const val EXTRA_USER_ROLE = "extra_user_role"

        /**
         * Helper method to start the service from anywhere.
         */
        fun startService(context: Context, userInput: String, userRole: String) {
            val intent = Intent(context, AnalysisService::class.java).apply {
                putExtra(EXTRA_USER_INPUT, userInput)
                putExtra(EXTRA_USER_ROLE, userRole)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
