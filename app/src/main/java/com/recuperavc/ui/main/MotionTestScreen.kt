package com.recuperavc.ui.main

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import java.time.Instant
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Motion test screen: shows a randomly positioned button that users must tap
 * as many times as possible within [durationSeconds]. After completion,
 * the result is saved to the database as a MotionReport and passed to [onFinish].
 */
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

    var clicks by remember { mutableStateOf(0) }
    var timeLeft by remember { mutableStateOf(durationSeconds) }
    var finished by remember { mutableStateOf(false) }
    var buttonPosition by remember { mutableStateOf(Offset.Zero) }

    val buttonSize = 56.dp
    val density = LocalDensity.current

    // Countdown timer
    LaunchedEffect(durationSeconds) {
        while (timeLeft > 0) {
            delay(1_000)
            timeLeft--
        }
        finished = true
        val minutes = durationSeconds / 60f
        val clicksPerMinute = if (minutes > 0) (clicks / minutes).toInt() else clicks

        // Persist result
        db.MotionReportDao().upsert(
            MotionReport(
                date = Instant.now(),
                secondsTotal = durationSeconds.toFloat(),
                clicksPerMinute = clicksPerMinute,
                totalClicks = clicks,
                userId = CurrentUser.ID
            )
        )
        onFinish(clicks)
    }

    @Suppress("UnusedBoxWithConstraintsScope")
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val maxHeightPx = with(density) { maxHeight.toPx() }
        val btnPx = with(density) { buttonSize.toPx() }

        fun moveButtonRandomly() {
            val x = Random.nextFloat() * (maxWidthPx - btnPx)
            val y = Random.nextFloat() * (maxHeightPx - btnPx - 120f) + 80f
            buttonPosition = Offset(x, y)   // <-- update state here
        }

        // Initialise button position
        LaunchedEffect(Unit) {
            moveButtonRandomly()
        }

        // Timer + click counter
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tempo restante: ${timeLeft}s",
                fontSize = 20.sp,
                color = GreenDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Cliques: $clicks",
                fontSize = 20.sp,
                color = GreenDark
            )
        }

        if (!finished) {
            // The moving button
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
                    modifier = Modifier.size(buttonSize),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GreenAccent,
                        contentColor = Color.White
                    )
                ) {}
            }
        } else {
            // Results screen
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Fim do teste!", fontSize = 22.sp, color = GreenDark)
                Spacer(Modifier.height(8.dp))
                Text("Total de cliques: $clicks", fontSize = 20.sp, color = GreenDark)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { onBack() },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDark)
                ) {
                    Text("Voltar", color = Color.White)
                }
            }
        }
    }
}
