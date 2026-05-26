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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
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

    if (viewModel.modelLoadFailed) {
        BackHandler(enabled = true) { sfx.play(Sfx.CLICK); onBack() }
        ModelLoadErrorScreen(
            appliedContrast = appliedContrast,
            appliedDark = appliedDark,
            appliedScale = appliedScale,
            textPrimary = textPrimary,
            accent = accent,
            backgroundSolid = bgSolid,
            onBack = { sfx.play(Sfx.CLICK); onBack() }
        )
        return
    }

    var showEndDialog by remember { mutableStateOf(false) }
    var showCancelTranscriptionDialog by remember { mutableStateOf(false) }

    val disposalActivity = context as? android.app.Activity
    DisposableEffect(viewModel) {
        onDispose {
            if (disposalActivity?.isChangingConfigurations != true) {
                viewModel.resetForExit()
            }
        }
    }

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
        isCancellingTranscription = viewModel.isCancellingTranscription,
        sessionCount = viewModel.sessionCount,
        onRecordTapped = { sfx.play(Sfx.CLICK); viewModel.toggleRecord() },
        onCancelRecording = { viewModel.cancelRecording() },
        onRequestCancelTranscription = { sfx.play(Sfx.CLICK); showCancelTranscriptionDialog = true },
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

    if (showCancelTranscriptionDialog) {
        CancelTranscriptionDialog(
            appliedContrast = appliedContrast,
            appliedDark = appliedDark,
            appliedScale = appliedScale,
            accent = accent,
            onConfirm = {
                sfx.play(Sfx.CLICK)
                viewModel.cancelTranscription()
                showCancelTranscriptionDialog = false
            },
            onDismiss = {
                sfx.play(Sfx.CLICK)
                showCancelTranscriptionDialog = false
            }
        )
    }

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
    isCancellingTranscription: Boolean,
    sessionCount: Int,
    onRecordTapped: () -> Unit,
    onCancelRecording: () -> Unit,
    onRequestCancelTranscription: () -> Unit,
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
            val scrollState = rememberScrollState()
            var bottomAreaHeightPx by remember { mutableStateOf(0) }
            val density = LocalDensity.current
            val bottomAreaHeightDp = with(density) { bottomAreaHeightPx.toDp() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(64.dp))

                Text(
                    text = "Sessão ${sessionCount} de 3 (mínimo)",
                    color = textPrimary,
                    fontSize = 18.sp * appliedScale,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(32.dp))

                Text(
                    text = "Pronuncie a frase abaixo:",
                    fontSize = 22.sp * appliedScale,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center,
                    lineHeight = 28.sp * appliedScale
                )
                Spacer(Modifier.height(20.dp))

                PhrasePresentation(
                    text = phraseText,
                    appliedContrast = appliedContrast,
                    appliedDark = appliedDark,
                    appliedScale = appliedScale,
                    textPrimary = textPrimary,
                    accent = accent
                )

                Spacer(Modifier.height(36.dp))

                AnimatedVisibility(
                    visible = isRecording && !isProcessing && !isCancelling,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RecordingPill(
                            appliedContrast = appliedContrast,
                            appliedDark = appliedDark,
                            appliedScale = appliedScale
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Toque no microfone novamente para enviar",
                            color = textPrimary.copy(alpha = 0.85f),
                            fontSize = 13.sp * appliedScale,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

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

                Spacer(Modifier.height(bottomAreaHeightDp + 16.dp))
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { bottomAreaHeightPx = it.height }
                    .padding(horizontal = 16.dp)
                    .padding(top = 18.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AnimatedVisibility(
                    visible = !isRecording && !isProcessing && !isCancelling,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TapToRecordHint(
                        text = "Toque no microfone para começar",
                        appliedScale = appliedScale
                    )
                }

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
                            .height(54.dp)
                    ) {
                        Text(
                            text = "Registrar e Salvar Sessão",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp * appliedScale
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isRecording,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Button(
                        onClick = onCancelRecording,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = when {
                                appliedContrast -> Color(0xFFFF5252)
                                appliedDark -> Color(0xFFB71C1C)
                                else -> Color(0xFFD32F2F)
                            },
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Cancelar gravação",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp * appliedScale
                        )
                    }
                }
            }
        }

        if (isProcessing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { /* block inputs */ }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(64.dp),
                        color = Color.White,
                        strokeWidth = 6.dp
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = if (isCancellingTranscription) "Cancelando envio do áudio..." else "Analisando sua gravação...",
                        fontSize = 18.sp * appliedScale,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    if (!isCancellingTranscription) {
                        Spacer(Modifier.height(28.dp))
                        Button(
                            onClick = onRequestCancelTranscription,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    appliedContrast -> Color(0xFFFF5252)
                                    appliedDark -> Color(0xFFB71C1C)
                                    else -> Color(0xFFD32F2F)
                                },
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Cancelar envio do áudio",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp * appliedScale
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CancelTranscriptionDialog(
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    accent: Color,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
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
        appliedContrast -> Color(0xFFFF5252)
        appliedDark -> Color(0xFFB71C1C)
        else -> Color(0xFFD32F2F)
    }
    val dismissContent = when {
        appliedContrast -> accent
        appliedDark -> Color(0xFF8BC34A)
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
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Cancelar envio", fontSize = 16.sp * appliedScale, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = dismissContent)
            ) { Text("Continuar análise", fontSize = 16.sp * appliedScale) }
        },
        title = {
            Text(
                "Cancelar envio do áudio?",
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp * appliedScale
            )
        },
        text = {
            Text(
                "Se cancelar agora, esta tentativa não será contabilizada nem armazenada. Tem certeza?",
                fontSize = 15.sp * appliedScale,
                lineHeight = 20.sp * appliedScale
            )
        }
    )
}

