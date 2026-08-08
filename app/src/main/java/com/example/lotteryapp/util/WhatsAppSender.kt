package com.example.lotteryapp.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.Ticket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WhatsAppSender {

    private const val COUNTRY_CODE = "506"

    fun sendTextReceipt(context: Context, raffle: Raffle, tickets: List<Ticket>) {
        val phone = tickets.firstOrNull()?.buyerPhone
        if (phone.isNullOrBlank()) return

        val message = buildMessage(raffle, tickets)
        sendCustomMessage(context, phone, message)
    }

    fun sendCustomMessage(context: Context, phone: String?, message: String) {
        if (phone.isNullOrBlank()) return

        var cleanPhone = phone.filter { it.isDigit() }
        
        // Si el número tiene 8 dígitos, se le agrega el código de país (Costa Rica)
        if (cleanPhone.length == 8) {
            cleanPhone = "$COUNTRY_CODE$cleanPhone"
        }

        val uri = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    private fun buildMessage(raffle: Raffle, tickets: List<Ticket>): String {
        val numbers = tickets.joinToString(", ") { it.number }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CR"))
        val dateText = dateFormat.format(Date(raffle.drawDate))
        val buyerName = tickets.firstOrNull()?.buyerName ?: ""
        val statusText = if (tickets.firstOrNull()?.status?.name == "SOLD") "Vendido" else "Apartado"
        
        return buildString {
            appendLine("🎟️ ${raffle.name}")
            appendLine("Premio: ${raffle.prizeName}")
            appendLine("Número(s): $numbers")
            appendLine("Fecha del sorteo: $dateText")
            appendLine("Precio por boleto: ₡${raffle.ticketPrice.toInt()}")
            appendLine("Comprador: $buyerName")
            append("Estado: $statusText")
        }
    }
}