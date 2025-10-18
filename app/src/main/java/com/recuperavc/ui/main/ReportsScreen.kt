package com.recuperavc.ui.main

import android.Manifest
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.core.content.ContextCompat
import com.recuperavc.R
import com.recuperavc.data.db.DbProvider
import com.recuperavc.models.relations.AudioReportWithFiles
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight
import com.recuperavc.ui.theme.OnBackground
import com.recuperavc.ui.sfx.Sfx
import com.recuperavc.ui.sfx.rememberSfxController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar

enum class ChartType { WPM, WER }
private enum class CoherenceChartType { TIME, ERRORS }
private enum class ReportTab { Audio, Coherence, Motion }

@Composable
fun ReportsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember(context) { DbProvider.db(context) }
    val sfx = rememberSfxController()

    var tab by remember { mutableStateOf(ReportTab.Audio) }
    var selectedAudioReport by remember { mutableStateOf<Pair<AudioReportWithFiles, ChartType>?>(null) }
    var selectedCoherenceReport by remember { mutableStateOf<Pair<com.recuperavc.models.CoherenceReport, CoherenceChartType>?>(null) }

    val now = remember { Instant.now() }
    val defaultStartDate = remember { now.minus(7, ChronoUnit.DAYS) }
    var startDate by remember { mutableStateOf(defaultStartDate) }
    var endDate by remember { mutableStateOf(now) }
    var isDateManuallySet by remember { mutableStateOf(false) }

    val notifPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { }

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

    val coherenceReports by db.coherenceReportDao()
        .observeAll()
        .collectAsState(initial = emptyList())

    val filteredCoherenceReports = remember(coherenceReports, startDate, endDate) {
        coherenceReports
            .filter { r ->
                val groups = parseCoherenceReportGroups(r.allTestsDescription)
                groups.isNotEmpty() && r.date >= startDate && r.date <= endDate
            }
            .sortedBy { it.date }
    }

    var phrases by remember { mutableStateOf<List<com.recuperavc.models.Phrase>>(emptyList()) }
    LaunchedEffect(Unit) {
        phrases = db.phraseDao().getAll()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
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
                    IconButton(onClick = { sfx.play(Sfx.CLICK); onBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = OnBackground)
                    }
                }
            }

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
                SegmentedTabs(
                    tab = tab,
                    onTab = {
                        sfx.play(Sfx.CLICK)
                        tab = it
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

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
                    },
                    onTapSound = { sfx.play(Sfx.CLICK) }
                )
                Spacer(Modifier.height(16.dp))

                when (tab) {
                    ReportTab.Audio -> AudioReportSection(
                        items = audioReports,
                        onSelectReport = { report, chartType -> selectedAudioReport = report to chartType },
                        onBarTapSound = { sfx.play(Sfx.CLICK) }
                    )
                    ReportTab.Coherence -> CoherenceReportSection(
                        items = filteredCoherenceReports,
                        onSelectReport = { report, chartType -> selectedCoherenceReport = report to chartType },
                        onBarTapSound = { sfx.play(Sfx.CLICK) }
                    )
                    ReportTab.Motion -> MotionReportSection(motionReports)
                }

                Spacer(Modifier.height(20.dp))
                ExportPdfButton(
                    tab = tab,
                    startDate = startDate,
                    endDate = endDate,
                    audioReports = audioReports,
                    coherenceReports = filteredCoherenceReports,
                    motionReports = motionReports,
                    onTapSound = { sfx.play(Sfx.CLICK) }
                )
                Spacer(Modifier.height(32.dp))
            }
        }

        selectedAudioReport?.let { (rep, chartType) ->
            AudioReportDetailDialog(
                report = rep,
                chartType = chartType,
                onDismiss = { selectedAudioReport = null },
                onAnyTap = { sfx.play(Sfx.CLICK) }
            )
        }

        selectedCoherenceReport?.let { (rep, chartType) ->
            CoherenceReportDetailDialog(
                report = rep,
                chartType = chartType,
                phraseMap = remember(phrases) { phrases.associateBy { it.id } },
                onDismiss = { selectedCoherenceReport = null },
                onAnyTap = { sfx.play(Sfx.CLICK) }
            )
        }
    }
}