/* --------------------------- Recording UI --------------------------- */

@Composable
private fun PhrasePresentation(
    text: String,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    textPrimary: Color,
    accent: Color
) {
    Text(
        text = text,
        fontSize = 34.sp * appliedScale,
        fontWeight = FontWeight.ExtraBold,
        color = textPrimary,
        textAlign = TextAlign.Center,
        lineHeight = 42.sp * appliedScale,
        letterSpacing = 0.5.sp,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RecordingPill(
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float
) {
    val recordingColor = when {
        appliedContrast -> Color(0xFFFF5252)
        appliedDark -> Color(0xFFEF5350)
        else -> Color(0xFFD32F2F)
    }
    val pillBg = when {
        appliedContrast -> Color.White.copy(alpha = 0.10f)
        appliedDark -> recordingColor.copy(alpha = 0.18f)
        else -> Color.White.copy(alpha = 0.92f)
    }
    val timerColor = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEDEDED)
        else -> Color(0xFF1B1B1B)
    }

    var elapsedSeconds by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        elapsedSeconds = 0
        while (true) {
            delay(1000L)
            elapsedSeconds++
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "rec_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse_alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(28.dp))
            .background(pillBg)
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(recordingColor.copy(alpha = pulseAlpha))
        )
        Text(
            text = "GRAVANDO",
            color = recordingColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 16.sp * appliedScale,
            letterSpacing = 1.4.sp
        )
        Text(
            text = String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60),
            color = timerColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp * appliedScale
        )
    }
}


