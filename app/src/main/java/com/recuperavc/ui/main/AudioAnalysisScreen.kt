package com.recuperavc.ui.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TrendingUp
import com.recuperavc.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.sfx.Sfx
import com.recuperavc.ui.sfx.rememberSfxController
import com.recuperavc.ui.theme.BackgroundGreen
import com.recuperavc.ui.theme.GreenAccent
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight
import com.recuperavc.ui.theme.GreenPrimary
import com.recuperavc.ui.theme.OnBackground
import com.recuperavc.ui.theme.OnSurface
import com.recuperavc.ui.util.InitialSettings
import com.recuperavc.ui.util.PaintSystemBars
import com.recuperavc.ui.util.rememberInitialSettings
import com.recuperavc.ui.components.*
import kotlinx.coroutines.flow.collectLatest

private val HighContrastAccent = Color(0xFFFFD600)

@Composable
fun AudioAnalysisScreen(
    viewModel: MainScreenViewModel = viewModel(factory = MainScreenViewModel.factory()),
    onBack: () -> Unit
) {
    val sfx = rememberSfxController()
    LaunchedEffect(viewModel, sfx) {
        viewModel.sfx.collectLatest { sfx.play(it) }
    }

    // Read settings with first-frame gate (prevents white flash)
    val context = LocalContext.current
    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val initial: InitialSettings? = rememberInitialSettings(settings)
    if (initial == null) {
        PaintSystemBars(background = Color.Black, lightIcons = false)
        Box(Modifier.fillMaxSize().background(Color.Black)) {}
        return
    }

    val appliedDark by settings.darkModeFlow.collectAsState(initial = initial.dark)
    val appliedContrast by settings.contrastFlow.collectAsState(initial = initial.contrast)
    val appliedScale by settings.sizeTextFlow.collectAsState(initial = initial.scale)

    // Palette
    val bgSolid = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF0E120F)
        else -> BackgroundGreen
    }
    val textPrimary = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEAEAEA)
        else -> OnBackground
    }
    val accent = if (appliedContrast) HighContrastAccent else GreenDark

    PaintSystemBars(
        background = if (appliedContrast || appliedDark) Color.Black else BackgroundGreen,
        lightIcons = !(appliedContrast || appliedDark)
    )

    var showEndDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        sfx.play(Sfx.CLICK)
        showEndDialog = true
    }

    AudioAnalysisContent(
        canTranscribe = viewModel.canTranscribe,
        isRecording = viewModel.isRecording,
        isLoading = viewModel.isLoading,
        isProcessing = viewModel.isProcessing,
        phraseText = viewModel.phraseText,
        isCancelling = viewModel.isCancelling,
        sessionCount = viewModel.sessionCount,
        onRecordTapped = { sfx.play(Sfx.CLICK); viewModel.toggleRecord() },
        onCancelRecording = { viewModel.cancelRecording() },
        onFinishSession = {
            sfx.play(Sfx.CLICK)
            viewModel.finishSession { saved ->
                if (saved) sfx.play(Sfx.RIGHT_ANSWER) else sfx.play(Sfx.WRONG_ANSWER)
            }
        },
        onBack = { sfx.play(Sfx.CLICK); showEndDialog = true },
        appliedContrast = appliedContrast,
        appliedDark = appliedDark,
        appliedScale = appliedScale,
        backgroundSolid = bgSolid,
        textPrimary = textPrimary,
        accent = accent
    )

    if (showEndDialog) {
        EndSessionDialog(
            count = viewModel.sessionCount,
            appliedContrast = appliedContrast,
            appliedDark = appliedDark,
            appliedScale = appliedScale,
            textPrimary = textPrimary,
            accent = accent,
            onConfirm = {
                sfx.play(Sfx.CLICK)
                if (viewModel.sessionCount >= 3) {
                    viewModel.finishSession { saved ->
                        if (saved) sfx.play(Sfx.RIGHT_ANSWER) else sfx.play(Sfx.WRONG_ANSWER)
                    }
                } else {
                    viewModel.discardSession()
                    onBack()
                }
                showEndDialog = false
            },
            onDismiss = { sfx.play(Sfx.CLICK); showEndDialog = false }
        )
    }

    viewModel.sessionSummary?.let { summary ->
        SessionSummaryScreen(
            summary = summary,
            appliedContrast = appliedContrast,
            appliedDark = appliedDark,
            appliedScale = appliedScale,
            textPrimary = textPrimary,
            accent = accent,
            onClose = { sfx.play(Sfx.CLICK); viewModel.dismissSummary() },
            onNavigateHome = { sfx.play(Sfx.CLICK); onBack() }
        )
    }
}

