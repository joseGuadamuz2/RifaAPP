package com.example.lotteryapp.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleModality
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.Winner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WhatsAppSender {

    private const val COUNTRY_CODE = "506"

    fun sendTextReceipt(context: Context, raffle: Raffle, tickets: List<Ticket>): Boolean {
        val phone = tickets.firstOrNull()?.buyerPhone
        if (phone.isNullOrBlank()) return false

        val message = buildMessage(raffle, tickets)
        return sendCustomMessage(context, phone, message)
    }

    fun sendCustomMessage(context: Context, phone: String?, message: String): Boolean {
        if (phone.isNullOrBlank()) return false

        var cleanPhone = phone.filter { it.isDigit() }

        // Si el número tiene 8 dígitos, se le agrega el código de país (Costa Rica)
        if (cleanPhone.length == 8) {
            cleanPhone = "$COUNTRY_CODE$cleanPhone"
        }

        val uri = Uri.parse("https://wa.me/$cleanPhone?text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    fun sendWinnerNotification(context: Context, raffle: Raffle, winner: Winner): Boolean {
        val phone = winner.buyerPhone
        if (phone.isNullOrBlank()) return false

        val message = buildWinnerMessage(raffle, winner)
        return sendCustomMessage(context, phone, message)
    }

    private fun buildWinnerMessage(raffle: Raffle, winner: Winner): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CR"))
        val dateText = dateFormat.format(Date(raffle.drawDate))
        val prize = buildString {
            append(winner.prizeName ?: raffle.prizeName)
            winner.prizeAmount?.let { append(" (₡${it.toInt()})") }
        }

        return buildString {
            appendLine("🏆 *¡FELICIDADES ${winner.buyerName.uppercase()}!* 🏆")
            appendLine("Has ganado la Rifa *${raffle.name}* 🎉")
            appendLine("🎁 *Premio:* $prize")
            appendLine("🔢 *Número ganador:* ${winner.winningNumber}")
            appendLine("📅 Sorteo: $dateText")
append("¡Enhorabuena! 🥳")
        }
    }

    private fun buildMessage(raffle: Raffle, tickets: List<Ticket>): String {
        val numbers = tickets.joinToString(", ") { it.number }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("es", "CR"))
        val dateText = dateFormat.format(Date(raffle.drawDate))
        val buyerName = tickets.firstOrNull()?.buyerName ?: ""
        val statusText = if (tickets.firstOrNull()?.status?.name == "SOLD") "Vendido" else "Apartado"
        val perGroup = raffle.modality == RaffleModality.GROUPS
        val total = if (perGroup) raffle.ticketPrice else raffle.ticketPrice * tickets.size

        return buildString {
            appendLine("🎟️ ${raffle.name}")
            appendLine("Premio: ${raffle.prizeName}")
            appendLine("Número(s): $numbers")
            appendLine("Fecha del sorteo: $dateText")
            appendLine(if (perGroup) "Precio por grupo: ₡${raffle.ticketPrice.toInt()}" else "Precio por boleto: ₡${raffle.ticketPrice.toInt()}")
            appendLine("Total: ₡${total.toInt()}")
            appendLine("Comprador: $buyerName")
            append("Estado: $statusText")
        }
    }
}