@Composable
private fun SegmentedTabs(tab: ReportTab, onTab: (ReportTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GreenLight.copy(alpha = 0.2f))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SegButton(
            modifier = Modifier.weight(1f),
            selected = tab == ReportTab.Audio,
            icon = Icons.Default.Mic,
            text = "Voz",
            onClick = { onTab(ReportTab.Audio) }
        )
        SegButton(
            modifier = Modifier.weight(1f),
            selected = tab == ReportTab.Coherence,
            icon = Icons.Default.TrendingUp,
            text = "Raciocínio",
            onClick = { onTab(ReportTab.Coherence) }
        )
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
    val bg = if (selected) GreenDark else Color.White
    val fg = if (selected) Color.White else GreenDark
    val elevation = if (selected) 4.dp else 0.dp

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(vertical = 14.dp, horizontal = 8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text,
                color = fg,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DateFilterCard(
    startDate: Instant,
    endDate: Instant,
    isManuallySet: Boolean,
    onStartDateChange: (Instant) -> Unit,
    onEndDateChange: (Instant) -> Unit,
    onTapSound: () -> Unit
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
                                onTapSound()
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
                                onTapSound()
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
    onSelectReport: (AudioReportWithFiles, ChartType) -> Unit,
    onBarTapSound: () -> Unit
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
            onBarClick = { idx -> items.getOrNull(idx)?.let { onSelectReport(it, ChartType.WPM) } },
            onBarTapSound = onBarTapSound
        )
    }
    Spacer(Modifier.height(16.dp))
    ChartCard(title = "Erros de Fala", subtitle = "Porcentagem de erro (quanto menor, melhor)") {
        BarChart(
            points = pointsWer,
            labels = labels,
            yAxisLabel = "WER (%)",
            onBarClick = { idx -> items.getOrNull(idx)?.let { onSelectReport(it, ChartType.WER) } },
            onBarTapSound = onBarTapSound
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
    onDismiss: () -> Unit,
    onAnyTap: () -> Unit
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
                detectTapGestures {
                    onAnyTap()
                    onDismiss()
                }
            }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center)
                .pointerInput(Unit) { },
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
                                            onAnyTap()
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
                        onAnyTap()
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
private fun CoherenceReportSection(
    items: List<com.recuperavc.models.CoherenceReport>,
    onSelectReport: (com.recuperavc.models.CoherenceReport, CoherenceChartType) -> Unit,
    onBarTapSound: () -> Unit
) {
    val pointsTime = items.map { it.averageTimePerTry }
    val pointsErrors = items.map { it.averageErrorsPerTry }
    val labels = items.map { r ->
        val local = LocalDateTime.ofInstant(r.date, ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("dd/MM").format(local)
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
                    Icons.Default.TrendingUp,
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

    ChartCard(title = "Tempo até acerto", subtitle = "Segundos por frase (quanto menor, melhor)") {
        BarChart(
            points = pointsTime,
            labels = labels,
            yAxisLabel = "s",
            onBarClick = { idx -> items.getOrNull(idx)?.let { onSelectReport(it, CoherenceChartType.TIME) } },
            onBarTapSound = onBarTapSound
        )
    }
    Spacer(Modifier.height(16.dp))
    ChartCard(title = "Tentativas por frase", subtitle = "Média de tentativas (quanto menor, melhor)") {
        BarChart(
            points = pointsErrors,
            labels = labels,
            yAxisLabel = "Tentativas",
            onBarClick = { idx -> items.getOrNull(idx)?.let { onSelectReport(it, CoherenceChartType.ERRORS) } },
            onBarTapSound = onBarTapSound
        )
    }
}

@Composable
private fun CoherenceReportDetailDialog(
    report: com.recuperavc.models.CoherenceReport,
    chartType: CoherenceChartType,
    phraseMap: Map<java.util.UUID, com.recuperavc.models.Phrase>,
    onDismiss: () -> Unit,
    onAnyTap: () -> Unit
) {
    val groups = remember(report.allTestsDescription) { parseCoherenceReportGroups(report.allTestsDescription) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .pointerInput(Unit) { detectTapGestures { onAnyTap(); onDismiss() } }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.Center)
                .pointerInput(Unit) { },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 650.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Teste de Raciocínio",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 24.sp,
                            color = GreenDark
                        )
                        val local = LocalDateTime.ofInstant(report.date, ZoneId.systemDefault())
                        Text(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").format(local),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black.copy(alpha = 0.6f)
                        )
                    }
                    Icon(
                        Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = GreenDark,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                val sr = if (groups.isNotEmpty()) groups.count { it.success }.toFloat() / groups.size.toFloat() else 0f
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenLight.copy(alpha = 0.15f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Resumo do Teste",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = GreenDark
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatsBox(
                                modifier = Modifier.weight(1f),
                                label = "Frases",
                                value = "${groups.size}",
                                icon = Icons.Default.Description
                            )
                            StatsBox(
                                modifier = Modifier.weight(1f),
                                label = "Taxa de Acerto",
                                value = "${String.format("%.0f", sr * 100)}%",
                                icon = Icons.Default.CheckCircle
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatsBox(
                                modifier = Modifier.weight(1f),
                                label = "Tempo Médio",
                                value = "${String.format("%.1f", report.averageTimePerTry)}s",
                                icon = Icons.Default.Timer
                            )
                            StatsBox(
                                modifier = Modifier.weight(1f),
                                label = "Tentativas Médias",
                                value = String.format("%.1f", report.averageErrorsPerTry),
                                icon = Icons.Default.RepeatOne
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text(
                    "Detalhes por Frase",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                Spacer(Modifier.height(12.dp))

                groups.forEachIndexed { idx, g ->
                    val phrase = g.phraseId?.let { phraseMap[it]?.description } ?: ""
                    val ok = g.success
                    val statusColor = if (ok) Color(0xFF2E7D32) else Color(0xFFD32F2F)

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Frase ${idx + 1}",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = statusColor.copy(alpha = 0.15f)
                                    )
                                ) {
                                    Text(
                                        text = if (ok) "✓ Acertou" else "✗ Errou",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (phrase.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                        Text(
                                            "Frase Correta:",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black.copy(alpha = 0.5f)
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            phrase,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                InfoCard(
                                    modifier = Modifier.weight(1f),
                                    label = "Tentativas",
                                    value = "${g.triesCount}"
                                )
                                InfoCard(
                                    modifier = Modifier.weight(1f),
                                    label = "Tempo",
                                    value = "${String.format("%.1f", g.timeUntilCorrectMs / 1000f)}s"
                                )
                            }

                            if (g.tries.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Histórico de Tentativas:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(8.dp))

                                g.tries.forEachIndexed { tIdx, t ->
                                    val tColor = if (t.correct) Color(0xFF2E7D32) else Color(0xFFD32F2F)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(tColor.copy(alpha = 0.08f))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "${tIdx + 1}.",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = tColor
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                t.typed,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.Black
                                            )
                                        }
                                        Text(
                                            "${String.format("%.1f", t.elapsedMs / 1000f)}s",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tColor
                                        )
                                    }
                                    if (tIdx < g.tries.lastIndex) Spacer(Modifier.height(6.dp))
                                }
                            }
                        }
                    }
                    if (idx < groups.lastIndex) Spacer(Modifier.height(12.dp))
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {
                        onAnyTap()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Fechar", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StatsBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = GreenDark,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                color = GreenDark
            )
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Black.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = GreenDark
            )
        }
    }
}

