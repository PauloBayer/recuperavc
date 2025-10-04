package com.recuperavc.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.recuperavc.data.CurrentUser
import com.recuperavc.data.db.DbProvider
import com.recuperavc.models.relations.AudioReportWithFiles
import com.recuperavc.ui.theme.BackgroundGreen
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight
import com.recuperavc.ui.theme.OnBackground
import kotlinx.coroutines.flow.map
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember(context) { DbProvider.db(context) }

    var tab by remember { mutableStateOf(ReportTab.Audio) }
    var selectedAudioReport by remember { mutableStateOf<AudioReportWithFiles?>(null) }

    val audioReports by db.audioReportDao().observeWithFilesForUser(CurrentUser.ID)
        .map { it.sortedBy { rep -> rep.files.minOfOrNull { f -> f.recordedAt } } }
        .collectAsState(initial = emptyList())
    val motionReports by db.MotionReportDao().observeForUser(CurrentUser.ID)
        .map { it.sortedBy { r -> r.date } }
        .collectAsState(initial = emptyList())

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(0f, h * 0.55f)
                        cubicTo(
                            w * 0.25f, h * 0.25f,
                            w * 0.45f, h * 0.95f,
                            w * 0.6f, h * 0.65f
                        )
                        cubicTo(
                            w * 0.8f, h * 0.35f,
                            w * 0.9f, h * 0.5f,
                            w, h * 0.4f
                        )
                        lineTo(w, 0f)
                        close()
                    }
                    drawPath(path, color = GreenLight)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = OnBackground)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Relatórios",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                SegmentedTabs(tab = tab, onTab = { tab = it })
                Spacer(Modifier.height(16.dp))

                when (tab) {
                    ReportTab.Audio -> AudioReportSection(audioReports) { selectedAudioReport = it }
                    ReportTab.Motion -> MotionReportSection(motionReports)
                }
            }
        }

        selectedAudioReport?.let { rep ->
            AudioReportDetailDialog(report = rep, onDismiss = { selectedAudioReport = null })
        }
    }
}

private enum class ReportTab { Audio, Motion }

