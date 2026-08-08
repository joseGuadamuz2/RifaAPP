package com.example.lotteryapp.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.lotteryapp.data.entity.Raffle
import com.example.lotteryapp.data.entity.Ticket
import com.example.lotteryapp.data.entity.TicketStatus
import java.io.File
import java.io.FileOutputStream

object ImageSharingHelper {

    fun shareAvailableNumbers(context: Context, raffle: Raffle, tickets: List<Ticket>) {
        val bitmap = generateRaffleImage(raffle, tickets)
        val uri = saveBitmapToCache(context, bitmap)
        if (uri != null) {
            shareImage(context, uri)
        }
    }

    private fun generateRaffleImage(raffle: Raffle, tickets: List<Ticket>): Bitmap {
        val availableTickets = tickets.filter { it.status == TicketStatus.AVAILABLE }.sortedBy { it.number }
        
        val width = 1080
        val padding = 60f
        val headerHeight = 300f
        val footerHeight = 150f
        
        val columns = 10
        val rows = if (availableTickets.isEmpty()) 1 else (availableTickets.size + columns - 1) / columns
        val cellSize = (width - (padding * 2)) / columns
        val gridHeight = rows * cellSize
        
        val height = (headerHeight + gridHeight + footerHeight + (padding * 2)).toInt()
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        
        // Background
        canvas.drawColor(Color.WHITE)
        
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Header
        paint.color = Color.parseColor("#1976D2")
        canvas.drawRect(0f, 0f, width.toFloat(), headerHeight, paint)
        
        paint.color = Color.WHITE
        paint.textSize = 60f
        paint.isFakeBoldText = true
        canvas.drawText(raffle.name, padding, 100f, paint)
        
        paint.textSize = 40f
        paint.isFakeBoldText = false
        canvas.drawText(raffle.prizeName, padding, 170f, paint)
        
        paint.textSize = 35f
        canvas.drawText("Números Disponibles", padding, 240f, paint)
        
        // Grid
        val startY = headerHeight + padding
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 30f
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#616161")
        }
        
        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#ECEFF1")
        }
        
        if (availableTickets.isEmpty()) {
            paint.color = Color.GRAY
            paint.textSize = 40f
            canvas.drawText("No hay números disponibles", padding, startY + 50f, paint)
        } else {
            availableTickets.forEachIndexed { index, ticket ->
                val col = index % columns
                val row = index / columns
                
                val left = padding + (col * cellSize)
                val top = startY + (row * cellSize)
                val right = left + cellSize - 10f
                val bottom = top + cellSize - 10f
                
                val rect = RectF(left, top, right, bottom)
                canvas.drawRoundRect(rect, 15f, 15f, cellPaint)
                
                val textX = rect.centerX()
                val textY = rect.centerY() - ((textPaint.descent() + textPaint.ascent()) / 2)
                canvas.drawText(ticket.number, textX, textY, textPaint)
            }
        }
        
        // Footer (Summary)
        val footerY = height - footerHeight + padding
        paint.color = Color.BLACK
        paint.textSize = 35f
        
        canvas.drawText("Total disponibles: ${availableTickets.size}", padding, footerY, paint)
        
        return bitmap
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): Uri? {
        return try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val file = File(cachePath, "raffle_status.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun shareImage(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir imagen de rifa"))
    }
}