/* ------------------------------ DIALOG ------------------------------ */

@Composable
private fun EndSessionDialog(
    count: Int,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    textPrimary: Color,
    accent: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Colors that depend on the current mode
    val container = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF1E1E1E)
        else -> Color.White
    }
    val titleColor = when {
        appliedContrast -> accent
        appliedDark -> Color.White
        else -> Color(0xFF1B1B1B)
    }
    val bodyColor = if (appliedContrast || appliedDark) Color.White else Color(0xFF3A3A3A)

    val confirmContainer = when {
        appliedContrast -> accent
        appliedDark -> GreenDark
        else -> Color.White
    }
    val confirmContent = when {
        appliedContrast -> Color.Black
        appliedDark -> Color.White
        else -> Color(0xFF2E7D32)
    }
    val dismissContent = when {
        appliedContrast -> accent
        appliedDark -> Color(0xFF8BC34A) // readable green on dark
        else -> GreenDark
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = container,
        titleContentColor = titleColor,
        textContentColor = bodyColor,
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = confirmContainer,
                    contentColor = confirmContent
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    if (count >= 3) "Salvar e Sair" else "Descartar e Sair",
                    fontSize = 16.sp * appliedScale
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = dismissContent)
            ) { Text("Continuar", fontSize = 16.sp * appliedScale) }
        },
        title = {
            Text(
                "Encerrar sessão",
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp * appliedScale
            )
        },
        text = {
            Text(
                if (count >= 3)
                    "Você gravou ${count} áudios. Deseja salvar o relatório e sair?"
                else
                    "Você gravou ${count} de 3 áudios mínimos. Se sair agora, os resultados não serão salvos.",
                fontSize = 15.sp * appliedScale,
                lineHeight = 20.sp * appliedScale
            )
        }
    )
}

