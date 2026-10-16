package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

data class GlassColorPalette(
  val bgDeep: androidx.compose.ui.graphics.Color = BgDarkDeep,
  val bgSurface: androidx.compose.ui.graphics.Color = BgDarkSurface,
  val bgElevated: androidx.compose.ui.graphics.Color = BgDarkElevated,
  val glassSubtle: androidx.compose.ui.graphics.Color = GlassWhiteSubtle,
  val glassMedium: androidx.compose.ui.graphics.Color = GlassWhiteMedium,
  val glassBorder: androidx.compose.ui.graphics.Color = GlassBorderSubtle,
  val glassBorderHighlight: androidx.compose.ui.graphics.Color = GlassBorderHighlight,
  val cyan: androidx.compose.ui.graphics.Color = NeonCyan,
  val emerald: androidx.compose.ui.graphics.Color = NeonEmerald,
  val coral: androidx.compose.ui.graphics.Color = NeonCoral,
  val amber: androidx.compose.ui.graphics.Color = NeonAmber,
  val textPrimary: androidx.compose.ui.graphics.Color = TextHighContrast,
  val textSecondary: androidx.compose.ui.graphics.Color = TextMediumContrast,
  val textMuted: androidx.compose.ui.graphics.Color = TextLowContrast
)

val LocalGlassColors = staticCompositionLocalOf { GlassColorPalette() }
val LocalGlassTypography = staticCompositionLocalOf { GlassTypography() }

object NotifyTheme {
  val colors: GlassColorPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalGlassColors.current

  val typography: GlassTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalGlassTypography.current
}

@Composable
fun NotifyAppTheme(
  content: @Composable () -> Unit
) {
  val colors = GlassColorPalette()
  val typography = GlassTypography()

  CompositionLocalProvider(
    LocalGlassColors provides colors,
    LocalGlassTypography provides typography,
    content = content
  )
}
