package com.recuperavc.ui.main

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recuperavc.data.db.DbProvider
import com.recuperavc.models.MotionReport
import com.recuperavc.ui.theme.GreenAccent
import com.recuperavc.ui.theme.GreenDark
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlin.random.Random
import com.recuperavc.ui.sfx.Sfx
import com.recuperavc.ui.sfx.rememberSfxController
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.util.InitialSettings
import com.recuperavc.ui.util.PaintSystemBars
import com.recuperavc.ui.util.rememberInitialSettings
import com.recuperavc.ui.components.*
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TouchApp

private enum class MotionMode { MOVING, STATIC }
private enum class Hand { RIGHT, LEFT }

private val HighContrastAccent = Color(0xFFFFD600)

@SuppressLint("UnrememberedMutableState")
@Composable
fun MotionTestScreen(
    modifier: Modifier = Modifier,
    durationSecondsWithMovement: Int = 30,
    durationSecondsWithoutMovement: Int = 20,
    onFinish: (Int) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val sfx = rememberSfxController()

    var showEndDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        sfx.play(Sfx.CLICK)
        showEndDialog = true
    }

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
    val bgRoot = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF101211)
        else -> Color.White
    }
    val textPrimary = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEAEAEA)
        else -> Color(0xFF1B1B1B)
    }
    val textSecondary = if (appliedContrast || appliedDark) Color(0xFFDDDDDD) else Color(0xFF3A3A3A)
    val dividerColor = if (appliedContrast) Color.White.copy(alpha = 0.18f)
    else if (appliedDark) Color.White.copy(alpha = 0.12f) else Color(0xFFE0E0E0)

    val accentSolid = if (appliedContrast) HighContrastAccent else GreenDark
    val accentAlt = if (appliedContrast) HighContrastAccent else GreenAccent
    val buttonFgOnAccent = if (appliedContrast) Color.Black else Color.White

    PaintSystemBars(background = if (appliedContrast || appliedDark) Color.Black else Color.White, lightIcons = !(appliedContrast || appliedDark))

    val db = remember(context) { DbProvider.db(context) }
    val scope = rememberCoroutineScope()

    // Choices
    var selectedHand by remember { mutableStateOf<Hand?>(null) }
    var isDominant by remember { mutableStateOf<Boolean?>(null) }
    var chosenMode by remember { mutableStateOf<MotionMode?>(null) }

    // Test cycle
    var testStarted by remember { mutableStateOf(false) }
    var clicks by remember { mutableStateOf(0) }
    var missedClicks by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(durationSecondsWithMovement) }
    var finished by remember { mutableStateOf(false) }
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
    var lastReport by remember { mutableStateOf<MotionReport?>(null) }

    // History
    val history by db.MotionReportDao().observeAll().collectAsState(initial = emptyList())

    val density = LocalDensity.current
    var headerHeightPx by remember { mutableStateOf(0f) }
    val headerMarginPx = 12f

    fun durationFor(mode: MotionMode?) =
        if (mode == MotionMode.STATIC) durationSecondsWithoutMovement else durationSecondsWithMovement

    if (showEndDialog) {
        val container = when {
            appliedContrast -> Color.Black
            appliedDark -> Color(0xFF1E1E1E)
            else -> Color.White
        }
        val titleColor = if (appliedContrast) accentSolid else if (appliedDark) Color.White else Color(0xFF1B1B1B)
        val bodyColor = if (appliedContrast || appliedDark) Color.White else Color(0xFF3A3A3A)
        val confirmContainer = when {
            appliedContrast -> accentSolid
            appliedDark -> GreenDark
            else -> Color.White
        }
        val confirmContent = when {
            appliedContrast -> Color.Black
            appliedDark -> Color.White
            else -> Color(0xFF2E7D32)
        }
        val dismissContent = when {
            appliedContrast -> accentSolid
            appliedDark -> Color(0xFF8BC34A)
            else -> GreenDark
        }

        AlertDialog(
            onDismissRequest = { sfx.play(Sfx.BUBBLE); showEndDialog = false },
            containerColor = container,
            titleContentColor = titleColor,
            textContentColor = bodyColor,
            confirmButton = {
                Button(
                    onClick = {
                        sfx.play(Sfx.CLICK)
                        showEndDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = confirmContainer,
                        contentColor = confirmContent
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Sair", fontSize = 16.sp * appliedScale)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { sfx.play(Sfx.CLICK); showEndDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = dismissContent)
                ) { Text("Continuar", fontSize = 16.sp * appliedScale) }
            },
            title = {
                Text("Encerrar sessão", fontWeight = FontWeight.SemiBold, fontSize = 20.sp * appliedScale)
            },
            text = {
                val message = if (testStarted && !finished) {
                    "O teste está em andamento. Se sair agora, o progresso será perdido."
                } else if (finished) {
                    "Deseja voltar para a tela inicial?"
                } else {
                    "Deseja cancelar e voltar para a tela inicial?"
                }
                Text(message, fontSize = 15.sp * appliedScale, lineHeight = 20.sp * appliedScale)
            }
        )
    }

    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .background(bgRoot)
            .pointerInput(testStarted, finished) {
                detectTapGestures(
                    onTap = {
                        if (testStarted && !finished) missedClicks++
                    }
                )
            }
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }

        val movingButtonSize = 56.dp
        val staticButtonSize = 120.dp
        val movingBtnPx = with(density) { movingButtonSize.toPx() }

        fun moveButtonRandomly() {
            val topLimit = (headerHeightPx + headerMarginPx)
            val availableHeight = (maxHeightPx - movingBtnPx - topLimit).coerceAtLeast(0f)
            val x = Random.nextFloat() * (maxWidthPx - movingBtnPx)
            val y = topLimit + Random.nextFloat() * availableHeight
            buttonPosition = Offset(x, y)
        }

        LaunchedEffect(testStarted) {
            if (testStarted) {
                val effectiveDuration = durationFor(chosenMode)
                timeLeft = effectiveDuration
                clicks = 0
                missedClicks = 0
                finished = false
                if (chosenMode == MotionMode.MOVING) moveButtonRandomly()

                while (timeLeft > 0) {
                    delay(1_000)
                    timeLeft--
                }

                val clicksPerMinute = if (effectiveDuration > 0) {
                    ((clicks * 60.0) / effectiveDuration).roundToInt()
                } else 0

                val report = MotionReport(
                    date = Instant.now(),
                    secondsTotal = effectiveDuration.toFloat(),
                    clicksPerMinute = clicksPerMinute,
                    totalClicks = clicks,
                    withRightHand = (selectedHand == Hand.RIGHT),
                    withMainHand = (isDominant == true),
                    withMovement = (chosenMode == MotionMode.MOVING),
                    missedClicks = missedClicks
                )
                scope.launch { db.MotionReportDao().upsert(report) }
                lastReport = report
                finished = true
                onFinish(clicks)
            }
        }

        // HEADER while running
        if (testStarted && !finished) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .onGloballyPositioned { coords -> headerHeightPx = coords.size.height.toFloat() },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Tempo restante: ${timeLeft}s", fontSize = (20.sp * appliedScale), color = textPrimary, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text("Cliques: $clicks", fontSize = (20.sp * appliedScale), color = textPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Errados: $missedClicks", fontSize = (16.sp * appliedScale), color = textSecondary)
            }
        }

        // Back (pre-test only)
        if (!testStarted && !finished) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { sfx.play(Sfx.CLICK); showEndDialog = true }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = if (appliedContrast || appliedDark) Color.White else GreenDark
                    )
                }
            }
        }

        // PRE-TEST
        if (!testStarted && !finished) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Antes de começar", fontSize = (22.sp * appliedScale), color = textPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))

                // Hand
                Text("Qual mão você vai usar?", fontSize = (16.sp * appliedScale), color = textPrimary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val selected = selectedHand == Hand.RIGHT
                    SelectButton(
                        label = "Direita",
                        selected = selected,
                        onClick = { selectedHand = Hand.RIGHT },
                        appliedContrast = appliedContrast,
                        appliedDark = appliedDark,
                        appliedScale = appliedScale
                    )
                    val selectedL = selectedHand == Hand.LEFT
                    SelectButton(
                        label = "Esquerda",
                        selected = selectedL,
                        onClick = { selectedHand = Hand.LEFT },
                        appliedContrast = appliedContrast,
                        appliedDark = appliedDark,
                        appliedScale = appliedScale
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Dominance
                Text("É a sua mão dominante?", fontSize = (16.sp * appliedScale), color = textPrimary)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SelectButton(
                        label = "Sim",
                        selected = isDominant == true,
                        onClick = { isDominant = true },
                        appliedContrast = appliedContrast,
                        appliedDark = appliedDark,
                        appliedScale = appliedScale
                    )
                    SelectButton(
                        label = "Não",
                        selected = isDominant == false,
                        onClick = { isDominant = false },
                        appliedContrast = appliedContrast,
                        appliedDark = appliedDark,
                        appliedScale = appliedScale
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Mode
                Text("Como deseja realizar o teste?", fontSize = (16.sp * appliedScale), color = textPrimary)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    BigSelectButton(
                        label = "Com movimento (botão muda de posição)",
                        selected = chosenMode == MotionMode.MOVING,
                        onClick = { chosenMode = MotionMode.MOVING },
                        appliedContrast = appliedContrast,
                        appliedDark = appliedDark,
                        appliedScale = appliedScale
                    )
                    BigSelectButton(
                        label = "Sem movimento (botão fixo)",
                        selected = chosenMode == MotionMode.STATIC,
                        onClick = { chosenMode = MotionMode.STATIC },
                        appliedContrast = appliedContrast,
                        appliedDark = appliedDark,
                        appliedScale = appliedScale
                    )
                }

                Spacer(Modifier.height(20.dp))

                val previewDuration = when (chosenMode) {
                    MotionMode.MOVING -> durationSecondsWithMovement
                    MotionMode.STATIC -> durationSecondsWithoutMovement
                    null -> null
                }
                Text(
                    text = previewDuration?.let { "Você terá ${it}s para tocar o botão o máximo possível." }
                        ?: "Você terá ${durationSecondsWithMovement}s com movimento ou ${durationSecondsWithoutMovement}s sem movimento.",
                    fontSize = (14.sp * appliedScale),
                    color = textSecondary
                )

                Spacer(Modifier.height(20.dp))

                val canStart = selectedHand != null && isDominant != null && chosenMode != null
                Button(
                    onClick = { sfx.play(Sfx.CLICK); testStarted = true },
                    enabled = canStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canStart) accentSolid else if (appliedContrast || appliedDark) Color(0xFF5A5A5A) else Color(0xFF9E9E9E),
                        contentColor = buttonFgOnAccent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Começar teste", color = buttonFgOnAccent, fontSize = (16.sp * appliedScale), fontWeight = FontWeight.SemiBold) }

            }
            return@BoxWithConstraints
        }

        // MOVING TEST
        if (testStarted && !finished && chosenMode == MotionMode.MOVING) {
            Box(
                modifier = Modifier.offset {
                    IntOffset(buttonPosition.x.roundToInt(), buttonPosition.y.roundToInt())
                }
            ) {
                Button(
                    onClick = { clicks++; sfx.play(Sfx.BUBBLE); moveButtonRandomly() },
                    modifier = Modifier.size(movingButtonSize),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentAlt,
                        contentColor = buttonFgOnAccent
                    ),
                    shape = RoundedCornerShape(100)
                ) {}
            }
        }

        // STATIC TEST
        if (testStarted && !finished && chosenMode == MotionMode.STATIC) {
            val headerHeightDp = with(density) { (headerHeightPx + headerMarginPx).toDp() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeightDp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { clicks++; sfx.play(Sfx.CLICK) },
                    modifier = Modifier.size(staticButtonSize),
                    colors = ButtonDefaults.buttonColors(containerColor = accentAlt, contentColor = buttonFgOnAccent),
                    shape = RoundedCornerShape(100)
                ) {}
            }
        }

        // RESULTS
        if (finished) {
            MotionResultScreen(
                lastReport = lastReport,
                history = history,
                appliedContrast = appliedContrast,
                appliedDark = appliedDark,
                appliedScale = appliedScale,
                accentSolid = accentSolid,
                onNewTest = {
                    sfx.play(Sfx.CLICK)
                    selectedHand = null
                    isDominant = null
                    chosenMode = null
                    testStarted = false
                    finished = false
                    clicks = 0
                    missedClicks = 0
                    timeLeft = durationSecondsWithMovement
                },
                onBack = { sfx.play(Sfx.CLICK); showEndDialog = true }
            )
        }
    }
}