@Composable
private fun SegmentedTabs(tab: ReportTab, onTab: (ReportTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GreenLight.copy(alpha = 0.15f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        SegButton(
            modifier = Modifier.weight(1f),
            selected = tab == ReportTab.Audio,
            icon = Icons.Default.Mic,
            text = "Voz",
            onClick = { onTab(ReportTab.Audio) }
        )
        Spacer(Modifier.width(8.dp))
        SegButton(
            modifier = Modifier.weight(1f),
            selected = tab == ReportTab.Motion,
            icon = Icons.Default.Gesture,
            text = "Tapping",
            onClick = { onTab(ReportTab.Motion) }
        )
    }
}

@Composable
private fun SegButton(modifier: Modifier = Modifier, selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    val bg = if (selected) GreenDark else Color.Transparent
    val fg = if (selected) Color.White else OnBackground
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, contentDescription = null, tint = fg)
        Spacer(Modifier.width(8.dp))
        Text(text, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AudioReportSection(items: List<AudioReportWithFiles>, onSelectReport: (AudioReportWithFiles) -> Unit) {
    val pointsWpm = items.map { it.report.averageWordsPerMinute }
    val pointsAcc = items.map { 100f - it.report.averageWordErrorRate }
    val labels = items.map { r ->
        val date = r.files.minByOrNull { it.recordedAt }?.recordedAt
        if (date != null) {
            val localDate = java.time.LocalDateTime.ofInstant(date, ZoneId.systemDefault())
            DateTimeFormatter.ofPattern("dd/MM").format(localDate)
        } else ""
    }

    if (items.isEmpty()) {
        Text(
            text = "Nenhum teste de voz realizado ainda",
            fontSize = 16.sp,
            color = OnBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(32.dp)
        )
        return
    }

    Text(
        text = "Toque nas barras para ver detalhes",
        fontSize = 16.sp,
        color = GreenDark,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))

    ChartCard(title = "Velocidade de Fala", subtitle = "Palavras por minuto (quanto maior, melhor)") {
        BarChart(points = pointsWpm, labels = labels) { idx ->
            items.getOrNull(idx)?.let { onSelectReport(it) }
        }
    }
    Spacer(Modifier.height(16.dp))
    ChartCard(title = "Precisão da Fala", subtitle = "Porcentagem de acertos (quanto maior, melhor)") {
        BarChart(points = pointsAcc, labels = labels) { idx ->
            items.getOrNull(idx)?.let { onSelectReport(it) }
        }
    }
    Spacer(Modifier.height(16.dp))
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Últimos Testes Realizados", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
            Spacer(Modifier.height(12.dp))
            items.takeLast(5).reversed().forEachIndexed { idx, r ->
                val date = r.files.minByOrNull { it.recordedAt }?.recordedAt
                val label = if (date != null) {
                    val localDate = java.time.LocalDateTime.ofInstant(date, ZoneId.systemDefault())
                    DateTimeFormatter.ofPattern("dd/MM").format(localDate)
                } else "#${idx + 1}"
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(12.dp)
                ) {
                    Text("Data: $label", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Velocidade", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            Text("${r.report.averageWordsPerMinute.toInt()} palavras/min", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Precisão", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            Text("${String.format("%.1f", 100 - r.report.averageWordErrorRate)}%", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                        }
                    }
                }
                if (idx < 4) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun AudioReportDetailDialog(report: AudioReportWithFiles, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center)
                .pointerInput(Unit) {
                    detectTapGestures { }
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Áudios do Teste",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.Black
                )
                Spacer(Modifier.height(4.dp))
                val date = report.files.minByOrNull { it.recordedAt }?.recordedAt
                if (date != null) {
                    val localDate = java.time.LocalDateTime.ofInstant(date, ZoneId.systemDefault())
                    Text(
                        text = "Realizado em ${DateTimeFormatter.ofPattern("dd/MM/yyyy").format(localDate)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(16.dp))

                if (report.files.isNotEmpty()) {
                    Text(
                        text = "Total: ${report.files.size} áudio(s)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = GreenDark
                    )
                    Spacer(Modifier.height(12.dp))

                    report.files.forEachIndexed { idx, audioFile ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Áudio ${idx + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.Black
                                    )
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = null,
                                        tint = GreenDark,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = audioFile.fileName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Duração: ${audioFile.audioDuration / 1000}s",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDark
                                )
                            }
                        }
                        if (idx < report.files.size - 1) Spacer(Modifier.height(8.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Fechar", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun MotionReportSection(items: List<com.recuperavc.models.MotionReport>) {
    val points = items.map { it.clicksPerMinute.toFloat() }
    val labels = items.map { r ->
        val localDate = java.time.LocalDateTime.ofInstant(r.date, ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("dd/MM").format(localDate)
    }

    ChartCard(title = "Coordenação Motora", subtitle = "Toques por minuto (quanto maior, melhor)") {
        BarChart(points = points, labels = labels, onBarClick = null)
    }
    Spacer(Modifier.height(12.dp))
    if (items.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Últimos Testes Realizados", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(Modifier.height(12.dp))
                items.takeLast(5).reversed().forEachIndexed { idx, r ->
                    val localDate = java.time.LocalDateTime.ofInstant(r.date, ZoneId.systemDefault())
                    val label = DateTimeFormatter.ofPattern("dd/MM").format(localDate)
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(12.dp)
                    ) {
                        Text("Data: $label", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Toques/Minuto", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                Text("${r.clicksPerMinute}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total de Toques", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                Text("${r.totalClicks}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Toques Errados", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                Text("${r.missedClicks}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (r.missedClicks > 3) Color.Red else GreenDark)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Duração Total", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                Text("${String.format("%.1f", r.secondsTotal)}s", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                            }
                        }
                    }
                    if (idx < 4) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                    Text(
                        text = subtitle,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = GreenDark, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) { content() }
        }
    }
}

@Composable
private fun BarChart(points: List<Float>, labels: List<String>, onBarClick: ((Int) -> Unit)?) {
    val maxY = (points.maxOrNull() ?: 1f).coerceAtLeast(1f) * 1.2f
    val minY = 0f
    var barRects by remember(points.size) { mutableStateOf(emptyList<androidx.compose.ui.geometry.Rect>()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .then(
                    if (onBarClick != null) {
                        Modifier.pointerInput(Unit) {
                            detectTapGestures { pos ->
                                barRects.forEachIndexed { idx, rect ->
                                    if (rect.contains(pos)) {
                                        onBarClick(idx)
                                    }
                                }
                            }
                        }
                    } else Modifier
                )
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val paddingH = 20f
                val paddingTop = 8f
                val paddingBottom = 8f
                val innerW = w - paddingH * 2
                val innerH = h - paddingTop - paddingBottom
                val n = points.size.coerceAtLeast(1)
                val stepX = innerW / n
                val rects = mutableListOf<androidx.compose.ui.geometry.Rect>()

                for (idx in points.indices) {
                    val v = points[idx]
                    val x = paddingH + idx * stepX
                    val barW = (stepX * 0.65f).coerceAtMost(60f)
                    val barHeight = ((v - minY) / (maxY - minY)) * innerH
                    val y = paddingTop + innerH - barHeight
                    val centerX = x + stepX / 2f - barW / 2f
                    val rect = androidx.compose.ui.geometry.Rect(
                        androidx.compose.ui.geometry.Offset(centerX, y),
                        androidx.compose.ui.geometry.Size(barW, barHeight)
                    )
                    rects.add(rect)

                    drawRoundRect(
                        color = GreenDark,
                        topLeft = rect.topLeft,
                        size = rect.size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                    )
                }
                barRects = rects
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SimpleChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GreenLight.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = GreenDark, fontSize = 12.sp)
    }
}

private data class Attempt(val phrase: String, val wpm: Int, val wer: Float)

private fun parseAudioReportDetails(desc: String): List<Attempt> {
    return try {
        val root = org.json.JSONObject(desc)
        val arr = root.optJSONArray("attempts") ?: return emptyList()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(Attempt(
                    phrase = o.optString("phrase"),
                    wpm = o.optInt("wpm"),
                    wer = o.optDouble("wer").toFloat()
                ))
            }
        }
    } catch (e: Exception) {
        emptyList()
    }
}
