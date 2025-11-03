package com.recuperavc.ui.main.reports.motion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recuperavc.models.MotionReport
import com.recuperavc.ui.main.reports.components.BarChart
import com.recuperavc.ui.main.reports.components.ChartCard
import com.recuperavc.ui.theme.LocalReportsPalette
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.util.InitialSettings
import com.recuperavc.ui.util.rememberInitialSettings

private fun Color.luma(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
private val Color.on: Color get() = if (luma() > 0.6f) Color.Black else Color.White

@Composable
fun MotionReportSection(
    items: List<MotionReport>,
    handFilter: Boolean?,
    dominantFilter: Boolean?,
    onHandFilterChange: (Boolean?) -> Unit,
    onDominantFilterChange: (Boolean?) -> Unit
) {
    val p = LocalReportsPalette.current

    val context = LocalContext.current
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val initial: InitialSettings? = rememberInitialSettings(settings)
    val scale = settings.sizeTextFlow.collectAsState(initial = initial?.scale ?: 1f).value

    val points = items.map { it.clicksPerMinute.toFloat() }
    val labels = items.map {
        val localDate = LocalDateTime.ofInstant(it.date, ZoneId.systemDefault())
        DateTimeFormatter.ofPattern("dd/MM").format(localDate)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = p.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (p.borderSoft != null) 0.dp else 4.dp),
        border = p.borderSoft?.let { BorderStroke(1.dp, it) }
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                Icon(Icons.Default.PanTool, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Mão Utilizada", fontSize = (14.sp * scale), fontWeight = FontWeight.Bold, color = p.textPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = handFilter == null,
                    onClick = { onHandFilterChange(null) },
                    label = { Text("Todas", fontSize = (13.sp * scale), fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = p.surface,
                        labelColor = p.textPrimary,
                        selectedContainerColor = p.accent,
                        selectedLabelColor = p.accent.on
                    ),
                    border = p.borderSoft?.let { BorderStroke(1.dp, it) }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = handFilter == false,
                    onClick = { onHandFilterChange(false) },
                    label = { Text("Esquerda", fontSize = (13.sp * scale), fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = p.surface,
                        labelColor = p.textPrimary,
                        selectedContainerColor = p.accent,
                        selectedLabelColor = p.accent.on
                    ),
                    border = p.borderSoft?.let { BorderStroke(1.dp, it) }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = handFilter == true,
                    onClick = { onHandFilterChange(true) },
                    label = { Text("Direita", fontSize = (13.sp * scale), fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = p.surface,
                        labelColor = p.textPrimary,
                        selectedContainerColor = p.accent,
                        selectedLabelColor = p.accent.on
                    ),
                    border = p.borderSoft?.let { BorderStroke(1.dp, it) }
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                Icon(Icons.Default.Star, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Mão Dominante", fontSize = (14.sp * scale), fontWeight = FontWeight.Bold, color = p.textPrimary)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = dominantFilter == null,
                    onClick = { onDominantFilterChange(null) },
                    label = { Text("Todas", fontSize = (13.sp * scale), fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = p.surface,
                        labelColor = p.textPrimary,
                        selectedContainerColor = p.accent,
                        selectedLabelColor = p.accent.on
                    ),
                    border = p.borderSoft?.let { BorderStroke(1.dp, it) }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = dominantFilter == true,
                    onClick = { onDominantFilterChange(true) },
                    label = { Text("Sim", fontSize = (13.sp * scale), fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = p.surface,
                        labelColor = p.textPrimary,
                        selectedContainerColor = p.accent,
                        selectedLabelColor = p.accent.on
                    ),
                    border = p.borderSoft?.let { BorderStroke(1.dp, it) }
                )
                FilterChip(
                    modifier = Modifier.weight(1f),
                    selected = dominantFilter == false,
                    onClick = { onDominantFilterChange(false) },
                    label = { Text("Não", fontSize = (13.sp * scale), fontWeight = FontWeight.SemiBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = p.surface,
                        labelColor = p.textPrimary,
                        selectedContainerColor = p.accent,
                        selectedLabelColor = p.accent.on
                    ),
                    border = p.borderSoft?.let { BorderStroke(1.dp, it) }
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    if (items.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = p.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (p.borderSoft != null) 0.dp else 4.dp),
            border = p.borderSoft?.let { BorderStroke(1.dp, it) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Gesture, contentDescription = null, tint = p.textSecondary, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Nenhum relatório encontrado", fontSize = (18.sp * scale), fontWeight = FontWeight.Bold, color = p.textSecondary, textAlign = TextAlign.Center)
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
            colors = CardDefaults.cardColors(containerColor = p.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = if (p.borderSoft != null) 0.dp else 4.dp),
            border = p.borderSoft?.let { BorderStroke(1.dp, it) }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Últimos Testes Realizados", fontWeight = FontWeight.Bold, fontSize = (18.sp * scale), color = p.textPrimary)
                Spacer(Modifier.height(12.dp))
                items.takeLast(5).reversed().forEachIndexed { idx, r ->
                    val localDate = LocalDateTime.ofInstant(r.date, ZoneId.systemDefault())
                    val label = DateTimeFormatter.ofPattern("dd/MM").format(localDate)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(LocalReportsPalette.current.surfaceVariant)
                            .padding(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Data: $label", fontWeight = FontWeight.Bold, fontSize = (16.sp * scale), color = p.textPrimary)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PanTool, contentDescription = null, tint = p.accent, modifier = Modifier.size(14.dp))
                                Text(if (r.withRightHand) "Direita" else "Esquerda", fontSize = (12.sp * scale), fontWeight = FontWeight.SemiBold, color = p.accent)
                                if (r.withMainHand) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Toques/Minuto", fontSize = (13.sp * scale), fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                                Text("${r.clicksPerMinute}", fontSize = (15.sp * scale), fontWeight = FontWeight.Bold, color = p.textPrimary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Total de Toques", fontSize = (13.sp * scale), fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                                Text("${r.totalClicks}", fontSize = (15.sp * scale), fontWeight = FontWeight.Bold, color = p.textPrimary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Toques Errados", fontSize = (13.sp * scale), fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                                Text("${r.missedClicks}", fontSize = (15.sp * scale), fontWeight = FontWeight.Bold, color = if (r.missedClicks > 3) Color(0xFFE57373) else p.textPrimary)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Duração Total", fontSize = (13.sp * scale), fontWeight = FontWeight.SemiBold, color = p.textSecondary)
                                Text("${String.format("%.1f", r.secondsTotal)}s", fontSize = (15.sp * scale), fontWeight = FontWeight.Bold, color = p.textPrimary)
                            }
                        }
                    }
                    if (idx < 4) Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}
