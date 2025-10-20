package com.recuperavc.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.recuperavc.ui.theme.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import com.recuperavc.ui.components.HistoryDialog
import com.recuperavc.ui.main.MainScreenViewModel
import com.recuperavc.ui.sfx.Sfx
import com.recuperavc.ui.sfx.rememberSfxController
import kotlinx.coroutines.flow.collectLatest

@Composable
fun AudioAnalysisScreen(viewModel: MainScreenViewModel = viewModel(factory = MainScreenViewModel.factory()), onBack: () -> Unit) {
    val sfx = rememberSfxController()

    // Play any sounds emitted by the ViewModel
    LaunchedEffect(viewModel, sfx) {
        viewModel.sfx.collectLatest { sfx.play(it) }
    }

    var showEndDialog by remember { mutableStateOf(false) }
    AudioAnalysisContent(
        canTranscribe = viewModel.canTranscribe,
        isRecording = viewModel.isRecording,
        isLoading = viewModel.isLoading,
        isProcessing = viewModel.isProcessing,
        phraseText = viewModel.phraseText,
        isCancelling = viewModel.isCancelling,
        sessionCount = viewModel.sessionCount,
        onRecordTapped = {
            sfx.play(Sfx.CLICK)                 // tap mic
            viewModel.toggleRecord()
        },
        onCancelRecording = {
            viewModel.cancelRecording()
        },
        onClearResults = { viewModel.clearResults() },
        onFinishSession = {
            sfx.play(Sfx.CLICK)                 // press “Salvar sessão”
            viewModel.finishSession { saved ->
                if (saved) sfx.play(Sfx.RIGHT_ANSWER) else sfx.play(Sfx.WRONG_ANSWER)
            }
        },
        onBack = {
            sfx.play(Sfx.CLICK)                 // top back button
            showEndDialog = true
        }
    )
    if (showEndDialog) {
        val count = viewModel.sessionCount
        AlertDialog(
            onDismissRequest = {
                sfx.play(Sfx.BUBBLE)
                showEndDialog = false
            },
            confirmButton = {
                Button(onClick = {
                    sfx.play(Sfx.CLICK)
                    if (count >= 3) {
                        viewModel.finishSession { saved ->
                            if (saved) sfx.play(Sfx.RIGHT_ANSWER) else sfx.play(Sfx.WRONG_ANSWER)
                        }
                    } else {
                        viewModel.discardSession()
                        onBack()
                    }
                    showEndDialog = false
                }) {
                    Text(if (count >= 3) "Salvar e Sair" else "Descartar e Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    sfx.play(Sfx.CLICK)
                    showEndDialog = false
                }) { Text("Continuar") }
            },
            title = { Text("Encerrar sessão") },
            text = {
                Text(
                    if (count >= 3)
                        "Você gravou ${count} áudios. Deseja salvar o relatório e sair?"
                    else
                        "Você gravou ${count} de 3 áudios mínimos. Se sair agora, os resultados não serão salvos."
                )
            }
        )
    }

    viewModel.sessionSummary?.let { summary ->
        SessionSummaryScreen(
            summary = summary,
            onClose = {
                sfx.play(Sfx.CLICK)
                viewModel.dismissSummary()
            },
            onNavigateHome = {
                sfx.play(Sfx.CLICK)
                onBack()
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioAnalysisContent(
    canTranscribe: Boolean,
    isRecording: Boolean,
    isLoading: Boolean,
    isProcessing: Boolean,
    phraseText: String,
    isCancelling: Boolean,
    sessionCount: Int,
    onRecordTapped: () -> Unit,
    onCancelRecording: () -> Unit,
    onClearResults: () -> Unit,
    onFinishSession: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .let { base ->
                if (isLoading || isProcessing) {
                    base.background(BackgroundGreen)
                } else {
                    base.background(
                        brush = Brush.radialGradient(
                            colors = listOf(GreenLight, GreenPrimary, BackgroundGreen),
                            radius = 1200f
                        )
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars) // evita notch/status bar
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack)
        }

        if (!isLoading) {
            Text(
                text = "Sessão ${sessionCount} de 3 (mínimo)",
                color = OnBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 72.dp)
                    .align(Alignment.TopCenter)
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
                Spacer(modifier = Modifier.height(120.dp))

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

                Column {
                    if (sessionCount >= 3) {
                        Button(
                            onClick = onFinishSession,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Registrar e Salvar Sessão",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (isRecording) {
                        Button(
                            onClick = onCancelRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F)
                            ),
                            modifier = Modifier
                                .size(56.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancelar gravação",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

        }
        
        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false),
                        onClick = { }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = OnBackground,
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analisando sua gravação...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = OnBackground,
                        textAlign = TextAlign.Center
                    )
                }
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
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Voltar",
            tint = OnBackground,
            modifier = Modifier.size(28.dp)
        )
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

@Composable
private fun SessionSummaryScreen(summary: MainScreenViewModel.SessionSummary, onClose: () -> Unit, onNavigateHome: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f))
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Relatório do Teste", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = OnBackground)
                Spacer(Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MetricCard(
                        title = "Velocidade média",
                        value = String.format("%.0f", summary.avgWpm),
                        unit = "WPM",
                        color = GreenDark,
                        icon = Icons.Default.Speed
                    )
                    MetricCard(
                        title = "Precisão média",
                        value = String.format("%.1f", 100 - summary.avgWer),
                        unit = "%",
                        color = GreenDark,
                        icon = Icons.Default.TrendingUp
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("Tentativas", fontWeight = FontWeight.SemiBold, color = OnBackground)
                Spacer(Modifier.height(8.dp))
                summary.items.forEachIndexed { idx, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${idx + 1}. ${item.phrase}", fontWeight = FontWeight.Medium, color = Color.Black)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("WPM: ${item.wpm}", color = GreenDark)
                                Text("Precisão: ${String.format("%.1f", 100 - item.wer)}%", color = GreenDark)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Novo teste", color = Color.White, fontSize = 16.sp)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNavigateHome,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = GreenDark
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Voltar ao Início", fontSize = 16.sp)
                }
            }
        }
    }
}
