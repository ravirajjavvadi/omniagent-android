package com.omniagent.app.service

import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
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
    val isSuspicious: Boolean, // e.g. lots of background activity but low foreground usage
    val estimatedBatteryDrain: Float // calculated pseudo-metric for demonstration/analysis
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

            // Example Heuristic: If an app is barely used in foreground (< 1 minute), 
            // but is constantly receiving events / wakes, or is unknown, flag it.
            // (UsageStats has limited direct 'background time' without Q+ features, so we use heuristics)
            // For true background time, we usually track active alarms/jobs, but for a live scan, 
            // a pseudo-heuristic based on lastTimeUsed vs SystemClock can highlight 'phantom' activity.
            
            val isSuspicious = foregroundTime < 60_000 && (endTime - lastUsed) < 3600_000 // Last used recently but zero interactive time
            val drainScore = if (isSuspicious) 85f else (foregroundTime / 3600_000f) * 10f // Fake drain correlator for demo

            healthStatsList.add(
                AppHealthStats(
                    packageName = usage.packageName,
                    appName = appName,
                    foregroundTimeMs = foregroundTime,
                    lastTimeUsed = lastUsed,
                    isSuspicious = isSuspicious,
                    estimatedBatteryDrain = drainScore.coerceAtMost(100f)
                )
            )
        }

        // Return top suspicious or heavy-drain apps first
        return healthStatsList.sortedByDescending { it.isSuspicious }.take(20)
    }
}
