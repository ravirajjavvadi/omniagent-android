package com.omniagent.app

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.omniagent.app.di.AppContainer

/**
 * Base Application class for the OmniAgent app.
 * Initializes the Dependency Injection container and Python environment.
 */
class OmniAgentApplication : Application() {
    
    // Application-level dependency container
    lateinit var container: AppContainer
        private set
    override fun onCreate() {
        super.onCreate()
        
        // Initialize manual DI container
        container = AppContainer(this)

        // Initialize Python for Android (Fixes "GenericPlatform" error)
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        // Schedule Periodic Cyber Scan (Safety First)
        scheduleCyberScan()
        
        // Start Live Security Guardian Service
        com.omniagent.app.service.OmniGuardianService.start(this)
    }

    private fun scheduleCyberScan() {
        val constraints = androidx.work.Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val scanRequest = androidx.work.PeriodicWorkRequestBuilder<com.omniagent.app.worker.CyberScanWorker>(
            4, java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "cyber_scan_periodic",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            scanRequest
        )
    }
}
