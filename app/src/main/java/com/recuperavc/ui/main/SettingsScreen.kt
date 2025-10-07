package com.recuperavc.ui.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.recuperavc.ui.theme.GreenDark
import com.recuperavc.ui.theme.GreenLight
import com.recuperavc.ui.theme.OnBackground
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    // Deixando aqui algumas configs para serem usadas para quando tiver a lógica
    onApply: (darkMode: Boolean, highContrast: Boolean, fontScale: Float) -> Unit = { _, _, _ -> }
) {
    var darkMode by remember { mutableStateOf(false) }
    var highContrast by remember { mutableStateOf(false) }
    var fontScale by remember { mutableStateOf(1.0f) }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Header
        Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.Top
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, tint = OnBackground)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Preferências do App",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = OnBackground
            )
            Spacer(Modifier.height(16.dp))

            // Card da aparência
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Aparência", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    Spacer(Modifier.height(8.dp))

                    SettingSwitchRow(
                        title = "Modo escuro",
                        subtitle = "Usa um tema com fundo escuro",
                        checked = darkMode,
                        onCheckedChange = { darkMode = it }
                    )

                    Spacer(Modifier.height(12.dp))

                    SettingSwitchRow(
                        title = "Alto contraste",
                        subtitle = "Melhora a legibilidade com maior contraste",
                        checked = highContrast,
                        onCheckedChange = { highContrast = it }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Card de tamanho de texto
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Tamanho do texto", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                    Spacer(Modifier.height(8.dp))

                    val percent = (fontScale * 100).roundToInt()
                    Text(
                        "Tamanho atual: $percent%",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(8.dp))

                    // Slider da fonte
                    Column {
                        Slider(
                            value = fontScale,
                            onValueChange = { fontScale = it.coerceIn(0.8f, 1.6f) },
                            valueRange = 0.8f..1.6f,
                            steps = 7 // Para dar mais opções pro usuário, podemos só alterar essa parte aqui
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { fontScale = (fontScale - 0.1f).coerceIn(0.8f, 1.6f) }) {
                                Text("A-", color = GreenDark, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { fontScale = 1.0f }) {
                                Text("Padrão", color = GreenDark, fontWeight = FontWeight.Bold)
                            }
                            TextButton(onClick = { fontScale = (fontScale + 0.1f).coerceIn(0.8f, 1.6f) }) {
                                Text("A+", color = GreenDark, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Texto de prévia
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (highContrast) Color.Black else Color(0xFFF5F5F5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Pré-visualização",
                        fontSize = (16.sp * fontScale),
                        fontWeight = FontWeight.Bold,
                        color = if (highContrast) Color.White else Color.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Este é um exemplo de como os textos ficarão com suas preferências.",
                        fontSize = (14.sp * fontScale),
                        color = if (highContrast) Color.White else Color.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Títulos, botões e conteúdos se adaptam ao tamanho e contraste.",
                        fontSize = (14.sp * fontScale),
                        color = if (highContrast) Color.White else Color.Black
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Ações do footer
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Voltar")
                }
                Button(
                    onClick = { onApply(darkMode, highContrast, fontScale); onBack() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
                ) {
                    Text("Aplicar", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
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
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = Color.Black)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 13.sp, color = Color.Black.copy(alpha = 0.7f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
