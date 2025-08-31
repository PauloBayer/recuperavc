package com.recuperavc.ui.main

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
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.LinearEasing
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.recuperavc.ui.theme.*
import com.recuperavc.ui.main.MainScreenViewModel
import com.recuperavc.ui.main.AnalysisResult

@Composable
fun MainScreen(viewModel: MainScreenViewModel = viewModel(factory = MainScreenViewModel.factory())) {
    MainScreenContent(
        canTranscribe = viewModel.canTranscribe,
        isRecording = viewModel.isRecording,
        isLoading = viewModel.isLoading,
        isProcessing = viewModel.isProcessing,
        transcriptionResult = viewModel.transcriptionResult,
        analysisResult = viewModel.analysisResult,
        phraseText = viewModel.phraseText,
        isCancelling = viewModel.isCancelling,
        onRecordTapped = viewModel::toggleRecord,
        onCancelRecording = viewModel::cancelRecording,
        onClearResults = {
            viewModel.clearResults()
            viewModel.loadNewPhrase()
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    canTranscribe: Boolean,
    isRecording: Boolean,
    isLoading: Boolean,
    isProcessing: Boolean,
    transcriptionResult: String,
    analysisResult: AnalysisResult?,
    phraseText: String,
    isCancelling: Boolean,
    onRecordTapped: () -> Unit,
    onCancelRecording: () -> Unit,
    onClearResults: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(GreenLight, GreenPrimary, BackgroundGreen),
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        isCancelling = isCancelling,
                        enabled = canTranscribe && !isProcessing && !isCancelling,
                        onClick = onRecordTapped,
                        onCancel = onCancelRecording
                    )
                }

                Text(
                    text = phraseText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp
                )

                Spacer(modifier = Modifier.height(32.dp))
                
                if (isRecording) {
                    Button(
                        onClick = onCancelRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFD32F2F)
                        ),
                        modifier = Modifier
                            .size(56.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancelar gravação",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

        }
        
        if ((transcriptionResult.isNotEmpty() || analysisResult != null) && !isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(8.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(GreenLight, GreenPrimary, BackgroundGreen),
                                radius = 1200f
                            )
                        )
                )
                
                ResultsSection(
                    transcriptionResult = transcriptionResult,
                    analysisResult = analysisResult,
                    onNewExercise = onClearResults
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
    isCancelling: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit
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
    val pulseAnimationDuration = 1200
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val pulseTransition = rememberInfiniteTransition(label = "pulse")

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
    
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseAnimationDuration, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    val loadingRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loading_rotation"
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
                }
                .let { modifier ->
                    if (isRecording) {
                        modifier.then(
                            Modifier.then(
                                Modifier.background(
                                    color = Color(0xFF4A4A4A),
                                    shape = CircleShape
                                )
                            )
                        )
                    } else modifier
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                isProcessing -> {
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = OnSurface,
                            strokeWidth = 4.dp
                        )
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Processando",
                            tint = OnSurface.copy(alpha = 0.6f),
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    rotationZ = loadingRotation
                                }
                        )
                    }
                }
                isRecording -> {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Gravando",
                        tint = OnSurface,
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer {
                                scaleX = pulseScale
                                scaleY = pulseScale
                            }
                    )
                }
                else -> {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Iniciar gravação",
                        tint = OnSurface,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultsSection(
    transcriptionResult: String,
    analysisResult: AnalysisResult?,
    onNewExercise: () -> Unit
) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Resultado da Análise",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground,
                textAlign = TextAlign.Center
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (transcriptionResult.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Text(
                                text = "Fala Reconhecida",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = GreenDark,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "\"$transcriptionResult\"",
                                fontSize = 18.sp,
                                fontStyle = FontStyle.Italic,
                                color = Color.Black.copy(alpha = 0.8f),
                                lineHeight = 24.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
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
                            icon = Icons.Default.Speed
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
                            icon = Icons.Default.TrendingUp
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White.copy(alpha = 0.92f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Text(
                            text = when {
                                result.wer <= 15 && result.wpm >= 80 -> "Ótimo progresso! Sua fala está bem desenvolvida."
                                result.wer <= 30 && result.wpm >= 40 -> "Progresso consistente! Continue com os exercícios."
                                else -> "Mantenha a regularidade nos exercícios de reabilitação."
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = when {
                                result.wer <= 15 && result.wpm >= 80 -> GreenDark
                                result.wer <= 30 && result.wpm >= 40 -> Color(0xFFE65100)
                                else -> Color(0xFF1565C0)
                            },
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            Button(
                onClick = onNewExercise,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenDark
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Novo Exercício",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
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
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Gray.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = value,
                    fontSize = 26.sp,
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