@Composable
private fun TapToRecordHint(
    text: String,
    appliedScale: Float
) {
    val color = Color.White
    val transition = rememberInfiniteTransition(label = "hint")
    val arrowOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "arrow"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(34.dp)
                .offset(y = (-arrowOffset).dp)
        )
        Text(
            text = text,
            fontSize = 17.sp * appliedScale,
            fontWeight = FontWeight.SemiBold,
            color = color,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp * appliedScale
        )
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
    val barCount = 28
    val barAnimations = (0 until barCount).map { i ->
        val seedHigh = ((i * 53 + 17) % 100) / 100f
        val seedLow = ((i * 31 + 7) % 100) / 100f
        val anim by infinite.animateFloat(
            initialValue = 0.18f + seedLow * 0.18f,
            targetValue = 0.55f + seedHigh * 0.45f,
            animationSpec = infiniteRepeatable(
                tween(420 + ((i * 71) % 380), easing = EaseInOut),
                RepeatMode.Reverse
            ),
            label = "bar_$i"
        )
        anim
    }

    val outerColor: Color
    val middleColor: Color
    val innerColor: Color
    val barColor: Color
    if (appliedContrast) {
        outerColor = Color.White.copy(alpha = 0.18f)
        middleColor = Color.White.copy(alpha = 0.32f)
        innerColor = accent.copy(alpha = 0.70f)
        barColor = accent
    } else if (appliedDark) {
        outerColor = GreenLight.copy(alpha = 0.15f)
        middleColor = GreenLight.copy(alpha = 0.25f)
        innerColor = GreenAccent.copy(alpha = 0.35f)
        barColor = Color(0xFF8BC34A)
    } else {
        outerColor = GreenLight.copy(alpha = 0.15f)
        middleColor = GreenLight.copy(alpha = 0.25f)
        innerColor = GreenAccent.copy(alpha = 0.35f)
        barColor = Color.White
    }

    val micBg = when {
        !enabled -> Color.Gray.copy(alpha = 0.7f)
        appliedContrast -> accent
        else -> GreenDark
    }
    val micIcon = if (appliedContrast) Color.Black else Color.White

    Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(300.dp)) {
            val center = size.center
            if (isRecording) {
                val rStart = 78.dp.toPx()
                val maxLen = 38.dp.toPx()
                barAnimations.forEachIndexed { i, value ->
                    val angle = (i * 2.0 * PI / barCount).toFloat()
                    val length = 8.dp.toPx() + value * maxLen
                    val sx = center.x + cos(angle) * rStart
                    val sy = center.y + sin(angle) * rStart
                    val ex = center.x + cos(angle) * (rStart + length)
                    val ey = center.y + sin(angle) * (rStart + length)
                    drawLine(
                        color = barColor,
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            } else {
                drawCircle(color = outerColor, radius = 140.dp.toPx(), center = center)
                drawCircle(color = middleColor, radius = 110.dp.toPx(), center = center)
                drawCircle(color = innerColor, radius = 80.dp.toPx(), center = center)
            }
        }

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
private fun ModelLoadErrorScreen(
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    textPrimary: Color,
    accent: Color,
    backgroundSolid: Color,
    onBack: () -> Unit
) {
    val root = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)
        .let { base ->
            when {
                appliedContrast -> base.background(Color.Black)
                appliedDark -> base.background(backgroundSolid)
                else -> base.background(
                    brush = Brush.radialGradient(
                        colors = listOf(GreenLight, GreenPrimary, BackgroundGreen),
                        radius = 1200f
                    )
                )
            }
        }

    val cardContainer = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF1E1E1E)
        else -> Color.White
    }
    val cardBorder = when {
        appliedContrast -> BorderStroke(1.dp, accent.copy(alpha = 0.7f))
        appliedDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
        else -> null
    }
    val titleColor = when {
        appliedContrast -> accent
        appliedDark -> Color.White
        else -> Color(0xFF1B1B1B)
    }
    val bodyColor = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEDEDED)
        else -> Color(0xFF3A3A3A)
    }
    val hintColor = when {
        appliedContrast -> Color.White.copy(alpha = 0.85f)
        appliedDark -> Color(0xFFCCCCCC)
        else -> Color(0xFF555555)
    }
    val iconTint = when {
        appliedContrast -> accent
        appliedDark -> Color(0xFFFFB74D)
        else -> Color(0xFFE65100)
    }
    val buttonContainer = when {
        appliedContrast -> accent
        appliedDark -> GreenDark
        else -> Color.White
    }
    val buttonContent = when {
        appliedContrast -> Color.Black
        appliedDark -> Color.White
        else -> Color(0xFF2E7D32)
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 6.dp),
                border = cardBorder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Modelo de IA indisponível",
                        fontSize = 22.sp * appliedScale,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 28.sp * appliedScale
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Não conseguimos carregar o modelo necessário para o teste de voz. Sem ele, sua fala não pode ser analisada.",
                        fontSize = 16.sp * appliedScale,
                        color = bodyColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp * appliedScale
                    )
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = "Por favor, reporte este problema à equipe do app para que possamos ajudar você.",
                        fontSize = 14.sp * appliedScale,
                        color = hintColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp * appliedScale
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonContainer,
                            contentColor = buttonContent
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Voltar para o início",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp * appliedScale
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BigMetric(
    title: String,
    value: String,
    unit: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    modifier: Modifier = Modifier
) {
    val container = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF2A2A2A)
        else -> Color(0xFFF7F7F7)
    }
    val border = when {
        appliedContrast -> BorderStroke(2.dp, color)
        appliedDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
        else -> BorderStroke(1.dp, Color(0xFFE6E6E6))
    }
    val titleColor = when {
        appliedContrast -> Color.White.copy(alpha = 0.85f)
        appliedDark -> Color(0xFFCCCCCC)
        else -> Color(0xFF555555)
    }
    val unitColor = when {
        appliedContrast -> Color.White.copy(alpha = 0.70f)
        appliedDark -> Color(0xFFAAAAAA)
        else -> Color(0xFF777777)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = container),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 2.dp),
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 15.sp * appliedScale,
                fontWeight = FontWeight.SemiBold,
                color = titleColor,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 36.sp * appliedScale,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = unit,
                fontSize = 13.sp * appliedScale,
                fontWeight = FontWeight.Medium,
                color = unitColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ComparisonRow(
    label: String,
    youValue: String,
    avgValue: String,
    isBetter: Boolean,
    diffText: String,
    appliedContrast: Boolean,
    accent: Color,
    bodyColor: Color,
    labelColor: Color,
    scale: Float
) {
    val goodColor = if (appliedContrast) Color(0xFFA5D6A7) else Color(0xFF2E7D32)
    val badColor = if (appliedContrast) Color(0xFFFF8A80) else Color(0xFFD32F2F)
    val badgeColor = if (isBetter) goodColor else badColor
    val badgeBg = badgeColor.copy(alpha = if (appliedContrast) 0.22f else 0.14f)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 16.sp * scale,
            fontWeight = FontWeight.Bold,
            color = bodyColor
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Você",
                    fontSize = 12.sp * scale,
                    color = labelColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = youValue,
                    fontSize = 17.sp * scale,
                    fontWeight = FontWeight.ExtraBold,
                    color = accent
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Média brasileira",
                    fontSize = 12.sp * scale,
                    color = labelColor,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = avgValue,
                    fontSize = 17.sp * scale,
                    fontWeight = FontWeight.Bold,
                    color = bodyColor
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(badgeBg)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = diffText,
                fontSize = 13.sp * scale,
                fontWeight = FontWeight.Bold,
                color = badgeColor
            )
        }
    }
}

