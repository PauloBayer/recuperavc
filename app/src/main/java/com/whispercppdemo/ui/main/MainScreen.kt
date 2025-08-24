package com.whispercppdemo.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.whispercppdemo.R
import com.whispercppdemo.ui.theme.*

@Composable
fun MainScreen(viewModel: MainScreenViewModel) {
    MainScreen(
        canTranscribe = viewModel.canTranscribe,
        isRecording = viewModel.isRecording,
        isLoading = viewModel.isLoading,
        isProcessing = viewModel.isProcessing,
        transcriptionResult = viewModel.transcriptionResult,
        analysisResult = viewModel.analysisResult,
        onRecordTapped = viewModel::toggleRecord,
        onClearResults = viewModel::clearResults
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    canTranscribe: Boolean,
    isRecording: Boolean,
    isLoading: Boolean,
    isProcessing: Boolean,
    transcriptionResult: String,
    analysisResult: AnalysisResult?,
    onRecordTapped: () -> Unit,
    onClearResults: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GreenLight,
                        GreenPrimary,
                        BackgroundGreen
                    ),
                    radius = 1200f
                )
            )
    ) {
        IconButton(
            onClick = { },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Fechar",
                tint = OnBackground,
                modifier = Modifier.size(28.dp)
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = OnBackground,
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Carregando modelo de IA...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnBackground,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(60.dp))

                Text(
                    text = "Pronuncie a frase abaixo\npara avaliar sua recuperação",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = OnBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp
                )

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    RecordingCircles(
                        isRecording = isRecording,
                        isProcessing = isProcessing,
                        enabled = canTranscribe && !isProcessing,
                        onClick = onRecordTapped
                    )
                }

                Text(
                    text = "O rato roeu a roupa do\nrei de roma",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            if ((transcriptionResult.isNotEmpty() || analysisResult != null) && !isProcessing) {
                ResultsOverlay(
                    transcriptionResult = transcriptionResult,
                    analysisResult = analysisResult,
                    onDismiss = onClearResults
                )
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RecordingCircles(
    isRecording: Boolean,
    isProcessing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val micPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.RECORD_AUDIO,
        onPermissionResult = { granted ->
            if (granted) {
                onClick()
            }
        }
    )

    val animationDuration = 2000
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    
    val outerCircleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "outer_scale"
    )

    val middleCircleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration + 300, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "middle_scale"
    )

    val innerCircleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDuration + 600, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "inner_scale"
    )

    Box(
        modifier = Modifier.size(300.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isRecording || isProcessing) {
            Canvas(
                modifier = Modifier.size(300.dp)
            ) {
                val center = size.center
                val outerRadius = 140.dp.toPx() * if (isRecording) outerCircleScale else 1f
                val middleRadius = 110.dp.toPx() * if (isRecording) middleCircleScale else 1f
                val innerRadius = 80.dp.toPx() * if (isRecording) innerCircleScale else 1f

                drawCircle(
                    color = GreenLight.copy(alpha = 0.2f),
                    radius = outerRadius,
                    center = center
                )

                drawCircle(
                    color = GreenLight.copy(alpha = 0.4f),
                    radius = middleRadius,
                    center = center
                )

                drawCircle(
                    color = GreenAccent.copy(alpha = 0.6f),
                    radius = innerRadius,
                    center = center
                )
            }
        } else {
            Canvas(
                modifier = Modifier.size(300.dp)
            ) {
                val center = size.center
                val outerRadius = 140.dp.toPx()
                val middleRadius = 110.dp.toPx()
                val innerRadius = 80.dp.toPx()

                drawCircle(
                    color = GreenLight.copy(alpha = 0.15f),
                    radius = outerRadius,
                    center = center
                )

                drawCircle(
                    color = GreenLight.copy(alpha = 0.25f),
                    radius = middleRadius,
                    center = center
                )

                drawCircle(
                    color = GreenAccent.copy(alpha = 0.35f),
                    radius = innerRadius,
                    center = center
                )
            }
        }

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    color = when {
                        !enabled -> Color.Gray.copy(alpha = 0.7f)
                        isRecording -> Color(0xFF4A4A4A)
                        isProcessing -> GreenDark
                        else -> GreenDark
                    }
                )
                .clickable(enabled = enabled) {
                    if (micPermissionState.status.isGranted) {
                        onClick()
                    } else {
                        micPermissionState.launchPermissionRequest()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = OnSurface,
                    strokeWidth = 4.dp
                )
            } else {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Parar gravação" else "Iniciar gravação",
                    tint = OnSurface,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
    }
}

@Composable
private fun ResultsOverlay(
    transcriptionResult: String,
    analysisResult: AnalysisResult?,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .clickable(enabled = false) { },
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📊 Acompanhamento de Progresso",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = GreenDark
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (transcriptionResult.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = GreenLight.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "📝 Fala Reconhecida:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "\"$transcriptionResult\"",
                                fontSize = 16.sp,
                                fontStyle = FontStyle.Italic,
                                color = GreenDark,
                                lineHeight = 22.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }

                analysisResult?.let { result ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricCard(
                            title = "Velocidade",
                            value = "${result.wpm}",
                            unit = "WPM",
                            color = when {
                                result.wpm >= 100 -> GreenDark
                                result.wpm >= 50 -> Color(0xFFF57C00)
                                else -> Color(0xFF1976D2)
                            },
                            icon = "⚡"
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        MetricCard(
                            title = "Precisão",
                            value = "${String.format("%.1f", 100 - result.wer)}",
                            unit = "%",
                            color = when {
                                result.wer <= 15 -> GreenDark
                                result.wer <= 30 -> Color(0xFFF57C00)
                                else -> Color(0xFF1976D2)
                            },
                            icon = "🎯"
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                result.wer <= 15 && result.wpm >= 80 -> GreenLight.copy(alpha = 0.15f)
                                result.wer <= 30 && result.wpm >= 40 -> Color(0xFFFFF3E0)
                                else -> Color(0xFFE3F2FD)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = when {
                                result.wer <= 15 && result.wpm >= 80 -> "🌟 Ótimo progresso! Sua fala está bem desenvolvida."
                                result.wer <= 30 && result.wpm >= 40 -> "📈 Progresso consistente! Continue com os exercícios."
                                else -> "🎯 Mantenha a regularidade nos exercícios de reabilitação."
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                result.wer <= 15 && result.wpm >= 80 -> GreenDark
                                result.wer <= 30 && result.wpm >= 40 -> Color(0xFFE65100)
                                else -> Color(0xFF1565C0)
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🎤 Novo Exercício",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    icon: String
) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.08f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = " $unit",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = color.copy(alpha = 0.7f)
                )
            }
        }
    }
}

