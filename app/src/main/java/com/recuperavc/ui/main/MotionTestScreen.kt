package com.recuperavc.ui.main

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
import com.recuperavc.data.CurrentUser
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

private enum class MotionMode { MOVING, STATIC }
private enum class Hand { RIGHT, LEFT }

@SuppressLint("UnrememberedMutableState")
@Composable
fun MotionTestScreen(
    modifier: Modifier = Modifier,
    durationSecondsWithMovement: Int = 60,
    durationSecondsWithoutMovement: Int = 30,
    onFinish: (Int) -> Unit = {},
    onBack: () -> Unit = {}
) {
    BackHandler(enabled = true) { onBack() }

    val context = LocalContext.current
    val db = remember(context) { DbProvider.db(context) }
    val scope = rememberCoroutineScope()

    // Escolhas do usuário antes do teste
    var selectedHand by remember { mutableStateOf<Hand?>(null) }
    var isDominant by remember { mutableStateOf<Boolean?>(null) }
    var chosenMode by remember { mutableStateOf<MotionMode?>(null) }

    // Controle do ciclo do teste
    var testStarted by remember { mutableStateOf(false) }
    var clicks by remember { mutableStateOf(0) }
    var missedClicks by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(durationSecondsWithMovement) }
    var finished by remember { mutableStateOf(false) }
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
    var lastReport by remember { mutableStateOf<MotionReport?>(null) }

    // Histórico
    val history by db.MotionReportDao()
        .observeForUser(CurrentUser.ID)
        .collectAsState(initial = emptyList())

    val density = LocalDensity.current

    // Medida do header pra evitar sobreposição do botão
    var headerHeightPx by remember { mutableStateOf(0f) }
    val headerMarginPx = 12f

    fun durationFor(mode: MotionMode?) =
        if (mode == MotionMode.STATIC) durationSecondsWithoutMovement else durationSecondsWithMovement

    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            // Cliques errados fora do botão
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

        // Tamanho dos botões
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

        // Quando o teste começa (após “Começar teste”), inicia timer e reseta contadores
        LaunchedEffect(testStarted) {
            if (testStarted) {
                // Duração efetiva de acordo com o modo
                val effectiveDuration = durationFor(chosenMode)

                timeLeft = effectiveDuration
                clicks = 0
                missedClicks = 0
                finished = false

                // Se modo com movimento, posiciona o botão inicialmente
                if (chosenMode == MotionMode.MOVING) moveButtonRandomly()

                while (timeLeft > 0) {
                    delay(1_000)
                    timeLeft--
                }

                // Cálculo correto por minuto baseado na duração efetiva
                val clicksPerMinute = if (effectiveDuration > 0f)
                    (clicks / effectiveDuration).toInt()
                else
                    clicks

                // Persistir com o novo modelo
                val report = MotionReport(
                    date = Instant.now(),
                    secondsTotal = effectiveDuration.toFloat(),
                    clicksPerMinute = clicksPerMinute,
                    totalClicks = clicks,
                    withRightHand = (selectedHand == Hand.RIGHT),
                    withMainHand = (isDominant == true),
                    withMovement = (chosenMode == MotionMode.MOVING),
                    missedClicks = missedClicks,
                    userId = CurrentUser.ID
                )
                scope.launch { db.MotionReportDao().upsert(report) }
                lastReport = report
                finished = true
                onFinish(clicks)
            }
        }

        // HEADER enquanto o teste roda
        if (testStarted && !finished) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .onGloballyPositioned { coords ->
                        headerHeightPx = coords.size.height.toFloat()
                    },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Tempo restante: ${timeLeft}s", fontSize = 20.sp, color = GreenDark)
                Spacer(Modifier.height(8.dp))
                Text("Cliques: $clicks", fontSize = 20.sp, color = GreenDark)
                Spacer(Modifier.height(4.dp))
                Text("Errados: $missedClicks", fontSize = 16.sp, color = GreenDark)
            }
        }

        // TELA DE PRÉ-TESTE
        if (!testStarted && !finished) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Antes de começar", fontSize = 22.sp, color = GreenDark)
                Spacer(Modifier.height(16.dp))

                // 1) Mão usada
                Text("Qual mão você vai usar?", fontSize = 16.sp, color = Color.Black)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { selectedHand = Hand.RIGHT },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedHand == Hand.RIGHT) GreenDark else GreenAccent
                        )
                    ) { Text("Direita", color = Color.White) }

                    Button(
                        onClick = { selectedHand = Hand.LEFT },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedHand == Hand.LEFT) GreenDark else GreenAccent
                        )
                    ) { Text("Esquerda", color = Color.White) }
                }

                Spacer(Modifier.height(16.dp))

                // 2) Dominância
                Text("É a sua mão dominante?", fontSize = 16.sp, color = Color.Black)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { isDominant = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDominant == true) GreenDark else GreenAccent
                        )
                    ) { Text("Sim", color = Color.White) }

                    Button(
                        onClick = { isDominant = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDominant == false) GreenDark else GreenAccent
                        )
                    ) { Text("Não", color = Color.White) }
                }

                Spacer(Modifier.height(16.dp))

                // 3) Modo do teste
                Text("Como deseja realizar o teste?", fontSize = 16.sp, color = Color.Black)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { chosenMode = MotionMode.MOVING },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (chosenMode == MotionMode.MOVING) GreenDark else GreenAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Com movimento (botão muda de posição)", color = Color.White) }

                    Button(
                        onClick = { chosenMode = MotionMode.STATIC },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (chosenMode == MotionMode.STATIC) GreenDark else GreenAccent
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Sem movimento (botão fixo)", color = Color.White) }
                }

                Spacer(Modifier.height(20.dp))

                // Texto informando a duração correta conforme a escolha
                val previewDuration = when (chosenMode) {
                    MotionMode.MOVING -> durationSecondsWithMovement
                    MotionMode.STATIC -> durationSecondsWithoutMovement
                    null -> null
                }
                Text(
                    text = previewDuration?.let { "Você terá ${it}s para tocar o botão o máximo possível." }
                        ?: "Você terá ${durationSecondsWithMovement}s com movimento ou ${durationSecondsWithoutMovement}s sem movimento.",
                    fontSize = 14.sp,
                    color = Color.Black
                )

                Spacer(Modifier.height(20.dp))

                val canStart = selectedHand != null && isDominant != null && chosenMode != null
                Button(
                    onClick = { testStarted = true },
                    enabled = canStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (canStart) GreenDark else Color(0xFF9E9E9E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Começar teste", color = Color.White) }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Voltar") }
            }
            return@BoxWithConstraints
        }

        // CORPO DO TESTE
        if (testStarted && !finished && chosenMode == MotionMode.MOVING) {
            // Botão que se movimenta
            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            buttonPosition.x.roundToInt(),
                            buttonPosition.y.roundToInt()
                        )
                    }
            ) {
                Button(
                    onClick = {
                        clicks++
                        moveButtonRandomly()
                    },
                    modifier = Modifier.size(movingButtonSize),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenAccent,
                        contentColor = Color.White
                    )
                ) {}
            }
        }

        if (testStarted && !finished && chosenMode == MotionMode.STATIC) {
            // Botão estático centralizado abaixo do header
            val headerHeightDp = with(density) { (headerHeightPx + headerMarginPx).toDp() }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeightDp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { clicks++ },
                    modifier = Modifier.size(staticButtonSize),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenAccent,
                        contentColor = Color.White
                    )
                ) {}
            }
        }

        // RESULTADOS
        if (finished) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Top,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Fim do teste!", fontSize = 22.sp, color = GreenDark)
                Spacer(Modifier.height(12.dp))

                lastReport?.let { report ->
                    ReportCard(report)
                    Spacer(Modifier.height(16.dp))
                }

                Divider(thickness = 1.dp, color = Color(0xFFE0E0E0))
                Spacer(Modifier.height(12.dp))

                if (history.isNotEmpty()) {
                    Text("Relatórios recentes", fontSize = 18.sp, color = GreenDark)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        history.take(5).forEach { r -> SmallReportRow(r) }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            // Reset total para novo teste
                            selectedHand = null
                            isDominant = null
                            chosenMode = null
                            testStarted = false
                            finished = false
                            clicks = 0
                            missedClicks = 0
                            timeLeft = durationSecondsWithMovement
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GreenAccent)
                    ) { Text("Novo teste", color = Color.White) }

                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
                    ) { Text("Voltar", color = Color.White) }
                }
            }
        }
    }
}

