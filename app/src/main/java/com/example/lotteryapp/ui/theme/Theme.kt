package com.example.lotteryapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LotteryColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainer,
    onPrimaryContainer = BrandOnPrimaryContainer,
    secondary = BrandSecondary,
    secondaryContainer = BrandSecondaryContainer,
    tertiary = BrandTertiary,
    tertiaryContainer = BrandTertiaryContainer,
    background = BrandBackground,
    surface = BrandSurface,
    error = BrandError
)

@Composable
fun LotteryAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LotteryColorScheme,
        typography = Typography,
        content = content
    )
}