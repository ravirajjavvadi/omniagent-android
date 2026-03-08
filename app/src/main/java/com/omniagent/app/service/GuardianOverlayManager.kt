package com.omniagent.app.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import com.omniagent.app.R

object GuardianOverlayManager {

    private const val TAG = "SentinelOverlay"
    private var isShowing = false
    private var overlayView: View? = null
    private var windowManager: WindowManager? = null

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
}
