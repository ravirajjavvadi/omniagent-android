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

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Neural Vision (Accessibility Service) Connected!")
        // Configuration is handled via res/xml/accessibility_service_config.xml
        // We listen to text changes and window state changes to capture URLs as they appear.
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // We only care about events where text is displayed or a window opens
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            
            val rootNode = rootInActiveWindow ?: return
            scanNodesForUrls(rootNode, event.packageName?.toString() ?: "UnknownApp")
        }
    }

    private fun scanNodesForUrls(node: AccessibilityNodeInfo, packageName: String) {
        if (node.text != null) {
            val text = node.text.toString()
            val matcher = Patterns.WEB_URL.matcher(text)
            
            while (matcher.find()) {
                val url = matcher.group()
                Log.w(TAG, "🚨 SUSPICIOUS LINK DETECTED in $packageName 🚨 -> $url")
                
                // TODO: Send to AI Kernel (Llama + Python) for actual threat analysis.
                // For now, if we see a URL, we will trigger the Sentinel Overlay as a test.
                
                GuardianOverlayManager.showThreatAlert(
                    context = this,
                    appName = packageName,
                    threatUrl = url,
                    threatLevel = "CRITICAL",
                    reason = "Unverified external link detected on screen."
                )
                
                // Once we find a URL and show an alert in this specific chunk of text, 
                // we break to avoid spamming the overlay for the exact same message.
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