private data class CoherencePhraseGroup(
    val phraseId: java.util.UUID?,
    val success: Boolean,
    val triesCount: Int,
    val timeUntilCorrectMs: Long,
    val tries: List<CoherencePhraseTry>
)

private data class CoherencePhraseTry(
    val typed: String,
    val correct: Boolean,
    val elapsedMs: Long
)

private fun parseCoherenceReportGroups(desc: String): List<CoherencePhraseGroup> {
    return try {
        val root = org.json.JSONObject(desc)
        val arr = root.optJSONArray("attempts") ?: return emptyList()
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val pidStr = o.optString("phraseId", null)
                val triesArr = o.optJSONArray("tries")
                val triesList = mutableListOf<CoherencePhraseTry>()
                if (triesArr != null) {
                    for (j in 0 until triesArr.length()) {
                        val to = triesArr.getJSONObject(j)
                        triesList.add(
                            CoherencePhraseTry(
                                typed = to.optString("typed", ""),
                                correct = to.optBoolean("correct", false),
                                elapsedMs = to.optLong("elapsedMs", 0L)
                            )
                        )
                    }
                }
                add(
                    CoherencePhraseGroup(
                        phraseId = pidStr?.let { runCatching { java.util.UUID.fromString(it) }.getOrNull() },
                        success = o.optBoolean("success", false),
                        triesCount = o.optInt("triesCount", triesList.size),
                        timeUntilCorrectMs = o.optLong("timeUntilCorrectMs", 0L),
                        tries = triesList
                    )
                )
            }
        }
    } catch (_: Exception) {
        emptyList()
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

@Composable
private fun BarChart(
    points: List<Float>,
    labels: List<String>,
    yAxisLabel: String? = null,
    onBarClick: ((Int) -> Unit)?,
    onBarTapSound: (() -> Unit)? = null
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
    val barWidthDp = 64

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
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
                                    onBarTapSound?.invoke()
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

        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(48.dp))
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

/* ---------------- PDF Export ---------------- */
@Composable
private fun ExportPdfButton(
    tab: ReportTab,
    startDate: Instant,
    endDate: Instant,
    audioReports: List<AudioReportWithFiles>,
    coherenceReports: List<com.recuperavc.models.CoherenceReport>,
    motionReports: List<com.recuperavc.models.MotionReport>,
    onTapSound: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Launcher for WRITE_EXTERNAL_STORAGE (pre-29)
    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* we re-run export in onClick after permission */ }

    // Launcher for POST_NOTIFICATIONS (API 33+)
    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* no-op; we’ll try to show the notification right after export */ }

    Button(
        onClick = {
            onTapSound?.invoke()

            // Pre-29: ask for legacy storage permission if needed
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                val perm = Manifest.permission.WRITE_EXTERNAL_STORAGE
                if (ContextCompat.checkSelfPermission(ctx, perm) != PackageManager.PERMISSION_GRANTED) {
                    storageLauncher.launch(perm)
                    return@Button
                }
            }
            // 33+: ask for notification permission (best-effort)
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
                        // (opcional) ainda mostra um toast
                        Toast.makeText(ctx, "PDF salvo em Downloads/RecuperAVC.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(ctx, "Falha ao salvar PDF.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        },
        colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(Icons.Default.Description, contentDescription = null, tint = Color.White)
        Spacer(Modifier.width(8.dp))
        Text("Exportar PDF", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ===== PDF export (fixed: no unresolved reference) =========================
private fun exportReportsToPdf(
    context: android.content.Context,
    tab: ReportTab,
    startDate: java.time.Instant,
    endDate: java.time.Instant,
    audioReports: List<com.recuperavc.models.relations.AudioReportWithFiles>,
    coherenceReports: List<com.recuperavc.models.CoherenceReport>,
    motionReports: List<com.recuperavc.models.MotionReport>
): android.net.Uri? {
    val doc = android.graphics.pdf.PdfDocument()

    // A4 page @ ~72dpi
    val pageW = 595
    val pageH = 842
    val margin = 32

    var pageNo = 1
    var y = margin

    // Page & canvas holders must exist before helpers
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
        // approx your GreenDark
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

    // Start first page
    newPage()

    // Header
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

    // Content
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

    // Finish last page
    currentPage?.let { doc.finishPage(it) }

    // Filename
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
            // MediaStore path => content:// always
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(
                    android.provider.MediaStore.MediaColumns.RELATIVE_PATH,
                    android.os.Environment.DIRECTORY_DOWNLOADS + "/RecuperAVC"
                )
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val collection = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(collection, values)

            uri?.let {
                resolver.openOutputStream(it)?.use { out -> doc.writeTo(out) }
                values.clear()
                values.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(it, values, null, null)
            }
            resultUri = uri
        } else {
            // API < 29 — save to public Downloads and return a **content://** via FileProvider
            val dir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            )
            val folder = java.io.File(dir, "RecuperAVC").apply { mkdirs() }
            val file = java.io.File(folder, fileName)

            java.io.FileOutputStream(file).use { out -> doc.writeTo(out) }

            // Index for gallery/providers
            android.media.MediaScannerConnection.scanFile(
                context,
                arrayOf(file.absolutePath),
                arrayOf("application/pdf"),
                null
            )

            // IMPORTANT: never return file:// — always content:// using your FileProvider
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
// ==========================================================================

// Nome do PDF exatamente no mesmo padrão usado na exportação
private fun buildReportFileName(
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