/* ---------------------------- MAIN CONTENT --------------------------- */

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
    onFinishSession: () -> Unit,
    onBack: () -> Unit,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    backgroundSolid: Color,
    textPrimary: Color,
    accent: Color
) {
    val root = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)
        .let { base ->
            when {
                appliedContrast -> base.background(Color.Black)
                isLoading || isProcessing || appliedDark -> base.background(backgroundSolid)
                else -> base.background(
                    brush = Brush.radialGradient(
                        colors = listOf(GreenLight, GreenPrimary, BackgroundGreen),
                        radius = 1200f
                    )
                )
            }
        }

    Box(modifier = root) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BackButton(onBack, tint = if (appliedContrast || appliedDark) Color.White else OnBackground)
        }

        if (!isLoading) {
            Text(
                text = "Sessão ${sessionCount} de 3 (mínimo)",
                color = textPrimary,
                fontSize = 18.sp * appliedScale,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .padding(top = 72.dp + WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .align(Alignment.TopCenter)
            )
        }

        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = textPrimary,
                        strokeWidth = 6.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Carregando modelo de IA...",
                        fontSize = 18.sp * appliedScale,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary,
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
                Spacer(Modifier.height(120.dp))

                Text(
                    text = "Pronuncie a frase abaixo\npara avaliar sua recuperação",
                    fontSize = 20.sp * appliedScale,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp * appliedScale
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
                        onCancel = onCancelRecording,
                        appliedContrast = appliedContrast,
                        appliedDark = appliedDark,
                        accent = accent
                    )
                }

                val chipContainer = when {
                    appliedContrast -> Color.Black
                    appliedDark -> Color(0xFF2A2A2A)
                    else -> Color.White.copy(alpha = 0.96f)
                }
                val chipLabel = when {
                    appliedContrast -> Color.White
                    appliedDark -> Color(0xFFEDEDED)
                    else -> Color(0xFF1B1B1B)
                }
                val chipBorder = when {
                    appliedContrast -> BorderStroke(1.dp, accent.copy(alpha = 0.7f))
                    appliedDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                    else -> null
                }
                val chipIconBg = when {
                    appliedContrast -> accent
                    appliedDark -> GreenDark
                    else -> GreenDark
                }
                val chipIconTint = if (appliedContrast) Color.Black else Color.White
                AnimatedVisibility(
                    visible = isRecording && !isProcessing && !isCancelling,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        shape = RoundedCornerShape(50),
                        colors = CardDefaults.cardColors(containerColor = chipContainer),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 3.dp),
                        border = chipBorder,
                        modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(chipIconBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = chipIconTint,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Toque no botão grande novamente para enviar agora",
                                fontSize = 14.sp * appliedScale,
                                color = chipLabel
                            )
                        }
                    }
                }

                Text(
                    text = phraseText,
                    fontSize = 24.sp * appliedScale,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 32.sp * appliedScale
                )

                Column {
                    if (sessionCount >= 3) {
                        Button(
                            onClick = onFinishSession,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appliedContrast) accent else Color.White,
                                contentColor = if (appliedContrast) Color.Black else Color(0xFF2E7D32)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "Registrar e Salvar Sessão",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp * appliedScale
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    if (isRecording) {
                        Button(
                            onClick = onCancelRecording,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD32F2F),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .size(64.dp)
                                .align(Alignment.CenterHorizontally),
                            shape = CircleShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar gravação",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(32.dp))
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
                        indication = null,
                        onClick = { /* block inputs */ }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = textPrimary,
                        strokeWidth = 6.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Analisando sua gravação...",
                        fontSize = 18.sp * appliedScale,
                        fontWeight = FontWeight.Medium,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/* --------------------------- Recording UI --------------------------- */

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun RecordingCircles(
    isRecording: Boolean,
    isProcessing: Boolean,
    isCancelling: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    accent: Color
) {
    val micPermissionState = rememberPermissionState(
        permission = android.Manifest.permission.RECORD_AUDIO,
        onPermissionResult = { granted -> if (granted) onClick() }
    )

    val infinite = rememberInfiniteTransition(label = "recording")
    val pulse = rememberInfiniteTransition(label = "pulse")
    val outerScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOut), RepeatMode.Reverse),
        label = "outer"
    )
    val middleScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(2300, easing = EaseInOut), RepeatMode.Reverse),
        label = "middle"
    )
    val innerScale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = EaseInOut), RepeatMode.Reverse),
        label = "inner"
    )
    val outerColor: Color
    val middleColor: Color
    val innerColor: Color
    if (appliedContrast) {
        outerColor = Color.White.copy(alpha = 0.20f)
        middleColor = Color.White.copy(alpha = 0.40f)
        innerColor = accent.copy(alpha = 0.75f)
    } else {
        outerColor = GreenLight.copy(alpha = if (isRecording || isProcessing) 0.20f else 0.15f)
        middleColor = GreenLight.copy(alpha = if (isRecording || isProcessing) 0.40f else 0.25f)
        innerColor = GreenAccent.copy(alpha = if (isRecording || isProcessing) 0.60f else 0.35f)
    }

    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val center = size.center
            drawCircle(color = outerColor, radius = 140.dp.toPx() * (if (isRecording) outerScale else 1f), center = center)
            drawCircle(color = middleColor, radius = 110.dp.toPx() * (if (isRecording) middleScale else 1f), center = center)
            drawCircle(color = innerColor, radius = 80.dp.toPx() * (if (isRecording) innerScale else 1f), center = center)
        }

        val micBg = when {
            !enabled -> Color.Gray.copy(alpha = 0.7f)
            appliedContrast -> accent
            isProcessing -> GreenDark
            else -> GreenDark
        }
        val micIcon = if (appliedContrast) Color.Black else Color.White

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(micBg)
                .clickable(enabled = enabled) {
                    if (micPermissionState.status.isGranted) onClick()
                    else micPermissionState.launchPermissionRequest()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isRecording) "Gravando" else "Iniciar gravação",
                tint = micIcon,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}

/* --------------------------- Misc components ------------------------ */