@Composable
private fun AttemptCard(
    index: Int,
    expected: String,
    transcribed: String,
    wpm: Int,
    wer: Double,
    container: Color,
    border: BorderStroke?,
    accent: Color,
    bodyColor: Color,
    labelColor: Color,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    scale: Float
) {
    val precision = (100.0 - wer).coerceIn(0.0, 100.0)
    val statBg = when {
        appliedContrast -> Color.White.copy(alpha = 0.06f)
        appliedDark -> Color.White.copy(alpha = 0.04f)
        else -> Color(0xFFEFEFEF)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 1.dp),
        border = border
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = "Tentativa $index",
                fontSize = 13.sp * scale,
                fontWeight = FontWeight.Bold,
                color = labelColor,
                letterSpacing = 0.5.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Frase apresentada",
                fontSize = 12.sp * scale,
                fontWeight = FontWeight.SemiBold,
                color = labelColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = expected,
                fontSize = 16.sp * scale,
                fontWeight = FontWeight.SemiBold,
                color = bodyColor,
                lineHeight = 22.sp * scale
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "O que você disse",
                fontSize = 12.sp * scale,
                fontWeight = FontWeight.SemiBold,
                color = labelColor
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (transcribed.isNotBlank()) transcribed else "—",
                fontSize = 16.sp * scale,
                fontWeight = FontWeight.SemiBold,
                color = accent,
                lineHeight = 22.sp * scale
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AttemptStat(
                    label = "Velocidade",
                    value = "$wpm palavras/min",
                    accent = accent,
                    bg = statBg,
                    labelColor = labelColor,
                    scale = scale,
                    modifier = Modifier.weight(1f)
                )
                AttemptStat(
                    label = "Precisão",
                    value = "${String.format("%.0f", precision)}%",
                    accent = accent,
                    bg = statBg,
                    labelColor = labelColor,
                    scale = scale,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun AttemptStat(
    label: String,
    value: String,
    accent: Color,
    bg: Color,
    labelColor: Color,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp * scale,
            color = labelColor,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp * scale,
            color = accent,
            fontWeight = FontWeight.Bold
        )
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
    val sheetBg = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF1E1E1E)
        else -> Color.White
    }
    val innerCardBg = when {
        appliedContrast -> Color(0xFF0B0B0B)
        appliedDark -> Color(0xFF2A2A2A)
        else -> Color(0xFFF7F7F7)
    }
    val titleColor = when {
        appliedContrast -> accent
        appliedDark -> Color.White
        else -> Color(0xFF1B1B1B)
    }
    val bodyColor = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEDEDED)
        else -> Color(0xFF1B1B1B)
    }
    val labelColor = when {
        appliedContrast -> Color.White.copy(alpha = 0.85f)
        appliedDark -> Color(0xFFB0B0B0)
        else -> Color(0xFF666666)
    }
    val sheetBorder = when {
        appliedContrast -> BorderStroke(1.dp, Color.White.copy(alpha = 0.20f))
        appliedDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
        else -> null
    }
    val innerBorder = when {
        appliedContrast -> BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        appliedDark -> BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
        else -> BorderStroke(1.dp, Color(0xFFE6E6E6))
    }

    val ctx = LocalContext.current
    val brWpm = kotlin.runCatching { ctx.getString(R.string.br_avg_wpm).replace(",", ".").toDouble() }.getOrElse { 150.0 }
    val brWer = kotlin.runCatching { ctx.getString(R.string.br_avg_wer).replace(",", ".").toDouble() }.getOrElse { 12.0 }
    val wpmUser = summary.avgWpm.toDouble()
    val werUser = summary.avgWer.toDouble()
    val userPrecision = (100.0 - werUser).coerceIn(0.0, 100.0)
    val brPrecision = (100.0 - brWer).coerceIn(0.0, 100.0)

    val wpmAbove = wpmUser >= brWpm * 1.1
    val wpmBelow = wpmUser <= brWpm * 0.9
    val werBetter = werUser <= brWer
    val werMuchBetter = werUser <= brWer * 0.9
    val werWorse = werUser >= brWer * 1.1
    val (headline, body) = when {
        wpmAbove && (werMuchBetter || werBetter) ->
            "Ótimo ritmo" to "Sua velocidade está acima da referência com boa precisão. Mantenha a prática regular e avance para frases mais longas quando se sentir confortável."
        wpmBelow && (werMuchBetter || werBetter) ->
            "Base sólida" to "Boa precisão. Agora, aumente a velocidade gradualmente: repita a frase, respire fundo e tente manter um ritmo contínuo."
        wpmAbove && werWorse ->
            "Ajuste fino" to "Velocidade alta, mas com mais erros. Diminua um pouco o ritmo e articule cada palavra com calma para melhorar a precisão."
        wpmBelow && werWorse ->
            "Seguimos juntos" to "É normal oscilar. Comece com frases curtas, foque em respirar e pronunciar com clareza. A velocidade vem com a prática."
        else ->
            "Bom caminho" to "Você está próximo da referência. Continue praticando e ajuste suavemente ritmo e articulação para evoluir."
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = sheetBg),
                elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 10.dp),
                border = sheetBorder
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Resultado da sessão",
                        fontSize = 26.sp * appliedScale,
                        fontWeight = FontWeight.ExtraBold,
                        color = titleColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        BigMetric(
                            title = "Velocidade",
                            value = String.format("%.0f", wpmUser),
                            unit = "palavras/min",
                            color = accent,
                            icon = Icons.Default.Speed,
                            appliedContrast = appliedContrast,
                            appliedDark = appliedDark,
                            appliedScale = appliedScale,
                            modifier = Modifier.weight(1f)
                        )
                        BigMetric(
                            title = "Precisão",
                            value = String.format("%.0f", userPrecision),
                            unit = "%",
                            color = accent,
                            icon = Icons.Default.CheckCircle,
                            appliedContrast = appliedContrast,
                            appliedDark = appliedDark,
                            appliedScale = appliedScale,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    val dividerColor = when {
                        appliedContrast -> Color.White.copy(alpha = 0.18f)
                        appliedDark -> Color.White.copy(alpha = 0.10f)
                        else -> Color(0xFFE0E0E0)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = innerCardBg),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (appliedContrast || appliedDark) 0.dp else 1.dp),
                        border = innerBorder
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                            Text(
                                text = "Comparação com a média brasileira",
                                fontSize = 14.sp * appliedScale,
                                fontWeight = FontWeight.Bold,
                                color = labelColor,
                                letterSpacing = 0.3.sp
                            )
                            Spacer(Modifier.height(14.dp))
                            val wpmDiffPct = if (brWpm > 0) ((wpmUser - brWpm) / brWpm * 100.0) else 0.0
                            ComparisonRow(
                                label = "Velocidade",
                                youValue = "${String.format("%.0f", wpmUser)} palavras/min",
                                avgValue = "${String.format("%.0f", brWpm)} palavras/min",
                                isBetter = wpmUser >= brWpm,
                                diffText = if (wpmUser >= brWpm)
                                    "Acima da média (+${String.format("%.0f", wpmDiffPct)}%)"
                                else
                                    "Abaixo da média (${String.format("%.0f", wpmDiffPct)}%)",
                                appliedContrast = appliedContrast,
                                accent = accent,
                                bodyColor = bodyColor,
                                labelColor = labelColor,
                                scale = appliedScale
                            )
                            Spacer(Modifier.height(16.dp))
                            val precDiffPct = if (brPrecision > 0) ((userPrecision - brPrecision) / brPrecision * 100.0) else 0.0
                            ComparisonRow(
                                label = "Precisão",
                                youValue = "${String.format("%.0f", userPrecision)}%",
                                avgValue = "${String.format("%.0f", brPrecision)}%",
                                isBetter = userPrecision >= brPrecision,
                                diffText = if (userPrecision >= brPrecision)
                                    "Acima da média (+${String.format("%.0f", precDiffPct)}%)"
                                else
                                    "Abaixo da média (${String.format("%.0f", precDiffPct)}%)",
                                appliedContrast = appliedContrast,
                                accent = accent,
                                bodyColor = bodyColor,
                                labelColor = labelColor,
                                scale = appliedScale
                            )

                            Spacer(Modifier.height(20.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(dividerColor)
                            )
                            Spacer(Modifier.height(18.dp))

                            Text(
                                text = headline,
                                fontSize = 19.sp * appliedScale,
                                fontWeight = FontWeight.ExtraBold,
                                color = accent
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = body,
                                fontSize = 15.sp * appliedScale,
                                color = bodyColor,
                                lineHeight = 22.sp * appliedScale
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Dicas",
                                fontSize = 13.sp * appliedScale,
                                fontWeight = FontWeight.Bold,
                                color = labelColor,
                                letterSpacing = 0.3.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            listOf(
                                "Respire com calma antes de falar",
                                "Articule cada sílaba claramente",
                                "Comece devagar e aumente o ritmo aos poucos"
                            ).forEach {
                                Text(
                                    text = "•  $it",
                                    fontSize = 14.sp * appliedScale,
                                    color = bodyColor,
                                    lineHeight = 20.sp * appliedScale
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Suas tentativas",
                        fontSize = 18.sp * appliedScale,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start
                    )
                    Spacer(Modifier.height(10.dp))
                    summary.items.forEachIndexed { idx, item ->
                        AttemptCard(
                            index = idx + 1,
                            expected = item.phrase,
                            transcribed = item.transcribed,
                            wpm = item.wpm,
                            wer = item.wer,
                            container = innerCardBg,
                            border = innerBorder,
                            accent = accent,
                            bodyColor = bodyColor,
                            labelColor = labelColor,
                            appliedContrast = appliedContrast,
                            appliedDark = appliedDark,
                            scale = appliedScale
                        )
                        if (idx < summary.items.size - 1) Spacer(Modifier.height(10.dp))
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = onClose,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = if (appliedContrast) Color.Black else Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Fazer novo teste",
                            fontSize = 17.sp * appliedScale,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onNavigateHome,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Text(
                            text = "Voltar ao início",
                            fontSize = 16.sp * appliedScale,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
