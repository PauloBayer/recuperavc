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
import kotlin.math.roundToInt
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.ui.factory.SettingsViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import com.recuperavc.ui.sfx.Sfx
import com.recuperavc.ui.sfx.rememberSfxController
import com.recuperavc.ui.sfx.withSfx

/* ------------------------- Cores ---------------------------- */
private val Olive = Color(0xFF5E6F48)
private val OliveDark = Color(0xFF4C5C3A)
private val ChipLime = Color(0xFFB9E87A)
private val ChipLimeText = Color(0xFF2B3A11)
private val ChipMint = Color(0xFFDDF7B5)
private val ChipMintText = Color(0xFF2A3A13)
private val CardTint = Color(0x1AFFFFFF)

/* ------------------------- Constantes ---------------------------- */
private const val MIN_REQUIRED = 3

/* ------------------------- Dados ---------------------------- */
data class CoherenceTry(val typedPhrase: String, val correct: Boolean, val elapsedMs: Long)
data class RoundResult(
    val phraseId: java.util.UUID,
    val typedPhrase: String,
    val timeElapsed: Long,
    val correct: Boolean,
    val tries: List<CoherenceTry>
)

/* ------------------------- Multi-Rodadas com limite = tamanho da lista ---------------------------- */
@Composable
fun SentenceArrangeMultiRound(
    context: Context,
    phrases: List<Phrase>,
    sfx: com.recuperavc.ui.sfx.SfxController,
    onBack: () -> Unit = {},
    onSave: (List<RoundResult>) -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(LocalContext.current))
) {
    val sessionLimit = phrases.size.coerceAtLeast(1) // garante >=1
    var index by rememberSaveable { mutableStateOf(0) }
    val results = remember { mutableStateListOf<RoundResult>() }
    var showEndDialog by remember { mutableStateOf(false) }

    // Back físico → diálogo (sair antes do limite)
    BackHandler(enabled = true) {
        sfx.play(Sfx.CLICK)
        showEndDialog = true
    }

    if (showEndDialog) {
        val count = results.size
        AlertDialog(
            onDismissRequest = {
                sfx.play(Sfx.BUBBLE)
                showEndDialog = false
            },
            confirmButton = {
                Button(onClick = {
                    sfx.play(Sfx.CLICK)
                    if (count >= MIN_REQUIRED) {
                        onSave(results.toList())
                    } else {
                        onBack()
                    }
                    showEndDialog = false
                }) {
                    Text(if (count >= MIN_REQUIRED) "Salvar e Sair" else "Descartar e Sair")
                }
            },
            dismissButton = {
                if (index < sessionLimit) {
                    TextButton(onClick = {
                        sfx.play(Sfx.CLICK)
                        showEndDialog = false
                    }) { Text("Continuar") }
                }
            },
            title = { Text("Encerrar sessão") },
            text = {
                Text(
                    if (results.size >= MIN_REQUIRED)
                        "Você montou ${results.size} frases. Deseja salvar o relatório e sair?"
                    else
                        "Você montou ${results.size} de $MIN_REQUIRED frases mínimas. Se sair agora, os resultados não serão salvos."
                )
            }
        )
    }

    val currentPhrase = phrases.getOrNull(index)
    if (currentPhrase == null) {
        // Falha defensiva; normalmente não chega aqui porque salvamos automaticamente ao atingir o limite
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Olive),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Não há mais frases nesta categoria.", color = Color.White)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { sfx.play(Sfx.CLICK); onBack() }) {
                    Text("Voltar")
                }
            }
        }
        return
    }

    SentenceArrangeScreen(
        context = context,
        phraseEntity = currentPhrase,
        sessionCount = results.size,
        sessionLimit = sessionLimit,
        sfx = sfx,
        canFinish = results.size >= MIN_REQUIRED,
        onFinishRequested = {
            sfx.play(Sfx.CLICK)
            onSave(results.toList())
        },
        onResult = { result ->
            // adiciona o resultado, avança índice e, se bateu no limite, salva automaticamente
            results.add(result)
            val nextIndex = index + 1
            index = nextIndex
            if (nextIndex >= sessionLimit) {
                // atingiu o máximo de frases para a categoria → salva automaticamente
                onSave(results.toList())
            }
        },
        onBack = {
            sfx.play(Sfx.CLICK)
            showEndDialog = true
        }
    )
}

