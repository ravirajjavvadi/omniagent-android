package com.omniagent.app.service

import android.app.ActivityManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import java.io.File
import java.io.RandomAccessFile

/**
 * Beast Mode Pulse - High-fidelity system health monitoring.
 * 
 * Monitors:
 * - CPU Temperature (thermal zones)
 * - RAM Pressure and usage
 * - Network integrity and speed
 * - Storage health
 * 
 * Provides color-coded threat levels:
 * - CYAN (Safe): Normal operation
 * - AMBER (Caution): Elevated metrics
 * - RED (Danger): Critical thresholds exceeded
 */
object BeastModePulseManager {

    private const val TAG = "BeastModePulse"

    // Temperature thresholds (in Celsius)
    const val TEMP_CAUTION = 40.0
    const val TEMP_DANGER = 50.0

    // RAM thresholds (percentage)
    const val RAM_CAUTION = 75.0
    const val RAM_DANGER = 90.0

    // Network thresholds
    const val NETWORK_OFFLINE = 0
    const val NETWORK_WEAK = 1
    const val NETWORK_STRONG = 2

    // Threat levels
    enum class ThreatLevel {
        SAFE,       // Cyan
        CAUTION,    // Amber  
        DANGER      // Red
    }

    /**
     * Gets complete system vitals with threat assessment.
     */
    fun getPulseData(context: Context): BeastModePulse {
        val cpuTemp = getCPUTemperature(context)
        val ramStatus = getRAMPressure(context)
        val networkStatus = getNetworkIntegrity(context)
        val storageStatus = getStorageHealth(context)

        // Calculate overall threat level
        val threatLevel = calculateOverallThreat(
            cpuTemp, ramStatus.percentUsed, networkStatus.status
        )

        return BeastModePulse(
            cpuTemperature = cpuTemp,
            cpuThreatLevel = getCPUTHreatLevel(cpuTemp),
            ramFreeBytes = ramStatus.freeBytes,
            ramTotalBytes = ramStatus.totalBytes,
            ramPercentUsed = ramStatus.percentUsed,
            ramThreatLevel = getRAMThreatLevel(ramStatus.percentUsed),
            networkStatus = networkStatus.status,
            networkType = networkStatus.type,
            networkThreatLevel = getNetworkThreatLevel(networkStatus.status),
            storageFreeBytes = storageStatus.freeBytes,
            storageTotalBytes = storageStatus.totalBytes,
            overallThreatLevel = threatLevel,
            timestamp = System.currentTimeMillis()
        )
    }

