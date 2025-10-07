package com.recuperavc.ui.main

import android.app.DatePickerDialog
import android.media.MediaPlayer
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recuperavc.R
import com.recuperavc.data.db.DbProvider
import com.recuperavc.models.relations.AudioReportWithFiles
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight
import com.recuperavc.ui.theme.OnBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar

enum class ChartType { WPM, WER }

@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember(context) { DbProvider.db(context) }

    var tab by remember { mutableStateOf(ReportTab.Audio) }
    var selectedAudioReport by remember { mutableStateOf<Pair<AudioReportWithFiles, ChartType>?>(null) }

    val now = remember { Instant.now() }
    val defaultStartDate = remember { now.minus(7, ChronoUnit.DAYS) }
    var startDate by remember { mutableStateOf(defaultStartDate) }
    var endDate by remember { mutableStateOf(now) }
    var isDateManuallySet by remember { mutableStateOf(false) }

    val rawAudioReports by db.audioReportDao()
        .observeAllWithFiles()
        .map { list ->
            list.filter { parseAudioReportDetails(it.report.allTestsDescription).isNotEmpty() }
        }
        .collectAsState(initial = emptyList())

    val audioReports = remember(rawAudioReports, startDate, endDate) {
        rawAudioReports
            .filter { rep ->
                val minDate = rep.files.minOfOrNull { it.recordedAt ?: Instant.EPOCH } ?: Instant.EPOCH
                minDate >= startDate && minDate <= endDate
            }
            .sortedBy { rep ->
                rep.files.minOfOrNull { it.recordedAt ?: Instant.EPOCH } ?: Instant.EPOCH
            }
    }

    val rawMotionReports by db.MotionReportDao()
        .observeAll()
        .map { it.sortedBy { r -> r.date } }
        .collectAsState(initial = emptyList())

    val motionReports = remember(rawMotionReports, startDate, endDate) {
        rawMotionReports.filter { it.date >= startDate && it.date <= endDate }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            // Header
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                Image(
                    painter = painterResource(id = R.drawable.wave_green),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
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

            // Título + Tabs
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp),
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
            }

            // Conteúdo scrollável
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DateFilterCard(
                    startDate = startDate,
                    endDate = endDate,
                    isManuallySet = isDateManuallySet,
                    onStartDateChange = {
                        startDate = it
                        isDateManuallySet = true
                    },
                    onEndDateChange = {
                        endDate = it
                        isDateManuallySet = true
                    }
                )
                Spacer(Modifier.height(16.dp))

                when (tab) {
                    ReportTab.Audio -> AudioReportSection(audioReports) { report, chartType ->
                        selectedAudioReport = report to chartType
                    }
                    ReportTab.Motion -> MotionReportSection(motionReports)
                }
            }
        }

        selectedAudioReport?.let { (rep, chartType) ->
            AudioReportDetailDialog(
                report = rep,
                chartType = chartType,
                onDismiss = { selectedAudioReport = null }
            )
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
            text = "Coordenação",
            onClick = { onTab(ReportTab.Motion) }
        )
    }
}

