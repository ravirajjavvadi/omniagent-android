package com.omniagent.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class OmniGuardianService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    companion object {
        const val CHANNEL_ID = "omni_guardian_channel"
        const val NOTIFICATION_ID = 2007
        const val TAG = "OmniGuardianService"
        
        fun start(context: Context) {
            val intent = Intent(context, OmniGuardianService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stop(context: Context) {
            val intent = Intent(context, OmniGuardianService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Guardian Service Created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Guardian Service Started")
        
        // Connect to the foreground to prevent system from killing the service
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start monitoring sub-systems
        startDeviceMonitoring()

        // STICKY implies if the system kills the service, it should recreate it when possible
        return START_STICKY
    }

    private fun startDeviceMonitoring() {
        Log.d(TAG, "Initializing Live Scanning Engine...")
        // TODO: Implement UsageStats polling
        // TODO: Implement BatteryManager correlator
        // TODO: Broadcast updates to UI
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Not bound, runs independently
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Guardian Service Destroyed")
        serviceJob.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "OmniGuardian Security Shield"
            val descriptionText = "Keeps the real-time AI security shield active."
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("OmniAgent Guardian Active")
            .setContentText("Your device is protected by the AI Neural Shield.")
            .setSmallIcon(android.R.drawable.ic_secure) // Generic built-in secure icon for now
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true) // Prevents user from swiping it away natively
            .build()
    }
}