    /**
     * Gets CPU temperature from thermal zones or battery stats.
     * Android provides thermal zone readings in /sys/class/thermal/
     */
    private fun getCPUTemperature(context: Context): Double {
        // Try to read from thermal zones
        try {
            val thermalDir = File("/sys/class/thermal")
            if (thermalDir.exists() && thermalDir.isDirectory) {
                val thermalZones = thermalDir.listFiles { file -> 
                    file.name.startsWith("thermal_zone") 
                }
                
                thermalZones?.forEach { zone ->
                    val tempFile = File(zone, "temp")
                    if (tempFile.exists()) {
                        try {
                            val tempValue = tempFile.readText().trim().toDouble()
                            // Temperature is usually in millidegrees
                            val tempCelsius = if (tempValue > 1000) tempValue / 1000.0 else tempValue
                            if (tempCelsius > 0) {
                                Log.d(TAG, "CPU Temperature from $zone: $tempCelsius°C")
                                return tempCelsius
                            }
                        } catch (e: Exception) {
                            // Continue to next zone
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading CPU temp from thermal zones: ${e.message}")
        }

        // Fallback: Estimate from battery temperature if available
        try {
            val batteryIntent = context.registerReceiver(
                null,
                android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            )
            val temp = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, -1)
            if (temp != null && temp > 0) {
                return temp / 10.0 // Convert from tenths of degree Celsius
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading battery temp: ${e.message}")
        }

        // Default fallback - return room temperature
        return 25.0
    }

    /**
     * Gets RAM pressure status.
     */
    private fun getRAMPressure(context: Context): RAMStatus {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val totalBytes = memoryInfo.totalMem
        val freeBytes = memoryInfo.availMem
        val usedBytes = totalBytes - freeBytes
        val percentUsed = (usedBytes.toDouble() / totalBytes.toDouble()) * 100

        return RAMStatus(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedBytes = usedBytes,
            percentUsed = percentUsed,
            isLowMemory = memoryInfo.lowMemory
        )
    }

    /**
     * Gets network integrity status.
     */
    private fun getNetworkIntegrity(context: Context): NetworkStatus {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return NetworkStatus(
            status = NETWORK_OFFLINE,
            type = "None",
            isMetered = false
        )

        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkStatus(
            status = NETWORK_OFFLINE,
            type = "Unknown",
            isMetered = false
        )

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val type = when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> "Unknown"
        }

        val status = when {
            !hasInternet || !hasValidated -> NETWORK_OFFLINE
            capabilities.linkDownstreamBandwidthKbps < 1000 -> NETWORK_WEAK // Less than 1 Mbps
            else -> NETWORK_STRONG
        }

        return NetworkStatus(
            status = status,
            type = type,
            isMetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        )
    }

    /**
     * Gets storage health status.
     */
    private fun getStorageHealth(context: Context): StorageStatus {
        val dataDir = Environment.getDataDirectory()
        val stat = StatFs(dataDir.path)

        val totalBytes = stat.blockSizeLong * stat.blockCountLong
        val freeBytes = stat.blockSizeLong * stat.availableBlocksLong
        val usedBytes = totalBytes - freeBytes
        val percentUsed = (usedBytes.toDouble() / totalBytes.toDouble()) * 100

        return StorageStatus(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedBytes = usedBytes,
            percentUsed = percentUsed
        )
    }

    private fun getCPUTHreatLevel(temp: Double): ThreatLevel {
        return when {
            temp >= TEMP_DANGER -> ThreatLevel.DANGER
            temp >= TEMP_CAUTION -> ThreatLevel.CAUTION
            else -> ThreatLevel.SAFE
        }
    }

    private fun getRAMThreatLevel(percentUsed: Double): ThreatLevel {
        return when {
            percentUsed >= RAM_DANGER -> ThreatLevel.DANGER
            percentUsed >= RAM_CAUTION -> ThreatLevel.CAUTION
            else -> ThreatLevel.SAFE
        }
    }

    private fun getNetworkThreatLevel(status: Int): ThreatLevel {
        return when (status) {
            NETWORK_OFFLINE -> ThreatLevel.SAFE // Offline is safe for OMNI
            NETWORK_WEAK -> ThreatLevel.CAUTION
            else -> ThreatLevel.SAFE
        }
    }

    private fun calculateOverallThreat(
        cpuTemp: Double,
        ramPercent: Double,
        networkStatus: Int
    ): ThreatLevel {
        // Any single danger-level metric triggers danger state
        // NOTE: NETWORK_OFFLINE is excluded from DANGER since OMNI is offline-first.
        if (cpuTemp >= TEMP_DANGER || ramPercent >= RAM_DANGER) {
            return ThreatLevel.DANGER
        }

        // Multiple caution-level metrics trigger caution state
        var cautionCount = 0
        if (cpuTemp >= TEMP_CAUTION) cautionCount++
        if (ramPercent >= RAM_CAUTION) cautionCount++
        if (networkStatus == NETWORK_WEAK) cautionCount++

        return if (cautionCount >= 2) ThreatLevel.CAUTION else ThreatLevel.SAFE
    }
}

// Data Classes

data class BeastModePulse(
    val cpuTemperature: Double,
    val cpuThreatLevel: BeastModePulseManager.ThreatLevel,
    val ramFreeBytes: Long,
    val ramTotalBytes: Long,
    val ramPercentUsed: Double,
    val ramThreatLevel: BeastModePulseManager.ThreatLevel,
    val networkStatus: Int,
    val networkType: String,
    val networkThreatLevel: BeastModePulseManager.ThreatLevel,
    val storageFreeBytes: Long,
    val storageTotalBytes: Long,
    val overallThreatLevel: BeastModePulseManager.ThreatLevel,
    val timestamp: Long
)

data class RAMStatus(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val percentUsed: Double,
    val isLowMemory: Boolean
)

data class NetworkStatus(
    val status: Int,
    val type: String,
    val isMetered: Boolean
)

data class StorageStatus(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val percentUsed: Double
)
