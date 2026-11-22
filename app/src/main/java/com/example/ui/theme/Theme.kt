package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class OcsTelemetryColors(
    val structuralEmerald: Color = StructuralEmerald,
    val structuralEmeraldDim: Color = StructuralEmeraldDim,
    val haltAmber: Color = HaltAmber,
    val haltAmberDim: Color = HaltAmberDim,
    val sonicViolet: Color = SonicViolet,
    val sonicVioletDim: Color = SonicVioletDim,
    val driftCrimson: Color = DriftCrimson,
    val driftCrimsonDim: Color = DriftCrimsonDim,
    val kickCyan: Color = KickCyan,
    val kickCyanDim: Color = KickCyanDim,
    val consoleSurface: Color = SlateConsole,
    val hairline: Color = HairlineGrid,
    val hairlineBright: Color = HairlineBright
)

val LocalOcsColors = staticCompositionLocalOf { OcsTelemetryColors() }

private val ObsidianDarkColorScheme = darkColorScheme(
    primary = KickCyan,
    onPrimary = KickCyanText,
    primaryContainer = KickCyanDim,
    onPrimaryContainer = TelemetryInk,
    secondary = HaltAmber,
    onSecondary = HaltAmberText,
    secondaryContainer = HaltAmberDim,
    onSecondaryContainer = TelemetryInk,
    tertiary = SonicViolet,
    onTertiary = SonicVioletText,
    tertiaryContainer = SonicVioletDim,
    onTertiaryContainer = TelemetryInk,
    background = ObsidianVoid,
    onBackground = TelemetryInk,
    surface = ObsidianPanel,
    onSurface = TelemetryInk,
    surfaceVariant = SlateConsole,
    onSurfaceVariant = TelemetryMuted,
    surfaceContainerLowest = ObsidianVoid,
    surfaceContainerLow = ObsidianPanel,
    surfaceContainer = ObsidianElevated,
    surfaceContainerHigh = SlateConsole,
    outline = HairlineGrid,
    outlineVariant = HairlineBright,
    error = DriftCrimson,
    onError = Color.White,
    errorContainer = DriftCrimsonDim,
    onErrorContainer = TelemetryInk
)

@Composable
fun OcsRuntimeTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOcsColors provides OcsTelemetryColors()) {
        MaterialTheme(
            colorScheme = ObsidianDarkColorScheme,
            typography = OcsTypography,
            content = content
        )
    }
}
