package com.example.scrollstop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FocusGuardColors = darkColorScheme(
    primary = Color(0xFF8FD9B6),
    onPrimary = Color(0xFF003826),
    primaryContainer = Color(0xFF1B3A2E),
    onPrimaryContainer = Color(0xFFB6F2D3),
    secondary = Color(0xFFB0BEC5),
    onSecondary = Color(0xFF1C2427),
    secondaryContainer = Color(0xFF2A3136),
    onSecondaryContainer = Color(0xFFD5E0E6),
    tertiary = Color(0xFFF2C46D),
    onTertiary = Color(0xFF3F2E00),
    tertiaryContainer = Color(0xFF3B3220),
    onTertiaryContainer = Color(0xFFFFE0A3),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF5C0A05),
    errorContainer = Color(0xFF3D1E1C),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0D0F11),
    onBackground = Color(0xFFE6E8EA),
    surface = Color(0xFF0D0F11),
    onSurface = Color(0xFFE6E8EA),
    surfaceVariant = Color(0xFF22262A),
    onSurfaceVariant = Color(0xFF9BA3AB),
    outline = Color(0xFF3A4046),
    outlineVariant = Color(0xFF2A2F34),
    surfaceContainerLowest = Color(0xFF08090A),
    surfaceContainerLow = Color(0xFF121417),
    surfaceContainer = Color(0xFF16191C),
    surfaceContainerHigh = Color(0xFF1C1F23),
    surfaceContainerHighest = Color(0xFF23272B),
)

@Composable
fun FocusGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FocusGuardColors,
        content = content,
    )
}