@Composable
private fun BackButton(onBack: () -> Unit, tint: Color) {
    IconButton(onClick = onBack) {
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Voltar",
            tint = tint,
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    appliedContrast: Boolean,
    appliedScale: Float,
    appliedDark: Boolean = false
) {
    val container = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF2A2A2A)
        else -> Color.White.copy(alpha = 0.95f)
    }
    val border = when {
        appliedContrast -> BorderStroke(2.dp, color)
        appliedDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        else -> null
    }
    val titleColor = when {
        appliedContrast -> Color.White.copy(alpha = 0.8f)
        appliedDark -> Color(0xFFCCCCCC)
        else -> Color.Gray.copy(alpha = 0.8f)
    }
    val valueColor = when {
        appliedContrast -> Color.White
        appliedDark -> Color.White
        else -> color
    }
    val unitColor = when {
        appliedContrast -> Color.White.copy(alpha = 0.7f)
        appliedDark -> Color(0xFFCCCCCC)
        else -> color.copy(alpha = 0.7f)
    }

    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 8.dp),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(text = title, fontSize = 13.sp * appliedScale, fontWeight = FontWeight.Medium, color = titleColor)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = value, fontSize = 26.sp * appliedScale, fontWeight = FontWeight.Bold, color = valueColor)
                Text(text = " $unit", fontSize = 14.sp * appliedScale, fontWeight = FontWeight.Medium, color = unitColor)
            }
        }
    }
}

