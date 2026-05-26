package com.recuperavc.ui.main.export

import android.content.ContentValues
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.MotionReport
import com.recuperavc.models.relations.AudioReportWithFiles
import com.recuperavc.ui.main.reports.components.audio.parseAudioReportDetails
import com.recuperavc.ui.main.reports.coherence.parseCoherenceReportGroups
import com.recuperavc.ui.main.reports.type.ReportTab
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val PAGE_W = 595
private const val PAGE_H = 842
private const val MARGIN = 32f
private const val HEADER_H = 88f
private const val FOOTER_H = 28f
private const val MAX_BARS_PER_CHART = 12

private val COLOR_GREEN_DARK = Color.rgb(0x68, 0x9F, 0x38)
private val COLOR_GREEN_LIGHT = Color.rgb(0x8B, 0xC3, 0x4A)
private val COLOR_WHITE = Color.WHITE
private val COLOR_TEXT_PRIMARY = Color.rgb(0x1B, 0x1B, 0x1B)
private val COLOR_TEXT_SECONDARY = Color.rgb(0x55, 0x55, 0x55)
private val COLOR_TEXT_LABEL = Color.rgb(0x77, 0x77, 0x77)
private val COLOR_BG_SOFT = Color.rgb(0xF7, 0xF7, 0xF7)
private val COLOR_BORDER = Color.rgb(0xE0, 0xE0, 0xE0)
private val COLOR_BORDER_SOFT = Color.rgb(0xEC, 0xEC, 0xEC)
private val COLOR_GRID = Color.rgb(0xEA, 0xEA, 0xEA)

private fun paint(
    size: Float,
    color: Int = COLOR_TEXT_PRIMARY,
    bold: Boolean = false,
    align: Paint.Align = Paint.Align.LEFT
) = Paint().apply {
    isAntiAlias = true
    textSize = size
    this.color = color
    typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
    textAlign = align
}

private fun fillPaint(color: Int, stroke: Boolean = false, strokeWidth: Float = 1f) = Paint().apply {
    isAntiAlias = true
    this.color = color
    style = if (stroke) Paint.Style.STROKE else Paint.Style.FILL
    this.strokeWidth = strokeWidth
}

private fun Instant.fmt(pattern: String): String {
    val ldt = LocalDateTime.ofInstant(this, ZoneId.systemDefault())
    return DateTimeFormatter.ofPattern(pattern).format(ldt)
}

private fun tabTitle(tab: ReportTab): String = when (tab) {
    ReportTab.Audio -> "Voz"
    ReportTab.Coherence -> "Raciocínio"
    ReportTab.Motion -> "Coordenação Motora"
}

private fun tabSubtitle(tab: ReportTab): String = when (tab) {
    ReportTab.Audio -> "Análise de fala — velocidade e precisão"
    ReportTab.Coherence -> "Montagem de frases — tempo e tentativas"
    ReportTab.Motion -> "Toques por minuto e total de toques"
}

