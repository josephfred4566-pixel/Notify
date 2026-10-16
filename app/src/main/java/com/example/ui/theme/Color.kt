package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val BgDarkDeep = Color(0xFF070A0F)
val BgDarkSurface = Color(0xFF0E1420)
val BgDarkElevated = Color(0xFF151E30)

// Glass Translucent Tints
val GlassWhiteUltraSubtle = Color(0x0AFFFFFF)
val GlassWhiteSubtle = Color(0x14FFFFFF)
val GlassWhiteMedium = Color(0x24FFFFFF)
val GlassWhiteProminent = Color(0x38FFFFFF)

val GlassBorderSubtle = Color(0x26FFFFFF)
val GlassBorderProminent = Color(0x59FFFFFF)
val GlassBorderHighlight = Color(0x99FFFFFF)

// Icon-Sourced 2-Color Accent Palette
val ElectricBlue = Color(0xFF3B82F6) // Sourced from the left side of the icon (Active states, primary actions, selected chips)
val ElectricBlueGlow = Color(0x333B82F6)

val CrimsonRed = Color(0xFFEF4444) // Sourced from the right side of the icon (Secondary/alert states, critical triggers, checkmark buttons when engaged)
val CrimsonRedGlow = Color(0x33EF4444)

// Mapping Neon aliases to Electric Blue & Crimson Red for unified theme branding
val NeonCyan = ElectricBlue
val NeonCyanGlow = ElectricBlueGlow
val NeonEmerald = ElectricBlue
val NeonEmeraldGlow = ElectricBlueGlow
val NeonCoral = CrimsonRed
val NeonCoralGlow = CrimsonRedGlow
val NeonAmber = ElectricBlue
val NeonAmberGlow = ElectricBlueGlow
val NeonViolet = ElectricBlue
val NeonVioletGlow = ElectricBlueGlow

// Text Contrast Hierarchy - All White with deliberate opacity
val TextHighContrast = Color.White
val TextMediumContrast = Color(0xCCFFFFFF) // 80% white
val TextLowContrast = Color(0x80FFFFFF)    // 50% white
val TextDisabled = Color(0x40FFFFFF)       // 25% white
