package com.example.lotteryapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Paleta de estados de boletos — consciente del tema claro/oscuro.
// Light: fondos claros con texto oscuro. Dark: fondos oscuros con texto claro.
data class TicketPalette(
    val availableBg: Color,
    val availableText: Color,
    val availableBorder: Color,
    val reservedBg: Color,
    val reservedText: Color,
    val soldBg: Color,
    val soldText: Color,
    val selectedBorder: Color,
    val statSold: Color,
    val statReserved: Color,
    val legendBorder: Color
)

private val LightTicketPalette = TicketPalette(
    availableBg = Color(0xFFF5F5F5),
    availableText = Color(0xFF424242),
    availableBorder = Color(0x1A9E9E9E),
    reservedBg = Color(0xFFFFD54F),
    reservedText = Color(0xFF7B5E00),
    soldBg = Color(0xFF66BB6A),
    soldText = Color.White,
    selectedBorder = Color(0xFF1976D2),
    statSold = Color(0xFF2E7D32),
    statReserved = Color(0xFF7B5E00),
    legendBorder = Color(0x4D9E9E9E)
)

private val DarkTicketPalette = TicketPalette(
    availableBg = Color(0xFF262B34),
    availableText = Color(0xFFC9CDD4),
    availableBorder = Color(0x3DFFFFFF),
    reservedBg = Color(0xFF6B5300),
    reservedText = Color(0xFFFFE082),
    soldBg = Color(0xFF2E7D32),
    soldText = Color(0xFFE8F5E9),
    selectedBorder = Color(0xFF60A5FA),
    statSold = Color(0xFF4ADE80),
    statReserved = Color(0xFFFFE082),
    legendBorder = Color(0x55FFFFFF)
)

@Composable
fun ticketPalette(): TicketPalette {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (dark) DarkTicketPalette else LightTicketPalette
}