/* ------------------------- Tela de Montar Frase ---------------------------- */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceArrangeScreen(
    context: Context,
    modifier: Modifier = Modifier,
    phraseEntity: Phrase,
    sessionCount: Int,              // quantas concluídas
    sessionLimit: Int,              // máximo permitido (tamanho da lista)
    sfx: com.recuperavc.ui.sfx.SfxController,
    canFinish: Boolean,             // >= MIN_REQUIRED
    onFinishRequested: () -> Unit,  // salvar & sair
    onResult: (RoundResult) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val phrase = phraseEntity.description
    val words = remember(phrase) { phrase.split(" ") }
    var arrangedWords by rememberSaveable(phrase) { mutableStateOf(listOf<String>()) }
    var availableWords by rememberSaveable(phrase) { mutableStateOf(words.shuffled()) }
    var result by rememberSaveable { mutableStateOf<Boolean?>(null) }
    val startTime = remember(phrase) { System.currentTimeMillis() }
    val tries = remember(phrase) { mutableStateListOf<CoherenceTry>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Olive)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Monte a frase") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                                contentDescription = "Voltar",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = OliveDark,
                        navigationIconContentColor = Color.White,
                        titleContentColor = Color.White
                    )
                )
            },
            // sem FAB no Scaffold (vamos posicionar os botões dentro do conteúdo)
            bottomBar = {
                BottomAppBar(containerColor = OliveDark) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Sessão $sessionCount de $MIN_REQUIRED (mínimo)",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        ) { padding ->
            // Conteúdo + camada de botões
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                /* ======= CONTEÚDO PRINCIPAL ======= */
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Monte a frase:",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )

                    // Área de frase montada
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
                                        onClick = ({
                                            arrangedWords = arrangedWords - word
                                            availableWords = availableWords + word
                                        }).withSfx(sfx, Sfx.CLICK),
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

                    // Palavras disponíveis
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        availableWords.forEach { word ->
                            WordChip(
                                text = word,
                                onClick = ({
                                    arrangedWords = arrangedWords + word
                                    availableWords = availableWords - word
                                }).withSfx(sfx, Sfx.CLICK),
                                shape = RoundedCornerShape(18.dp),
                                container = ChipLime,
                                content = ChipLimeText
                            )
                        }
                    }

                    Spacer(Modifier.height(64.dp)) // respiro
                }

                /* ======= CAMADA DOS BOTÕES ======= */
                val bottomOffset = 16.dp // evitar colisão com BottomAppBar

                // LIMPAR — canto inferior esquerdo
                TextButton(
                    onClick = {
                        sfx.play(Sfx.CLICK)
                        arrangedWords = emptyList()
                        availableWords = words.shuffled()
                        result = null
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = bottomOffset)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Text("Limpar", color = Color.White)
                }

                // SALVAR TESTE — centro inferior (manual, fica visível com >= MIN_REQUIRED)
                if (canFinish) {
                    Button(
                        onClick = onFinishRequested,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = bottomOffset)
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        Text("Salvar Teste", color = OliveDark, fontWeight = FontWeight.Bold)
                    }
                }

                // VERIFICAR — canto inferior direito
                val canVerify = arrangedWords.isNotEmpty()
                ExtendedFloatingActionButton(
                    onClick = {
                        if (!canVerify) {
                            sfx.play(Sfx.WRONG_ANSWER)
                        } else {
                            sfx.play(Sfx.CLICK)

                            val elapsed = System.currentTimeMillis() - startTime
                            val typed = arrangedWords.joinToString(" ")
                            val correct = typed == phrase
                            result = correct
                            tries.add(CoherenceTry(typed, correct, elapsed))

                            if (correct) {
                                sfx.play(Sfx.RIGHT_ANSWER)
                                onResult(
                                    RoundResult(
                                        phraseId = phraseEntity.id,
                                        typedPhrase = typed,
                                        timeElapsed = elapsed,
                                        correct = true,
                                        tries = tries.toList()
                                    )
                                )
                                arrangedWords = emptyList()
                                availableWords = words.shuffled()
                                result = null
                            } else {
                                sfx.play(Sfx.WRONG_ANSWER)
                            }
                        }
                    },
                    containerColor = ChipLime,
                    contentColor = ChipLimeText,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = bottomOffset)
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) { Text("Verificar") }
            }
        }
    }
}

