package com.omniagent.app.ui.features.cyber

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniagent.app.service.AppHealthStats
import com.omniagent.app.service.DeviceVitals
import android.provider.Settings
import android.text.TextUtils
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import com.omniagent.app.ui.theme.OmniColors
import com.omniagent.app.viewmodel.OmniAgentViewModel

@Composable
fun CyberSecScreen(
    viewModel: OmniAgentViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val deviceVitals by viewModel.deviceVitals.collectAsState()
    val suspiciousApps by viewModel.suspiciousApps.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshCyberSecVitals()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.Background)
    ) {
        // Decorative background glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(OmniColors.ModuleCyber.copy(alpha = 0.15f), Color.Transparent),
                    center = center.copy(x = size.width * 0.2f, y = size.height * 0.1f)
                ),
                radius = 600f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(OmniColors.Accent.copy(alpha = 0.1f), Color.Transparent),
                    center = center.copy(x = size.width * 0.8f, y = size.height * 0.8f)
                ),
                radius = 500f
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                CyberSecHeader()
            }

            item {
                StatusOrb()
            }

            item {
                SectionHeader(title = "GUARDIAN CONTROLS", icon = Icons.Default.Shield)
            }

            item {
                PermissionGrid()
            }

            item {
                SectionHeader(title = "DEVICE VITALS", icon = Icons.Default.Bolt)
            }

            item {
                DeviceVitalsCards(vitals = deviceVitals)
            }
            
            item {
                SectionHeader(title = "THREAT MONITOR (LIVE)", icon = Icons.Default.Radar)
            }
            
            item {
                ThreatMonitor(apps = suspiciousApps)
            }
        }
    }
}

@Composable
private fun CyberSecHeader() {
    Column(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 32.dp)
            .fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = OmniColors.ModuleCyber,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "CyberSec Pillar",
                style = MaterialTheme.typography.headlineMedium,
                color = OmniColors.TextPrimary,
                fontWeight = FontWeight.Black
            )
        }
        Text(
            text = "Guardian Mode: Active Shield 🛡️",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp, start = 44.dp)
        )
    }
}

@Composable
private fun StatusOrb() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing ring
        Box(
            modifier = Modifier
                .size(200.dp)
                .border(2.dp, OmniColors.ModuleCyber.copy(alpha = 0.3f), CircleShape)
        )
        
        // Inner Glow
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            OmniColors.ModuleCyber.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SECURE",
                style = MaterialTheme.typography.headlineMedium,
                color = OmniColors.ModuleCyber,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Text(
                text = "No Threats Detected",
                style = MaterialTheme.typography.labelMedium,
                color = OmniColors.TextTertiary
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = OmniColors.TextTertiary, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = OmniColors.TextTertiary,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun PermissionGrid() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    // Real-time state that recomposes when the app resumes
    var hasOverlay by remember { mutableStateOf(false) }
    var hasAccessibility by remember { mutableStateOf(false) }
    var hasNotificationAccess by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasOverlay = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(context)
                } else true
                hasAccessibility = isAccessibilityServiceEnabled(context, com.omniagent.app.service.OmniAccessibilityLinkScanner::class.java)
                hasNotificationAccess = isNotificationServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            PermissionCard(
                title = "Neural Vision",
                icon = "👁️",
                desc = "Live Link Scan",
                enabled = hasAccessibility,
                onToggle = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    context.startActivity(intent)
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            PermissionCard(
                title = "Sentinel",
                icon = "🛡️",
                desc = "Active Overlay",
                enabled = hasOverlay,
                onToggle = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            PermissionCard(
                title = "Signal Watch",
                icon = "🛰️",
                desc = "In-App Alerts",
                enabled = hasNotificationAccess,
                onToggle = {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        context.startActivity(intent)
                    }
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            PermissionCard(
                title = "Deep Scan",
                icon = "📁",
                desc = "File Guardian",
                enabled = false, // Placeholder for future File storage scan
                onToggle = {
                     val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                     context.startActivity(intent)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    icon: String,
    desc: String,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(OmniColors.Surface)
            .border(
                1.dp,
                if (enabled) OmniColors.ModuleCyber.copy(alpha = 0.5f) else OmniColors.Border,
                RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 24.sp)
                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = OmniColors.ModuleCyber,
                        checkedTrackColor = OmniColors.ModuleCyber.copy(alpha = 0.2f)
                    )
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = OmniColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.labelSmall,
                color = OmniColors.TextTertiary
            )
        }
    }
}

