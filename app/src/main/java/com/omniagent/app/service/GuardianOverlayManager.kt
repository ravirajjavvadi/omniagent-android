package com.omniagent.app.service

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.FileObserver
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.omniagent.app.R
import kotlinx.coroutines.*
import java.io.File

object GuardianOverlayManager {

    private const val TAG = "SentinelOverlay"
    private var isShowing = false
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null
    private var guidedOverlayView: View? = null
    
    // Ransomware Shield - File Observers
    private var ransomwareObservers = mutableListOf<RansomwareFileObserver>()
    private var ransomwareShieldActive = false
    private val shieldScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Detection thresholds for ransomware behavior
    private const val RANSOMWARE_MODIFICATION_THRESHOLD = 10 // Files modified in
    private const val RANSOMWARE_TIME_WINDOW_MS = 5000L // 5 seconds window
    private val recentModifications = mutableMapOf<String, Long>()

    /**
     * Shows a persistent, floating "Sticky Alert" via the SYSTEM_ALERT_WINDOW permission.
     */
    fun showThreatAlert(context: Context, appName: String, threatUrl: String, threatLevel: String, reason: String) {
        if (!hasOverlayPermission(context)) {
            Log.e(TAG, "Cannot show Sentinel Alert. SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        if (isShowing) {
            Log.d(TAG, "Alert already showing, ignoring duplicate request.")
            return
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Inflate the custom beast-mode overlay view
        val inflater = LayoutInflater.from(context)
        overlayView = inflater.inflate(R.layout.guardian_threat_overlay, null)

        // Find views and set data
        val txtTitle = overlayView?.findViewById<TextView>(R.id.overlayThreatTitle)
        val txtApp = overlayView?.findViewById<TextView>(R.id.overlayAppOrigin)
        val txtUrl = overlayView?.findViewById<TextView>(R.id.overlayThreatUrl)
        val btnBlock = overlayView?.findViewById<Button>(R.id.btnOverlayBlock)
        val btnIgnore = overlayView?.findViewById<Button>(R.id.btnOverlayIgnore)

        txtTitle?.text = "🚨 $threatLevel THREAT DETECTED 🚨"
        txtApp?.text = "Origin: $appName"
        txtUrl?.text = threatUrl

        // Setup WindowManager Layout Parameters
        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        )
        
        // Stick to the top of the screen
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.y = 100 // push down slightly below status bar

        // Button Actions
        btnBlock?.setOnClickListener {
            // Log neutralization and dismiss
            Log.d(TAG, "Threat Neutralized by user.")
            removeOverlay()
        }

        btnIgnore?.setOnClickListener {
            // Allow the user to dismiss
            Log.w(TAG, "Threat Ignored by user.")
            removeOverlay()
        }

        // Add to window
        try {
            windowManager?.addView(overlayView, params)
            isShowing = true
            Log.d(TAG, "Sentinel Threat Overlay displayed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display overlay: ${e.message}")
        }
    }

    /**
     * Shows the guided overlay for Neural Shield setup on older Android devices.
     */
    fun showNeuralShieldGuide(context: Context, onOpenSettings: () -> Unit, onDismiss: () -> Unit) {
        if (!hasOverlayPermission(context)) {
            Log.e(TAG, "Cannot show guided overlay. SYSTEM_ALERT_WINDOW permission missing.")
            return
        }

        if (guidedOverlayView != null) {
            Log.d(TAG, "Guided overlay already showing.")
            return
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val inflater = LayoutInflater.from(context)
        guidedOverlayView = inflater.inflate(R.layout.neural_shield_guide_overlay, null)

        val btnOpenSettings = guidedOverlayView?.findViewById<Button>(R.id.btnOpenSettings)
        val btnDismiss = guidedOverlayView?.findViewById<Button>(R.id.btnDismiss)

        btnOpenSettings?.setOnClickListener {
            onOpenSettings()
            removeGuidedOverlay()
        }

        btnDismiss?.setOnClickListener {
            onDismiss()
            removeGuidedOverlay()
        }

        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.CENTER

        try {
            windowManager?.addView(guidedOverlayView, params)
            Log.d(TAG, "Neural Shield guided overlay displayed.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display guided overlay: ${e.message}")
        }
    }

    private fun removeGuidedOverlay() {
        if (guidedOverlayView != null) {
            windowManager?.removeView(guidedOverlayView)
            guidedOverlayView = null
        }
    }

    private fun removeOverlay() {
        if (isShowing && overlayView != null) {
            windowManager?.removeView(overlayView)
            overlayView = null
            isShowing = false
        }
    }

    private fun hasOverlayPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true
        }
    }
    
