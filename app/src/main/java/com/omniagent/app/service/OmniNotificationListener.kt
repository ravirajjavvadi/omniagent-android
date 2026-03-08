package com.omniagent.app.service

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.util.Patterns

class OmniNotificationListener : NotificationListenerService() {

    companion object {
        const val TAG = "SignalWatch"
        // List of packages we care about for Deep Link/Phishing tracking
        val MONITORED_APPS = listOf(
            "com.whatsapp",
            "com.facebook.orca", // Messenger
            "org.telegram.messenger",
            "com.google.android.apps.messaging", // SMS
            "com.viber.voip"
        )
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Signal Watch (Notification Listener) Connected!")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName
        
        // We only actively scan high-risk communication apps for links
        if (!MONITORED_APPS.contains(packageName)) {
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

        val fullMessage = "$title $text"
        val matcher = Patterns.WEB_URL.matcher(fullMessage)

        while (matcher.find()) {
            val url = matcher.group()
            Log.w(TAG, "🚨 SUSPICIOUS LINK in Notification ($packageName) 🚨 -> $url")
            
            // Display Sentinel Overlay for the incoming notification link
            GuardianOverlayManager.showThreatAlert(
                context = this,
                appName = "Messager/Notification ($packageName)",
                threatUrl = url,
                threatLevel = "HIGH",
                reason = "Background link detected in incoming message."
            )
            
            // We just trigger on the first found link per notification to prevent spam
            break 
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        // Can be used to clear analytics or threat databases when user dismisses
    }
}
