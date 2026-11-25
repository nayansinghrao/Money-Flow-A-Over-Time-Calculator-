package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val MoneyFlowColorScheme = darkColorScheme(
    primary = JadeBrand,
    onPrimary = Color(0xFF041C13),
    primaryContainer = MintSoftContainer,
    onPrimaryContainer = MintHighlight,
    secondary = MintHighlight,
    onSecondary = Color(0xFF052417),
    secondaryContainer = PineDeep,
    onSecondaryContainer = MintHighlight,
    tertiary = AmberOvertime,
    onTertiary = Color(0xFF281C02),
    tertiaryContainer = AmberOvertimeContainer,
    onTertiaryContainer = AmberOvertime,
    background = CharcoalBackground,
    onBackground = TextPrimary,
    surface = CharcoalSurface,
    onSurface = TextPrimary,
    surfaceVariant = CharcoalSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = CharcoalBackground,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = CharcoalSurfaceElevated,
    surfaceContainerHigh = CharcoalSurfacePressed,
    outline = CharcoalOutline,
    outlineVariant = CharcoalOutlineSubtle,
    error = ErrorCoral,
    onError = Color(0xFF2D0608),
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark
)

val MoneyFlowShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MoneyFlowTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MoneyFlowColorScheme,
        typography = MoneyFlowTypography,
        shapes = MoneyFlowShapes,
        content = content
    )
}
