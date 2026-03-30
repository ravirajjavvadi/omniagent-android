package com.omniagent.app.service

import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.util.Calendar

data class DeviceVitals(
    val batteryPercent: Int,
    val ramFreeBytes: Long,
    val ramTotalBytes: Long,
    val storageFreeBytes: Long,
    val storageTotalBytes: Long
)

data class AppHealthStats(
    val packageName: String,
    val appName: String,
    val foregroundTimeMs: Long,
    val lastTimeUsed: Long,
    val isSuspicious: Boolean,
    val batteryDrain: Int,
    val riskScore: Int = 0
)

class DeviceHealthManager(private val context: Context) {

    companion object {
        private const val TAG = "DeviceHealthManager"
    }

    /**
     * Gets total system vitals: Battery, RAM, and Storage.
     * Use this for the top-level "DEVICE VITALS" UI.
     */
    fun getVitals(): DeviceVitals {
        // Battery
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level != -1 && scale != -1) (level * 100 / scale) else 0

        // RAM
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        // Storage
        val path = Environment.getDataDirectory()
        val storageFree = path.usableSpace
        val storageTotal = path.totalSpace

        return DeviceVitals(
            batteryPercent = batteryPct,
            ramFreeBytes = memoryInfo.availMem,
            ramTotalBytes = memoryInfo.totalMem,
            storageFreeBytes = storageFree,
            storageTotalBytes = storageTotal
        )
    }

    /**
     * Scans apps for suspicious background activity and battery drain patterns.
     * Requires PACKAGE_USAGE_STATS permission to be granted by the user.
     */
    fun scanAppActivity(): List<AppHealthStats> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val pm = context.packageManager

        // Get usage stats for the last 24 hours
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis

        val stats: List<UsageStats>? = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        if (stats.isNullOrEmpty()) {
            Log.w(TAG, "No usage stats Available. Did the user grant PACKAGE_USAGE_STATS permission?")
            return emptyList()
        }

        val healthStatsList = mutableListOf<AppHealthStats>()

        for (usage in stats) {
            // Ignore system packages for this basic scan (unless deeply analyzing)
            if (usage.packageName.startsWith("com.android.") || usage.packageName.startsWith("android")) {
                continue
            }

            // Fallback for app name
            var appName = usage.packageName
            try {
                val appInfo = pm.getApplicationInfo(usage.packageName, 0)
                appName = pm.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                // Ignore
            }

            val foregroundTime = usage.totalTimeInForeground
            val lastUsed = usage.lastTimeUsed

            // Improved Heuristic: 
            // 1. Suspicious: Recent activity but minimal foreground time, or non-Play Store origin
            val isFromUntrustedSource = try {
                val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    pm.getInstallSourceInfo(usage.packageName).installingPackageName
                } else {
                    @Suppress("DEPRECATION")
                    pm.getInstallerPackageName(usage.packageName)
                }
                installer == null || (installer != "com.android.vending" && installer != "com.google.android.feedback")
            } catch (e: Exception) { false }

            val isSuspicious = (foregroundTime < 30_000 && (endTime - lastUsed) < 600_000) || isFromUntrustedSource
            
            // Refined Battery Drain: Using a coefficient-based model (mAh estimation)
            // typical drain: ~50mAh per hour of foreground
            // background drain: ~5mAh per hour
            val hoursForeground = foregroundTime / 3600_000f
            val hoursBackground = ((endTime - startTime) - foregroundTime) / 3600_000f
            val estimatedMAh = (hoursForeground * 50f) + (hoursBackground * 2f)
            
            // Scale drain to a percentage (0-100) for display, where 100 is "critical impact"
            val drainScore = (estimatedMAh / 10f) * if (isSuspicious) 2f else 1f

            healthStatsList.add(
                AppHealthStats(
                    packageName = usage.packageName,
                    appName = appName,
                    foregroundTimeMs = foregroundTime,
                    lastTimeUsed = lastUsed,
                    isSuspicious = isSuspicious,
                    batteryDrain = drainScore.toInt(),
                    riskScore = if (isSuspicious) 80 else 0
                )
            )
        }

        // Return top suspicious or heavy-drain apps first
        return healthStatsList.sortedWith(compareByDescending<AppHealthStats> { it.isSuspicious }.thenByDescending { it.batteryDrain }).take(20)
    }

    fun getTopResourceUsage(limit: Int = 10): List<AppHealthStats> {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, calendar.timeInMillis, System.currentTimeMillis())
        if (stats.isNullOrEmpty()) return emptyList()

        val pm = context.packageManager

        return stats.sortedByDescending { it.totalTimeInForeground }
            .take(limit)
            .map { usage ->
                val packageName = usage.packageName
                val label = try {
                    pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                } catch (e: Exception) {
                    packageName
                }
                
                // Estimate drain for display purposes (even if not "suspicious")
                val drain = (usage.totalTimeInForeground / 1000.0 / 3600.0) * 150 // Approx 150mA/h active
                
                AppHealthStats(
                    packageName = packageName,
                    appName = label,
                    foregroundTimeMs = usage.totalTimeInForeground,
                    lastTimeUsed = usage.lastTimeUsed,
                    isSuspicious = false, // Generic resource usage
                    batteryDrain = drain.toInt(),
                    riskScore = 0
                )
            }
    }
}
