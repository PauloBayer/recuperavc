package com.recuperavc.ui.main.export

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.MotionReport
import com.recuperavc.models.relations.AudioReportWithFiles
import com.recuperavc.ui.main.reports.type.ReportTab
import java.time.Instant

/**
 * Standalone PDF export helpers.
 * These use android.graphics.* (fully-qualified) to avoid clashes
 * with androidx.compose.ui.graphics.* Paint/Color.
 */
fun exportReportsToPdf(
    context: android.content.Context,
    tab: ReportTab,
    startDate: Instant,
    endDate: Instant,
    audioReports: List<AudioReportWithFiles>,
    coherenceReports: List<CoherenceReport>,
    motionReports: List<MotionReport>
): Uri? {
    val doc = android.graphics.pdf.PdfDocument()

    val pageW = 595  // A4 @ 72dpi: 595 x 842
    val pageH = 842
    val margin = 32

    var pageNo = 1
    var y = margin

    var currentPage: android.graphics.pdf.PdfDocument.Page? = null
    var canvas: android.graphics.Canvas? = null

    // --- Paints (android.graphics.Paint!) ---
    val titlePaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 18f
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT_BOLD,
            android.graphics.Typeface.BOLD
        )
        color = android.graphics.Color.BLACK
    }
    val subPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 12f
        color = android.graphics.Color.DKGRAY
    }
    val textPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 12f
        color = android.graphics.Color.BLACK
    }
    val greenPaint = android.graphics.Paint().apply {
        isAntiAlias = true
        textSize = 12f
        color = android.graphics.Color.rgb(34, 99, 57) // GreenDark-ish
    }

    fun newPage() {
        val info = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW, pageH, pageNo++).create()
        currentPage = doc.startPage(info)
        canvas = currentPage!!.canvas
        y = margin
    }

    fun ensureLine(height: Int = 18) {
        if (y + height > pageH - margin) {
            currentPage?.let { doc.finishPage(it) }
            newPage()
        }
        y += height
    }

    fun drawTextLine(text: String, paint: android.graphics.Paint, left: Int = margin) {
        ensureLine()
        canvas!!.drawText(text, left.toFloat(), y.toFloat(), paint)
    }

    fun Instant.format(pattern: String): String {
        val ldt = java.time.LocalDateTime.ofInstant(this, java.time.ZoneId.systemDefault())
        return java.time.format.DateTimeFormatter.ofPattern(pattern).format(ldt)
    }

    fun sectionHeader(title: String) {
        ensureLine(16)
        canvas!!.drawText(title, margin.toFloat(), y.toFloat(), greenPaint)
        ensureLine(6)
        canvas!!.drawLine(
            margin.toFloat(), y.toFloat(),
            (pageW - margin).toFloat(), y.toFloat(),
            subPaint
        )
        ensureLine(6)
    }

    // ---- Build document ----
    newPage()

    drawTextLine("RecuperAVC — Relatórios", titlePaint)
    drawTextLine("Período: ${startDate.format("dd/MM/yyyy")} até ${endDate.format("dd/MM/yyyy")}", subPaint)
    drawTextLine(
        "Aba atual: " + when (tab) {
            ReportTab.Audio -> "Voz"
            ReportTab.Coherence -> "Raciocínio"
            ReportTab.Motion -> "Coordenação"
        },
        subPaint
    )
    ensureLine(12)

    when (tab) {
        ReportTab.Audio -> {
            sectionHeader("Relatórios de Voz")
            if (audioReports.isEmpty()) {
                drawTextLine("Nenhum relatório no período.", textPaint)
            } else {
                audioReports.forEachIndexed { idx, r ->
                    val minDate = r.files.minOfOrNull { it.recordedAt ?: Instant.EPOCH } ?: Instant.EPOCH
                    drawTextLine("• #${idx + 1}  Data: ${minDate.format("dd/MM/yyyy HH:mm")}", textPaint)
                    drawTextLine(
                        "   WPM médio: ${r.report.averageWordsPerMinute.toInt()}  •  Precisão: ${
                            String.format("%.1f", 100 - r.report.averageWordErrorRate)
                        }%",
                        textPaint
                    )
                    ensureLine(6)
                }
            }
        }
        ReportTab.Coherence -> {
            sectionHeader("Relatórios de Raciocínio")
            if (coherenceReports.isEmpty()) {
                drawTextLine("Nenhum relatório no período.", textPaint)
            } else {
                coherenceReports.forEachIndexed { idx, r ->
                    drawTextLine("• #${idx + 1}  Data: ${r.date.format("dd/MM/yyyy HH:mm")}", textPaint)
                    drawTextLine(
                        "   Tempo médio: ${String.format("%.1f", r.averageTimePerTry)}s  •  Tentativas médias: ${
                            String.format("%.1f", r.averageErrorsPerTry)
                        }",
                        textPaint
                    )
                    ensureLine(6)
                }
            }
        }
        ReportTab.Motion -> {
            sectionHeader("Relatórios de Coordenação")
            if (motionReports.isEmpty()) {
                drawTextLine("Nenhum relatório no período.", textPaint)
            } else {
                motionReports.forEachIndexed { idx, r ->
                    drawTextLine("• #${idx + 1}  Data: ${r.date.format("dd/MM/yyyy HH:mm")}", textPaint)
                    drawTextLine(
                        "   Toques/min: ${r.clicksPerMinute}  •  Total: ${r.totalClicks}  •  Errados: ${r.missedClicks}",
                        textPaint
                    )
                    ensureLine(6)
                }
            }
        }
    }

    currentPage?.let { doc.finishPage(it) }

    val fileName = buildReportFileName(tab, startDate, endDate)

    return try {
        val resultUri: Uri?
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    Environment.DIRECTORY_DOWNLOADS + "/RecuperAVC"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(collection, values)

            uri?.let {
                resolver.openOutputStream(it)?.use { out -> doc.writeTo(out) }
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(it, values, null, null)
            }
            resultUri = uri
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val folder = java.io.File(dir, "RecuperAVC").apply { mkdirs() }
            val file = java.io.File(folder, fileName)

            java.io.FileOutputStream(file).use { out -> doc.writeTo(out) }

            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("application/pdf"),
                null
            )

            resultUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                context.packageName + ".provider",
                file
            )
        }
        resultUri
    } catch (_: Exception) {
        null
    } finally {
        try { doc.close() } catch (_: Exception) {}
    }
}

fun buildReportFileName(
    tab: ReportTab,
    startDate: Instant,
    endDate: Instant
): String {
    fun Instant.fmt(p: String): String {
        val ldt = java.time.LocalDateTime.ofInstant(this, java.time.ZoneId.systemDefault())
        return java.time.format.DateTimeFormatter.ofPattern(p).format(ldt)
    }
    val kind = when (tab) {
        ReportTab.Audio -> "Voz_"
        ReportTab.Coherence -> "Raciocinio_"
        ReportTab.Motion -> "Coordenacao_"
    }
    return "RecuperAVC_${kind}${startDate.fmt("yyyyMMdd")}-${endDate.fmt("yyyyMMdd")}.pdf"
}
