package com.example.lotteryapp.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

data class PdfClientRow(
    val name: String,
    val numbers: String,
    val total: String
)

object PdfReportHelper {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 44f
    private const val LINE_HEIGHT = 22f
    private const val MAX_CHARS = 58

    fun generateAndSharePdf(
        context: Context,
        raffleName: String,
        totalRecaudado: String,
        montoPendiente: String,
        boletosVendidos: String,
        boletosPendientes: String,
        clientes: List<PdfClientRow>
    ) {
        val uri = generatePdf(
            context, raffleName, totalRecaudado, montoPendiente,
            boletosVendidos, boletosPendientes, clientes
        )
        if (uri != null) sharePdf(context, uri)
    }

    private fun generatePdf(
        context: Context,
        raffleName: String,
        totalRecaudado: String,
        montoPendiente: String,
        boletosVendidos: String,
        boletosPendientes: String,
        clientes: List<PdfClientRow>
    ): Uri? {
        val document = PdfDocument()
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A237E")
            textSize = 26f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#3F51B5")
            textSize = 16f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 15f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }
        val clientNamePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 14f
            isFakeBoldText = true
            textAlign = Paint.Align.LEFT
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = 13f
            textAlign = Paint.Align.LEFT
        }
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B0BEC5")
            strokeWidth = 1.5f
        }
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        var page = document.startPage(pageInfo)
        var pageNumber = 1
        var y = MARGIN

        fun newPageIfNeeded(needed: Float) {
            if (y + needed > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                )
                y = MARGIN
            }
        }

        fun drawText(text: String, paint: Paint, indent: Float = 0f) {
            newPageIfNeeded(LINE_HEIGHT)
            page.canvas.drawText(text, MARGIN + indent, y, paint)
            y += LINE_HEIGHT
        }

        fun drawWrapped(text: String, paint: Paint, indent: Float = 0f) {
            var remaining = text
            while (remaining.length > MAX_CHARS) {
                var cut = remaining.lastIndexOf(' ', MAX_CHARS)
                if (cut <= 0) cut = MAX_CHARS
                drawText(remaining.substring(0, cut), paint, indent)
                remaining = remaining.substring(cut).trimStart()
            }
            if (remaining.isNotEmpty()) drawText(remaining, paint, indent)
        }

        fun drawSeparator() {
            newPageIfNeeded(12f)
            y += 6f
            page.canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
            y += 12f
        }

        // Encabezado
        page.canvas.drawText("REPORTE DE VENTAS", PAGE_WIDTH / 2f, 90f, titlePaint)
        page.canvas.drawText(raffleName.uppercase(), PAGE_WIDTH / 2f, 118f, subtitlePaint)
        y = 150f

        drawSeparator()
        drawText("Recaudado:  $totalRecaudado", sectionPaint)
        drawText("Por cobrar:  $montoPendiente", bodyPaint)
        drawText("Boletos pagados:  $boletosVendidos", bodyPaint)
        drawText("Boletos pendientes:  $boletosPendientes", bodyPaint)

        drawSeparator()
        drawText("DETALLE DE CLIENTES", sectionPaint)
        y += 6f

        if (clientes.isEmpty()) {
            drawText("No hay clientes registrados aun.", bodyPaint)
        } else {
            clientes.forEachIndexed { index, cliente ->
                if (index > 0) drawText("", bodyPaint)
                drawText("${index + 1}. ${cliente.name}  -  ${cliente.total}", clientNamePaint)
                drawWrapped("Boletos: ${cliente.numbers}", bodyPaint, 12f)
            }
        }

        drawSeparator()
        drawText("Generado por Rifador", bodyPaint)

        document.finishPage(page)

        return try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val safeName = raffleName.replace(Regex("[^A-Za-z0-9]"), "_")
            val file = File(reportsDir, "reporte_${safeName}_${System.currentTimeMillis()}.pdf")
            document.writeTo(FileOutputStream(file))
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            null
        } finally {
            document.close()
        }
    }

    private fun sharePdf(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartir Reporte PDF"))
    }
}