@Composable
private fun SegButton(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
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
private fun DateFilterCard(
    startDate: Instant,
    endDate: Instant,
    isManuallySet: Boolean,
    onStartDateChange: (Instant) -> Unit,
    onEndDateChange: (Instant) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Filtro de Data",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.Black
            )
            Spacer(Modifier.height(8.dp))
            if (!isManuallySet) {
                Text(
                    text = "Mostrando dados dos últimos 7 dias",
                    fontSize = 14.sp,
                    color = Color.Black.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(12.dp))
            } else {
                Spacer(Modifier.height(4.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Início", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .clickable {
                                val cal = Calendar.getInstance()
                                val localDateTime = LocalDateTime.ofInstant(startDate, ZoneId.systemDefault())
                                cal.set(localDateTime.year, localDateTime.monthValue - 1, localDateTime.dayOfMonth)

                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val selected = LocalDateTime.of(year, month + 1, day, 0, 0)
                                        onStartDateChange(selected.atZone(ZoneId.systemDefault()).toInstant())
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(
                                LocalDateTime.ofInstant(startDate, ZoneId.systemDefault())
                            ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenDark
                        )
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = GreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Fim", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                            .clickable {
                                val cal = Calendar.getInstance()
                                val localDateTime = LocalDateTime.ofInstant(endDate, ZoneId.systemDefault())
                                cal.set(localDateTime.year, localDateTime.monthValue - 1, localDateTime.dayOfMonth)

                                DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val selected = LocalDateTime.of(year, month + 1, day, 23, 59, 59)
                                        onEndDateChange(selected.atZone(ZoneId.systemDefault()).toInstant())
                                    },
                                    cal.get(Calendar.YEAR),
                                    cal.get(Calendar.MONTH),
                                    cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(
                                LocalDateTime.ofInstant(endDate, ZoneId.systemDefault())
                            ),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenDark
                        )
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = GreenDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioReportSection(
    items: List<AudioReportWithFiles>,
    onSelectReport: (AudioReportWithFiles, ChartType) -> Unit
) {
    val pointsWpm = items.map { it.report.averageWordsPerMinute }
    val pointsWer = items.map { it.report.averageWordErrorRate }
    val labels = items.map { r ->
        val date = r.files.minByOrNull { it.recordedAt ?: Instant.EPOCH }?.recordedAt
        if (date != null) {
            val localDate = LocalDateTime.ofInstant(date, ZoneId.systemDefault())
            DateTimeFormatter.ofPattern("dd/MM").format(localDate)
        } else ""
    }

    if (items.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Nenhum relatório encontrado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
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
        BarChart(
            points = pointsWpm,
            labels = labels,
            yAxisLabel = "WPM",
            onBarClick = { idx -> items.getOrNull(idx)?.let { onSelectReport(it, ChartType.WPM) } }
        )
    }
    Spacer(Modifier.height(16.dp))
    ChartCard(title = "Erros de Fala", subtitle = "Porcentagem de erro (quanto menor, melhor)") {
        BarChart(
            points = pointsWer,
            labels = labels,
            yAxisLabel = "WER (%)",
            onBarClick = { idx -> items.getOrNull(idx)?.let { onSelectReport(it, ChartType.WER) } }
        )
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
                val date = r.files.minByOrNull { it.recordedAt ?: Instant.EPOCH }?.recordedAt
                val label = if (date != null) {
                    val localDate = LocalDateTime.ofInstant(date, ZoneId.systemDefault())
                    DateTimeFormatter.ofPattern("dd/MM").format(localDate)
                } else "#${idx + 1}"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
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
private fun AudioReportDetailDialog(
    report: AudioReportWithFiles,
    chartType: ChartType,
    onDismiss: () -> Unit
) {
    val attempts = remember(report.report.allTestsDescription) {
        parseAudioReportDetails(report.report.allTestsDescription)
    }
    val attemptsByFileId = remember(attempts) { attempts.associateBy { it.fileId } }

    var currentlyPlayingIndex by remember { mutableStateOf<Int?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) {
                // click fora fecha
                detectTapGestures { onDismiss() }
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center)
                .pointerInput(Unit) { /* captura toques internos */ },
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
                val filesSorted = remember(report.files) { report.files.sortedBy { it.recordedAt } }
                val date = filesSorted.minByOrNull { it.recordedAt }?.recordedAt
                if (date != null) {
                    val localDate = LocalDateTime.ofInstant(date, ZoneId.systemDefault())
                    Text(
                        text = "Realizado em ${DateTimeFormatter.ofPattern("dd/MM/yyyy").format(localDate)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
                Spacer(Modifier.height(16.dp))

                if (filesSorted.isNotEmpty()) {
                    Text(
                        text = "Total: ${filesSorted.size} áudio(s)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = GreenDark
                    )
                    Spacer(Modifier.height(12.dp))

                    filesSorted.forEachIndexed { idx, audioFile ->
                        val attempt = attemptsByFileId[audioFile.id.toString()] ?: attempts.getOrNull(idx)

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
                                    IconButton(
                                        onClick = {
                                            if (currentlyPlayingIndex == idx) {
                                                mediaPlayer?.stop()
                                                mediaPlayer?.release()
                                                mediaPlayer = null
                                                currentlyPlayingIndex = null
                                            } else {
                                                mediaPlayer?.stop()
                                                mediaPlayer?.release()
                                                val file = File(audioFile.path)
                                                if (file.exists()) {
                                                    mediaPlayer = MediaPlayer().apply {
                                                        setDataSource(audioFile.path)
                                                        prepare()
                                                        setOnCompletionListener {
                                                            currentlyPlayingIndex = null
                                                        }
                                                        start()
                                                    }
                                                    currentlyPlayingIndex = idx
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            if (currentlyPlayingIndex == idx) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = GreenDark,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
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
                                    text = "Duração: ${(audioFile.audioDuration / 1000)}s",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDark
                                )
                            }
                        }
                        if (idx < report.files.size - 1) Spacer(Modifier.height(8.dp))

                        if (currentlyPlayingIndex == idx) {
                            SimpleWaveform(modifier = Modifier.fillMaxWidth().height(60.dp))
                            Spacer(Modifier.height(8.dp))
                        }

                        if (attempt != null && (attempt.phrase.isNotEmpty() || attempt.transcribed.isNotEmpty())) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(12.dp)
                            ) {
                                if (attempt.phrase.isNotEmpty()) {
                                    Text(
                                        "Esperado:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        attempt.phrase,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                if (attempt.transcribed.isNotEmpty()) {
                                    Text(
                                        "Transcrito:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        attempt.transcribed,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = GreenDark
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Duração", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                Text(
                                    "${audioFile.audioDuration / 1000}s",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GreenDark
                                )
                            }
                            if (attempt != null) {
                                when (chartType) {
                                    ChartType.WPM -> {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("WPM", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                            Text("${attempt.wpm}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                                        }
                                    }
                                    ChartType.WER -> {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("WER", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                                            Text("${String.format("%.1f", attempt.wer)}%", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = GreenDark)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        mediaPlayer?.stop()
                        mediaPlayer?.release()
                        mediaPlayer = null
                        currentlyPlayingIndex = null
                        onDismiss()
                    },
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
private fun SimpleWaveform(modifier: Modifier = Modifier) {
    var phase by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(50)
            phase += 0.3f
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2f
        val amplitude = h * 0.3f
        val numBars = 40
        val barWidth = w / numBars

        for (i in 0 until numBars) {
            val x = i * barWidth
            val offset = (i * 0.5f + phase) % 6.28f
            val barHeight = amplitude * kotlin.math.sin(offset) * 0.5f + amplitude * 0.5f
            drawRect(
                color = GreenDark,
                topLeft = androidx.compose.ui.geometry.Offset(x + barWidth * 0.2f, centerY - barHeight / 2),
                size = androidx.compose.ui.geometry.Size(barWidth * 0.6f, barHeight)
            )
        }
    }
}

@Composable
private fun MotionReportSection(items: List<com.recuperavc.models.MotionReport>) {
    val points = items.map { it.clicksPerMinute.toFloat() }
    val labels = items.map { r ->
        val localDate = LocalDateTime.ofInstant(r.date, ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("dd/MM").format(localDate)
    }

    if (items.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Gesture,
                    contentDescription = null,
                    tint = Color.Black.copy(alpha = 0.3f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Nenhum relatório encontrado",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    ChartCard(title = "Coordenação Motora", subtitle = "Toques por minuto (quanto maior, melhor)") {
        BarChart(points = points, labels = labels, yAxisLabel = "Toques/min", onBarClick = null)
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
                    val localDate = LocalDateTime.ofInstant(r.date, ZoneId.systemDefault())
                    val label = DateTimeFormatter.ofPattern("dd/MM").format(localDate)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
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
            Box(modifier = Modifier.fillMaxWidth().height(200.dp)) { content() }
        }
    }
}

/**
 * Gráfico de barras com rolagem horizontal + rótulos sincronizados
 * e eixo Y fixo à esquerda. Clique por barra opcional.
 */
@Composable
private fun BarChart(
    points: List<Float>,
    labels: List<String>,
    yAxisLabel: String? = null,
    onBarClick: ((Int) -> Unit)?
) {
    if (points.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sem dados", color = Color.Black.copy(alpha = 0.6f))
        }
        return
    }

    val maxY = (points.maxOrNull() ?: 1f).coerceAtLeast(1f)
    val minY = 0f
    val scrollState = rememberScrollState()
    val barWidthDp = 64 // ajuste fino do tamanho das barras

    Column(modifier = Modifier.fillMaxSize()) {

        // Área principal: eixo Y fixo + barras com scroll
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {

            // Eixo Y
            Column(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (yAxisLabel != null) {
                        Text(
                            yAxisLabel,
                            fontSize = 10.sp,
                            color = Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(
                        "${maxY.toInt()}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Text(
                    "${(maxY * 0.75f).toInt()}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    "${(maxY * 0.5f).toInt()}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    "${(maxY * 0.25f).toInt()}",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    "${minY.toInt()}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            // Barras (scroll sincronizado com labels)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
            ) {
                val totalWidth = (points.size * barWidthDp).dp
                Row(
                    modifier = Modifier
                        .width(totalWidth)
                        .fillMaxHeight(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    points.forEachIndexed { idx, value ->
                        val ratio = if (maxY == minY) 0f
                        else ((value - minY) / (maxY - minY)).coerceIn(0f, 1f)
                        val safeRatio = if (ratio == 0f && points.isNotEmpty()) 0.03f else ratio

                        Box(
                            modifier = Modifier
                                .width(barWidthDp.dp)
                                .fillMaxHeight()
                                .padding(horizontal = 8.dp)
                                .clickable(enabled = onBarClick != null) {
                                    onBarClick?.invoke(idx)
                                },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxHeight(safeRatio)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GreenDark)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Rótulos (eixo X) — usam o mesmo scrollState das barras
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(48.dp)) // compensar eixo Y
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(scrollState)
                    .padding(vertical = 4.dp)
            ) {
                val totalWidth = (labels.size * barWidthDp).dp
                Row(
                    modifier = Modifier.width(totalWidth),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    labels.forEach { label ->
                        Box(
                            modifier = Modifier.width(barWidthDp.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
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

private data class Attempt(
    val fileId: String?,
    val phrase: String,
    val wpm: Int,
    val wer: Float,
    val transcribed: String = ""
)

private fun parseAudioReportDetails(desc: String): List<Attempt> {
    return try {
        val root = org.json.JSONObject(desc)
        val arr = root.optJSONArray("attempts") ?: return emptyList()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                add(
                    Attempt(
                        fileId = o.optString("fileId", null),
                        phrase = o.optString("phrase", ""),
                        wpm = o.optInt("wpm", 0),
                        wer = o.optDouble("wer", 0.0).toFloat(),
                        transcribed = o.optString("transcribed", "")
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
    }
}