private class PdfWriter(
    private val doc: PdfDocument,
    val tab: ReportTab,
    val startDate: Instant,
    val endDate: Instant
) {
    var page: PdfDocument.Page? = null
    var canvas: Canvas? = null
    var pageNo: Int = 0
    var y: Float = 0f
    val contentLeft: Float = MARGIN
    val contentRight: Float = PAGE_W - MARGIN
    val contentWidth: Float = contentRight - contentLeft
    val contentBottom: Float = PAGE_H - MARGIN - FOOTER_H

    fun startPage() {
        pageNo += 1
        val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create()
        val p = doc.startPage(info)
        page = p
        canvas = p.canvas
        drawHeader()
        y = MARGIN + HEADER_H + 18f
    }

    fun endPage() {
        canvas?.let { drawFooter(it, pageNo) }
        page?.let { doc.finishPage(it) }
        page = null
        canvas = null
    }

    fun ensureSpace(needed: Float) {
        if (y + needed > contentBottom) {
            endPage()
            startPage()
        }
    }

    private fun drawHeader() {
        val c = canvas ?: return
        c.drawRect(0f, 0f, PAGE_W.toFloat(), HEADER_H + MARGIN - 6f, fillPaint(COLOR_GREEN_DARK))
        c.drawRect(0f, HEADER_H + MARGIN - 6f, PAGE_W.toFloat(), HEADER_H + MARGIN, fillPaint(COLOR_GREEN_LIGHT))

        c.drawText("RecuperAVC", MARGIN, 38f, paint(20f, COLOR_WHITE, bold = true))
        c.drawText("Relatório de ${tabTitle(tab)}", MARGIN, 60f, paint(13f, COLOR_WHITE))
        c.drawText(tabSubtitle(tab), MARGIN, 78f, paint(10.5f, Color.argb(220, 255, 255, 255)))

        val period = "${startDate.fmt("dd/MM/yyyy")} – ${endDate.fmt("dd/MM/yyyy")}"
        c.drawText("Período", contentRight, 38f, paint(10f, Color.argb(220, 255, 255, 255), align = Paint.Align.RIGHT))
        c.drawText(period, contentRight, 56f, paint(13f, COLOR_WHITE, bold = true, align = Paint.Align.RIGHT))
    }

    private fun drawFooter(c: Canvas, no: Int) {
        c.drawLine(MARGIN, (PAGE_H - MARGIN - FOOTER_H + 2f), contentRight,
            (PAGE_H - MARGIN - FOOTER_H + 2f), fillPaint(COLOR_BORDER, stroke = true, strokeWidth = 0.6f))
        c.drawText("RecuperAVC — Relatório de ${tabTitle(tab)}",
            MARGIN, PAGE_H - MARGIN - 8f,
            paint(9f, COLOR_TEXT_LABEL))
        c.drawText("Página $no", contentRight, PAGE_H - MARGIN - 8f,
            paint(9f, COLOR_TEXT_LABEL, align = Paint.Align.RIGHT))
    }
}

private fun drawSectionTitle(w: PdfWriter, title: String, subtitle: String?) {
    val c = w.canvas ?: return
    c.drawText(title, w.contentLeft, w.y + 16f, paint(16f, COLOR_TEXT_PRIMARY, bold = true))
    w.y += 22f
    if (!subtitle.isNullOrBlank()) {
        c.drawText(subtitle, w.contentLeft, w.y + 12f, paint(11f, COLOR_TEXT_SECONDARY))
        w.y += 16f
    }
    c.drawLine(w.contentLeft, w.y + 4f, w.contentRight, w.y + 4f,
        fillPaint(COLOR_BORDER_SOFT, stroke = true, strokeWidth = 1f))
    w.y += 14f
}

private fun drawEmptyState(w: PdfWriter, message: String) {
    val c = w.canvas ?: return
    val h = 60f
    w.ensureSpace(h + 8f)
    val rect = RectF(w.contentLeft, w.y, w.contentRight, w.y + h)
    c.drawRoundRect(rect, 14f, 14f, fillPaint(COLOR_BG_SOFT))
    c.drawRoundRect(rect, 14f, 14f, fillPaint(COLOR_BORDER, stroke = true, strokeWidth = 1f))
    c.drawText(message, (w.contentLeft + w.contentRight) / 2f, w.y + h / 2f + 5f,
        paint(12f, COLOR_TEXT_SECONDARY, align = Paint.Align.CENTER))
    w.y += h + 12f
}

private fun fmtValue(v: Float, isPercent: Boolean = false, isInt: Boolean = false): String {
    if (isPercent) return String.format("%.0f%%", v)
    if (isInt) return v.toInt().toString()
    return if (v >= 10f) v.toInt().toString() else String.format("%.1f", v)
}