/* ------------------------- Container Principal ---------------------------- */
@Composable
fun SentenceArrange(
    context: Context,
    onBackToHome: () -> Unit = {}
) {
    val sfx = rememberSfxController()

    var phrases by remember { mutableStateOf<List<Phrase>>(emptyList()) }
    var showResults by rememberSaveable { mutableStateOf(false) }
    val results = remember { mutableStateListOf<RoundResult>() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val db = DbProvider.db(context)
        phrases = db.phraseDao().getAll() // lista já filtrada pela categoria escolhida na navegação
    }

    if (showResults) {
        val phraseMap = remember(phrases) { phrases.associateBy { it.id } }
        SentenceResultScreen(
            phrases = results.map { phraseMap[it.phraseId]?.description ?: "" },
            results = results,
            sfx = sfx,
            onBackToHome = {
                onBackToHome()
            },
            onRestart = {
                sfx.play(Sfx.CLICK)
                results.clear()
                showResults = false
            }
        )
    } else if (phrases.isNotEmpty()) {
        SentenceArrangeMultiRound(
            context = context,
            phrases = phrases,
            sfx = sfx,
            onBack = { onBackToHome() },
            onSave = { finalResults ->
                scope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    val db = DbProvider.db(context)
                    val count = finalResults.size
                    val avgTimeUntilCorrectSec =
                        if (count > 0) finalResults.map { it.timeElapsed }.average().toFloat() / 1000f else 0f
                    val avgTries =
                        if (count > 0) finalResults.map { it.tries.size }.average().toFloat() else 0f
                    val successRate =
                        if (count > 0) finalResults.count { it.correct }.toFloat() / count.toFloat() else 0f
                    val reportId = java.util.UUID.randomUUID()
                    val mainPhraseId = finalResults.firstOrNull()?.phraseId

                    val attemptsArray = org.json.JSONArray().apply {
                        finalResults.forEach { r ->
                            val triesArr = org.json.JSONArray().apply {
                                r.tries.forEach { t ->
                                    put(org.json.JSONObject().apply {
                                        put("typed", t.typedPhrase)
                                        put("correct", t.correct)
                                        put("elapsedMs", t.elapsedMs)
                                    })
                                }
                            }
                            put(org.json.JSONObject().apply {
                                put("phraseId", r.phraseId.toString())
                                put("success", r.correct)
                                put("triesCount", r.tries.size)
                                put("timeUntilCorrectMs", r.timeElapsed)
                                put("tries", triesArr)
                            })
                        }
                    }

                    val desc = org.json.JSONObject().apply {
                        put("count", count)
                        put("avgTimeUntilCorrectSec", avgTimeUntilCorrectSec)
                        put("avgTries", avgTries)
                        put("successRate", successRate)
                        put("attempts", attemptsArray)
                    }.toString()

                    val report = CoherenceReport(
                        id = reportId,
                        averageErrorsPerTry = avgTries,
                        averageTimePerTry = avgTimeUntilCorrectSec,
                        allTestsDescription = desc,
                        date = java.time.Instant.now(),
                        phraseId = mainPhraseId
                    )

                    db.coherenceReportDao().upsert(report)
                    finalResults.map { it.phraseId }.distinct().forEach { pid ->
                        db.coherenceReportDao().link(
                            CoherenceReportGroup(
                                idCoherenceReport = reportId,
                                idPhrase = pid
                            )
                        )
                    }
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        results.clear()
                        results.addAll(finalResults)
                        showResults = true
                    }
                }
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
    sfx: com.recuperavc.ui.sfx.SfxController,
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
                                text = "Frase correta: ${phrases.getOrNull(idx) ?: "—"}",
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
                    onClick = { sfx.play(Sfx.CLICK); onRestart() },
                    colors = ButtonDefaults.buttonColors(containerColor = ChipLime),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Reiniciar teste", color = ChipLimeText)
                }

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = { sfx.play(Sfx.CLICK); onBackToHome() },
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
