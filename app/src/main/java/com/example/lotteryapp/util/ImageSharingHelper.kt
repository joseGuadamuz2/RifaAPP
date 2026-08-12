package com.example.lotteryapp.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.RaffleSource
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object ImageSharingHelper {

    fun shareAvailableNumbers(context: Context, raffle: Raffle, tickets: List<Ticket>) {
        val availableTickets = tickets.filter { it.status == TicketStatus.AVAILABLE }.sortedBy { it.number }
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date(raffle.drawDate))
        
        val localeCR = Locale.forLanguageTag("es-CR")
        val currencyFormat = NumberFormat.getCurrencyInstance(localeCR).apply {
            maximumFractionDigits = 0
        }

        val sourceText = when(raffle.source) {
            RaffleSource.LOTERIA_NACIONAL -> "Lotería Nacional"
            RaffleSource.CHANCES -> "Chances"
            RaffleSource.SORTEO -> "Sorteo Especial"
            RaffleSource.MANUAl -> "Manual"
            else -> "Sorteo General"
        }

        // Generar el Flyer Publicitario Premium
        val bitmap = generateRaffleFlyer(context, raffle, availableTickets, dateStr, sourceText, currencyFormat)
        val uri = saveBitmapToCache(context, bitmap)
        
        if (uri != null) {
            val shareText = """
                🎟️ *¡GRAN RIFA DISPONIBLE!* 🎟️
                
                📌 *Rifa:* ${raffle.name}
                🎁 *Premio:* ${raffle.prizeName}
                💰 *Valor del número:* ${currencyFormat.format(raffle.ticketPrice)}
                📅 *Fecha del sorteo:* $dateStr
                🎰 *Sorteo por:* $sourceText
                
                ✨ Quedan *${availableTickets.size}* números disponibles.
                ¡Pedí el tuyo ahora y probá tu suerte! 🚀
            """.trimIndent()

            shareImageAndText(context, uri, shareText)
        }
    }

    private fun generateRaffleFlyer(
        context: Context, 
        raffle: Raffle, 
        availableTickets: List<Ticket>, 
        dateStr: String,
        sourceText: String,
        currencyFormat: NumberFormat
    ): Bitmap {
        val width = 1080
        val padding = 60f
        val headerHeight = 720f
        val footerHeight = 160f
        
        val columns = 10
        val rows = if (availableTickets.isEmpty()) 1 else (availableTickets.size + columns - 1) / columns
        val cellSize = (width - (padding * 2)) / columns
        val gridHeight = rows * cellSize
        
        val height = (headerHeight + gridHeight + footerHeight + (padding * 2)).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Encabezado con degradado Azul Premium
        val headerRect = RectF(0f, 0f, width.toFloat(), headerHeight)
        paint.shader = LinearGradient(0f, 0f, 0f, headerHeight, 
            Color.parseColor("#1A237E"), Color.parseColor("#3F51B5"), Shader.TileMode.CLAMP)
        canvas.drawRect(headerRect, paint)
        paint.shader = null

        // Foto del Premio con Marco
        raffle.prizePhotoPath?.let { path ->
            val prizeBitmap = loadBitmap(context, path)
            if (prizeBitmap != null) {
                val imgSize = 480f
                val imgRect = RectF(width - imgSize - padding, 80f, width - padding, 80f + imgSize)
                
                val clipPath = Path().apply { addRoundRect(imgRect, 45f, 45f, Path.Direction.CW) }
                canvas.save()
                canvas.clipPath(clipPath)
                canvas.drawBitmap(prizeBitmap, null, imgRect, paint)
                canvas.restore()
                
                paint.color = Color.WHITE
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 10f
                canvas.drawRoundRect(imgRect, 45f, 45f, paint)
                paint.style = Paint.Style.FILL
            }
        }
        
        // Textos del Flyer
        paint.color = Color.WHITE
        paint.isFakeBoldText = true
        paint.textSize = 75f
        canvas.drawText(raffle.name.uppercase(), padding, 150f, paint)
        
        paint.textSize = 50f
        paint.isFakeBoldText = false
        canvas.drawText("🎁 Premio: ${raffle.prizeName}", padding, 240f, paint)
        
        paint.color = Color.parseColor("#FFD600") // Amarillo para el precio
        paint.isFakeBoldText = true
        paint.textSize = 70f
        canvas.drawText("💰 Valor: ${currencyFormat.format(raffle.ticketPrice)}", padding, 340f, paint)
        
        paint.color = Color.WHITE
        paint.isFakeBoldText = false
        paint.textSize = 42f
        canvas.drawText("📅 Fecha: $dateStr", padding, 430f, paint)
        canvas.drawText("🎰 Tipo: $sourceText", padding, 490f, paint)
        
        paint.textSize = 38f
        paint.isFakeBoldText = true
        canvas.drawText("NÚMEROS DISPONIBLES:", padding, 650f, paint)
        
        // Cuadrícula
        val startY = headerHeight + padding
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 34f
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#283593")
            isFakeBoldText = true
        }
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#F5F7F9") }
        
        availableTickets.forEachIndexed { index, ticket ->
            val col = index % columns
            val row = index / columns
            val left = padding + (col * cellSize)
            val top = startY + (row * cellSize)
            val rect = RectF(left, top, left + cellSize - 20f, top + cellSize - 20f)
            canvas.drawRoundRect(rect, 22f, 22f, cellPaint)
            canvas.drawText(ticket.number, rect.centerX(), rect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2), textPaint)
        }
        
        // Pie de página
        paint.color = Color.parseColor("#5C6BC0")
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 40f
        canvas.drawText("¡Solo quedan ${availableTickets.size} números libres!", padding, height - padding, paint)
        
        return bitmap
    }

    private fun loadBitmap(context: Context, path: String): Bitmap? {
        return try {
            val uri = Uri.parse(path)
            if (path.startsWith("content://")) {
                context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
            } else {
                BitmapFactory.decodeFile(path)
            }
        } catch (e: Exception) { null }
    }

    fun persistPickedImage(context: Context, uri: Uri): String? {
        return try {
            val dir = File(context.filesDir, "prize_images").apply { mkdirs() }
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val ext = when {
                mimeType.contains("png") -> "png"
                mimeType.contains("webp") -> "webp"
                else -> "jpg"
            }
            val file = File(dir, "prize_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file.absolutePath
        } catch (e: Exception) { null }
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(cachePath, "flyer_rifa_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) { null }
    }

    private fun shareImageAndText(context: Context, uri: Uri, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Rifa"))
    }
}
