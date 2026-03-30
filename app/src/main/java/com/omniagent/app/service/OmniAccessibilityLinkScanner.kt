package com.omniagent.app.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.util.Patterns
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class OmniAccessibilityLinkScanner : AccessibilityService() {

    companion object {
        const val TAG = "OmniNeuralVision"
    }

    private lateinit var historyManager: ScanHistoryManager

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Neural Vision (Accessibility Service) Connected!")
        historyManager = ScanHistoryManager(this)
        
        historyManager.addEvent(ScanEvent(
            id = System.currentTimeMillis().toString(),
            type = EventType.APP_SCAN,
            title = "Neural Shield Active",
            description = "Security monitoring started.",
            timestamp = System.currentTimeMillis(),
            riskLevel = RiskLevel.LOW
        ))
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val packageName = event.packageName?.toString() ?: "UnknownApp"
            val rootNode = rootInActiveWindow ?: return
            scanNodesForUrls(rootNode, packageName)
        }
    }

    private fun scanNodesForUrls(node: AccessibilityNodeInfo, packageName: String) {
        if (node.text != null) {
            val text = node.text.toString()
            val matcher = Patterns.WEB_URL.matcher(text)
            
            while (matcher.find()) {
                val url = matcher.group()
                Log.w(TAG, "🚨 SUSPICIOUS LINK DETECTED in $packageName 🚨 -> $url")
                
                // Report live scan
                NeuralShieldManager.reportScan(url, packageName)
                
                // Add to history
                historyManager.addEvent(ScanEvent(
                    id = System.currentTimeMillis().toString(),
                    type = EventType.URL_SCAN,
                    title = "Link Scanned",
                    description = "Detected $url in $packageName",
                    timestamp = System.currentTimeMillis(),
                    packageName = packageName,
                    riskLevel = RiskLevel.MEDIUM
                ))
                
                GuardianOverlayManager.showThreatAlert(
                    context = this,
                    appName = packageName,
                    threatUrl = url,
                    threatLevel = "CRITICAL",
                    reason = "Unverified external link detected on screen."
                )
                
                break 
            }
        }
        
        // Traverse child nodes
        for (i in 0 until node.childCount) {
            val childNode = node.getChild(i)
            if (childNode != null) {
                scanNodesForUrls(childNode, packageName)
                childNode.recycle() // Important to prevent memory leaks in Accessibility Services
            }
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Neural Vision Interrupted! Protection Degraded.")
    }
}
