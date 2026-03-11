package com.omniagent.app.service

import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.util.Calendar

/**
 * Permission Warden - Deep-layer app auditing system.
 * 
 * Detects:
 * 1. Ghost Apps - Apps with high-risk permissions but minimal foreground usage
 * 2. Sleeper Services - Apps running background services/listeners when not active
 * 3. High-Risk Permission combinations
 */
object PermissionWardenManager {

    private const val TAG = "PermissionWarden"

    // High-risk permissions that could indicate malware or spyware
    private val HIGH_RISK_PERMISSIONS = listOf(
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.READ_CALL_LOG,
        android.Manifest.permission.READ_CONTACTS,
        android.Manifest.permission.WRITE_CONTACTS,
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.READ_PHONE_STATE,
        android.Manifest.permission.BIND_ACCESSIBILITY_SERVICE,
        android.Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
        android.Manifest.permission.RECEIVE_BOOT_COMPLETED,
        android.Manifest.permission.PROCESS_OUTGOING_CALLS,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    // Threshold for "Ghost App" - minimal foreground time
    private const val GHOST_APP_FOREGROUND_THRESHOLD_MS = 60_000L // 1 minute
    private const val GHOST_APP_LAST_USED_THRESHOLD_MS = 24 * 60 * 60 * 1000L // 24 hours

    /**
     * Scans for Ghost Apps - applications that have high-risk permissions
     * but are barely used (potentially running silently in background).
     */
    fun scanGhostApps(context: Context): List<GhostAppInfo> {
        val ghostApps = mutableListOf<GhostAppInfo>()
        val pm = context.packageManager
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        // Get usage stats for last 24 hours
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val startTime = calendar.timeInMillis

        val stats: List<UsageStats>? = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        try {
            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            for (appInfo in packages) {
                // Skip system apps unless they have signature permissions
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (isSystemApp) continue

                // Get permissions for this app
                val permissions = getRequestedPermissions(pm, appInfo.packageName)
                val highRiskPerms = permissions.filter { it in HIGH_RISK_PERMISSIONS }

                if (highRiskPerms.isEmpty()) continue // Skip apps without high-risk permissions

                // Find usage stats
                val usage = stats?.find { it.packageName == appInfo.packageName }

                val foregroundTime = usage?.totalTimeInForeground ?: 0L
                val lastUsed = usage?.lastTimeUsed ?: 0L

                // Determine if it's a ghost app
                val isGhostApp = foregroundTime < GHOST_APP_FOREGROUND_THRESHOLD_MS &&
                        (endTime - lastUsed) < GHOST_APP_LAST_USED_THRESHOLD_MS &&
                        lastUsed > 0 // Has been used at least once

                if (isGhostApp) {
                    val appName = pm.getApplicationLabel(appInfo).toString()
                    ghostApps.add(
                        GhostAppInfo(
                            packageName = appInfo.packageName,
                            appName = appName,
                            highRiskPermissions = highRiskPerms,
                            foregroundTimeMs = foregroundTime,
                            lastTimeUsed = lastUsed,
                            riskScore = calculateGhostRiskScore(highRiskPerms, foregroundTime, lastUsed, endTime)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning ghost apps: ${e.message}")
        }

        return ghostApps.sortedByDescending { it.riskScore }
    }

    /**
     * Scans for Sleeper Services - apps that have running services or receivers
     * even when not actively used.
     */
    fun scanSleeperServices(context: Context): List<SleeperServiceInfo> {
        val sleeperApps = mutableListOf<SleeperServiceInfo>()
        val pm = context.packageManager
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        try {
            // Get running services
            val runningServices = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                activityManager.getRunningServices(Integer.MAX_VALUE)
            } else {
                @Suppress("DEPRECATION")
                activityManager.getRunningServices(Integer.MAX_VALUE)
            }

            // Get running processes
            val runningProcesses = activityManager.runningAppProcesses ?: emptyList()

            val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)

            for (appInfo in packages) {
                // Skip system apps
                val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (isSystemApp) continue

                // Check if app has running services
                val services = runningServices?.filter { it.service.packageName == appInfo.packageName }
                    ?: emptyList()

                // Check if app has running processes
                val processes = runningProcesses?.filter { 
                    it.pkgList?.contains(appInfo.packageName) == true 
                } ?: emptyList()

                if (services.isNotEmpty() || processes.isNotEmpty()) {
                    // Get permissions
                    val permissions = getRequestedPermissions(pm, appInfo.packageName)
                    val highRiskPerms = permissions.filter { it in HIGH_RISK_PERMISSIONS }

                    val appName = pm.getApplicationLabel(appInfo).toString()
                    sleeperApps.add(
                        SleeperServiceInfo(
                            packageName = appInfo.packageName,
                            appName = appName,
                            runningServices = services.map { it.service.className },
                            processImportance = processes.firstOrNull()?.importance ?: 0,
                            highRiskPermissions = highRiskPerms,
                            isBackgroundProcess = processes.any { 
                                it.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND 
                            }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning sleeper services: ${e.message}")
        }

        return sleeperApps.sortedByDescending { it.highRiskPermissions.size }
    }

    /**
     * Full DNA analysis - combines ghost apps and sleeper services for complete picture.
     */
    fun performAppDNAAnalysis(context: Context): AppDNAReport {
        val ghostApps = scanGhostApps(context)
        val sleeperServices = scanSleeperServices(context)

        // Find apps that are both ghost AND have sleeper services (highest threat)
        val criticalThreats = ghostApps.filter { ghost ->
            sleeperServices.any { it.packageName == ghost.packageName }
        }

        return AppDNAReport(
            ghostApps = ghostApps,
            sleeperServices = sleeperServices,
            criticalThreats = criticalThreats,
            scanTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Gets detailed permission analysis for a specific app.
     */
    fun analyzeAppPermissions(context: Context, packageName: String): AppPermissionAnalysis {
        val pm = context.packageManager

        return try {
            val appInfo = pm.getApplicationInfo(packageName, 0)
            val appName = pm.getApplicationLabel(appInfo).toString()
            val permissions = getRequestedPermissions(pm, packageName)

            val highRiskPerms = permissions.filter { it in HIGH_RISK_PERMISSIONS }
            val allPermDetails = permissions.map { perm ->
                val isGranted = pm.checkPermission(perm, packageName) == PackageManager.PERMISSION_GRANTED
                PermissionDetail(
                    name = perm,
                    isGranted = isGranted,
                    isHighRisk = perm in HIGH_RISK_PERMISSIONS
                )
            }

            AppPermissionAnalysis(
                packageName = packageName,
                appName = appName,
                permissions = allPermDetails,
                highRiskCount = highRiskPerms.size,
                grantRate = if (permissions.isNotEmpty()) 
                    allPermDetails.count { it.isGranted }.toFloat() / permissions.size 
                else 0f
            )
        } catch (e: PackageManager.NameNotFoundException) {
            AppPermissionAnalysis(
                packageName = packageName,
                appName = packageName,
                permissions = emptyList(),
                highRiskCount = 0,
                grantRate = 0f
            )
        }
    }

    private fun getRequestedPermissions(pm: PackageManager, packageName: String): List<String> {
        return try {
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            packageInfo.requestedPermissions?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun calculateGhostRiskScore(
        highRiskPerms: List<String>,
        foregroundTime: Long,
        lastUsed: Long,
        currentTime: Long
    ): Int {
        var score = 0

        // Base score from high-risk permissions
        score += highRiskPerms.size * 10

        // Additional score for very low usage
        if (foregroundTime < 10_000) score += 20 // Less than 10 seconds
        else if (foregroundTime < 60_000) score += 10 // Less than 1 minute

        // Score for recent activity (suspicious if active recently but low foreground time)
        val timeSinceLastUse = currentTime - lastUsed
        if (timeSinceLastUse < 6 * 60 * 60 * 1000 && foregroundTime < 60_000) {
            score += 15 // Active recently but barely used = suspicious
        }

        return score
    }
}

// Data Classes

data class GhostAppInfo(
    val packageName: String,
    val appName: String,
    val highRiskPermissions: List<String>,
    val foregroundTimeMs: Long,
    val lastTimeUsed: Long,
    val riskScore: Int
)

data class SleeperServiceInfo(
    val packageName: String,
    val appName: String,
    val runningServices: List<String>,
    val processImportance: Int,
    val highRiskPermissions: List<String>,
    val isBackgroundProcess: Boolean
)

data class AppDNAReport(
    val ghostApps: List<GhostAppInfo>,
    val sleeperServices: List<SleeperServiceInfo>,
    val criticalThreats: List<GhostAppInfo>,
    val scanTimestamp: Long
)

data class AppPermissionAnalysis(
    val packageName: String,
    val appName: String,
    val permissions: List<PermissionDetail>,
    val highRiskCount: Int,
    val grantRate: Float
)

data class PermissionDetail(
    val name: String,
    val isGranted: Boolean,
    val isHighRisk: Boolean
)
