package com.recuperavc.ui.settings

import android.app.Activity
import android.graphics.drawable.ColorDrawable
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight
import com.recuperavc.ui.theme.OnBackground
import kotlin.math.roundToInt
import com.recuperavc.ui.sfx.Sfx
import com.recuperavc.ui.sfx.rememberSfxController
import com.recuperavc.ui.util.PaintSystemBars
import com.recuperavc.ui.util.InitialSettings
import com.recuperavc.ui.util.PaintSystemBars
import com.recuperavc.ui.util.rememberInitialSettings

private val HighContrastAccent = Color(0xFFFFD600)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onApply: (darkMode: Boolean, contrast: Boolean, fontScale: Float) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val sfx = rememberSfxController()

    // 1) BLOCK UNTIL FIRST SETTINGS ARRIVE (prevents white flash)
    val initial: InitialSettings? = rememberInitialSettings(viewModel)
    if (initial == null) {
        PaintSystemBars(background = Color.Black, lightIcons = false)
        Box(Modifier.fillMaxSize().background(Color.Black)) {} // solid safe placeholder
        return
    }

    // === APPLIED STATE (VM) seeded with real initial values ===
    val appliedDark by viewModel.darkModeFlow.collectAsState(initial = initial.dark)
    val appliedContrast by viewModel.contrastFlow.collectAsState(initial = initial.contrast)
    val appliedScale by viewModel.sizeTextFlow.collectAsState(initial = initial.scale)

    // === PENDING STATE (local; only committed on "Aplicar") ===
    var pendingDark by remember { mutableStateOf(appliedDark) }
    var pendingContrast by remember { mutableStateOf(appliedContrast) }
    var pendingScale by remember { mutableStateOf(appliedScale) }

    LaunchedEffect(appliedDark, appliedContrast, appliedScale) {
        pendingDark = appliedDark
        pendingContrast = appliedContrast
        pendingScale = appliedScale
    }

    // Unsaved changes warning
    val hasUnsavedChanges by derivedStateOf {
        (pendingDark != appliedDark) ||
                (pendingContrast != appliedContrast) ||
                (kotlin.math.abs(pendingScale - appliedScale) > 1e-3f)
    }
    var showDiscardDialog by remember { mutableStateOf(false) }

    // === PALETTE (applied) ===
    val appliedBackground = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF121212)
        else -> Color.White
    }
    val appliedText = when {
        appliedContrast -> Color.White
        appliedDark -> Color(0xFFEAEAEA)
        else -> Color.Black
    }
    val appliedAccent = if (appliedContrast) HighContrastAccent else GreenDark
    val appliedRowBackground = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF1E1E1E)
        else -> Color(0xFFF8F8F8)
    }

    val appliedSwitchColors = when {
        appliedContrast -> SwitchDefaults.colors(
            checkedThumbColor = HighContrastAccent,
            checkedTrackColor = Color.White,
            uncheckedThumbColor = Color.DarkGray,
            uncheckedTrackColor = Color.Gray
        )
        appliedDark -> SwitchDefaults.colors(
            checkedThumbColor = GreenLight,
            checkedTrackColor = Color(0xFF2E7D32),
            uncheckedThumbColor = Color.LightGray,
            uncheckedTrackColor = Color.DarkGray
        )
        else -> SwitchDefaults.colors()
    }

    val sliderColors = if (appliedContrast) {
        SliderDefaults.colors(
            thumbColor = HighContrastAccent,
            activeTrackColor = HighContrastAccent,
            inactiveTrackColor = Color.DarkGray,
            activeTickColor = Color.Black,
            inactiveTickColor = Color.Black
        )
    } else SliderDefaults.colors()

    // Paint system bars for the real frame too
    PaintSystemBars(
        background = appliedBackground,
        lightIcons = !(appliedContrast || appliedDark)
    )

    // Navigation guard
    val navigateBackCheck: () -> Unit = {
        sfx.play(Sfx.CLICK)
        if (hasUnsavedChanges) showDiscardDialog = true else onBack()
    }
    BackHandler(enabled = true) { navigateBackCheck() }

    val headerHeight = 160.dp

    Box(modifier = Modifier
        .fillMaxSize()
        .background(appliedBackground)
        .windowInsetsPadding(WindowInsets.systemBars)
    ) {

        // HEADER
        Box(modifier = Modifier.fillMaxWidth().height(headerHeight)) {
            if (!appliedContrast) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(0f, h * 0.55f)
                        cubicTo(w * 0.25f, h * 0.25f, w * 0.45f, h * 0.95f, w * 0.6f, h * 0.65f)
                        cubicTo(w * 0.8f, h * 0.35f, w * 0.9f, h * 0.5f, w, h * 0.4f)
                        lineTo(w, 0f); close()
                    }
                    drawPath(path, color = GreenLight)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                IconButton(onClick = { navigateBackCheck() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = if (appliedContrast || appliedDark) Color.White else OnBackground
                    )
                }
            }
        }

        // CONTENT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = headerHeight, start = 16.dp, end = 16.dp, bottom = 16.dp)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Preferências do App",
                fontSize = 22.sp * appliedScale,
                fontWeight = FontWeight.Bold,
                color = if (appliedContrast) appliedAccent else appliedText
            )
            Spacer(Modifier.height(16.dp))

            // Aparência
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = appliedBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Aparência",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp * appliedScale,
                        color = if (appliedContrast) appliedAccent else appliedText
                    )
                    Spacer(Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "Alto contraste",
                        subtitle = "Preto absoluto + texto branco; acento amarelo para foco/ativos",
                        checked = pendingContrast,
                        onCheckedChange = { enable ->
                            sfx.play(Sfx.CLICK)
                            if (enable) { pendingContrast = true; pendingDark = false } else pendingContrast = false
                        },
                        textColor = appliedText,
                        rowBackground = appliedRowBackground,
                        switchColors = appliedSwitchColors,
                        scale = appliedScale
                    )

                    Spacer(Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "Modo escuro",
                        subtitle = "Cinza-escuro com texto claro; menos agressivo que alto contraste",
                        checked = pendingDark,
                        onCheckedChange = { enable ->
                            sfx.play(Sfx.CLICK)
                            if (enable) { pendingDark = true; pendingContrast = false } else pendingDark = false
                        },
                        textColor = appliedText,
                        rowBackground = appliedRowBackground,
                        switchColors = appliedSwitchColors,
                        scale = appliedScale
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tamanho do texto
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = appliedBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Tamanho do texto",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp * appliedScale,
                        color = if (appliedContrast) appliedAccent else appliedText
                    )
                    Spacer(Modifier.height(8.dp))

                    val pendingPercent = (pendingScale * 100).roundToInt()
                    val appliedPercent = (appliedScale * 100).roundToInt()

                    Text(
                        "Aplicado: ${appliedPercent}%, Proposto: ${pendingPercent}%",
                        fontSize = 14.sp * appliedScale,
                        fontWeight = FontWeight.SemiBold,
                        color = appliedText.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))

                    Column {
                        val sliderInteractions = remember { MutableInteractionSource() }
                        var playedForGesture by remember { mutableStateOf(false) }

                        val min = 0.8f
                        val max = 1.6f
                        val step = 0.1f
                        val stepsCount = ((max - min) / step).toInt() - 1

                        var sliderValue by remember { mutableStateOf(pendingScale) }
                        LaunchedEffect(pendingScale) { sliderValue = pendingScale }

                        LaunchedEffect(sliderInteractions) {
                            sliderInteractions.interactions.collect { interaction ->
                                when (interaction) {
                                    is PressInteraction.Press -> if (!playedForGesture) { sfx.play(Sfx.CLICK); playedForGesture = true }
                                    is PressInteraction.Release, is PressInteraction.Cancel -> playedForGesture = false
                                    is DragInteraction.Start -> if (!playedForGesture) { sfx.play(Sfx.CLICK); playedForGesture = true }
                                    is DragInteraction.Stop, is DragInteraction.Cancel -> playedForGesture = false
                                }
                            }
                        }

                        Slider(
                            value = sliderValue,
                            onValueChange = { v ->
                                val clamped = v.coerceIn(min, max)
                                val quantized = (clamped * 10f).roundToInt() / 10f
                                if (quantized != sliderValue) sfx.play(Sfx.CLICK)
                                sliderValue = quantized
                            },
                            valueRange = min..max,
                            steps = stepsCount,
                            onValueChangeFinished = { pendingScale = sliderValue },
                            interactionSource = sliderInteractions,
                            colors = sliderColors
                        )

                        val textBtnColors = ButtonDefaults.textButtonColors(contentColor = appliedAccent)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = {
                                sfx.play(Sfx.CLICK)
                                pendingScale = (pendingScale - 0.1f).coerceIn(0.8f, 1.6f)
                                sliderValue = pendingScale
                            }, colors = textBtnColors) {
                                Text("A-", fontWeight = FontWeight.Bold, fontSize = 14.sp * appliedScale)
                            }
                            TextButton(onClick = {
                                sfx.play(Sfx.CLICK)
                                pendingScale = 1.0f
                                sliderValue = pendingScale
                            }, colors = textBtnColors) {
                                Text("Padrão", fontWeight = FontWeight.Bold, fontSize = 14.sp * appliedScale)
                            }
                            TextButton(onClick = {
                                sfx.play(Sfx.CLICK)
                                pendingScale = (pendingScale + 0.1f).coerceIn(0.8f, 1.6f)
                                sliderValue = pendingScale
                            }, colors = textBtnColors) {
                                Text("A+", fontWeight = FontWeight.Bold, fontSize = 14.sp * appliedScale)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // PREVIEW — pending
            val previewBackground = when {
                pendingContrast -> Color.Black
                pendingDark -> Color(0xFF121212)
                else -> Color(0xFFF5F5F5)
            }
            val previewText = when {
                pendingContrast -> Color.White
                pendingDark -> Color(0xFFEAEAEA)
                else -> Color.Black
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = previewBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Pré-visualização",
                        fontSize = 16.sp * pendingScale,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingContrast) HighContrastAccent else previewText
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Este é um exemplo de como os textos ficarão com suas preferências.",
                        fontSize = 14.sp * pendingScale,
                        color = if (pendingContrast) HighContrastAccent else previewText
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Títulos, botões e conteúdos se adaptam ao tamanho e contraste.",
                        fontSize = 14.sp * pendingScale,
                        color = if (pendingContrast) HighContrastAccent else previewText
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    sfx.play(Sfx.CLICK)
                    viewModel.setDarkMode(pendingDark)
                    viewModel.setContrastText(pendingContrast)
                    viewModel.setSizeText(pendingScale)
                    onApply(pendingDark, pendingContrast, pendingScale)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (appliedContrast) HighContrastAccent else GreenDark,
                    contentColor = if (appliedContrast) Color.Black else Color.White
                )
            ) {
                Text("Aplicar", fontSize = 16.sp * appliedScale, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    // Unsaved dialog
    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Alterações não aplicadas") },
            text = { Text("Você fez mudanças que ainda não foram aplicadas. Se sair agora, elas serão perdidas.") },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            sfx.play(Sfx.CLICK)
                            showDiscardDialog = false
                            viewModel.setDarkMode(pendingDark)
                            viewModel.setContrastText(pendingContrast)
                            viewModel.setSizeText(pendingScale)
                            onApply(pendingDark, pendingContrast, pendingScale)
                            onBack()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = appliedAccent)
                    ) { Text("Aplicar e sair") }
                    Spacer(Modifier.width(8.dp))
                    TextButton(
                        onClick = { sfx.play(Sfx.CLICK); showDiscardDialog = false; onBack() },
                        colors = ButtonDefaults.textButtonColors(contentColor = appliedText)
                    ) { Text("Descartar e sair") }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { sfx.play(Sfx.CLICK); showDiscardDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = appliedAccent)
                ) { Text("Continuar editando") }
            },
            containerColor = appliedBackground,
            titleContentColor = if (appliedContrast) appliedAccent else appliedText,
            textContentColor = appliedText
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color,
    rowBackground: Color,
    switchColors: SwitchColors = SwitchDefaults.colors(),
    scale: Float
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(rowBackground)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp * scale, color = textColor)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 13.sp * scale, color = textColor.copy(alpha = 0.7f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = switchColors)
    }
}
