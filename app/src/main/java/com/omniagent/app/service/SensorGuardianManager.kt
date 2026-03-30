package com.omniagent.app.service

import android.app.AppOpsManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import java.util.Calendar

/**
 * Sensor Guardian - Monitors Camera, Microphone, and GPS sensor access by apps.
 * Provides a heatmap visualization of which apps accessed sensors in the last 24 hours.
 */
object SensorGuardianManager {

    private const val TAG = "SensorGuardian"

    // Sensor types
    const val SENSOR_CAMERA = 0
    const val SENSOR_MICROPHONE = 1
    const val SENSOR_LOCATION = 2

    // High-risk threshold: apps accessing sensitive sensors while in background
    private const val BACKGROUND_ACCESS_THRESHOLD_MS = 60_000L // 1 minute in background

    /**
     * Gets sensor access information for all apps in the last 24 hours.
     * Returns a map of sensor type to list of apps that accessed it.
     */
    fun getSensorAccessHeatmap(context: Context): Map<Int, List<SensorAccessInfo>> {
        val result = mutableMapOf<Int, MutableList<SensorAccessInfo>>()
        result[SENSOR_CAMERA] = mutableListOf()
        result[SENSOR_MICROPHONE] = mutableListOf()
        result[SENSOR_LOCATION] = mutableListOf()

        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        // Get usage stats for last 24 hours
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis

        val stats: List<UsageStats>? = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        if (stats.isNullOrEmpty()) {
            Log.w(TAG, "No usage stats available. Permission may be missing.")
            return result
        }

        for (usage in stats) {
            // Skip system packages
            if (usage.packageName.startsWith("com.android.") || 
                usage.packageName.startsWith("android")) {
                continue
            }

            // Get app name
            val appName = getAppName(pm, usage.packageName)

            // Check camera access
            val cameraAccess = checkSensorAccess(context, appOps, usage.packageName, AppOpsManager.OPSTR_CAMERA)
            if (cameraAccess.isNotEmpty) {
                val isBackgroundRisk = usage.totalTimeInForeground < BACKGROUND_ACCESS_THRESHOLD_MS &&
                        (endTime - usage.lastTimeUsed) < BACKGROUND_ACCESS_THRESHOLD_MS
                result[SENSOR_CAMERA]?.add(
                    SensorAccessInfo(
                        packageName = usage.packageName,
                        appName = appName,
                        accessTimeMs = cameraAccess.lastAccessTime,
                        isBackgroundAccess = isBackgroundRisk,
                        riskLevel = calculateRiskLevel(isBackgroundRisk, cameraAccess.accessCount)
                    )
                )
            }

            // Check microphone access
            val micAccess = checkSensorAccess(context, appOps, usage.packageName, AppOpsManager.OPSTR_RECORD_AUDIO)
            if (micAccess.isNotEmpty) {
                val isBackgroundRisk = usage.totalTimeInForeground < BACKGROUND_ACCESS_THRESHOLD_MS &&
                        (endTime - usage.lastTimeUsed) < BACKGROUND_ACCESS_THRESHOLD_MS
                result[SENSOR_MICROPHONE]?.add(
                    SensorAccessInfo(
                        packageName = usage.packageName,
                        appName = appName,
                        accessTimeMs = micAccess.lastAccessTime,
                        isBackgroundAccess = isBackgroundRisk,
                        riskLevel = calculateRiskLevel(isBackgroundRisk, micAccess.accessCount)
                    )
                )
            }

            // Check location (GPS) access
            val locationAccess = checkSensorAccess(context, appOps, usage.packageName, AppOpsManager.OPSTR_FINE_LOCATION)
            if (locationAccess.isNotEmpty) {
                val isBackgroundRisk = usage.totalTimeInForeground < BACKGROUND_ACCESS_THRESHOLD_MS &&
                        (endTime - usage.lastTimeUsed) < BACKGROUND_ACCESS_THRESHOLD_MS
                result[SENSOR_LOCATION]?.add(
                    SensorAccessInfo(
                        packageName = usage.packageName,
                        appName = appName,
                        accessTimeMs = locationAccess.lastAccessTime,
                        isBackgroundAccess = isBackgroundRisk,
                        riskLevel = calculateRiskLevel(isBackgroundRisk, locationAccess.accessCount)
                    )
                )
            }
        }

        // Sort by risk level (highest first) and return
        result[SENSOR_CAMERA] = result[SENSOR_CAMERA]?.sortedByDescending { it.riskLevel }?.toMutableList() ?: mutableListOf()
        result[SENSOR_MICROPHONE] = result[SENSOR_MICROPHONE]?.sortedByDescending { it.riskLevel }?.toMutableList() ?: mutableListOf()
        result[SENSOR_LOCATION] = result[SENSOR_LOCATION]?.sortedByDescending { it.riskLevel }?.toMutableList() ?: mutableListOf()

        return result
    }

