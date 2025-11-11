package com.recuperavc.ui.main.reports.coherence

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.Phrase
import com.recuperavc.ui.main.reports.type.CoherenceChartType
import com.recuperavc.ui.main.reports.components.BarChart
import com.recuperavc.ui.main.reports.components.ChartCard
import com.recuperavc.ui.theme.LocalReportsPalette
import java.util.UUID
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.util.InitialSettings
import com.recuperavc.ui.util.rememberInitialSettings
import org.json.JSONObject

/* helpers */
private fun Color.luma(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
private val Color.on: Color get() = if (luma() > 0.6f) Color.Black else Color.White

@Composable
fun CoherenceReportSection(
    items: List<CoherenceReport>,
    onSelectReport: (CoherenceReport, CoherenceChartType) -> Unit,
    onBarTapSound: () -> Unit
) {
    val p = LocalReportsPalette.current

    val context = LocalContext.current
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val initial: InitialSettings? = rememberInitialSettings(settings)
    val appliedScale = settings.sizeTextFlow.collectAsState(initial = initial?.scale ?: 1f).value

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
            colors = CardDefaults.cardColors(containerColor = p.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (p.borderSoft != null) 0.dp else 4.dp),
            border = p.borderSoft?.let { BorderStroke(1.dp, it) }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = p.textSecondary, modifier = Modifier.size(64.dp))
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

    ChartCard(title = "Tempo até acerto", subtitle = "Segundos por frase (quanto menor, melhor)") {
        BarChart(
            points = pointsTime,
            labels = labels,
            yAxisLabel = "Tempo (s)",
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
fun CoherenceReportDetailDialog(
    report: CoherenceReport,
    chartType: CoherenceChartType,
    phraseMap: Map<UUID, Phrase>,
    onDismiss: () -> Unit,
    onAnyTap: () -> Unit
) {
    val p = LocalReportsPalette.current

    val context = LocalContext.current
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val initial: InitialSettings? = rememberInitialSettings(settings)
    val appliedScale = settings.sizeTextFlow.collectAsState(initial = initial?.scale ?: 1f).value
    val appliedContrast = settings.contrastFlow.collectAsState(initial = initial?.contrast ?: false).value

    val groups = remember(report.allTestsDescription) { parseCoherenceReportGroups(report.allTestsDescription) }

    // Colors that avoid green in High-Contrast
    val okColor = if (appliedContrast) p.accent else Color(0xFF2E7D32)
    val okBg = okColor.copy(alpha = if (appliedContrast) 0.25f else 0.15f)
    val errColor = if (appliedContrast) Color(0xFFFF8A80) else Color(0xFFD32F2F)
    val errBg = errColor.copy(alpha = 0.15f)

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
                .align(Alignment.Center),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = p.sheet),
            elevation = CardDefaults.cardElevation(defaultElevation = if (p.borderSoft != null) 0.dp else 12.dp),
            border = p.borderSoft?.let { BorderStroke(1.dp, it) }
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
                        Text("Teste de Raciocínio", fontWeight = FontWeight.ExtraBold, fontSize = (24.sp * appliedScale), color = p.accent)
                        val local = LocalDateTime.ofInstant(report.date, ZoneId.systemDefault())
                        Text(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm").format(local),
                            fontSize = (14.sp * appliedScale),
                            fontWeight = FontWeight.SemiBold,
                            color = p.textSecondary
                        )
                    }
                    Icon(Icons.Default.TrendingUp, contentDescription = null, tint = p.accent, modifier = Modifier.size(32.dp))
                }

                Spacer(Modifier.height(20.dp))

                val totalTries = groups.sumOf { it.tries.size }
                val correctTries = groups.sumOf { it.tries.count { t -> t.correct } }
                val sr = if (totalTries > 0) correctTries.toFloat() / totalTries.toFloat() else 0f
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = p.surfaceVariant),
                    border = p.borderSoft?.let { BorderStroke(1.dp, it) }
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Resumo do Teste", fontWeight = FontWeight.Bold, fontSize = (16.sp * appliedScale), color = p.textPrimary)
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatsBox(modifier = Modifier.weight(1f), label = "Frases", value = "${groups.size}", icon = Icons.Default.Description)
                            StatsBox(modifier = Modifier.weight(1f), label = "Taxa de Acerto", value = "${String.format("%.0f", sr * 100)}%", icon = Icons.Default.CheckCircle)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatsBox(modifier = Modifier.weight(1f), label = "Tempo Médio", value = "${String.format("%.1f", report.averageTimePerTry)}s", icon = Icons.Default.Timer)
                            StatsBox(modifier = Modifier.weight(1f), label = "Tentativas Médias", value = String.format("%.1f", report.averageErrorsPerTry), icon = Icons.Default.RepeatOne)
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Text("Detalhes por Frase", fontWeight = FontWeight.Bold, fontSize = (18.sp * appliedScale), color = p.textPrimary)
                Spacer(Modifier.height(12.dp))

                groups.forEachIndexed { idx, g ->
                    val phrase = g.phraseId?.let { phraseMap[it]?.description } ?: ""
                    val success = g.success

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = p.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (p.borderSoft != null) 0.dp else 2.dp),
                        border = p.borderSoft?.let { BorderStroke(1.dp, it) }
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Frase ${idx + 1}", fontWeight = FontWeight.ExtraBold, fontSize = (18.sp * appliedScale), color = p.textPrimary)
                                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = if (success) okBg else errBg)) {
                                    Text(
                                        text = if (success) "✓ Acertou" else "✗ Errou",
                                        fontSize = (14.sp * appliedScale),
                                        fontWeight = FontWeight.Bold,
                                        color = if (success) okColor else errColor,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            if (phrase.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = p.surfaceVariant),
                                    border = p.borderSoft?.let { BorderStroke(1.dp, it) }
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                        Text("Frase Correta:", fontSize = (11.sp * appliedScale), fontWeight = FontWeight.Bold, color = p.textSecondary)
                                        Spacer(Modifier.height(4.dp))
                                        Text(phrase, fontSize = (15.sp * appliedScale), fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                InfoCard(modifier = Modifier.weight(1f), label = "Tentativas", value = "${g.triesCount}")
                                InfoCard(modifier = Modifier.weight(1f), label = "Tempo", value = "${String.format("%.1f", g.timeUntilCorrectMs / 1000f)}s")
                            }

                            if (g.tries.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                Text("Histórico de Tentativas:", fontSize = (12.sp * appliedScale), fontWeight = FontWeight.Bold, color = p.textSecondary)
                                Spacer(Modifier.height(8.dp))

                                g.tries.forEachIndexed { tIdx, t ->
                                    val tCol = if (t.correct) okColor else errColor
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(tCol.copy(alpha = if (appliedContrast) 0.18f else 0.08f))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Text("${tIdx + 1}.", fontSize = (13.sp * appliedScale), fontWeight = FontWeight.ExtraBold, color = tCol)
                                            Spacer(Modifier.width(8.dp))
                                            Text(t.typed, fontSize = (14.sp * appliedScale), fontWeight = FontWeight.SemiBold, color = p.textPrimary)
                                        }
                                        Text("${String.format("%.1f", t.elapsedMs / 1000f)}s", fontSize = (13.sp * appliedScale), fontWeight = FontWeight.Bold, color = tCol)
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
                    onClick = { onAnyTap(); onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent, contentColor = p.accent.on),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Fechar", color = p.accent.on, fontSize = (17.sp * appliedScale), fontWeight = FontWeight.Bold)
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
    val p = LocalReportsPalette.current
    val context = LocalContext.current
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val initial: InitialSettings? = rememberInitialSettings(settings)
    val appliedScale = settings.sizeTextFlow.collectAsState(initial = initial?.scale ?: 1f).value

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = p.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (p.borderSoft != null) 0.dp else 1.dp),
        border = p.borderSoft?.let { BorderStroke(1.dp, it) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = (20.sp * appliedScale), color = p.textPrimary)
            Text(label, fontSize = (11.sp * appliedScale), fontWeight = FontWeight.SemiBold, color = p.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String
) {
    val p = LocalReportsPalette.current
    val context = LocalContext.current
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val initial: InitialSettings? = rememberInitialSettings(settings)
    val appliedScale = settings.sizeTextFlow.collectAsState(initial = initial?.scale ?: 1f).value

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = p.surfaceVariant),
        border = p.borderSoft?.let { BorderStroke(1.dp, it) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = (11.sp * appliedScale), fontWeight = FontWeight.Bold, color = p.textSecondary)
            Spacer(Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = (16.sp * appliedScale), color = p.textPrimary)
        }
    }
}

/* parsing stays unchanged below */

data class CoherencePhraseGroup(
    val phraseId: UUID?,
    val success: Boolean,
    val triesCount: Int,
    val timeUntilCorrectMs: Long,
    val tries: List<CoherencePhraseTry>
)

data class CoherencePhraseTry(
    val typed: String,
    val correct: Boolean,
    val elapsedMs: Long
)

fun parseCoherenceReportGroups(desc: String): List<CoherencePhraseGroup> {
    return try {
        val root = JSONObject(desc)
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
                        phraseId = pidStr?.let { runCatching { UUID.fromString(it) }.getOrNull() },
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
