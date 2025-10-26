package com.recuperavc.ui.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight

@Composable
fun ChartCard(title: String, subtitle: String, content: @Composable () -> Unit) {
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
fun BarChart(
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
    val ticks = listOf(1f, 0.75f, 0.5f, 0.25f, 0f)
    val tickValues = ticks.map { minY + (maxY - minY) * it }
    fun fmt(v: Float): String = if (maxY >= 10f) v.toInt().toString() else String.format("%.1f", v)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.width(56.dp).height(17.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                if (yAxisLabel != null) {
                    Text(
                        yAxisLabel,
                        fontSize = 11.sp,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                }
            }
            Spacer(Modifier.weight(1f))
        }

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
            BoxWithConstraints(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight()
            ) {
                val containerHeight = maxHeight
                tickValues.forEachIndexed { index, v ->
                    val t = ticks[index]
                    val yPosition = 1f - t
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopEnd)
                            .offset(y = with(LocalDensity.current) {
                                (yPosition * containerHeight.toPx()).toDp() - 6.dp
                            }),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            fmt(v),
                            fontSize = 10.sp,
                            fontWeight = if (v == maxY || v == minY) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (v == maxY || v == minY) Color.Black else Color.Black.copy(alpha = 0.6f),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .horizontalScroll(scrollState)
            ) {
                val totalWidth = (points.size * barWidthDp).dp
                Box(
                    modifier = Modifier
                        .width(totalWidth)
                        .fillMaxHeight()
                ) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        val h = size.height
                        val w = size.width
                        tickValues.forEachIndexed { i, _ ->
                            val t = ticks[i]
                            val y = (1f - t) * h
                            drawLine(
                                color = Color(0xFFE0E0E0),
                                start = Offset(0f, y),
                                end = Offset(w, y),
                                strokeWidth = 1f
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .matchParentSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        points.forEachIndexed { idx, value ->
                            val ratio = if (maxY == minY) 0f else ((value - minY) / (maxY - minY)).coerceIn(0f, 1f)
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
                                        .fillMaxHeight(ratio)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(GreenDark)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(56.dp))
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
fun SimpleChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(GreenLight.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text, color = GreenDark, fontSize = 12.sp)
    }
}
