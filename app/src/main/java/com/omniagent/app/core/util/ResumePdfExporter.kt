package com.omniagent.app.core.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ResumePdfExporter {
    private const val TAG = "ResumePdfExporter"

    fun exportResume(context: Context, data: com.omniagent.app.core.model.ResumeData) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        
        val paint = Paint()
        var yPos = 50f

        // Template Configuration
        val (headerColor, accentColor, showSidebar) = when(data.templateId) {
            1 -> Triple(Color.parseColor("#3B82F6"), Color.parseColor("#93C5FD"), false) // Modern
            2 -> Triple(Color.parseColor("#8B5CF6"), Color.parseColor("#C4B5FD"), false) // Creative
            3 -> Triple(Color.parseColor("#1F2937"), Color.parseColor("#D1D5DB"), true)  // Executive
            else -> Triple(Color.BLACK, Color.GRAY, false) // Basic
        }

        // Draw Header
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 24f
        paint.color = headerColor
        canvas.drawText(data.fullName.ifBlank { "Professional Resume" }, 50f, yPos, paint)
        
        yPos += 30f
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = Color.DKGRAY
        canvas.drawText("${data.email} | ${data.phone}", 50f, yPos, paint)
        
        // Draw Separator
        yPos += 20f
        paint.color = accentColor
        paint.strokeWidth = 2f
        canvas.drawLine(50f, yPos, 545f, yPos, paint)
        
        // Draw Sections
        yPos += 40f
        paint.color = Color.BLACK
        if (data.jobTitle.isNotBlank()) {
            drawSection(canvas, paint, "EXPERIENCE: ${data.jobTitle}", data.experienceDescription, 50f, yPos, headerColor).also { yPos = it }
            yPos += 20f
        }
        
        if (data.education.isNotBlank()) {
            drawSection(canvas, paint, "EDUCATION", data.education, 50f, yPos, headerColor).also { yPos = it }
            yPos += 20f
        }

        if (data.skills.isNotBlank()) {
            drawSection(canvas, paint, "TECHNICAL SKILLS", data.skills, 50f, yPos, headerColor).also { yPos = it }
        }

        pdfDocument.finishPage(page)

        val fileName = "Beast_Resume_${System.currentTimeMillis()}.pdf"
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                
                val resolver = context.contentResolver
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        pdfDocument.writeTo(outputStream)
                    }
                    Log.i(TAG, "Resume exported successfully via MediaStore: $fileName")
                    android.widget.Toast.makeText(context, "Saved to Downloads: $fileName", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    throw IOException("Failed to create MediaStore entry")
                }
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                pdfDocument.writeTo(FileOutputStream(file))
                Log.i(TAG, "Resume exported successfully to ${file.absolutePath}")
                android.widget.Toast.makeText(context, "Saved to Downloads: $fileName", android.widget.Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing PDF: ${e.message}")
            android.widget.Toast.makeText(context, "Export Failed: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }

    private fun drawSection(canvas: Canvas, paint: Paint, title: String, content: String, x: Float, y: Float, headerColor: Int): Float {
        var currentY = y
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 14f
        paint.color = headerColor
        canvas.drawText(title, x, currentY, paint)
        
        currentY += 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 11f
        paint.color = Color.BLACK
        
        // Slightly better text wrapping
        val maxWidth = 500f
        val words = content.split(" ", "\n")
        var currentLine = StringBuilder()
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val textWidth = paint.measureText(testLine)
            
            if (textWidth > maxWidth || word.contains("\n")) {
                canvas.drawText(currentLine.toString(), x, currentY, paint)
                currentY += 15f
                currentLine = StringBuilder(word.replace("\n", ""))
            } else {
                currentLine = StringBuilder(testLine)
            }
        }
        
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine.toString(), x, currentY, paint)
            currentY += 15f
        }
        
        return currentY
    }
}
