package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val BackUpLightColorScheme = lightColorScheme(
    primary = RescueCobalt,
    onPrimary = Color.White,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = OnCobaltContainer,
    secondary = SignalOrange,
    onSecondary = Color.White,
    secondaryContainer = SignalAmberContainer,
    onSecondaryContainer = OnSignalAmberContainer,
    tertiary = TealVerified,
    onTertiary = Color.White,
    tertiaryContainer = TealContainer,
    onTertiaryContainer = OnTealContainer,
    background = BoneBackground,
    onBackground = SlateInk,
    surface = CrispSurface,
    onSurface = SlateInk,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = MutedSlate,
    outline = HairlineOutline,
    error = AlertRed,
    onError = Color.White,
    errorContainer = AlertRedContainer,
    onErrorContainer = Color(0xFF7F1D1D)
)

private val BackUpDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = Color(0xFF061B30),
    primaryContainer = CobaltMid,
    onPrimaryContainer = Color(0xFFDCE8F6),
    secondary = DarkSecondary,
    onSecondary = Color(0xFF3A1300),
    secondaryContainer = Color(0xFF5E2200),
    onSecondaryContainer = SignalAmberContainer,
    tertiary = DarkTertiary,
    onTertiary = Color(0xFF002925),
    tertiaryContainer = Color(0xFF0B4640),
    onTertiaryContainer = TealContainer,
    background = DarkBackground,
    onBackground = Color(0xFFEAF0F6),
    surface = DarkSurface,
    onSurface = Color(0xFFEAF0F6),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA3B4C6),
    outline = DarkOutline,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = AlertRedContainer
)

val BackUpShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun BackUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) BackUpDarkColorScheme else BackUpLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BackUpTypography,
        shapes = BackUpShapes,
        content = content
    )
}
