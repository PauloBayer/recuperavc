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

@SuppressLint("UnrememberedMutableState")
@Composable
fun MotionTestScreen(
    modifier: Modifier = Modifier,
    durationSeconds: Int = 60,
    onFinish: (Int) -> Unit = {},
    onBack: () -> Unit = {}
) {
    BackHandler(enabled = true) { onBack() }

    val context = LocalContext.current
    val db = remember(context) { DbProvider.db(context) }
    val scope = rememberCoroutineScope()

    // Modo escolhido pelo usuário
    var chosenMode by remember { mutableStateOf<MotionMode?>(null) }

    // Infos do teste
    var clicks by remember { mutableStateOf(0) }
    var missedClicks by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(durationSeconds) }
    var finished by remember { mutableStateOf(false) }
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }
    var lastReport by remember { mutableStateOf<MotionReport?>(null) }

    // Histórico
    val history by db.MotionReportDao()
        .observeForUser(CurrentUser.ID)
        .collectAsState(initial = emptyList())

    val density = LocalDensity.current

    // Medindo aqui o tamanho do header pro botão nunca aparecer em cima
    var headerHeightPx by remember { mutableStateOf(0f) }
    val headerMarginPx = 12f // mais uma margem de segurança

    // Timer aqui que só começa dps q o tipo do teste é escolhido
    LaunchedEffect(chosenMode) {
        if (chosenMode != null) {
            timeLeft = durationSeconds
            clicks = 0
            missedClicks = 0
            finished = false
            while (timeLeft > 0) {
                delay(1_000)
                timeLeft--
            }
            val minutes = durationSeconds / 60f
            val clicksPerMinute = if (minutes > 0) (clicks / minutes).toInt() else clicks

            val report = MotionReport(
                date = Instant.now(),
                secondsTotal = durationSeconds.toFloat(),
                clicksPerMinute = clicksPerMinute,
                totalClicks = clicks,
                missedClicks = missedClicks,
                userId = CurrentUser.ID
            )
            scope.launch { db.MotionReportDao().upsert(report) }
            lastReport = report
            finished = true
            onFinish(clicks)
        }
    }

    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            // Cliques errados aqui
            .pointerInput(chosenMode, finished) {
                detectTapGestures(
                    onTap = {
                        if (!finished && chosenMode != null) missedClicks++
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

        // Pro botão começar a se mover
        LaunchedEffect(chosenMode, headerHeightPx) {
            if (chosenMode == MotionMode.MOVING) moveButtonRandomly()
        }

        // Componentes antes de começar o teste
        if (chosenMode == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Como deseja realizar o teste?", fontSize = 22.sp, color = GreenDark)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { chosenMode = MotionMode.MOVING },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Com movimento (botão muda de posição)", color = Color.White) }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { chosenMode = MotionMode.STATIC },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Sem movimento (botão fixo)", color = Color.White) }
                Spacer(Modifier.height(24.dp))
                Text(
                    "Você terá ${durationSeconds}s para tocar o botão o máximo possível.",
                    fontSize = 14.sp,
                    color = Color.Black
                )
            }
            return@BoxWithConstraints
        }

        // Header enquanto o teste estiver rodando
        if (!finished) {
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

        // Corpo do teste fica aqui
        if (!finished && chosenMode == MotionMode.MOVING) {
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

        if (!finished && chosenMode == MotionMode.STATIC) {
            // Botão que fica parado
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

        // Componentes dos resultados recentes
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
                        onClick = { chosenMode = null }, // run again
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
            Text("Cliques errados: ${report.missedClicks}", fontSize = 14.sp, color = Color.Black) // wording updated
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
            }
        }
    }
}