/* ----------------------- Select helpers (pre-test) ----------------------- */

@Composable
private fun SelectButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float
) {
    val container = when {
        selected && appliedContrast -> HighContrastAccent
        selected -> GreenDark
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF2A2A2A)
        else -> GreenAccent
    }
    val content = when {
        selected && appliedContrast -> Color.Black
        selected -> Color.White
        appliedContrast -> Color.White
        appliedDark -> Color.White
        else -> Color.White
    }
    val border = if (!selected && appliedContrast) BorderStroke(2.dp, HighContrastAccent) else null

    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(12.dp),
        border = border
    ) { Text(label, fontSize = (14.sp * appliedScale), fontWeight = FontWeight.Medium) }
}

@Composable
private fun BigSelectButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float
) {
    val container = when {
        selected && appliedContrast -> HighContrastAccent
        selected -> GreenDark
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF2A2A2A)
        else -> GreenAccent
    }
    val content = when {
        selected && appliedContrast -> Color.Black
        selected -> Color.White
        appliedContrast -> Color.White
        appliedDark -> Color.White
        else -> Color.White
    }
    val border = if (!selected && appliedContrast) BorderStroke(2.dp, HighContrastAccent) else null

    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        shape = RoundedCornerShape(12.dp),
        border = border
    ) { Text(label, fontSize = (14.sp * appliedScale)) }
}

