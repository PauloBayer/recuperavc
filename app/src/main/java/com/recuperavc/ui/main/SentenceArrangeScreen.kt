@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.recuperavc.ui.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recuperavc.data.db.DbProvider
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.CoherenceReportGroup
import com.recuperavc.models.Phrase
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.factory.SettingsViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState


/* ------------------------- Cores ---------------------------- */
private val Olive = Color(0xFF5E6F48)
private val OliveDark = Color(0xFF4C5C3A)
private val ChipLime = Color(0xFFB9E87A)
private val ChipLimeText = Color(0xFF2B3A11)
private val ChipMint = Color(0xFFDDF7B5)
private val ChipMintText = Color(0xFF2A3A13)
private val CardTint = Color(0x1AFFFFFF)

/* ------------------------- Dados ---------------------------- */
data class RoundResult(val typedPhrase: String, val timeElapsed: Long)

/* ------------------------- Tela Multi Rodadas ---------------------------- */
@Composable
fun SentenceArrangeMultiRound(
    context: Context,
    phrases: List<Phrase>,
    onBack: () -> Unit = {},
    onFinished: (List<RoundResult>) -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(LocalContext.current))
) {
    val totalRounds = 3
    var currentRound by rememberSaveable { mutableStateOf(0) }
    val results = remember { mutableStateListOf<RoundResult>() }

    val darkMode by viewModel.darkModeFlow.collectAsState(initial = false)
    val contrast by viewModel.contrastFlow.collectAsState(initial = false)
    val fontScale by viewModel.sizeTextFlow.collectAsState(initial = 1.0f)


    BackHandler(enabled = true) { onBack() }

    SentenceArrangeScreen(
        context = context,
        phraseEntity = phrases[currentRound],
        round = currentRound + 1,
        totalRounds = totalRounds,
        onResult = { correct, typed, elapsed ->
            results.add(RoundResult(typed, elapsed))
            if (currentRound + 1 < totalRounds) {
                currentRound++
            } else {
                onFinished(results.toList())
            }
        },
        onBack = onBack
    )
}

/* ------------------------- Tela de Montar Frase ---------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceArrangeScreen(
    context: Context,
    modifier: Modifier = Modifier,
    phraseEntity: Phrase,
    round: Int,
    totalRounds: Int,
    onResult: (Boolean, String, Long) -> Unit = { _, _, _ -> },
    onBack: () -> Unit = {}
) {
    val phrase = phraseEntity.description
    val words = phrase.split(" ")
    var arrangedWords by rememberSaveable(phrase) { mutableStateOf(listOf<String>()) }
    var availableWords by rememberSaveable(phrase) { mutableStateOf(words.shuffled()) }
    var result by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val startTime = remember { System.currentTimeMillis() }
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ChipLime)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Monte a frase ($round/$totalRounds)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = OliveDark,
                    navigationIconContentColor = Color.White,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = OliveDark,
                actions = {
                    TextButton(onClick = {
                        arrangedWords = listOf()
                        availableWords = words.shuffled()
                        result = null
                    }) {
                        Text("Limpar", color = Color.White)
                    }
                },
                floatingActionButton = {
                    val canProceed = arrangedWords.isNotEmpty()
                    val buttonText = if (round == totalRounds) "Enviar" else "Verificar"

                    ExtendedFloatingActionButton(
                        onClick = {
                            if (canProceed) {
                                val endTime = System.currentTimeMillis()
                                val elapsed = endTime - startTime
                                val typedPhrase = arrangedWords.joinToString(" ")
                                val correct = typedPhrase == phrase
                                result = correct

                                coroutineScope.launch {
                                    val db = DbProvider.db(context)

                                    val reportId = UUID.randomUUID()
                                    val report = CoherenceReport(
                                        id = reportId,
                                        averageErrorsPerTry = if (correct) 0f else 100f,
                                        averageTimePerTry = elapsed.toFloat() / 1000f,
                                        allTestsDescription = "typed=$typedPhrase;correct=$correct;elapsed=$elapsed",
                                        phraseId = phraseEntity.id,
                                    )

                                    db.coherenceReportDao().insert(report)
                                    db.coherenceReportDao().link(
                                        CoherenceReportGroup(
                                            idCoherenceReport = reportId,
                                            idPhrase = phraseEntity.id
                                        )
                                    )

                                    onResult(correct, typedPhrase, elapsed)
                                }
                            }
                        },
                        containerColor = ChipLime,
                        contentColor = ChipLimeText,
                        modifier = Modifier.alpha(if (canProceed) 1f else 0.45f)
                    ) { Text(buttonText) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Monte a frase:",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardTint)
                    .padding(12.dp)
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (arrangedWords.isEmpty()) {
                        AssistiveHint()
                    } else {
                        arrangedWords.forEach { word ->
                            WordChip(
                                text = word,
                                onClick = {
                                    arrangedWords = arrangedWords - word
                                    availableWords = availableWords + word
                                },
                                shape = RoundedCornerShape(18.dp),
                                container = ChipMint,
                                content = ChipMintText
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = result != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ResultMessageBox(isCorrect = result == true)
                }
            }

            Divider(color = Color.White.copy(alpha = .2f))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                availableWords.forEach { word ->
                    WordChip(
                        text = word,
                        onClick = {
                            arrangedWords = arrangedWords + word
                            availableWords = availableWords - word
                        },
                        shape = RoundedCornerShape(18.dp),
                        container = ChipLime,
                        content = ChipLimeText
                    )
                }
            }

            Spacer(Modifier.height(64.dp))
        }
    }
}

/* ------------------------- Container Principal ---------------------------- */
@Composable
fun SentenceArrange(
    context: Context,
    onBackToHome: () -> Unit = {}
) {
    var phrases by remember { mutableStateOf<List<Phrase>>(emptyList()) }
    var showResults by rememberSaveable { mutableStateOf(false) }
    val results = remember { mutableStateListOf<RoundResult>() }

    LaunchedEffect(Unit) {
        val db = DbProvider.db(context)
        phrases = db.phraseDao().getAll()
    }

    if (showResults) {
        SentenceResultScreen(
            phrases = phrases.map { it.description },
            results = results,
            onBackToHome = onBackToHome,
            onRestart = {
                results.clear()
                showResults = false
            }
        )
    } else if (phrases.isNotEmpty()) {
        SentenceArrangeMultiRound(
            context = context,
            phrases = phrases,
            onBack = onBackToHome,
            onFinished = { finalResults ->
                results.clear()
                results.addAll(finalResults)
                showResults = true
            }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Carregando frases...", color = Color.White)
        }
    }
}

