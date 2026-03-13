package com.omniagent.app.ui.features.cyber

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omniagent.app.service.*
import com.omniagent.app.ui.theme.OmniColors
import com.omniagent.app.viewmodel.OmniAgentViewModel

@Composable
fun CyberSecScreen(
    viewModel: OmniAgentViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val deviceVitals by viewModel.deviceVitals.collectAsState()
    val suspiciousApps by viewModel.suspiciousApps.collectAsState()

    val context = LocalContext.current

    // Beast Mode Pulse state
    var beastModePulse by remember { mutableStateOf<BeastModePulse?>(null) }
    
    // Sensor Guardian state
    var sensorHeatmap by remember { mutableStateOf<Map<Int, List<SensorAccessInfo>>>(emptyMap()) }
    
    // Permission Warden state
    var appDNAReport by remember { mutableStateOf<AppDNAReport?>(null) }

    // Ransomware Shield state
    var ransomwareShieldActive by remember { mutableStateOf(false) }

    // Show guided overlay state
    var showGuidedOverlay by remember { mutableStateOf(false) }

    // Refresh data on compose
    LaunchedEffect(Unit) {
        viewModel.refreshCyberSecVitals()
        
        // Get Beast Mode Pulse
        beastModePulse = BeastModePulseManager.getPulseData(context)
        
        // Get Sensor Heatmap
        sensorHeatmap = SensorGuardianManager.getSensorAccessHeatmap(context)
        
        // Get App DNA Analysis
        appDNAReport = PermissionWardenManager.performAppDNAAnalysis(context)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OmniColors.Background)
    ) {
        // Decorative background glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            val threatColor = when (beastModePulse?.overallThreatLevel) {
                BeastModePulseManager.ThreatLevel.DANGER -> OmniColors.Danger
                BeastModePulseManager.ThreatLevel.CAUTION -> OmniColors.Warning
                else -> OmniColors.ModuleCyber
            }
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(threatColor.copy(alpha = 0.15f), Color.Transparent),
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
                // Beast Mode Pulse Orb
                BeastModePulseOrb(pulse = beastModePulse)
            }

            item {
                SectionHeader(title = "GUARDIAN CONTROLS", icon = Icons.Default.Shield)
            }

            item {
                GuardianControlsGrid(
                    onNeuralShieldToggle = {
                        val status = NeuralShieldManager.getServiceInfo(context)
                        if (status.isEnabled) {
                            // Already enabled - just open settings
                            NeuralShieldManager.openAccessibilitySettings(context)
                        } else if (status.requiresGuidedOverlay) {
                            // Show guided overlay for older Android
                            showGuidedOverlay = true
                        } else {
                            // Direct navigation for Android 12+
                            NeuralShieldManager.openAccessibilitySettings(context)
                        }
                    },
                    onRansomwareShieldToggle = {
                        ransomwareShieldActive = !ransomwareShieldActive
                        if (ransomwareShieldActive) {
                            GuardianOverlayManager.startRansomwareShield(context)
                        } else {
                            GuardianOverlayManager.stopRansomwareShield()
                        }
                    },
                    ransomwareShieldActive = ransomwareShieldActive
                )
            }

            item {
                SectionHeader(title = "BEAST MODE PULSE", icon = Icons.Default.Bolt)
            }

            item {
                BeastModeVitalsCards(pulse = beastModePulse)
            }

            item {
                SectionHeader(title = "SENSOR GUARDIAN (24H)", icon = Icons.Default.Sensors)
            }

            item {
                SensorGuardianHeatmap(heatmap = sensorHeatmap)
            }

            item {
                SectionHeader(title = "PERMISSION WARDEN", icon = Icons.Default.AdminPanelSettings)
            }

            item {
                PermissionWardenResults(report = appDNAReport)
            }

            item {
                SectionHeader(title = "THREAT MONITOR (LIVE)", icon = Icons.Default.Radar)
            }

            item {
                ThreatMonitor(apps = suspiciousApps)
            }
        }

        // Guided Overlay Dialog
        if (showGuidedOverlay) {
            AlertDialog(
                onDismissRequest = { showGuidedOverlay = false },
                title = { Text("🛡️ Enable Neural Shield", color = OmniColors.Primary) },
                text = {
                    Column {
                        Text("Follow these steps on older Android versions:", 
                            color = OmniColors.TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        NeuralShieldManager.getGuidedSteps().forEach { step ->
                            Text(step, color = OmniColors.TextPrimary, 
                                modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showGuidedOverlay = false
                        NeuralShieldManager.openAccessibilitySettings(context)
                    }) {
                        Text("Open Settings", color = OmniColors.Primary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showGuidedOverlay = false }) {
                        Text("Cancel", color = OmniColors.TextTertiary)
                    }
                },
                containerColor = OmniColors.SurfaceElevated
            )
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
            text = "Beast Mode: Active Shield 🛡️",
            style = MaterialTheme.typography.bodyMedium,
            color = OmniColors.TextSecondary,
            modifier = Modifier.padding(top = 4.dp, start = 44.dp)
        )
    }
}