/* ------------------------- Motion Result Screen ---------------------------- */
@Composable
private fun MotionResultScreen(
    lastReport: MotionReport?,
    history: List<MotionReport>,
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    accentSolid: Color,
    onNewTest: () -> Unit,
    onBack: () -> Unit
) {
    ResultDialogContainer(
        appliedContrast = appliedContrast,
        appliedDark = appliedDark
    ) {
        ResultTitle(
            text = "Resultado do Teste",
            appliedContrast = appliedContrast,
            appliedDark = appliedDark,
            appliedScale = appliedScale,
            accent = accentSolid
        )
        Spacer(Modifier.height(16.dp))

        lastReport?.let { report ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResultMetricCard(
                    title = "Cliques/min",
                    value = "${report.clicksPerMinute}",
                    unit = "CPM",
                    color = accentSolid,
                    icon = Icons.Default.Speed,
                    appliedContrast = appliedContrast,
                    appliedDark = appliedDark,
                    appliedScale = appliedScale
                )
                ResultMetricCard(
                    title = "Total cliques",
                    value = "${report.totalClicks}",
                    unit = "",
                    color = accentSolid,
                    icon = Icons.Default.TouchApp,
                    appliedContrast = appliedContrast,
                    appliedDark = appliedDark,
                    appliedScale = appliedScale
                )
            }
            Spacer(Modifier.height(16.dp))

            ResultSectionLabel(
                text = "Detalhes",
                appliedContrast = appliedContrast,
                appliedDark = appliedDark,
                appliedScale = appliedScale
            )
            Spacer(Modifier.height(8.dp))

            ResultItemCard(
                appliedContrast = appliedContrast,
                appliedDark = appliedDark
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        ResultItemText(
                            text = "Duração",
                            appliedContrast = appliedContrast,
                            appliedDark = appliedDark,
                            appliedScale = appliedScale,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13f
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${report.secondsTotal.toInt()}s",
                            fontSize = 16.sp * appliedScale,
                            fontWeight = FontWeight.Bold,
                            color = accentSolid
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        ResultItemText(
                            text = "Cliques errados",
                            appliedContrast = appliedContrast,
                            appliedDark = appliedDark,
                            appliedScale = appliedScale,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13f
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${report.missedClicks}",
                            fontSize = 16.sp * appliedScale,
                            fontWeight = FontWeight.Bold,
                            color = accentSolid
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                ResultItemText(
                    text = "Mão: ${if (report.withRightHand) "Direita" else "Esquerda"} ${if (report.withMainHand) "(Dominante)" else "(Não dominante)"}",
                    appliedContrast = appliedContrast,
                    appliedDark = appliedDark,
                    appliedScale = appliedScale,
                    fontSize = 14f
                )
                Spacer(Modifier.height(4.dp))
                ResultItemText(
                    text = "Modo: ${if (report.withMovement) "Com movimento" else "Sem movimento"}",
                    appliedContrast = appliedContrast,
                    appliedDark = appliedDark,
                    appliedScale = appliedScale,
                    fontSize = 14f
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onNewTest,
            colors = ButtonDefaults.buttonColors(
                containerColor = accentSolid,
                contentColor = if (appliedContrast) Color.Black else Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "Novo teste",
                color = if (appliedContrast) Color.Black else Color.White,
                fontSize = 16.sp * appliedScale,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(8.dp))

        if (appliedContrast) {
            OutlinedButton(
                onClick = onBack,
                border = BorderStroke(2.dp, accentSolid),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentSolid),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Voltar ao Início",
                    fontSize = 16.sp * appliedScale,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenAccent,
                    contentColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Voltar ao Início",
                    color = Color.White,
                    fontSize = 16.sp * appliedScale,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