private fun drawChartCard(
    w: PdfWriter,
    title: String,
    subtitle: String,
    yAxisLabel: String,
    points: List<Float>,
    labels: List<String>,
    barColor: Int,
    valueFormatter: (Float) -> String
) {
    val plotHeight = 150f
    val headerHeight = 70f
    val xLabelHeight = 22f
    val padding = 14f
    val blockHeight = headerHeight + plotHeight + xLabelHeight + padding * 2f
    w.ensureSpace(blockHeight + 14f)

    val c = w.canvas ?: return
    val cardRect = RectF(w.contentLeft, w.y, w.contentRight, w.y + blockHeight)

    c.drawRoundRect(cardRect, 14f, 14f, fillPaint(COLOR_WHITE))
    c.drawRoundRect(cardRect, 14f, 14f, fillPaint(COLOR_BORDER, stroke = true, strokeWidth = 1f))

    val innerLeft = cardRect.left + padding
    val innerRight = cardRect.right - padding
    val innerTop = cardRect.top + padding

    c.drawText(title, innerLeft, innerTop + 14f, paint(14f, COLOR_TEXT_PRIMARY, bold = true))
    c.drawText(subtitle, innerLeft, innerTop + 32f, paint(10.5f, COLOR_TEXT_LABEL))
    c.drawText(yAxisLabel, innerLeft, innerTop + 58f, paint(10f, COLOR_GREEN_DARK, bold = true))

    val plotLeft = innerLeft + 40f
    val plotRight = innerRight - 4f
    val plotTop = innerTop + headerHeight
    val plotBottom = plotTop + plotHeight
    val plotWidth = plotRight - plotLeft

    val maxRaw = points.maxOrNull() ?: 0f
    val maxY = if (maxRaw <= 0f) 1f else maxRaw * 1.1f
    val ticks = listOf(1f, 0.75f, 0.5f, 0.25f, 0f)

    val tickTextPaint = paint(8.5f, COLOR_TEXT_LABEL, align = Paint.Align.RIGHT)
    val gridPaint = fillPaint(COLOR_GRID, stroke = true, strokeWidth = 0.6f)

    ticks.forEach { t ->
        val yLine = plotTop + (1f - t) * plotHeight
        c.drawLine(plotLeft, yLine, plotRight, yLine, gridPaint)
        val value = maxY * t
        c.drawText(valueFormatter(value), plotLeft - 4f, yLine + 3f, tickTextPaint)
    }

    val barPaint = fillPaint(barColor)
    val xLabelPaint = paint(8.5f, COLOR_TEXT_SECONDARY, align = Paint.Align.CENTER)
    val valuePaint = paint(8.5f, COLOR_TEXT_PRIMARY, bold = true, align = Paint.Align.CENTER)

    val slot = if (points.isNotEmpty()) plotWidth / points.size else 0f
    val barWidth = (slot * 0.55f).coerceAtMost(26f).coerceAtLeast(6f)

    points.forEachIndexed { i, v ->
        val ratio = (v / maxY).coerceIn(0f, 1f)
        val minVisible = 0.015f
        val visibleRatio = if (v > 0f) ratio.coerceAtLeast(minVisible) else 0f
        val cx = plotLeft + slot * i + slot / 2f
        val bx = cx - barWidth / 2f
        val by = plotBottom - visibleRatio * plotHeight
        c.drawRoundRect(RectF(bx, by, bx + barWidth, plotBottom), 4f, 4f, barPaint)
        labels.getOrNull(i)?.let { lbl ->
            c.drawText(lbl, cx, plotBottom + 12f, xLabelPaint)
        }
        if (slot >= 26f) {
            c.drawText(valueFormatter(v), cx, by - 3f, valuePaint)
        }
    }

    w.y += blockHeight + 14f
}

private fun drawChartChunks(
    w: PdfWriter,
    title: String,
    subtitle: String,
    yAxisLabel: String,
    points: List<Float>,
    labels: List<String>,
    barColor: Int,
    valueFormatter: (Float) -> String
) {
    if (points.isEmpty()) return
    val total = points.size
    val chunks = (total + MAX_BARS_PER_CHART - 1) / MAX_BARS_PER_CHART
    for (idx in 0 until chunks) {
        val from = idx * MAX_BARS_PER_CHART
        val to = (from + MAX_BARS_PER_CHART).coerceAtMost(total)
        val chunkTitle = if (chunks > 1) "$title (parte ${idx + 1} de $chunks)" else title
        drawChartCard(
            w = w,
            title = chunkTitle,
            subtitle = subtitle,
            yAxisLabel = yAxisLabel,
            points = points.subList(from, to),
            labels = labels.subList(from, to),
            barColor = barColor,
            valueFormatter = valueFormatter
        )
    }
}

