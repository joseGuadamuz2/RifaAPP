package com.example.lotteryapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LotteryLightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    onSecondary = Color.White,
    secondaryContainer = BrandSecondaryContainer,
    onSecondaryContainer = BrandOnSecondaryContainer,
    tertiary = BrandTertiary,
    onTertiary = Color.White,
    tertiaryContainer = BrandTertiaryContainer,
    onTertiaryContainer = BrandOnTertiaryContainer,
    background = BrandBackground,
    onBackground = BrandOnSurface,
    surface = BrandSurface,
    onSurface = BrandOnSurface,
    surfaceVariant = BrandSurfaceVariant,
    onSurfaceVariant = BrandOnSurfaceVariant,
    outline = BrandOutline,
    outlineVariant = BrandOutlineVariant,
    error = BrandError,
    onError = Color.White,
    errorContainer = BrandErrorContainer,
    onErrorContainer = BrandOnErrorContainer
)

private val LotteryDarkColors = darkColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = BrandPrimaryContainer,
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF4ADE80),
    onTertiary = Color(0xFF14532D),
    tertiaryContainer = Color(0xFF166534),
    onTertiaryContainer = Color(0xFFDCFCE7),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE5E7EB),
    surface = Color(0xFF171A21),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF262B34),
    onSurfaceVariant = Color(0xFF9CA3AF),
    outline = Color(0xFF4B5563),
    outlineVariant = Color(0xFF374151),
    error = Color(0xFFF87171),
    onError = Color(0xFF7F1D1D),
    errorContainer = Color(0xFF991B1B),
    onErrorContainer = Color(0xFFFEE2E2)
)

@Composable
fun LotteryAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) LotteryDarkColors else LotteryLightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}