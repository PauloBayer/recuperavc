package com.recuperavc.ui.main.motion

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recuperavc.models.MotionReport
import com.recuperavc.ui.main.components.BarChart
import com.recuperavc.ui.main.components.ChartCard
import com.recuperavc.ui.theme.GreenDark
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MotionReportSection(items: List<MotionReport>) {
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