private fun drawKeyValueCard(
    w: PdfWriter,
    title: String,
    cells: List<Pair<String, String>>,
    accentColor: Int,
    extraLines: List<String> = emptyList()
) {
    val padding = 14f
    val titleH = 18f
    val rowH = 36f
    val rows = (cells.size + 1) / 2
    val extraH = if (extraLines.isEmpty()) 0f else extraLines.size * 14f + 8f
    val height = padding * 2f + titleH + rows * rowH + extraH

    w.ensureSpace(height + 10f)
    val c = w.canvas ?: return
    val rect = RectF(w.contentLeft, w.y, w.contentRight, w.y + height)
    c.drawRoundRect(rect, 12f, 12f, fillPaint(COLOR_BG_SOFT))
    c.drawRoundRect(rect, 12f, 12f, fillPaint(COLOR_BORDER_SOFT, stroke = true, strokeWidth = 0.8f))

    val innerLeft = rect.left + padding
    val innerRight = rect.right - padding
    val innerTop = rect.top + padding

    c.drawText(title, innerLeft, innerTop + 11f, paint(12.5f, COLOR_TEXT_PRIMARY, bold = true))

    val colWidth = (innerRight - innerLeft) / 2f
    cells.forEachIndexed { i, pair ->
        val row = i / 2
        val col = i % 2
        val cx = innerLeft + col * colWidth
        val cy = innerTop + titleH + row * rowH
        c.drawText(pair.first, cx, cy + 12f, paint(9.5f, COLOR_TEXT_LABEL, bold = true))
        c.drawText(pair.second, cx, cy + 28f, paint(13.5f, accentColor, bold = true))
    }

    var ey = innerTop + titleH + rows * rowH
    extraLines.forEach { line ->
        c.drawText(line, innerLeft, ey + 10f, paint(10f, COLOR_TEXT_SECONDARY))
        ey += 14f
    }

    w.y += height + 10f
}

private fun drawSummaryStrip(w: PdfWriter, items: List<Triple<String, String, Int>>) {
    if (items.isEmpty()) return
    val padding = 12f
    val height = 70f
    w.ensureSpace(height + 12f)
    val c = w.canvas ?: return
    val totalWidth = w.contentWidth
    val gap = 10f
    val cellWidth = (totalWidth - gap * (items.size - 1)) / items.size

    items.forEachIndexed { i, (label, value, color) ->
        val x = w.contentLeft + i * (cellWidth + gap)
        val rect = RectF(x, w.y, x + cellWidth, w.y + height)
        c.drawRoundRect(rect, 12f, 12f, fillPaint(COLOR_BG_SOFT))
        c.drawRoundRect(rect, 12f, 12f, fillPaint(COLOR_BORDER_SOFT, stroke = true, strokeWidth = 0.8f))
        c.drawText(label, rect.left + padding, rect.top + 18f,
            paint(9.5f, COLOR_TEXT_LABEL, bold = true))
        c.drawText(value, rect.left + padding, rect.top + 46f,
            paint(20f, color, bold = true))
    }
    w.y += height + 14f
}

