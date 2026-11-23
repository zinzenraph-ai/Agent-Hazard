package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

private fun studioColorScheme(darkTheme: Boolean, accentStyle: String): ColorScheme {
    val (primaryAccent, primaryDarkContainer, secondaryAccent) = when (accentStyle) {
        "emerald" -> Triple(Color(0xFF22C55E), Color(0xFF0A2E1A), Color(0xFF38BDF8))
        "amber" -> Triple(Color(0xFFFFB300), Color(0xFF2E2100), Color(0xFF00E5FF))
        "violet" -> Triple(Color(0xFFA78BFA), Color(0xFF251A47), Color(0xFF34D399))
        "ocean" -> Triple(Color(0xFF38BDF8), Color(0xFF0A2838), Color(0xFFFFB300))
        else -> Triple(HazardAmber, HazardAmberDark, SignalCyan) // Default Hazard identity
    }

    return if (darkTheme) {
        darkColorScheme(
            primary = primaryAccent,
            onPrimary = Color(0xFF0D1117),
            primaryContainer = primaryDarkContainer,
            onPrimaryContainer = primaryAccent,
            secondary = secondaryAccent,
            onSecondary = Color(0xFF001F24),
            secondaryContainer = SignalCyanDark,
            onSecondaryContainer = secondaryAccent,
            tertiary = ProfitEmerald,
            onTertiary = Color(0xFF002110),
            tertiaryContainer = ProfitEmeraldContainer,
            onTertiaryContainer = ProfitEmerald,
            error = RiskCoral,
            onError = Color(0xFF330005),
            errorContainer = RiskCoralContainer,
            onErrorContainer = RiskCoral,
            background = ObsidianBg,
            onBackground = TextPrimary,
            surface = StudioSurface,
            onSurface = TextPrimary,
            surfaceVariant = StudioSurfaceElevated,
            onSurfaceVariant = TextSecondary,
            outline = StudioBorder,
            outlineVariant = StudioBorder.copy(alpha = 0.6f)
        )
    } else {
        val lightPrimary = when (accentStyle) {
            "emerald" -> Color(0xFF15803D)
            "amber" -> Color(0xFFB45309)
            "violet" -> Color(0xFF6D28D9)
            "ocean" -> Color(0xFF0284C7)
            else -> Color(0xFF0F172A)
        }
        lightColorScheme(
            primary = lightPrimary,
            onPrimary = Color.White,
            primaryContainer = primaryAccent.copy(alpha = 0.18f),
            onPrimaryContainer = lightPrimary,
            secondary = Color(0xFF0284C7),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE0F2FE),
            onSecondaryContainer = Color(0xFF075985),
            tertiary = Color(0xFF15803D),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFDCFCE7),
            onTertiaryContainer = Color(0xFF166534),
            error = Color(0xFFDC2626),
            onError = Color.White,
            errorContainer = Color(0xFFFEE2E2),
            onErrorContainer = Color(0xFF991B1B),
            background = Color(0xFFF4F6FA),
            onBackground = Color(0xFF0F172A),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF0F172A),
            surfaceVariant = Color(0xFFE8EEF5),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFFCBD5E1),
            outlineVariant = Color(0xFFE2E8F0)
        )
    }
}

private val HazardWyattColorScheme = darkColorScheme(
    primary = WyattSunGold,
    onPrimary = Color(0xFF241A00),
    primaryContainer = Color(0xFF3F3005),
    onPrimaryContainer = WyattSunGold,
    secondary = WyattRocketTeal,
    onSecondary = Color(0xFF002822),
    secondaryContainer = Color(0xFF0C3D36),
    onSecondaryContainer = WyattRocketTeal,
    tertiary = WyattCoral,
    onTertiary = Color(0xFF2E0B00),
    tertiaryContainer = Color(0xFF471A0E),
    onTertiaryContainer = WyattCoral,
    error = RiskCoral,
    onError = Color.White,
    background = WyattSkyBg,
    onBackground = WyattTextPrimary,
    surface = WyattSurface,
    onSurface = WyattTextPrimary,
    surfaceVariant = WyattSurfaceBright,
    onSurfaceVariant = WyattTextSecondary,
    outline = Color(0xFF36598E),
    outlineVariant = Color(0xFF284775)
)

private val StudioShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val WyattShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(34.dp)
)

@Composable
fun HazardTheme(
    isWyattMode: Boolean = false,
    darkTheme: Boolean = true,
    accentStyle: String = "hazard",
    textScale: Float = 1.0f,
    content: @Composable () -> Unit
) {
    val baseDensity = LocalDensity.current
    val scaledDensity = Density(
        density = baseDensity.density,
        fontScale = (baseDensity.fontScale * textScale.coerceIn(0.85f, 1.45f))
    )
    val colors = if (isWyattMode) {
        HazardWyattColorScheme
    } else {
        studioColorScheme(darkTheme = darkTheme, accentStyle = accentStyle)
    }

    CompositionLocalProvider(LocalDensity provides scaledDensity) {
        MaterialTheme(
            colorScheme = colors,
            typography = if (isWyattMode) WyattTypography else StudioTypography,
            shapes = if (isWyattMode) WyattShapes else StudioShapes,
            content = content
        )
    }
}