@Composable
private fun DeviceVitalsCards(vitals: DeviceVitals?) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val batPct = vitals?.batteryPercent?.toString()?.plus("%") ?: "--%"
        val ramGB = vitals?.ramFreeBytes?.let { "%.1f GB".format(it / (1024f * 1024f * 1024f)) } ?: "-- GB"
        
        VitalCard("CPU Load", "Live", Icons.Default.Memory, OmniColors.Primary)
        VitalCard("RAM Free", ramGB, Icons.Default.Storage, OmniColors.Accent)
        VitalCard("Battery", batPct, Icons.Default.BatteryChargingFull, OmniColors.Secondary)
    }
}

@Composable
private fun VitalCard(label: String, value: String, icon: ImageVector, color: Color) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(OmniColors.SurfaceElevated)
            .padding(16.dp)
    ) {
        Column {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = value, style = MaterialTheme.typography.headlineSmall, color = OmniColors.TextPrimary, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = OmniColors.TextTertiary)
        }
    }
}

@Composable
private fun ThreatMonitor(apps: List<AppHealthStats>) {
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(OmniColors.Surface)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (apps.isEmpty()) {
                Text(
                    text = "No suspicious apps detected right now. (Note: UsageStats permission might be needed).",
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniColors.TextTertiary
                )
            } else {
                apps.forEach { app ->
                    val color = if (app.isSuspicious) OmniColors.Danger else OmniColors.Warning
                    val icon = if (app.isSuspicious) Icons.Default.Warning else Icons.Default.Info
                    val text = "${app.appName} (Drain: ${app.estimatedBatteryDrain.toInt()}%)"
                    val timeSub = "Active Foreground: ${app.foregroundTimeMs / 1000}s"
                    LogItem(text, timeSub, icon, color)
                }
            }
        }
    }
}

@Composable
private fun LogItem(text: String, time: String, icon: ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = OmniColors.TextSecondary)
            Text(text = time, style = MaterialTheme.typography.labelSmall, color = OmniColors.TextTertiary, fontSize = 9.sp)
        }
    }
}

// ==== PERMISSION UTILS ====

private fun isAccessibilityServiceEnabled(context: Context, accessibilityService: Class<*>): Boolean {
    var accessibilityEnabled = 0
    val service = context.packageName + "/" + accessibilityService.canonicalName
    try {
        accessibilityEnabled = Settings.Secure.getInt(
            context.applicationContext.contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED
        )
    } catch (e: Settings.SettingNotFoundException) {
        // Assume false
    }
    val stringColonSplitter = TextUtils.SimpleStringSplitter(':')
    if (accessibilityEnabled == 1) {
        val settingValue = Settings.Secure.getString(
            context.applicationContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (settingValue != null) {
            stringColonSplitter.setString(settingValue)
            while (stringColonSplitter.hasNext()) {
                val accessibilityServiceStr = stringColonSplitter.next()
                if (accessibilityServiceStr.equals(service, ignoreCase = true)) {
                    return true
                }
            }
        }
    }
    return false
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    if (!TextUtils.isEmpty(flat)) {
        val names = flat.split(":")
        for (i in names.indices) {
            val cn = android.content.ComponentName.unflattenFromString(names[i])
            if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                return true
            }
        }
    }
    return false
}