@Composable
private fun BeastModePulseOrb(pulse: BeastModePulse?) {
    val threatLevel = pulse?.overallThreatLevel ?: BeastModePulseManager.ThreatLevel.SAFE
    
    val (statusText, statusColor, bgGlow) = when (threatLevel) {
        BeastModePulseManager.ThreatLevel.DANGER -> Triple("ALERT", OmniColors.Danger, OmniColors.Danger.copy(alpha = 0.3f))
        BeastModePulseManager.ThreatLevel.CAUTION -> Triple("CAUTION", OmniColors.Warning, OmniColors.Warning.copy(alpha = 0.3f))
        else -> Triple("SECURE", OmniColors.Primary, OmniColors.Primary.copy(alpha = 0.3f))
    }

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
                .border(2.dp, statusColor.copy(alpha = 0.3f), CircleShape)
        )
        
        // Inner Glow
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            statusColor.copy(alpha = 0.4f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.headlineMedium,
                color = statusColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            Text(
                text = when (threatLevel) {
                    BeastModePulseManager.ThreatLevel.DANGER -> "Resource Critical"
                    BeastModePulseManager.ThreatLevel.CAUTION -> "Monitoring Load"
                    else -> "No Threats Detected"
                },
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
private fun GuardianControlsGrid(
    onNeuralShieldToggle: () -> Unit,
    onRansomwareShieldToggle: () -> Unit,
    ransomwareShieldActive: Boolean
) {
    val context = LocalContext.current
    
    // Check Neural Shield status using robust method
    var neuralShieldEnabled by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        neuralShieldEnabled = NeuralShieldManager.isNeuralShieldEnabled(context)
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            PermissionCard(
                title = "Neural Vision",
                icon = "👁️",
                desc = "Live Link Scan",
                enabled = neuralShieldEnabled,
                onToggle = onNeuralShieldToggle,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            PermissionCard(
                title = "Ransomware Shield",
                icon = "🛡️",
                desc = "File Guard",
                enabled = ransomwareShieldActive,
                onToggle = onRansomwareShieldToggle,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            PermissionCard(
                title = "Sensor Guardian",
                icon = "📡",
                desc = "Hardware Monitor",
                enabled = true,
                onToggle = { },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            PermissionCard(
                title = "Permission Warden",
                icon = "🔐",
                desc = "App DNA Scan",
                enabled = true,
                onToggle = { },
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
private fun BeastModeVitalsCards(pulse: BeastModePulse?) {
    val cpuTemp = pulse?.cpuTemperature?.let { "%.1f°C".format(it) } ?: "--"
    val ramUsed = pulse?.ramPercentUsed?.let { "%.0f%%".format(it) } ?: "--"
    val networkStatus = pulse?.networkType ?: "Offline"
    
    val cpuColor = when (pulse?.cpuThreatLevel) {
        BeastModePulseManager.ThreatLevel.DANGER -> OmniColors.Danger
        BeastModePulseManager.ThreatLevel.CAUTION -> OmniColors.Warning
        else -> OmniColors.Primary
    }
    
    val ramColor = when (pulse?.ramThreatLevel) {
        BeastModePulseManager.ThreatLevel.DANGER -> OmniColors.Danger
        BeastModePulseManager.ThreatLevel.CAUTION -> OmniColors.Warning
        else -> OmniColors.Accent
    }
    
    val netColor = when (pulse?.networkThreatLevel) {
        BeastModePulseManager.ThreatLevel.DANGER -> OmniColors.Danger
        BeastModePulseManager.ThreatLevel.CAUTION -> OmniColors.Warning
        else -> OmniColors.Secondary
    }

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        VitalCard("CPU Temp", cpuTemp, Icons.Default.Thermostat, cpuColor)
        VitalCard("RAM Use", ramUsed, Icons.Default.Memory, ramColor)
        VitalCard("Network", networkStatus, Icons.Default.Wifi, netColor)
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
            Text(
                text = value, 
                style = MaterialTheme.typography.headlineSmall, 
                color = OmniColors.TextPrimary, 
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label, 
                style = MaterialTheme.typography.labelSmall, 
                color = OmniColors.TextTertiary
            )
        }
    }
}

@Composable
private fun SensorGuardianHeatmap(heatmap: Map<Int, List<SensorAccessInfo>>) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(OmniColors.Surface)
            .padding(16.dp)
    ) {
        // Camera sensors
        Text("📷 Camera", color = OmniColors.TextSecondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val cameraApps = heatmap[SensorGuardianManager.SENSOR_CAMERA] ?: emptyList()
        if (cameraApps.isEmpty()) {
            Text("No camera access detected", color = OmniColors.TextTertiary, fontSize = 12.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cameraApps.take(5)) { app ->
                    SensorAppChip(app)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Microphone
        Text("🎤 Microphone", color = OmniColors.TextSecondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val micApps = heatmap[SensorGuardianManager.SENSOR_MICROPHONE] ?: emptyList()
        if (micApps.isEmpty()) {
            Text("No microphone access detected", color = OmniColors.TextTertiary, fontSize = 12.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(micApps.take(5)) { app ->
                    SensorAppChip(app)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Location
        Text("📍 Location", color = OmniColors.TextSecondary, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        val locationApps = heatmap[SensorGuardianManager.SENSOR_LOCATION] ?: emptyList()
        if (locationApps.isEmpty()) {
            Text("No location access detected", color = OmniColors.TextTertiary, fontSize = 12.sp)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(locationApps.take(5)) { app ->
                    SensorAppChip(app)
                }
            }
        }
    }
}

@Composable
private fun SensorAppChip(app: SensorAccessInfo) {
    val chipColor = when (app.riskLevel) {
        3 -> OmniColors.Danger
        2 -> OmniColors.Warning
        else -> OmniColors.Primary
    }
    
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(chipColor.copy(alpha = 0.2f))
            .border(1.dp, chipColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = app.appName,
            color = chipColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PermissionWardenResults(report: AppDNAReport?) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(OmniColors.Surface)
            .padding(16.dp)
    ) {
        val ghostApps = report?.ghostApps ?: emptyList()
        val sleeperApps = report?.sleeperServices ?: emptyList()
        
        if (ghostApps.isEmpty() && sleeperApps.isEmpty()) {
            Text(
                "✅ No suspicious apps detected",
                color = OmniColors.Secondary,
                fontWeight = FontWeight.Bold
            )
        } else {
            // Ghost Apps
            if (ghostApps.isNotEmpty()) {
                Text(
                    "👻 Ghost Apps: ${ghostApps.size}",
                    color = OmniColors.Warning,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                ghostApps.take(3).forEach { app ->
                    GhostAppItem(app)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
            
            if (ghostApps.isNotEmpty() && sleeperApps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Sleeper Services
            if (sleeperApps.isNotEmpty()) {
                Text(
                    "😴 Sleeper Services: ${sleeperApps.size}",
                    color = OmniColors.Danger,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                sleeperApps.take(3).forEach { app ->
                    SleeperAppItem(app)
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
private fun GhostAppItem(app: GhostAppInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(app.appName, color = OmniColors.TextSecondary, fontSize = 12.sp)
        Text(
            "Risk: ${app.riskScore}", 
            color = when {
                app.riskScore > 30 -> OmniColors.Danger
                app.riskScore > 15 -> OmniColors.Warning
                else -> OmniColors.TextTertiary
            },
            fontSize = 12.sp
        )
    }
}

@Composable
private fun SleeperAppItem(app: SleeperServiceInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(app.appName, color = OmniColors.TextSecondary, fontSize = 12.sp)
        Text(
            "${app.runningServices.size} services", 
            color = if (app.runningServices.isNotEmpty()) OmniColors.Danger else OmniColors.TextTertiary,
            fontSize = 12.sp
        )
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
                    text = "No suspicious apps detected right now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = OmniColors.TextTertiary
                )
            } else {
                apps.take(5).forEach { app ->
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
