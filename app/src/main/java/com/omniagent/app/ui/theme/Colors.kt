package com.omniagent.app.ui.theme

import androidx.compose.ui.graphics.Color

// === OMNIAGENT DARK THEME PALETTE ===
// Inspired by control center / OS terminal aesthetics

object OmniColors {
    // Background layers
    val Background = Color(0xFF0D1117)
    val Surface = Color(0xFF161B22)
    val SurfaceElevated = Color(0xFF1C2128)
    val SurfaceBright = Color(0xFF21262D)
    val CardBg = Color(0xFF1A1F27)

    // Primary accent — Electric Blue
    val Primary = Color(0xFF58A6FF)
    val PrimaryDim = Color(0xFF388BFD)
    val PrimaryGlow = Color(0x3358A6FF) // 20% opacity

    // Secondary — Neon Green (success/active)
    val Secondary = Color(0xFF7EE787)
    val SecondaryDim = Color(0xFF56D364)
    val SecondaryGlow = Color(0x337EE787)

    // Accent — Purple (highlights)
    val Accent = Color(0xFFD2A8FF)
    val AccentDim = Color(0xFFBC8CFF)
    val AccentGlow = Color(0x33D2A8FF)

    // Warning — Amber
    val Warning = Color(0xFFF0883E)
    val WarningDim = Color(0xFFDB6D28)
    val WarningGlow = Color(0x33F0883E)

    // Danger — Red
    val Danger = Color(0xFFF85149)
    val DangerDim = Color(0xFFDA3633)

    // Text
    val TextPrimary = Color(0xFFE6EDF3)
    val TextSecondary = Color(0xFF8B949E)
    val TextTertiary = Color(0xFF6E7681)

    // Semantic Aliases
    val Success = Secondary
    val Info = Primary
    val Error = Danger
    val Hint = TextTertiary

    // Borders
    val Border = Color(0xFF30363D)
    val BorderFocused = Color(0xFF58A6FF)

    // Module-specific colors
    val ModuleCoding = Color(0xFF79C0FF)    // Light Blue
    val ModuleCyber = Color(0xFFF85149)     // Red
    val ModuleResume = Color(0xFF7EE787)    // Green
    val ModuleStartup = Color(0xFFF0883E)   // Orange

    // Gradients
    val GradientStart = Color(0xFF58A6FF)
    val GradientEnd = Color(0xFFD2A8FF)

    fun getModuleColor(module: String): Color {
        return when (module.lowercase()) {
            "coding" -> ModuleCoding
            "cybersecurity" -> ModuleCyber
            "resume" -> ModuleResume
            "startup" -> ModuleStartup
            else -> Primary
        }
    }
}
