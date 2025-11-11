package com.recuperavc.ui.main.reports.components.audio

import android.media.MediaPlayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.models.relations.AudioReportWithFiles
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.util.InitialSettings
import com.recuperavc.ui.util.rememberInitialSettings
import com.recuperavc.ui.main.reports.type.ChartType
import com.recuperavc.ui.main.reports.components.BarChart
import com.recuperavc.ui.main.reports.components.ChartCard
import com.recuperavc.ui.theme.LocalReportsPalette
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.sin

private fun Color.luma(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
private val Color.on: Color get() = if (luma() > 0.6f) Color.Black else Color.White

@Composable
fun AudioReportSection(
    items: List<AudioReportWithFiles>,
    onSelectReport: (AudioReportWithFiles, ChartType) -> Unit,
    onBarTapSound: () -> Unit
) {
    val p = LocalReportsPalette.current

    val context = LocalContext.current
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val initial: InitialSettings? = rememberInitialSettings(settings)
    val appliedScale = settings.sizeTextFlow.collectAsState(initial = initial?.scale ?: 1f).value

    if (items.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = p.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (p.borderSoft != null) 0.dp else 4.dp),
            border = p.borderSoft?.let { BorderStroke(1.dp, it) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Mic, contentDescription = null, tint = p.textSecondary, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Nenhum relatório encontrado",
                    fontSize = (18.sp * appliedScale),
                    fontWeight = FontWeight.Bold,
                    color = p.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    Text(
        text = "Toque nas barras para ver detalhes",
        fontSize = (16.sp * appliedScale),
        color = p.accent,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(Modifier.height(16.dp))

    ChartCard(title = "Velocidade de Fala", subtitle = "Palavras por minuto (quanto maior, melhor)") {
        BarChart(
            points = items.map { it.report.averageWordsPerMinute },
            labels = items.map { r ->
                val date = r.files.minByOrNull { it.recordedAt ?: Instant.EPOCH }?.recordedAt
                if (date != null) {
                    val local = LocalDateTime.ofInstant(date, ZoneId.systemDefault())
                    DateTimeFormatter.ofPattern("dd/MM").format(local)
                } else ""
            },
            yAxisLabel = "WPM",
            onBarClick = { idx -> items.getOrNull(idx)?.let { onSelectReport(it, ChartType.WPM) } },
            onBarTapSound = onBarTapSound
        )
    }
    Spacer(Modifier.height(16.dp))
    ChartCard(title = "Erros de Fala", subtitle = "Porcentagem de erro (quanto menor, melhor)") {
        BarChart(
            points = items.map { it.report.averageWordErrorRate },
            labels = items.map { r ->
                val date = r.files.minByOrNull { it.recordedAt ?: Instant.EPOCH }?.recordedAt
                if (date != null) {
                    val local = LocalDateTime.ofInstant(date, ZoneId.systemDefault())
                    DateTimeFormatter.ofPattern("dd/MM").format(local)
                } else ""
            },
            yAxisLabel = "WER (%)",
            onBarClick = { idx -> items.getOrNull(idx)?.let { onSelectReport(it, ChartType.WER) } },
            onBarTapSound = onBarTapSound
        )
    }
    Spacer(Modifier.height(16.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = p.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (p.borderSoft != null) 0.dp else 4.dp),
        border = p.borderSoft?.let { BorderStroke(1.dp, it) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Últimos Testes Realizados", fontWeight = FontWeight.Bold, fontSize = (18.sp * appliedScale), color = p.textPrimary)
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
                        .background(p.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Text("Data: $label", fontWeight = FontWeight.Bold, fontSize = (16.sp * appliedScale), color = p.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Velocidade", fontSize = (13.sp * appliedScale), fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                            Text("${r.report.averageWordsPerMinute.toInt()} palavras/min", fontSize = (15.sp * appliedScale), fontWeight = FontWeight.Bold, color = p.accent)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Precisão", fontSize = (13.sp * appliedScale), fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                            Text("${String.format("%.1f", 100 - r.report.averageWordErrorRate)}%", fontSize = (15.sp * appliedScale), fontWeight = FontWeight.Bold, color = p.accent)
                        }
                    }
                }
                if (idx < 4) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/* ------------------------ Detail dialog (colors only) ------------------------ */

@Composable
fun AudioReportDetailDialog(
    report: AudioReportWithFiles,
    chartType: ChartType,
    onDismiss: () -> Unit,
    onAnyTap: () -> Unit
) {
    val p = LocalReportsPalette.current

    val context = LocalContext.current
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val initial: InitialSettings? = rememberInitialSettings(settings)
    val appliedScale = settings.sizeTextFlow.collectAsState(initial = initial?.scale ?: 1f).value

    val attempts = remember(report.report.allTestsDescription) { parseAudioReportDetails(report.report.allTestsDescription) }
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
            .pointerInput(Unit) { detectTapGestures { onAnyTap(); onDismiss() } }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = p.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (p.borderSoft != null) 0.dp else 8.dp),
            border = p.borderSoft?.let { BorderStroke(1.dp, it) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 600.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text("Áudios do Teste", fontWeight = FontWeight.Bold, fontSize = (22.sp * appliedScale), color = p.textPrimary)
                Spacer(Modifier.height(4.dp))
                val filesSorted = remember(report.files) { report.files.sortedBy { it.recordedAt } }
                val date = filesSorted.minByOrNull { it.recordedAt }?.recordedAt
                if (date != null) {
                    val localDate = LocalDateTime.ofInstant(date, ZoneId.systemDefault())
                    Text(
                        text = "Realizado em ${DateTimeFormatter.ofPattern("dd/MM/yyyy").format(localDate)}",
                        fontSize = (15.sp * appliedScale),
                        fontWeight = FontWeight.SemiBold,
                        color = p.textSecondary
                    )
                }
                Spacer(Modifier.height(16.dp))

                if (filesSorted.isNotEmpty()) {
                    Text("Total: ${filesSorted.size} áudio(s)", fontWeight = FontWeight.Bold, fontSize = (17.sp * appliedScale), color = p.accent)
                    Spacer(Modifier.height(12.dp))

                    filesSorted.forEachIndexed { idx, audioFile ->
                        val attempt = attemptsByFileId[audioFile.id.toString()] ?: attempts.getOrNull(idx)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = p.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Áudio ${idx + 1}", fontWeight = FontWeight.Bold, fontSize = (17.sp * appliedScale), color = p.textPrimary)
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
                                                        setOnCompletionListener { currentlyPlayingIndex = null }
                                                        start()
                                                    }
                                                    currentlyPlayingIndex = idx
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(if (currentlyPlayingIndex == idx) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = null, tint = p.accent, modifier = Modifier.size(28.dp))
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(audioFile.fileName, fontSize = (14.sp * appliedScale), fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                                Spacer(Modifier.height(4.dp))
                                Text("Duração: ${(audioFile.audioDuration / 1000)}s", fontSize = (15.sp * appliedScale), fontWeight = FontWeight.Bold, color = p.accent)
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
                                    .background(p.surface)
                                    .padding(12.dp)
                            ) {
                                if (attempt.phrase.isNotEmpty()) {
                                    Text("Esperado:", fontSize = (12.sp * appliedScale), fontWeight = FontWeight.Bold, color = p.textSecondary)
                                    Text(attempt.phrase, fontSize = (14.sp * appliedScale), fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                                    Spacer(Modifier.height(8.dp))
                                }
                                if (attempt.transcribed.isNotEmpty()) {
                                    Text("Transcrito:", fontSize = (12.sp * appliedScale), fontWeight = FontWeight.Bold, color = p.textSecondary)
                                    Text(attempt.transcribed, fontSize = (14.sp * appliedScale), fontWeight = FontWeight.SemiBold, color = p.accent)
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Duração", fontSize = (12.sp * appliedScale), fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                                Text("${audioFile.audioDuration / 1000}s", fontSize = (14.sp * appliedScale), fontWeight = FontWeight.Bold, color = p.accent)
                            }
                            if (attempt != null) {
                                when (chartType) {
                                    ChartType.WPM -> {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("WPM", fontSize = (12.sp * appliedScale), fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                                            Text("${attempt.wpm}", fontSize = (14.sp * appliedScale), fontWeight = FontWeight.Bold, color = p.accent)
                                        }
                                    }
                                    ChartType.WER -> {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("WER", fontSize = (12.sp * appliedScale), fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                                            Text("${String.format("%.1f", attempt.wer)}%", fontSize = (14.sp * appliedScale), fontWeight = FontWeight.Bold, color = p.accent)
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
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.accent.on),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Fechar", color = p.accent.on, fontSize = (16.sp * appliedScale), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SimpleWaveform(modifier: Modifier = Modifier) {
    var phase by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) { while (true) { delay(50); phase += 0.3f } }

    val p = LocalReportsPalette.current

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
            val barHeight = amplitude * sin(offset) * 0.5f + amplitude * 0.5f
            drawRect(
                color = p.accent,
                topLeft = Offset(x + barWidth * 0.2f, centerY - barHeight / 2),
                size = Size(barWidth * 0.6f, barHeight)
            )
        }
    }
}

/* ----------------------------- parsing helpers ----------------------------- */

data class Attempt(
    val fileId: String?,
    val phrase: String,
    val wpm: Int,
    val wer: Float,
    val transcribed: String = ""
)

fun parseAudioReportDetails(desc: String): List<Attempt> {
    return try {
        val root = JSONObject(desc)
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