/* ------------------------- Componentes de UI ---------------------------- */
@Composable
private fun WordChip(
    text: String,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    container: Color,
    content: Color
) {
    var pressed by remember(text) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120))
    val alpha by animateFloatAsState(if (pressed) 0.85f else 1f, tween(120))

    Surface(
        color = container,
        contentColor = content,
        shape = shape,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .scale(scale)
            .alpha(alpha)
            .clip(shape)
            .clickable {
                pressed = true
                onClick()
                pressed = false
            }
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun AssistiveHint() {
    Text(
        "Toque nos botões para montar a frase",
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        textAlign = TextAlign.Center,
        color = Color.White.copy(alpha = 0.85f)
    )
}

@Composable
private fun ResultMessageBox(isCorrect: Boolean) {
    val bg = if (isCorrect) Color(0xFFDCFCE7) else Color(0xFFFFE4E6)
    val fg = if (isCorrect) Color(0xFF065F46) else Color(0xFF991B1B)
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(10.dp)
    ) {
        Text(
            text = if (isCorrect) "Perfeito! ✔︎" else "A frase está errada. Tente novamente.",
            color = fg,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

/* ------------------------- Tela de Resultados ---------------------------- */
@Composable
fun SentenceResultScreen(
    phrases: List<String>,
    results: List<RoundResult>,
    onBackToHome: () -> Unit = {},
    onRestart: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .wrapContentHeight(),
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
                Text(
                    "Resultados Finais",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(Modifier.height(16.dp))

                results.forEachIndexed { idx, r ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardTint)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Frase correta: ${phrases[idx]}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Sua frase: ${r.typedPhrase}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black
                            )
                            Text(
                                text = "Tempo: ${formatTime(r.timeElapsed)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = ChipLime),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Reiniciar teste", color = ChipLimeText)
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = onBackToHome,
                    colors = ButtonDefaults.buttonColors(containerColor = OliveDark),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Voltar para tela inicial", color = Color.White)
                }
            }
        }
    }
}

/* ------------------------- Util ---------------------------- */
private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val centiseconds = ((ms % 1000) / 10.0).roundToInt()
    return "$seconds.${centiseconds}s"
}
