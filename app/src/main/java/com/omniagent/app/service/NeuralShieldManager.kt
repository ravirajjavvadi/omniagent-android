package com.omniagent.app.service

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Helper object for managing OmniAccessibilityLinkScanner (Neural Vision) accessibility service.
 * Provides robust status checking with exact class name matching and smart navigation.
 */
object NeuralShieldManager {

    private const val TAG = "NeuralShieldManager"
    
    // The exact class name of our accessibility service
    const val SERVICE_CLASS_NAME = "com.omniagent.app.service.OmniAccessibilityLinkScanner"
    const val SERVICE_SHORT_NAME = "OmniAgent Neural Shield"

    /**
     * Checks if OmniAccessibilityLinkScanner is enabled using exact class name matching.
     * This is more robust than checking just the canonical name.
     */
    fun isNeuralShieldEnabled(context: Context): Boolean {
        val enabledServices = getEnabledAccessibilityServices(context)
        
        if (enabledServices.isNullOrEmpty()) {
            return false
        }

        val expectedComponentName = "${context.packageName}/$SERVICE_CLASS_NAME"
        
        // Check for exact class name match
        return enabledServices.any { serviceInfo ->
            serviceInfo == SERVICE_CLASS_NAME ||
            serviceInfo.endsWith(".$SERVICE_CLASS_NAME") ||
            serviceInfo == expectedComponentName
        }
    }

    /**
     * Gets the list of enabled accessibility services from system settings.
     */
    private fun getEnabledAccessibilityServices(context: Context): List<String>? {
        try {
            val enabledServicesSetting = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            
            if (enabledServicesSetting.isNullOrEmpty()) {
                return null
            }
            
            return enabledServicesSetting.split(":")
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting enabled accessibility services: ${e.message}")
            return null
        }
    }

    /**
     * Determines the best navigation intent based on Android version.
     * Android 12+ (S): Uses ACTION_ACCESSIBILITY_DETAILS_SETTINGS for direct navigation
     * to the specific service toggle.
     * Older versions: Opens general accessibility settings.
     */
    fun getNavigationIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ - Direct to service details
            val componentName = ComponentName(
                context.packageName,
                SERVICE_CLASS_NAME
            )
            Intent("android.settings.ACCESSIBILITY_DETAILS_SETTINGS").apply {
                setData(android.net.Uri.fromParts("package", componentName.flattenToString(), null))
            }
        } else {
            // Older Android versions - General accessibility settings
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }
    }

    /**
     * Determines if guided overlay instructions should be shown.
     * This is recommended for older Android versions where direct navigation isn't available.
     */
    fun shouldShowGuidedOverlay(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S
    }

    /**
     * Returns guided step instructions for older Android versions.
     */
    fun getGuidedSteps(): List<String> {
        return listOf(
            "1. Tap 'Downloaded Apps' or 'Your Services'",
            "2. Scroll down and find 'OmniAgent'",
            "3. Tap on 'OmniAgent Neural Shield'",
            "4. Toggle ON the Neural Shield switch",
            "5. Confirm by tapping 'OK' or 'Allow'"
        )
    }

    /**
     * Opens accessibility settings with optional fallback to general settings.
     */
    fun openAccessibilitySettings(context: Context) {
        try {
            val intent = getNavigationIntent(context)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Log.d(TAG, "Opened accessibility settings")
        } catch (e: Exception) {
            // Fallback to general accessibility settings
            Log.w(TAG, "Direct navigation failed, using fallback: ${e.message}")
            try {
                val fallbackIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(fallbackIntent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open accessibility settings: ${e2.message}")
            }
        }
    }

    /**
     * Gets the current service info including display name and status.
     */
    private var _lastScannedUrl = MutableStateFlow<String?>(null)
    val lastScannedUrl = _lastScannedUrl.asStateFlow()

    private var _lastScannedApp = MutableStateFlow<String?>(null)
    val lastScannedApp = _lastScannedApp.asStateFlow()

    fun reportScan(url: String, packageName: String) {
        _lastScannedUrl.value = url
        _lastScannedApp.value = packageName
    }

    fun getServiceInfo(context: Context): NeuralShieldStatus {
        val isEnabled = isNeuralShieldEnabled(context)
        val serviceInfo = try {
            val accessibilityService = Class.forName(SERVICE_CLASS_NAME)
                .asSubclass(AccessibilityService::class.java)
            val componentName = ComponentName(context, accessibilityService)
            componentName.flattenToShortString()
        } catch (e: Exception) {
            SERVICE_SHORT_NAME
        }

        return NeuralShieldStatus(
            isEnabled = isEnabled,
            serviceName = serviceInfo,
            displayName = SERVICE_SHORT_NAME,
            requiresGuidedOverlay = shouldShowGuidedOverlay()
        )
    }
}

data class NeuralShieldStatus(
    val isEnabled: Boolean,
    val serviceName: String,
    val displayName: String,
    val requiresGuidedOverlay: Boolean
)
