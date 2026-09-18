package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val RdStudioDarkColorScheme = darkColorScheme(
  primary = RdRedPrimary,
  onPrimary = Color.White,
  primaryContainer = RdRedContainer,
  onPrimaryContainer = RdRedLight,
  secondary = AudioWaveBlue,
  onSecondary = Color.Black,
  secondaryContainer = StudioCardElevated,
  onSecondaryContainer = AudioWaveBlue,
  tertiary = AudioWaveGreen,
  onTertiary = Color.Black,
  background = StudioBlack,
  onBackground = StudioTextPrimary,
  surface = StudioDarkCharcoal,
  onSurface = StudioTextPrimary,
  surfaceVariant = StudioCardSurface,
  onSurfaceVariant = StudioTextSecondary,
  outline = StudioBorder,
  outlineVariant = StudioBorderLight
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force professional studio dark interface by default
  dynamicColor: Boolean = false, // Keep RD Studio brand identity consistent
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = RdStudioDarkColorScheme,
    typography = Typography,
    content = content
  )
}