fun exportReportsToPdf(
    context: android.content.Context,
    tab: ReportTab,
    startDate: Instant,
    endDate: Instant,
    audioReports: List<AudioReportWithFiles>,
    coherenceReports: List<CoherenceReport>,
    motionReports: List<MotionReport>
): Uri? {
    val doc = PdfDocument()
    val w = PdfWriter(doc, tab, startDate, endDate)
    w.startPage()

    when (tab) {
        ReportTab.Audio -> drawAudioSection(w, audioReports)
        ReportTab.Coherence -> drawCoherenceSection(w, coherenceReports)
        ReportTab.Motion -> drawMotionSection(w, motionReports)
    }

    w.endPage()

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

private fun drawAudioSection(w: PdfWriter, items: List<AudioReportWithFiles>) {
    if (items.isEmpty()) {
        drawSectionTitle(w, "Relatórios de Voz", "Nenhum dado no período selecionado")
        drawEmptyState(w, "Nenhum relatório encontrado.")
        return
    }

    val avgWpm = items.map { it.report.averageWordsPerMinute }.average().toFloat()
    val avgWerRaw = items.map { it.report.averageWordErrorRate }.average().toFloat()
    val avgPrecision = (100f - avgWerRaw).coerceIn(0f, 100f)

    drawSectionTitle(w, "Resumo do período", null)
    drawSummaryStrip(w, listOf(
        Triple("Testes realizados", items.size.toString(), COLOR_GREEN_DARK),
        Triple("Velocidade média", "${avgWpm.toInt()} p/min", COLOR_GREEN_DARK),
        Triple("Precisão média", "${avgPrecision.toInt()}%", COLOR_GREEN_DARK)
    ))

    drawSectionTitle(w, "Gráficos de Voz", "Toque a tendência ao longo dos testes")

    val sorted = items.sortedBy { rep ->
        rep.files.minOfOrNull { it.recordedAt ?: Instant.EPOCH } ?: Instant.EPOCH
    }
    val labels = sorted.map { ar ->
        val d = ar.files.minOfOrNull { it.recordedAt ?: Instant.EPOCH }
        if (d == null || d == Instant.EPOCH) "—" else d.fmt("dd/MM")
    }
    val velocPoints = sorted.map { it.report.averageWordsPerMinute }
    val precisionPoints = sorted.map { (100f - it.report.averageWordErrorRate).coerceIn(0f, 100f) }

    drawChartChunks(
        w = w,
        title = "Velocidade de Fala",
        subtitle = "Palavras por minuto (quanto maior, melhor)",
        yAxisLabel = "Palavras/min",
        points = velocPoints,
        labels = labels,
        barColor = COLOR_GREEN_DARK,
        valueFormatter = { fmtValue(it, isInt = true) }
    )

    drawChartChunks(
        w = w,
        title = "Precisão da Fala",
        subtitle = "Porcentagem de acerto (quanto maior, melhor)",
        yAxisLabel = "Precisão (%)",
        points = precisionPoints,
        labels = labels,
        barColor = COLOR_GREEN_LIGHT,
        valueFormatter = { fmtValue(it, isPercent = true) }
    )

    drawSectionTitle(w, "Detalhes dos testes", "Mais recentes primeiro")

    sorted.reversed().forEachIndexed { idx, r ->
        val date = r.files.minOfOrNull { it.recordedAt ?: Instant.EPOCH } ?: Instant.EPOCH
        val attempts = runCatching { parseAudioReportDetails(r.report.allTestsDescription) }.getOrNull()
        val attemptsCount = attempts?.size ?: r.files.size
        val precision = (100f - r.report.averageWordErrorRate).coerceIn(0f, 100f)
        drawKeyValueCard(
            w = w,
            title = "Teste ${sorted.size - idx} — ${if (date == Instant.EPOCH) "—" else date.fmt("dd/MM/yyyy 'às' HH:mm")}",
            cells = listOf(
                "Velocidade" to "${r.report.averageWordsPerMinute.toInt()} palavras/min",
                "Precisão" to String.format("%.1f%%", precision),
                "Tentativas" to attemptsCount.toString(),
                "Áudios gravados" to r.files.size.toString()
            ),
            accentColor = COLOR_GREEN_DARK
        )
    }
}

private fun drawCoherenceSection(w: PdfWriter, items: List<CoherenceReport>) {
    if (items.isEmpty()) {
        drawSectionTitle(w, "Relatórios de Raciocínio", "Nenhum dado no período selecionado")
        drawEmptyState(w, "Nenhum relatório encontrado.")
        return
    }

    val avgTime = items.map { it.averageTimePerTry }.average().toFloat()
    val avgTries = items.map { it.averageErrorsPerTry }.average().toFloat()
    val totalGroups = items.sumOf { runCatching { parseCoherenceReportGroups(it.allTestsDescription).size }.getOrDefault(0) }
    val correctGroups = items.sumOf {
        runCatching { parseCoherenceReportGroups(it.allTestsDescription).count { g -> g.success } }.getOrDefault(0)
    }
    val successRate = if (totalGroups > 0) correctGroups.toFloat() / totalGroups.toFloat() * 100f else 0f

    drawSectionTitle(w, "Resumo do período", null)
    drawSummaryStrip(w, listOf(
        Triple("Testes realizados", items.size.toString(), COLOR_GREEN_DARK),
        Triple("Tempo médio", String.format("%.1fs", avgTime), COLOR_GREEN_DARK),
        Triple("Taxa de acerto", "${successRate.toInt()}%", COLOR_GREEN_DARK)
    ))

    drawSectionTitle(w, "Gráficos de Raciocínio", "Acompanhamento dos testes")

    val sorted = items.sortedBy { it.date }
    val labels = sorted.map { it.date.fmt("dd/MM") }
    val timePoints = sorted.map { it.averageTimePerTry }
    val triesPoints = sorted.map { it.averageErrorsPerTry }

    drawChartChunks(
        w = w,
        title = "Tempo até acerto",
        subtitle = "Segundos por frase (quanto menor, melhor)",
        yAxisLabel = "Tempo (s)",
        points = timePoints,
        labels = labels,
        barColor = COLOR_GREEN_DARK,
        valueFormatter = { String.format("%.1fs", it) }
    )

    drawChartChunks(
        w = w,
        title = "Tentativas por frase",
        subtitle = "Média de tentativas (quanto menor, melhor)",
        yAxisLabel = "Tentativas",
        points = triesPoints,
        labels = labels,
        barColor = COLOR_GREEN_LIGHT,
        valueFormatter = { String.format("%.1f", it) }
    )

    drawSectionTitle(w, "Detalhes dos testes", "Mais recentes primeiro")

    sorted.reversed().forEachIndexed { idx, r ->
        val groups = runCatching { parseCoherenceReportGroups(r.allTestsDescription) }.getOrDefault(emptyList())
        val correct = groups.count { it.success }
        val rate = if (groups.isNotEmpty()) correct.toFloat() / groups.size.toFloat() * 100f else 0f
        drawKeyValueCard(
            w = w,
            title = "Teste ${sorted.size - idx} — ${r.date.fmt("dd/MM/yyyy 'às' HH:mm")}",
            cells = listOf(
                "Tempo médio" to String.format("%.1fs", r.averageTimePerTry),
                "Tentativas médias" to String.format("%.1f", r.averageErrorsPerTry),
                "Frases" to groups.size.toString(),
                "Taxa de acerto" to "${rate.toInt()}%"
            ),
            accentColor = COLOR_GREEN_DARK
        )
    }
}

private fun drawMotionSection(w: PdfWriter, items: List<MotionReport>) {
    if (items.isEmpty()) {
        drawSectionTitle(w, "Relatórios de Coordenação", "Nenhum dado no período selecionado")
        drawEmptyState(w, "Nenhum relatório encontrado.")
        return
    }

    val avgTpm = items.map { it.clicksPerMinute }.average().toFloat()
    val totalTouches = items.sumOf { it.totalClicks }

    drawSectionTitle(w, "Resumo do período", null)
    drawSummaryStrip(w, listOf(
        Triple("Testes realizados", items.size.toString(), COLOR_GREEN_DARK),
        Triple("TPM médio", avgTpm.toInt().toString(), COLOR_GREEN_DARK),
        Triple("Toques totais", totalTouches.toString(), COLOR_GREEN_DARK)
    ))

    drawSectionTitle(w, "Coordenação Motora", "TPM — Toques por minuto (quanto maior, melhor)")

    val sorted = items.sortedBy { it.date }
    val labels = sorted.map { it.date.fmt("dd/MM") }
    val tpmPoints = sorted.map { it.clicksPerMinute.toFloat() }

    drawChartChunks(
        w = w,
        title = "Toques por minuto (TPM)",
        subtitle = "Toques registrados por minuto (quanto maior, melhor)",
        yAxisLabel = "TPM",
        points = tpmPoints,
        labels = labels,
        barColor = COLOR_GREEN_DARK,
        valueFormatter = { fmtValue(it, isInt = true) }
    )

    drawSectionTitle(w, "Detalhes dos testes", "Mais recentes primeiro")

    sorted.reversed().forEachIndexed { idx, r ->
        val hand = if (r.withRightHand) "Direita" else "Esquerda"
        val dom = if (r.withMainHand) "Dominante" else "Não dominante"
        val mode = if (r.withMovement) "Com movimento" else "Sem movimento"
        drawKeyValueCard(
            w = w,
            title = "Teste ${sorted.size - idx} — ${r.date.fmt("dd/MM/yyyy 'às' HH:mm")}",
            cells = listOf(
                "TPM (toques/min)" to r.clicksPerMinute.toString(),
                "Total de toques" to r.totalClicks.toString(),
                "Toques errados" to r.missedClicks.toString(),
                "Duração" to String.format("%.0fs", r.secondsTotal)
            ),
            accentColor = COLOR_GREEN_DARK,
            extraLines = listOf("Mão: $hand ($dom)  •  Modo: $mode")
        )
    }
}

fun buildReportFileName(
    tab: ReportTab,
    startDate: Instant,
    endDate: Instant
): String {
    val kind = when (tab) {
        ReportTab.Audio -> "Voz_"
        ReportTab.Coherence -> "Raciocinio_"
        ReportTab.Motion -> "Coordenacao_"
    }
    return "RecuperAVC_${kind}${startDate.fmt("yyyyMMdd")}-${endDate.fmt("yyyyMMdd")}.pdf"
}
