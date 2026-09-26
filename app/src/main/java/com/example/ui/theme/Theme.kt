package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SpaceColorScheme = darkColorScheme(
  primary = IndigoPrimary,
  onPrimary = Color.White,
  primaryContainer = IndigoVibrant,
  onPrimaryContainer = Color.White,
  secondary = CyanNeon,
  onSecondary = Color(0xFF04060E),
  secondaryContainer = CyanBright,
  onSecondaryContainer = Color.White,
  tertiary = AccentAmber,
  background = SpaceBackground,
  onBackground = TextPrimary,
  surface = SpaceSurface,
  onSurface = TextPrimary,
  surfaceVariant = SpaceCard,
  onSurfaceVariant = TextSecondary,
  outline = SpaceCardBorder
)

@Composable
fun MyApplicationTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = SpaceColorScheme,
    typography = Typography,
    content = content
  )
}
