package com.recuperavc.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight
import com.recuperavc.ui.theme.OnBackground
import kotlin.math.roundToInt
import com.recuperavc.ui.sfx.Sfx
import com.recuperavc.ui.sfx.rememberSfxController

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onApply: (darkMode: Boolean, contrast: Boolean, fontScale: Float) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(context))
    val sfx = rememberSfxController()

    // Observe VM
    val darkMode by viewModel.darkModeFlow.collectAsState(initial = false)
    val contrast by viewModel.contrastFlow.collectAsState(initial = false)
    val sizeText by viewModel.sizeTextFlow.collectAsState(initial = 1.0f)

    var sliderValue by remember { mutableStateOf(sizeText) }
    LaunchedEffect(sizeText) { sliderValue = sizeText }

    // Colors based on prefs
    val backgroundColor = when {
        contrast -> Color.Black
        darkMode -> Color(0xFF121212)
        else -> Color.White
    }
    val textColor = if (contrast || darkMode) Color.White else Color.Black

    // Handle system back
    BackHandler(enabled = true) {
        sfx.play(Sfx.CLICK)
        onBack()
    }

    val headerHeight = 160.dp

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor)) {

        // HEADER (drawn at the very top)
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(headerHeight)
        ) {
            // Green wave-ish header
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(0f, h * 0.55f)
                    cubicTo(
                        w * 0.25f, h * 0.25f,
                        w * 0.45f, h * 0.95f,
                        w * 0.6f, h * 0.65f
                    )
                    cubicTo(
                        w * 0.8f, h * 0.35f,
                        w * 0.9f, h * 0.5f,
                        w, h * 0.4f
                    )
                    lineTo(w, 0f)
                    close()
                }
                drawPath(path, color = GreenLight)
            }

            // Back arrow — same look as ReportsScreen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                IconButton(onClick = { sfx.play(Sfx.CLICK); onBack() }) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        tint = OnBackground
                    )
                }
            }
        }

        // CONTENT — shifted *below* the header so it does not cover the arrow
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = headerHeight, start = 16.dp, end = 16.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Preferências do App",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
            Spacer(Modifier.height(16.dp))

            // Aparência
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Aparência", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                    Spacer(Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "Alto contraste",
                        subtitle = "Melhora a legibilidade com maior contraste",
                        checked = contrast,
                        onCheckedChange = {
                            sfx.play(Sfx.CLICK)
                            viewModel.setContrastText(it)
                        },
                        textColor = textColor
                    )

                    Spacer(Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "Modo escuro",
                        subtitle = "Usa um tema com fundo escuro",
                        checked = darkMode,
                        onCheckedChange = {
                            sfx.play(Sfx.CLICK)
                            viewModel.setDarkMode(it)
                        },
                        textColor = textColor
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Tamanho do texto
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = backgroundColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Tamanho do texto", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                    Spacer(Modifier.height(8.dp))

                    val percent = (sizeText * 100).roundToInt()
                    Text(
                        "Tamanho atual: $percent%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))

                    Column {
                        Slider(
                            value = sliderValue,
                            onValueChange = { sliderValue = it },
                            valueRange = 0.8f..1.6f,
                            steps = 7,
                            onValueChangeFinished = { viewModel.setSizeText(sliderValue) }
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { viewModel.setSizeText((sizeText - 0.1f).coerceIn(0.8f, 1.6f)) }) {
                                Text("A-", color = GreenDark, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { viewModel.setSizeText(1.0f) }) {
                                Text("Padrão", color = GreenDark, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { viewModel.setSizeText((sizeText + 0.1f).coerceIn(0.8f, 1.6f)) }) {
                                Text("A+", color = GreenDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Pré-visualização
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (contrast) textColor else Color(0xFFF5F5F5)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "Pré-visualização",
                        fontSize = (16.sp * sizeText),
                        fontWeight = FontWeight.Bold,
                        color = if (contrast) backgroundColor else textColor
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Este é um exemplo de como os textos ficarão com suas preferências.",
                        fontSize = (14.sp * sizeText),
                        color = if (contrast) backgroundColor else textColor
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Títulos, botões e conteúdos se adaptam ao tamanho e contraste.",
                        fontSize = (14.sp * sizeText),
                        color = if (contrast) backgroundColor else textColor
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    sfx.play(Sfx.CLICK)
                    viewModel.setDarkMode(darkMode)
                    viewModel.setContrastText(contrast)
                    viewModel.setSizeText(sizeText)
                    onApply(darkMode, contrast, sizeText)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
            ) {
                Text("Aplicar", color = Color.White)
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF8F8F8))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = textColor)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 13.sp, color = textColor.copy(alpha = 0.7f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
