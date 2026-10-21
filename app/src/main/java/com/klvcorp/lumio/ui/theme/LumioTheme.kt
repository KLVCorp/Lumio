package com.klvcorp.lumio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val dark = darkColorScheme(
    primary = Color(0xFFBC9BFF), onPrimary = Color(0xFF221437),
    primaryContainer = Color(0xFF4C376B), onPrimaryContainer = Color(0xFFECDCFF),
    secondary = Color(0xFF90D9E4), background = Color(0xFF111019), onBackground = Color(0xFFF4F0FA),
    surface = Color(0xFF111019), onSurface = Color(0xFFF4F0FA),
    surfaceVariant = Color(0xFF272432), onSurfaceVariant = Color(0xFFD2CCDD),
    outline = Color(0xFF726C7C), error = Color(0xFFFFA2B0)
)
private val light = lightColorScheme(
    primary = Color(0xFF7653B6), onPrimary = Color.White,
    background = Color(0xFFFAF8FF), onBackground = Color(0xFF25212D),
    surface = Color.White, onSurface = Color(0xFF25212D),
    surfaceVariant = Color(0xFFEEE9F3), onSurfaceVariant = Color(0xFF49414E),
    outline = Color(0xFF77717C)
)
@Composable
fun LumioTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) dark else light, content = content)
}
