// Developer: Chetraj Jaishi
package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Purple80,
    onPrimary = Color(0xFF381E72),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    secondary = PurpleGrey80,
    background = SleekBackgroundDark,
    onBackground = SleekOnBackgroundDark,
    surface = SleekSurfaceDark,
    onSurface = SleekOnSurfaceDark,
    surfaceVariant = SleekSurfaceVariantDark,
    onSurfaceVariant = SleekOnSurfaceVariantDark,
    outline = SleekOutline,
    outlineVariant = SleekOutlineVariant
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPrimary,
    onPrimary = SleekOnPrimary,
    primaryContainer = SleekPrimaryContainer,
    onPrimaryContainer = SleekOnPrimaryContainer,
    secondary = PurpleGrey40,
    background = SleekBackgroundLight,
    onBackground = SleekOnBackgroundLight,
    surface = SleekSurfaceLight,
    onSurface = SleekOnSurfaceLight,
    surfaceVariant = SleekSurfaceVariant,
    onSurfaceVariant = SleekOnSurfaceVariant,
    outline = SleekOutline,
    outlineVariant = SleekOutlineVariant
  )

private val MonochromeLightColorScheme = lightColorScheme(
    primary = Color(0xFF0F172A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE2E8F0),
    onPrimaryContainer = Color(0xFF0F172A),
    secondary = Color(0xFF64748B),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1)
)

private val MonochromeDarkColorScheme = darkColorScheme(
    primary = Color(0xFFF1F5F9),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF334155),
    onPrimaryContainer = Color(0xFFF1F5F9),
    secondary = Color(0xFF94A3B8),
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF475569)
)

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  isMonochrome: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      isMonochrome -> {
        if (darkTheme) MonochromeDarkColorScheme else MonochromeLightColorScheme
      }
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }
      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