// Relatório no final
@Composable
private fun ReportCard(report: MotionReport) {
    val dateStr = remember(report.date) {
        LocalDateTime.ofInstant(report.date, ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F8F1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Relatório do teste", fontSize = 18.sp, color = GreenDark)
            Spacer(Modifier.height(8.dp))
            Text("Data/Hora: $dateStr", fontSize = 14.sp, color = Color.Black)
            Text("Duração: ${report.secondsTotal.toInt()}s", fontSize = 14.sp, color = Color.Black)
            Text("Cliques totais: ${report.totalClicks}", fontSize = 14.sp, color = Color.Black)
            Text("Cliques por minuto: ${report.clicksPerMinute}", fontSize = 14.sp, color = Color.Black)
            Text("Cliques errados: ${report.missedClicks}", fontSize = 14.sp, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            // Novos atributos
            Text(
                "Mão usada: ${if (report.withRightHand) "Direita" else "Esquerda"}",
                fontSize = 14.sp,
                color = Color.Black
            )
            Text(
                "Mão dominante: ${if (report.withMainHand) "Sim" else "Não"}",
                fontSize = 14.sp,
                color = Color.Black
            )
            Text(
                "Modo: ${if (report.withMovement) "Com movimento" else "Sem movimento"}",
                fontSize = 14.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun SmallReportRow(report: MotionReport) {
    val dateStr = remember(report.date) {
        LocalDateTime.ofInstant(report.date, ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("• $dateStr", fontSize = 13.sp, color = Color.Black)
                Text(
                    "   ${report.totalClicks} cliques • ${report.clicksPerMinute} cpm • ${report.missedClicks} errados",
                    fontSize = 13.sp,
                    color = Color.Black
                )
                Text(
                    "   ${if (report.withRightHand) "Mão direita" else "Mão esquerda"} • " +
                            (if (report.withMainHand) "Dominante" else "Não dominante") + " • " +
                            (if (report.withMovement) "Com movimento" else "Sem movimento"),
                    fontSize = 12.sp,
                    color = Color.Black
                )
            }
        }
    }
}
