package com.recuperavc.ui.main.export

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import com.recuperavc.models.MotionReport
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.relations.AudioReportWithFiles
import com.recuperavc.ui.main.ReportTab
import com.recuperavc.ui.theme.GreenDark
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant

@Composable
fun ExportPdfButton(
    tab: ReportTab,
    startDate: Instant,
    endDate: Instant,
    audioReports: List<AudioReportWithFiles>,
    coherenceReports: List<CoherenceReport>,
    motionReports: List<MotionReport>,
    onTapSound: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    Button(
        onClick = {
            onTapSound?.invoke()

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED) {
                    storageLauncher.launch(perm)
                    return@Button
                }
            }
            if (Build.VERSION.SDK_INT >= 33) {
                val perm = Manifest.permission.POST_NOTIFICATIONS
                if (ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED) {
                    notifLauncher.launch(perm)
                }
            }

            scope.launch(Dispatchers.IO) {
                val uri = exportReportsToPdf(ctx, tab, startDate, endDate, audioReports, coherenceReports, motionReports)
                withContext(Dispatchers.Main) {
                    if (uri != null) {
                        val fileName = buildReportFileName(tab, startDate, endDate)
                        com.recuperavc.util.ExportNotification.notifyPdfSaved(ctx, uri, fileName)
                        Toast.makeText(ctx, "PDF salvo em Downloads/RecuperAVC.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(ctx, "Falha ao salvar PDF.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.Default.Description, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text("Exportar PDF", color = Color.White, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
    }
}

fun exportReportsToPdf(
    context: android.content.Context,
    tab: ReportTab,
    startDate: java.time.Instant,
    endDate: java.time.Instant,
    audioReports: List<com.recuperavc.models.relations.AudioReportWithFiles>,
    coherenceReports: List<com.recuperavc.models.CoherenceReport>,
    motionReports: List<com.recuperavc.models.MotionReport>
): android.net.Uri? {
    val doc = android.graphics.pdf.PdfDocument()

    val pageW = 595
    val pageH = 842
    val margin = 32

    var pageNo = 1
    var y = margin

    var currentPage: android.graphics.pdf.PdfDocument.Page? = null
    var canvas: android.graphics.Canvas? = null

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
        color = android.graphics.Color.rgb(34, 99, 57)
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

    fun java.time.Instant.format(pattern: String): String {
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
                    val minDate = r.files.minOfOrNull { it.recordedAt ?: java.time.Instant.EPOCH } ?: java.time.Instant.EPOCH
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

    val fileName = buildString {
        append("RecuperAVC_")
        append(
            when (tab) {
                ReportTab.Audio -> "Voz_"
                ReportTab.Coherence -> "Raciocinio_"
                ReportTab.Motion -> "Coordenacao_"
            }
        )
        append("${startDate.format("yyyyMMdd")}-${endDate.format("yyyyMMdd")}.pdf")
    }

    return try {
        val resultUri: android.net.Uri?
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_DOWNLOADS + "/RecuperAVC"
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
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
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
    startDate: java.time.Instant,
    endDate: java.time.Instant
): String {
    fun java.time.Instant.fmt(p: String): String {
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
