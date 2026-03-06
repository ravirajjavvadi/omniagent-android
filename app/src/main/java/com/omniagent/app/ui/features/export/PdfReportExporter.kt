package com.omniagent.app.ui.features.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.omniagent.app.core.model.AnalysisLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * PDF Report Exporter — generates structured, branded PDF reports from analysis logs.
 * Uses Android's built-in PdfDocument API (fully open-source, zero dependencies).
 */
object PdfReportExporter {

    private const val PAGE_WIDTH = 595  // A4 portrait
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val WATERMARK_TEXT = "OmniAgent AI • Confidential"

    /**
     * Export a single analysis log as a professional PDF.
     */
    fun exportReport(context: Context, log: AnalysisLog, decryptedResult: String): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawReport(canvas, log, decryptedResult)
            drawWatermark(canvas)

            pdfDocument.finishPage(page)

            // Save to Downloads directory
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "OmniAgent_Report_${log.classifiedModule}_$dateStr.pdf"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            Toast.makeText(context, "PDF saved: $fileName", Toast.LENGTH_LONG).show()
            file
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    /**
     * Export all logs as a consolidated PDF.
     */
    fun exportAllLogs(context: Context, logs: List<AnalysisLog>, decryptor: (String) -> String): File? {
        return try {
            val pdfDocument = PdfDocument()

            logs.forEachIndexed { index, log ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                val decryptedResult = decryptor(log.resultJson)
                drawReport(canvas, log, decryptedResult)
                drawWatermark(canvas)
                pdfDocument.finishPage(page)
            }

            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "OmniAgent_AllReports_$dateStr.pdf"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            Toast.makeText(context, "All logs exported: $fileName", Toast.LENGTH_LONG).show()
            file
        } catch (e: Exception) {
            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    private fun drawReport(canvas: Canvas, log: AnalysisLog, decryptedResult: String) {
        val titlePaint = Paint().apply {
            color = Color.rgb(99, 102, 241) // Indigo
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val headerPaint = Paint().apply {
            color = Color.rgb(30, 30, 30)
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(60, 60, 60)
            textSize = 12f
            isAntiAlias = true
        }
        val linePaint = Paint().apply {
            color = Color.rgb(200, 200, 200)
            strokeWidth = 1f
        }
        val accentPaint = Paint().apply {
            color = Color.rgb(99, 102, 241)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val datePaint = Paint().apply {
            color = Color.rgb(120, 120, 120)
            textSize = 11f
            isAntiAlias = true
        }

        var y = MARGIN + 30f

        // Title
        canvas.drawText("OmniAgent AI — Analysis Report", MARGIN, y, titlePaint)
        y += 30f

        // Horizontal rule
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 20f

        // Timestamp
        val date = SimpleDateFormat("MMM dd, yyyy  HH:mm:ss", Locale.getDefault())
            .format(Date(log.timestamp))
        canvas.drawText("Date: $date", MARGIN, y, datePaint)
        y += 16f
        canvas.drawText("Role: ${log.userRole.replaceFirstChar { it.uppercase() }}", MARGIN, y, datePaint)
        y += 30f

        // Classification section
        canvas.drawText("CLASSIFICATION", MARGIN, y, headerPaint)
        y += 22f
        canvas.drawText("Module: ${log.classifiedModule.replaceFirstChar { it.uppercase() }}", MARGIN + 16f, y, bodyPaint)
        y += 18f
        canvas.drawText("Confidence: ${String.format("%.1f", log.confidence * 100)}%", MARGIN + 16f, y, bodyPaint)
        y += 18f
        canvas.drawText("Confidence Level: ${log.confidenceLevel}", MARGIN + 16f, y, bodyPaint)
        y += 30f

        // User Input section
        canvas.drawText("USER INPUT", MARGIN, y, headerPaint)
        y += 22f
        val inputLines = wrapText(log.userInput, bodyPaint, PAGE_WIDTH - 2 * MARGIN - 32f)
        inputLines.forEach { line ->
            canvas.drawText(line, MARGIN + 16f, y, bodyPaint)
            y += 16f
        }
        y += 14f

        // Analysis Result section
        canvas.drawText("ANALYSIS RESULT", MARGIN, y, headerPaint)
        y += 22f
        val resultLines = wrapText(decryptedResult.take(1200), bodyPaint, PAGE_WIDTH - 2 * MARGIN - 32f)
        resultLines.forEach { line ->
            if (y < PAGE_HEIGHT - MARGIN - 60f) {
                canvas.drawText(line, MARGIN + 16f, y, bodyPaint)
                y += 16f
            }
        }

        // Footer
        y = PAGE_HEIGHT - MARGIN - 20f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
        y += 16f
        canvas.drawText("Generated by OmniAgent AI Platform • Fully Offline • open-source", MARGIN, y, datePaint)
    }

    private fun drawWatermark(canvas: Canvas) {
        val watermarkPaint = Paint().apply {
            color = Color.argb(25, 99, 102, 241)
            textSize = 42f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC)
            isAntiAlias = true
        }
        canvas.save()
        canvas.rotate(-35f, PAGE_WIDTH / 2f, PAGE_HEIGHT / 2f)
        canvas.drawText(WATERMARK_TEXT, 60f, PAGE_HEIGHT / 2f, watermarkPaint)
        canvas.restore()
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ", "\n")
        val lines = mutableListOf<String>()
        var currentLine = ""

        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = testLine
            } else {
                if (currentLine.isNotEmpty()) lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }
}