    /**
     * Checks if an app has accessed a specific sensor.
     */
    private fun checkSensorAccess(
        context: Context,
        appOps: AppOpsManager,
        packageName: String,
        opStr: String
    ): SensorAccessResult {
        try {
            val uid = try {
                context.packageManager.getApplicationInfo(packageName, 0).uid
            } catch (e: PackageManager.NameNotFoundException) {
                return SensorAccessResult(false, 0, 0)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // On Android 10+, we use unsafeCheckOpRaw for current state
                val mode = appOps.unsafeCheckOpRaw(opStr, uid, packageName)
                
                // For historical data, standard SDK access is limited. 
                // We'll use the last used time from UsageStats as a proxy if AppOps history is inaccessible.
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val calendar = Calendar.getInstance()
                val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis - 86400000, calendar.timeInMillis)
                val appUsage = stats.find { it.packageName == packageName }
                
                return if (mode == AppOpsManager.MODE_ALLOWED || (appUsage?.lastTimeUsed ?: 0) > 0) {
                    SensorAccessResult(true, appUsage?.lastTimeUsed ?: System.currentTimeMillis(), 1)
                } else {
                    SensorAccessResult(false, 0, 0)
                }
            } else {
                // Legacy approach for older Android versions
                @Suppress("DEPRECATION")
                val mode = appOps.checkOp(opStr, uid, packageName)
                return if (mode == AppOpsManager.MODE_ALLOWED) {
                    SensorAccessResult(true, System.currentTimeMillis(), 1)
                } else {
                    SensorAccessResult(false, 0, 0)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking sensor access for $packageName: ${e.message}")
            return SensorAccessResult(false, 0, 0)
        }
    }

    /**
     * Simplified check for sensor permission status.
     */
    fun hasSensorPermission(context: Context, packageName: String, sensorType: Int): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val opStr = when (sensorType) {
            SENSOR_CAMERA -> AppOpsManager.OPSTR_CAMERA
            SENSOR_MICROPHONE -> AppOpsManager.OPSTR_RECORD_AUDIO
            SENSOR_LOCATION -> AppOpsManager.OPSTR_FINE_LOCATION
            else -> return false
        }

        return try {
            val uid = context.packageManager.getApplicationInfo(packageName, 0).uid
            @Suppress("DEPRECATION")
            val mode = appOps.checkOp(opStr, uid, packageName)
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets all apps that have any sensitive sensor permissions (camera, mic, location).
     */
    fun getAppsWithSensorPermissions(context: Context): List<SensorAppInfo> {
        val pm = context.packageManager
        val result = mutableListOf<SensorAppInfo>()

        try {
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            for (appInfo in packages) {
                // Skip system apps
                if (appInfo.packageName.startsWith("com.android.") ||
                    appInfo.packageName.startsWith("android")) {
                    continue
                }

                val hasCamera = pm.checkPermission(android.Manifest.permission.CAMERA, appInfo.packageName) == 
                    PackageManager.PERMISSION_GRANTED
                val hasMic = pm.checkPermission(android.Manifest.permission.RECORD_AUDIO, appInfo.packageName) == 
                    PackageManager.PERMISSION_GRANTED
                val hasLocation = pm.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, appInfo.packageName) == 
                    PackageManager.PERMISSION_GRANTED

                if (hasCamera || hasMic || hasLocation) {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    result.add(
                        SensorAppInfo(
                            packageName = appInfo.packageName,
                            appName = appName,
                            hasCameraPermission = hasCamera,
                            hasMicPermission = hasMic,
                            hasLocationPermission = hasLocation
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting apps with sensor permissions: ${e.message}")
        }

        return result.sortedByDescending { it.riskScore }
    }

    private fun getAppName(pm: PackageManager, packageName: String): String {
            return try {
                val appInfo = pm.getApplicationInfo(packageName, 0)
                pm.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                packageName
            }
        }

        private fun calculateRiskLevel(isBackgroundAccess: Boolean, accessCount: Int): Int {
            return when {
                isBackgroundAccess && accessCount > 5 -> 3 // HIGH RISK
                isBackgroundAccess || accessCount > 3 -> 2  // MEDIUM RISK
                else -> 1 // LOW RISK
            }
        }
    }

data class SensorAccessInfo(
    val packageName: String,
    val appName: String,
    val accessTimeMs: Long,
    val isBackgroundAccess: Boolean,
    val riskLevel: Int // 1=Low, 2=Medium, 3=High
)

data class SensorAccessResult(
    val hasAccess: Boolean,
    val lastAccessTime: Long,
    val accessCount: Int
) {
    val isNotEmpty: Boolean get() = hasAccess && accessCount > 0
}

data class SensorAppInfo(
    val packageName: String,
    val appName: String,
    val hasCameraPermission: Boolean,
    val hasMicPermission: Boolean,
    val hasLocationPermission: Boolean
) {
    val riskScore: Int
        get() = (if (hasCameraPermission) 1 else 0) + 
                (if (hasMicPermission) 1 else 0) + 
                (if (hasLocationPermission) 1 else 0)
}
