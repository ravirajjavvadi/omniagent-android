package com.omniagent.app.ui.theme

import androidx.compose.ui.graphics.Color

// === OMNIAGENT DARK THEME PALETTE ===
// Inspired by control center / OS terminal aesthetics

object OmniColors {
    // === DEEP SPACE VIBRANT PALETTE ===
    
    // Background layers (Rich Obsidian)
    val Background = Color(0xFF080A0C)
    val Surface = Color(0xFF111418)
    val SurfaceElevated = Color(0xFF1A1D23)
    val SurfaceBright = Color(0xFF24292F)
    val CardBg = Color(0xFF141A21)

    // Primary — Cyber Cyan (Glow focus)
    val Primary = Color(0xFF00F2FF)
    val PrimaryDim = Color(0xFF00B4D8)
    val PrimaryGlow = Color(0x4D00F2FF) // 30% opacity

    // Secondary — Matrix Green
    val Secondary = Color(0xFF39FF14)
    val SecondaryDim = Color(0xFF2ECC71)
    val SecondaryGlow = Color(0x4D39FF14)

    // Accent — Electric Purple
    val Accent = Color(0xFFBD00FF)
    val AccentDim = Color(0xFF9B00D3)
    val AccentGlow = Color(0x4DBD00FF)

    // Warning — High-Vis Orange
    val Warning = Color(0xFFFF9F00)
    val WarningDim = Color(0xFFE67E22)
    val WarningGlow = Color(0x4DFF9F00)

    // Danger — Blood Red
    val Danger = Color(0xFFFF3131)
    val DangerDim = Color(0xFFD90429)

    // Text (Ultra Contrast)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0B8C1)
    val TextTertiary = Color(0xFF808B96)

    // Semantic Aliases
    val Success = Secondary
    val Info = Primary
    val Error = Danger
    val Hint = TextTertiary

    // Borders (Neon Glow)
    val Border = Color(0xFF2D333B)
    val BorderFocused = Color(0xFF00F2FF)

    // Module-specific colors (Vibrant Variants)
    val ModuleCoding = Color(0xFF00F2FF)    // Cyan
    val ModuleCyber = Color(0xFFFF3131)     // Red
    val ModuleResume = Color(0xFF39FF14)    // Green
    val ModuleStartup = Color(0xFFFF9F00)   // Orange

    // Gradients (Premium Flow)
    val GradientStart = Color(0xFF00F2FF)   // Cyan
    val GradientCenter = Color(0xFFBD00FF)  // Purple
    val GradientEnd = Color(0xFFFF3131)     // Red

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