    // ==================== RANSOMWARE SHIELD ====================
    
    /**
     * Starts monitoring sensitive directories for ransomware-like behavior.
     * Monitors: DCIM, Downloads, Documents
     */
    fun startRansomwareShield(context: Context) {
        if (ransomwareShieldActive) {
            Log.d(TAG, "Ransomware Shield already active")
            return
        }
        
        ransomwareShieldActive = true
        
        // Monitor DCIM (Camera photos)
        val dcimPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).absolutePath
        monitorDirectory(context, dcimPath)
        
        // Monitor Downloads
        val downloadsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        monitorDirectory(context, downloadsPath)
        
        // Monitor Documents
        val documentsPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
        monitorDirectory(context, documentsPath)
        
        Log.i(TAG, "Ransomware Shield activated - monitoring sensitive directories")
    }
    
    /**
     * Stops the ransomware shield monitoring.
     */
    fun stopRansomwareShield() {
        ransomwareObservers.forEach { it.stopWatching() }
        ransomwareObservers.clear()
        ransomwareShieldActive = false
        recentModifications.clear()
        Log.i(TAG, "Ransomware Shield deactivated")
    }
    
    private fun monitorDirectory(context: Context, path: String) {
        try {
            val directory = File(path)
            if (directory.exists() && directory.isDirectory) {
                val observer = RansomwareFileObserver(context, path)
                observer.startWatching()
                ransomwareObservers.add(observer)
                Log.d(TAG, "Monitoring directory: $path")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to monitor directory $path: ${e.message}")
        }
    }
    
    /**
     * FileObserver subclass that detects mass file modifications.
     */
    private class RansomwareFileObserver(
        private val context: Context,
        private val path: String
    ) : FileObserver(path, FileObserver.CLOSE_WRITE or FileObserver.MOVED_TO or FileObserver.MOVED_FROM or FileObserver.DELETE or FileObserver.MOVE_SELF) {
        
        private val tag = "RansomwareObserver[$path]"
        
        override fun onEvent(event: Int, fileName: String?) {
            if (fileName == null) return
            
            val currentTime = System.currentTimeMillis()
            
            // Record modification
            recentModifications[fileName] = currentTime
            
            // Clean old entries (older than 30 seconds)
            val iterator = recentModifications.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if ((currentTime - entry.value) > 30000) {
                    iterator.remove()
                }
            }
            
            // Check for ransomware-like behavior
            checkForRansomwareBehavior(context)
        }
    }
    
    private fun checkForRansomwareBehavior(context: Context) {
        val currentTime = System.currentTimeMillis()
        
        // Count modifications in the time window
        val recentCount = recentModifications.count { (currentTime - it.value) <= RANSOMWARE_TIME_WINDOW_MS }
        
        if (recentCount >= RANSOMWARE_MODIFICATION_THRESHOLD) {
            Log.wtf(TAG, "🚨 RANSOMWARE BEHAVIOR DETECTED! $recentCount files modified in ${RANSOMWARE_TIME_WINDOW_MS}ms")
            
            // Trigger Emergency Shield Wall
            shieldScope.launch {
                withContext(Dispatchers.Main) {
                    showRansomwareAlert(context)
                }
            }
            
            // Clear recent modifications to avoid spam
            recentModifications.clear()
        }
    }
    
    private fun showRansomwareAlert(context: Context) {
        showThreatAlert(
            context = context,
            appName = "Ransomware Shield",
            threatUrl = "Mass file modification detected in sensitive directories",
            threatLevel = "CRITICAL",
            reason = "Possible ransomware encryption in progress!"
        )
    }
}