@Composable
private fun SessionSummaryScreen(
    summary: MainScreenViewModel.SessionSummary,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    textPrimary: Color,
    accent: Color,
    onClose: () -> Unit,
    onNavigateHome: () -> Unit
) {
    val cardContainer = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF1E1E1E)
        else -> Color.White
    }
    val titleColor = when {
        appliedContrast -> accent
        appliedDark -> Color.White
        else -> Color(0xFF1B1B1B)
    }
    val labelColor = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEDEDED)
        else -> Color(0xFF3A3A3A)
    }
    val itemCardContainer = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF2A2A2A)
        else -> Color(0xFFF5F5F5)
    }
    val itemTextColor = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEDEDED)
        else -> Color(0xFF1B1B1B)
    }
    val cardBorder = when {
        appliedContrast -> BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        appliedDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        else -> null
    }

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
            colors = CardDefaults.cardColors(containerColor = cardContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 6.dp),
            border = cardBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Relatório do Teste",
                    fontSize = 22.sp * appliedScale,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    MetricCard(
                        title = "Velocidade média",
                        value = String.format("%.0f", summary.avgWpm),
                        unit = "WPM",
                        color = accent,
                        icon = Icons.Default.Speed,
                        appliedContrast = appliedContrast,
                        appliedScale = appliedScale,
                        appliedDark = appliedDark
                    )
                    MetricCard(
                        title = "Precisão média",
                        value = String.format("%.1f", 100 - summary.avgWer),
                        unit = "%",
                        color = accent,
                        icon = Icons.Default.TrendingUp,
                        appliedContrast = appliedContrast,
                        appliedScale = appliedScale,
                        appliedDark = appliedDark
                    )
                }
                Spacer(Modifier.height(16.dp))
                val ctx = LocalContext.current
                val brWpm = kotlin.runCatching { ctx.getString(R.string.br_avg_wpm).replace(",", ".").toDouble() }.getOrElse { 150.0 }
                val brWer = kotlin.runCatching { ctx.getString(R.string.br_avg_wer).replace(",", ".").toDouble() }.getOrElse { 12.0 }
                val wpmUser = summary.avgWpm.toDouble()
                val werUser = summary.avgWer.toDouble()
                val goodColor = if (appliedContrast) accent else Color(0xFF2E7D32)
                val badColor = if (appliedContrast) Color(0xFFFF8A80) else Color(0xFFD32F2F)
                val goodBg = if (appliedContrast) accent.copy(alpha = 0.2f) else goodColor.copy(alpha = 0.15f)
                val badBg = badColor.copy(alpha = 0.15f)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = itemCardContainer),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 1.dp),
                    border = if (appliedContrast) BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)) else if (appliedDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("Referência (PT-BR)", fontWeight = FontWeight.SemiBold, color = labelColor, fontSize = 14.sp * appliedScale)
                        Spacer(Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WPM", fontWeight = FontWeight.Medium, color = itemTextColor, fontSize = 12.sp * appliedScale)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("BR: ${String.format("%.0f", brWpm)}", color = labelColor, fontSize = 12.sp * appliedScale)
                                    Text("Você: ${String.format("%.0f", wpmUser)}", color = accent, fontSize = 12.sp * appliedScale)
                                }
                                Spacer(Modifier.height(6.dp))
                                val wpmAbove = wpmUser >= brWpm
                                val wpmDiffPct = if (brWpm > 0) ((wpmUser - brWpm) / brWpm * 100.0) else 0.0
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (wpmAbove) goodBg else badBg)
                                ) {
                                    Text(
                                        text = if (wpmAbove) "Acima da média (+${String.format("%.0f", wpmDiffPct)}%)" else "Abaixo da média (${String.format("%.0f", wpmDiffPct)}%)",
                                        color = if (wpmAbove) goodColor else badColor,
                                        fontSize = 12.sp * appliedScale,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("WER", fontWeight = FontWeight.Medium, color = itemTextColor, fontSize = 12.sp * appliedScale)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("BR: ${String.format("%.1f", brWer)}%", color = labelColor, fontSize = 12.sp * appliedScale)
                                    Text("Você: ${String.format("%.1f", werUser)}%", color = accent, fontSize = 12.sp * appliedScale)
                                }
                                Spacer(Modifier.height(6.dp))
                                val werBetter = werUser <= brWer
                                val werDiffPct = if (brWer > 0) ((brWer - werUser) / brWer * 100.0) else 0.0
                                Card(
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = if (werBetter) goodBg else badBg)
                                ) {
                                    Text(
                                        text = if (werBetter) "Melhor que a média (+${String.format("%.0f", werDiffPct)}%)" else "Pior que a média (${String.format("%.0f", -werDiffPct)}%)",
                                        color = if (werBetter) goodColor else badColor,
                                        fontSize = 12.sp * appliedScale,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                val wpmAbove = wpmUser >= brWpm * 1.1
                val wpmBelow = wpmUser <= brWpm * 0.9
                val werBetter = werUser <= brWer * 1.0
                val werMuchBetter = werUser <= brWer * 0.9
                val werWorse = werUser >= brWer * 1.1
                val headline: String
                val body: String
                when {
                    wpmAbove && (werMuchBetter || werBetter) -> {
                        headline = "Ótimo ritmo"
                        body = "Sua velocidade está acima da referência com boa precisão. Mantenha a prática regular e avance para frases mais longas quando se sentir confortável."
                    }
                    wpmBelow && (werMuchBetter || werBetter) -> {
                        headline = "Base sólida"
                        body = "Boa precisão. Agora, aumente a velocidade gradualmente: repita a frase, respire fundo e tente manter um ritmo contínuo."
                    }
                    wpmAbove && werWorse -> {
                        headline = "Ajuste fino"
                        body = "Velocidade alta, mas com mais erros. Diminua um pouco o ritmo e articule cada palavra com calma para melhorar a precisão."
                    }
                    wpmBelow && werWorse -> {
                        headline = "Seguimos juntos"
                        body = "É normal oscilar. Comece com frases curtas, foque em respirar e pronunciar com clareza. A velocidade vem com a prática."
                    }
                    else -> {
                        headline = "Bom caminho"
                        body = "Você está próximo da referência. Continue praticando e ajuste suavemente ritmo e articulação para evoluir."
                    }
                }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = itemCardContainer),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 1.dp),
                    border = if (appliedContrast) BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)) else if (appliedDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(headline, fontWeight = FontWeight.Bold, color = accent, fontSize = 16.sp * appliedScale)
                        Spacer(Modifier.height(6.dp))
                        Text(body, color = itemTextColor, fontSize = 14.sp * appliedScale)
                        Spacer(Modifier.height(8.dp))
                        Text("Dicas rápidas", fontWeight = FontWeight.SemiBold, color = labelColor, fontSize = 12.sp * appliedScale)
                        Spacer(Modifier.height(4.dp))
                        Text("• Respiração tranquila antes de falar", color = itemTextColor, fontSize = 12.sp * appliedScale)
                        Text("• Articule sílabas com clareza", color = itemTextColor, fontSize = 12.sp * appliedScale)
                        Text("• Comece devagar e aumente o ritmo aos poucos", color = itemTextColor, fontSize = 12.sp * appliedScale)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Tentativas", fontWeight = FontWeight.SemiBold, color = labelColor)
                Spacer(Modifier.height(8.dp))
                summary.items.forEachIndexed { idx, item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = itemCardContainer),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 1.dp),
                        border = if (appliedContrast) BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)) else if (appliedDark) BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)) else null
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${idx + 1}. ${item.phrase}", fontWeight = FontWeight.Medium, color = itemTextColor)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("WPM: ${item.wpm}", color = accent)
                                Text("Precisão: ${String.format("%.1f", 100 - item.wer)}%", color = accent)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = if (appliedContrast) Color.Black else Color.White),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Novo teste", fontSize = 16.sp * appliedScale)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onNavigateHome,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Voltar ao Início", fontSize = 16.sp * appliedScale) }
            }
        }
    }
}
