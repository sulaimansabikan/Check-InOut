package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.PunchLog
import com.example.data.Worker
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private val hrFormat = DecimalFormat("0.0")

    fun generateMonthlyReport(
        context: Context,
        monthYear: String, // e.g. "2026-06"
        workers: List<Worker>,
        allLogs: List<PunchLog>,
        companyName: String = "SULAIMAN INTEGRATED SERVICES",
        companyAddress: String = "No 15, Jalan Perusahaan 2, Kawasan Perindustrian,\n43000 Kajang, Selangor Darul Ehsan",
        recipientEmail: String = "sulaimansabikan@gmail.com"
    ): File? {
        val pdfDocument = PdfDocument()

        val pageWidth = 595
        val pageHeight = 842

        val targetMonthLogs = allLogs.filter { it.date.startsWith(monthYear) }

        val paintText = Paint().apply {
            color = Color.BLACK
            textSize = 10f
            isAntiAlias = true
        }
        val paintTitle = Paint().apply {
            color = Color.rgb(26, 82, 118) // Slate blue
            textSize = 18f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val paintSubTitle = Paint().apply {
            color = Color.DKGRAY
            textSize = 11f
            isAntiAlias = true
        }
        val paintBorder = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
        val paintHeaderBg = Paint().apply {
            color = Color.rgb(235, 245, 251) // Soft premium light blue
            style = Paint.Style.FILL
        }
        val paintTotalRowBg = Paint().apply {
            color = Color.rgb(242, 243, 244)
            style = Paint.Style.FILL
        }

        // --- PAGE 1: OVERVIEW REPORT ---
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Header
        canvas.drawText("PUNCH CARD RUMAH - LAPORAN BULANAN", 35f, 50f, paintTitle)
        
        val displayMonth = formatMonthHeader(monthYear)
        canvas.drawText("Bulan Laporan: $displayMonth", 35f, 70f, paintSubTitle)
        canvas.drawText("Syarikat: $companyName", 35f, 85f, paintSubTitle)
        canvas.drawText("E-mel Penghantaran: $recipientEmail", 35f, 100f, paintSubTitle)

        // Draw Divider
        canvas.drawLine(35f, 115f, pageWidth - 35f, 115f, paintBorder)

        // Title Section
        val boldPaint = Paint(paintText).apply { isFakeBoldText = true; textSize = 12f }
        canvas.drawText("RINGKASAN KEHADIRAN DAN RUMUSAN JAM KERJA", 35f, 140f, boldPaint)

        // Table headers (Worker, Total Days Present, Total Hours, Late count, Early Out count)
        val yStart = 160f
        val rowHeight = 25f
        
        // Draw Table Header Background
        canvas.drawRect(35f, yStart, pageWidth - 35f, yStart + rowHeight, paintHeaderBg)
        canvas.drawRect(35f, yStart, pageWidth - 35f, yStart + rowHeight, paintBorder)

        val headerTextPaint = Paint(paintText).apply { isFakeBoldText = true }
        canvas.drawText("Nama Pekerja", 40f, yStart + 16f, headerTextPaint)
        canvas.drawText("Hari Hadir", 220f, yStart + 16f, headerTextPaint)
        canvas.drawText("Jumlah Jam", 310f, yStart + 16f, headerTextPaint)
        canvas.drawText("Lewat (Kali)", 400f, yStart + 16f, headerTextPaint)
        canvas.drawText("Balik Awal", 490f, yStart + 16f, headerTextPaint)

        var currentY = yStart + rowHeight
        var grandTotalHours = 0.0
        var grandTotalPresence = 0

        for (worker in workers) {
            val workerLogs = targetMonthLogs.filter { it.workerId == worker.id }
            val totalMinutes = workerLogs.sumOf { it.durationMinutes }
            val totalHours = totalMinutes / 60.0
            val presenceCount = workerLogs.count { it.punchInTime != null }
            val lateCount = workerLogs.count { it.isLate }
            val earlyOutCount = workerLogs.count { it.isEarlyOut }

            grandTotalHours += totalHours
            grandTotalPresence += presenceCount

            // Draw content
            canvas.drawText(worker.name, 40f, currentY + 16f, paintText)
            canvas.drawText("$presenceCount Hari", 220f, currentY + 16f, paintText)
            canvas.drawText(hrFormat.format(totalHours) + " Jam", 310f, currentY + 16f, paintText)
            canvas.drawText("$lateCount kali", 400f, currentY + 16f, paintText)
            canvas.drawText("$earlyOutCount kali", 490f, currentY + 16f, paintText)

            canvas.drawLine(35f, currentY + rowHeight, pageWidth - 35f, currentY + rowHeight, paintBorder)
            currentY += rowHeight
        }

        // Draw Grand Total Row
        canvas.drawRect(35f, currentY, pageWidth - 35f, currentY + rowHeight, paintTotalRowBg)
        canvas.drawRect(35f, currentY, pageWidth - 35f, currentY + rowHeight, paintBorder)
        canvas.drawText("JUMLAH BESAR", 40f, currentY + 16f, boldPaint)
        canvas.drawText("$grandTotalPresence Hari", 220f, currentY + 16f, boldPaint)
        canvas.drawText(hrFormat.format(grandTotalHours) + " Jam", 310f, currentY + 16f, boldPaint)

        // Draw Rules Footer on Page 1
        val yRules = currentY + 60f
        canvas.drawLine(35f, yRules - 10f, pageWidth - 35f, yRules - 10f, paintBorder)
        canvas.drawText("PANDUAN SYARIKAT & VERIFIKASI SEKATAN:", 35f, yRules + 10f, boldPaint)
        
        val paintRules = Paint(paintText).apply { textSize = 9f; color = Color.GRAY }
        canvas.drawText("1. Punch in sebelum jam 08:00 Pagi. Punch out selepas jam 17:00 (5:00 petang).", 35f, yRules + 30f, paintRules)
        canvas.drawText("2. Sekatan rangkaian Wifi diaktifkan secara mandatori. Log di atas hanya direkodkan jika disambung ke Wifi Rumah.", 35f, yRules + 45f, paintRules)
        canvas.drawText("3. Laporan ini menjumlahkan masa bekerja paling awal masuk dan paling lambat keluar bagi setiap hari.", 35f, yRules + 60f, paintRules)

        canvas.drawText("Muka surat 1 (Ringkasan)", pageWidth / 2f - 50f, pageHeight - 30f, paintRules)

        pdfDocument.finishPage(page)

        // --- PAGES 2+: DETAIL LOGS PER WORKER ---
        for (worker in workers) {
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas

            // Header for worker detail
            canvas.drawText("LAPORAN BULANAN PEKERJA: ${worker.name.uppercase()}", 35f, 50f, paintTitle)
            canvas.drawText("Bulan: $displayMonth", 35f, 70f, paintSubTitle)
            
            canvas.drawLine(35f, 85f, pageWidth - 35f, 85f, paintBorder)

            // Table labels
            val startYTable = 110f
            canvas.drawRect(35f, startYTable, pageWidth - 35f, startYTable + rowHeight, paintHeaderBg)
            canvas.drawRect(35f, startYTable, pageWidth - 35f, startYTable + rowHeight, paintBorder)

            canvas.drawText("Tarikh", 40f, startYTable + 16f, headerTextPaint)
            canvas.drawText("Punch In", 130f, startYTable + 16f, headerTextPaint)
            canvas.drawText("Punch Out", 230f, startYTable + 16f, headerTextPaint)
            canvas.drawText("Tempoh", 330f, startYTable + 16f, headerTextPaint)
            canvas.drawText("Nota / Status", 420f, startYTable + 16f, headerTextPaint)

            var yLog = startYTable + rowHeight
            
            // Filter logs for this worker and of this year month, sorted by date ASC
            val workerLogs = targetMonthLogs.filter { it.workerId == worker.id }.sortedBy { it.date }

            if (workerLogs.isEmpty()) {
                canvas.drawText("Tiada rekod kehadiran untuk pekerja ini bagi bulan pilihan.", 50f, yLog + 30f, paintSubTitle)
            } else {
                for (log in workerLogs) {
                    canvas.drawText(log.date, 40f, yLog + 16f, paintText)
                    
                    val inTimeStr = log.punchInTime?.let {
                        ServerTimeService.formatToMalaysiaTime(it, "hh:mm:ss a")
                    } ?: "TIADA"
                    canvas.drawText(inTimeStr, 130f, yLog + 16f, paintText)

                    val outTimeStr = log.punchOutTime?.let {
                        ServerTimeService.formatToMalaysiaTime(it, "hh:mm:ss a")
                    } ?: "TIADA"
                    canvas.drawText(outTimeStr, 230f, yLog + 16f, paintText)

                    val hoursDouble = log.durationMinutes / 60.0
                    val durStr = if (log.punchInTime != null && log.punchOutTime != null) {
                        "${hrFormat.format(hoursDouble)} Jam (${log.durationMinutes}m)"
                    } else "0m"
                    canvas.drawText(durStr, 330f, yLog + 16f, paintText)

                    // Note logic
                    val notes = ArrayList<String>()
                    if (log.isLate) notes.add("Lewat datang")
                    if (log.isEarlyOut) notes.add("Pulang awal")
                    if (log.punchInTime != null && log.punchOutTime == null) notes.add("Belum punch-out")
                    if (log.punchInTime == null && log.punchOutTime == null) notes.add("Tiada rekod")

                    val noteStr = if (notes.isNotEmpty()) notes.joinToString(", ") else "Lengkap"
                    val paintStatus = Paint(paintText).apply {
                        color = if (log.isLate || log.isEarlyOut) Color.rgb(203, 67, 53) else Color.rgb(39, 174, 96)
                    }
                    canvas.drawText(noteStr, 420f, yLog + 16f, paintStatus)

                    canvas.drawLine(35f, yLog + rowHeight, pageWidth - 35f, yLog + rowHeight, paintBorder)
                    yLog += rowHeight
                }
            }

            canvas.drawText("Muka surat $pageNumber", pageWidth / 2f - 20f, pageHeight - 30f, paintRules)
            pdfDocument.finishPage(page)
        }

        return try {
            val fileName = "Laporan_Kehadiran_PunchCard_${monthYear.replace("-", "_")}.pdf"
            val cachePath = File(context.cacheDir, "reports")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            val pdfFile = File(cachePath, fileName)
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun formatMonthHeader(monthYear: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val formatter = SimpleDateFormat("MMMM yyyy", Locale("ms", "MY"))
            val date = parser.parse(monthYear)
            if (date != null) formatter.format(date) else monthYear
        } catch (e: Exception) {
            monthYear
        }
    }

    fun shareReportEmail(context: Context, pdfFile: File, monthYear: String, recipientEmail: String = "sulaimansabikan@gmail.com") {
        val authority = "${context.packageName}.fileprovider"
        try {
            val contentUri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)
            val displayMonth = formatMonthHeader(monthYear)

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail))
                putExtra(Intent.EXTRA_SUBJECT, "Laporan Kehadiran Bulanan - $displayMonth")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Salam Pengurus,\n\n" +
                            "Dilampirkan laporan kehadiran bulanan kakitangan bagi bulan $displayMonth.\n\n" +
                            "Laporan ini menunjukkan jumlah jam kerja, hari hadir bertugas serta status lewat masuk atau pulang awal bagi setiap pekerja.\n\n" +
                            "Maklumat Ringkasan Fail:\n" +
                            "• Bilangan Pekerja: Berdasarkan pendaftaran aktif\n" +
                            "• Pemfailan: PDF Automatik\n" +
                            "• Sekatan Sambungan: WiFi Rumah\n\n" +
                            "Sila semak lampiran fail PDF untuk butiran penuh.\n\n" +
                            "Terima kasih.\n\nAplikasi Punch Card Rumah"
                )
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(emailIntent, "Hantar Laporan PDF ke Email..."))

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Gagal berkongsi PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
