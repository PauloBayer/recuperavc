@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.recuperavc.ui.main

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.recuperavc.data.db.DbProvider
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.CoherenceReportGroup
import com.recuperavc.models.Phrase
import com.recuperavc.models.SettingsViewModel
import com.recuperavc.models.enums.PhraseType
import com.recuperavc.ui.factory.SettingsViewModelFactory
import com.recuperavc.ui.sfx.Sfx
import com.recuperavc.ui.sfx.rememberSfxController
import com.recuperavc.ui.sfx.withSfx
import com.recuperavc.ui.util.InitialSettings
import com.recuperavc.ui.util.PaintSystemBars
import com.recuperavc.ui.util.rememberInitialSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import com.recuperavc.library.PhraseManager
import com.recuperavc.ui.components.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import com.recuperavc.ui.theme.GreenDark

/* ------------------------- Paleta base ---------------------------- */
private val Olive = Color(0xFF5E6F48)
private val OliveDark = Color(0xFF4C5C3A)
private val ChipLime = Color(0xFFB9E87A)
private val ChipLimeText = Color(0xFF2B3A11)
private val ChipMint = Color(0xFFDDF7B5)
private val ChipMintText = Color(0xFF2A3A13)
private val CardTintLight = Color(0x1AFFFFFF)
private val HighContrastAccent = Color(0xFFFFD600)

/* ------------------------- Constantes ---------------------------- */
private const val MIN_REQUIRED = 3
private val actionHeight = 48.dp

/* ------------------------- Dados ---------------------------- */
data class CoherenceTry(val typedPhrase: String, val correct: Boolean, val elapsedMs: Long)
data class RoundResult(
    val phraseId: java.util.UUID,
    val typedPhrase: String,
    val timeElapsed: Long,
    val correct: Boolean,
    val tries: List<CoherenceTry>
)

/* =======================================================================
   ENTRY POINT — sem flicker + settings aplicados no 1º frame
   ======================================================================= */
@Composable
fun SentenceArrange(
    context: Context,
    onBackToHome: () -> Unit = {},
    selectedType: PhraseType = PhraseType.SHORT
) {
    val sfx = rememberSfxController()

    val settings: SettingsViewModel = viewModel(factory = SettingsViewModelFactory(LocalContext.current))
    val initial: InitialSettings? = rememberInitialSettings(settings)
    if (initial == null) {
        PaintSystemBars(background = Color.Black, lightIcons = false)
        Box(Modifier.fillMaxSize().background(Color.Black)) {}
        return
    }
    val appliedDark by settings.darkModeFlow.collectAsState(initial = initial.dark)
    val appliedContrast by settings.contrastFlow.collectAsState(initial = initial.contrast)
    val appliedScale by settings.sizeTextFlow.collectAsState(initial = initial.scale)

    val bgRoot = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF101211)
        else -> Olive
    }
    val textPrimary = Color.White
    val textOnCard = if (appliedContrast || appliedDark) Color.White else Color.Black
    val accent = if (appliedContrast) HighContrastAccent else ChipLime
    val accentText = if (appliedContrast) Color.Black else ChipLimeText
    val topBarColor = when {
        appliedContrast -> Color.Black
        appliedDark -> Color(0xFF1B1C1A)
        else -> OliveDark
    }
    val cardTint = when {
        appliedContrast -> Color.White.copy(alpha = 0.06f)
        appliedDark -> Color.White.copy(alpha = 0.08f)
        else -> CardTintLight
    }

    PaintSystemBars(
        background = if (appliedContrast || appliedDark) Color.Black else OliveDark,
        lightIcons = !(appliedContrast || appliedDark)
    )

    val scope = rememberCoroutineScope()

    var showResults by rememberSaveable { mutableStateOf(false) }
    val results = remember { mutableStateListOf<RoundResult>() }

    if (showResults) {
        var phraseMap by remember(results) { mutableStateOf<Map<java.util.UUID, String>>(emptyMap()) }
        LaunchedEffect(results) {
            val ids = results.map { it.phraseId }.distinct()
            val map = withContext(Dispatchers.IO) {
                val dao = DbProvider.db(context).phraseDao()
                val all = dao.getAll()
                all.filter { it.id in ids }.associate { it.id to it.description }
            }
            phraseMap = map
        }

        SentenceResultScreen(
            phrases = results.map { r -> phraseMap[r.phraseId] ?: "" },
            results = results,
            sfx = sfx,
            appliedContrast = appliedContrast,
            appliedDark = appliedDark,
            appliedScale = appliedScale,
            bgOverlay = Color.Black.copy(alpha = 0.35f),
            cardContainer = if (appliedContrast) Color.Black else Color.White,
            titleColor = if (appliedContrast) HighContrastAccent else textOnCard,
            primaryBtnContainer = accent,
            primaryBtnContent = accentText,
            secondaryBtnContainer = OliveDark,
            secondaryBtnContent = Color.White,
            onBackToHome = { onBackToHome() },
            onRestart = {
                sfx.play(Sfx.CLICK)
                results.clear()
                showResults = false
            }
        )
    } else {
        SentenceArrangeMultiRound(
            context = context,
            selectedType = selectedType,
            sfx = sfx,
            onBack = { onBackToHome() },
            onSave = { finalResults ->
                scope.launch(Dispatchers.IO) {
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

                    withContext(Dispatchers.Main) {
                        results.clear()
                        results.addAll(finalResults)
                        showResults = true
                    }
                }
            },
            // tema/aparência
            appliedContrast = appliedContrast,
            appliedDark = appliedDark,
            appliedScale = appliedScale,
            bgRoot = bgRoot,
            textPrimary = textPrimary,
            textOnCard = textOnCard,
            topBarColor = topBarColor,
            cardTint = cardTint,
            chipAvailContainer = if (appliedContrast) HighContrastAccent else ChipLime,
            chipAvailText = if (appliedContrast) Color.Black else ChipLimeText,
            chipArrContainer = if (appliedContrast) Color.White else ChipMint,
            chipArrText = if (appliedContrast) Color.Black else ChipMintText,
            accent = accent,
            accentText = accentText
        )
    }
}

/* =======================================================================
   MULTI-RODADAS — busca frase aleatória por tipo selecionado
   ======================================================================= */
@Composable
fun SentenceArrangeMultiRound(
    context: Context,
    selectedType: PhraseType,
    sfx: com.recuperavc.ui.sfx.SfxController,
    onBack: () -> Unit = {},
    onSave: (List<RoundResult>) -> Unit,
    // tema/aparência
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    bgRoot: Color,
    textPrimary: Color,
    textOnCard: Color,
    topBarColor: Color,
    cardTint: Color,
    chipAvailContainer: Color,
    chipAvailText: Color,
    chipArrContainer: Color,
    chipArrText: Color,
    accent: Color,
    accentText: Color,
) {
    val scope = rememberCoroutineScope()
    val phraseManager = remember(context) { PhraseManager(context) }
    val results = remember { mutableStateListOf<RoundResult>() }
    var showEndDialog by remember { mutableStateOf(false) }

    var currentPhrase by remember { mutableStateOf<Phrase?>(null) }

    // Initial load
    LaunchedEffect(selectedType) {
        currentPhrase = phraseManager.getNextPhrase(selectedType)
    }

    BackHandler(enabled = true) {
        sfx.play(Sfx.CLICK)
        showEndDialog = true
    }

    if (showEndDialog) {
        val count = results.size
        val container = when {
            appliedContrast -> Color.Black
            appliedDark -> Color(0xFF1E1E1E)
            else -> Color.White
        }
        val titleColor = if (appliedContrast) accent else if (appliedDark) Color.White else Color(0xFF1B1B1B)
        val bodyColor = if (appliedContrast || appliedDark) Color.White else Color(0xFF3A3A3A)

        AlertDialog(
            onDismissRequest = { sfx.play(Sfx.BUBBLE); showEndDialog = false },
            containerColor = container,
            titleContentColor = titleColor,
            textContentColor = bodyColor,
            confirmButton = {
                Button(
                    onClick = {
                        sfx.play(Sfx.CLICK)
                        if (count >= MIN_REQUIRED) onSave(results.toList()) else onBack()
                        showEndDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (appliedContrast) accent else Color.White,
                        contentColor = if (appliedContrast) Color.Black else Color(0xFF2E7D32)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (count >= MIN_REQUIRED) "Salvar e Sair" else "Descartar e Sair",
                        fontSize = 16.sp * appliedScale
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { sfx.play(Sfx.CLICK); showEndDialog = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = accent)
                ) { Text("Continuar", fontSize = 16.sp * appliedScale) }
            },
            title = { Text("Encerrar sessão", fontWeight = FontWeight.SemiBold, fontSize = 20.sp * appliedScale) },
            text = {
                Text(
                    if (results.size >= MIN_REQUIRED)
                        "Você montou ${results.size} frases. Deseja salvar o relatório e sair?"
                    else
                        "Você montou ${results.size} de $MIN_REQUIRED frases mínimas. Se sair agora, os resultados não serão salvos.",
                    fontSize = 15.sp * appliedScale,
                    lineHeight = 20.sp * appliedScale
                )
            }
        )
    }

    val phrase = currentPhrase
    if (phrase == null) {
        Box(modifier = Modifier.fillMaxSize().background(bgRoot), contentAlignment = Alignment.Center) {
            Text("Carregando frase...", color = textPrimary, fontSize = 16.sp * appliedScale)
        }
        return
    }

    SentenceArrangeScreen(
        context = context,
        phraseEntity = phrase,
        sessionCount = results.size,
        sessionLimit = Int.MAX_VALUE,
        sfx = sfx,
        canFinish = results.size >= MIN_REQUIRED,
        onFinishRequested = { sfx.play(Sfx.CLICK); onSave(results.toList()) },
        onResult = { result ->
            results.add(result)
            scope.launch {
                val nextDeferred = async(Dispatchers.IO) { phraseManager.getNextPhrase(selectedType) }
                delay(3000) // Keep banner visible 3s before swapping phrase
                currentPhrase = nextDeferred.await()
            }
        },
        onBack = { sfx.play(Sfx.CLICK); showEndDialog = true },
        // aparência
        appliedContrast = appliedContrast,
        appliedDark = appliedDark,
        appliedScale = appliedScale,
        bgRoot = bgRoot,
        textPrimary = textPrimary,
        textOnCard = textOnCard,
        topBarColor = topBarColor,
        cardTint = cardTint,
        chipAvailContainer = chipAvailContainer,
        chipAvailText = chipAvailText,
        chipArrContainer = chipArrContainer,
        chipArrText = chipArrText,
        accent = accent,
        accentText = accentText
    )
}

/* =======================================================================
   TELA DE MONTAR FRASE — ações inferiores com FlowRow (sem overlap)
   ======================================================================= */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentenceArrangeScreen(
    context: Context,
    modifier: Modifier = Modifier,
    phraseEntity: Phrase,
    sessionCount: Int,
    sessionLimit: Int,
    sfx: com.recuperavc.ui.sfx.SfxController,
    canFinish: Boolean,
    onFinishRequested: () -> Unit,
    onResult: (RoundResult) -> Unit = {},
    onBack: () -> Unit = {},
    // aparência
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    bgRoot: Color,
    textPrimary: Color,
    textOnCard: Color,
    topBarColor: Color,
    cardTint: Color,
    chipAvailContainer: Color,
    chipAvailText: Color,
    chipArrContainer: Color,
    chipArrText: Color,
    accent: Color,
    accentText: Color
) {
    val phrase = phraseEntity.description
    val words = remember(phrase) { phrase.split(" ") }
    var arrangedWords by rememberSaveable(phrase) { mutableStateOf(listOf<String>()) }
    var availableWords by rememberSaveable(phrase) { mutableStateOf(words.shuffled()) }

    // RESULT STATE + SNAPSHOT FOR STABLE CONTENT DURING EXIT
    var result by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var bannerIsCorrect by rememberSaveable { mutableStateOf<Boolean?>(null) }

    val startTime = remember(phrase) { System.currentTimeMillis() }
    val tries = remember(phrase) { mutableStateListOf<CoherenceTry>() }

    // Keep the banner kind stable and visible for 3s; avoid content flip during fade-out
    LaunchedEffect(result) {
        if (result != null) {
            bannerIsCorrect = result  // snapshot the kind once
            delay(3000)
            result = null             // hide (AnimatedVisibility exit plays, content remains stable)
            // keep bannerIsCorrect as last kind; it will be overwritten on next result
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgRoot)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Monte a frase", color = textPrimary, fontSize = 18.sp * appliedScale) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar", tint = textPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = topBarColor,
                        navigationIconContentColor = textPrimary,
                        titleContentColor = textPrimary
                    )
                )
            },
            bottomBar = {
                BottomAppBar(containerColor = topBarColor) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Sessão $sessionCount de $MIN_REQUIRED (mínimo)",
                            color = textPrimary.copy(alpha = 0.9f),
                            fontSize = 14.sp * appliedScale
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Monte a frase:",
                        color = textPrimary,
                        fontSize = 18.sp * appliedScale,
                        fontWeight = FontWeight.Medium
                    )

                    // Área de frase montada
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(cardTint)
                            .padding(12.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (arrangedWords.isEmpty()) {
                                AssistiveHint(textPrimary = textPrimary, appliedScale = appliedScale)
                            } else {
                                arrangedWords.forEach { word ->
                                    WordChip(
                                        text = word,
                                        onClick = ({
                                            arrangedWords = arrangedWords - word
                                            availableWords = availableWords + word
                                        }).withSfx(sfx, Sfx.CLICK),
                                        shape = RoundedCornerShape(18.dp),
                                        container = chipArrContainer,
                                        content = chipArrText,
                                        appliedScale = appliedScale
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = result != null,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            ResultMessageBox(
                                isCorrect = (bannerIsCorrect == true),
                                appliedContrast = appliedContrast,
                                appliedScale = appliedScale
                            )
                        }
                    }

                    Divider(color = textPrimary.copy(alpha = .2f))

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
                                container = chipAvailContainer,
                                content = chipAvailText,
                                appliedScale = appliedScale
                            )
                        }
                    }

                    Spacer(Modifier.height(64.dp))
                }

                /* ---------------- Bottom Actions ---------------- */
                FlowRow(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .windowInsetsPadding(WindowInsets.navigationBars),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 2
                ) {
                    // LIMPAR
                    TextButton(
                        onClick = {
                            sfx.play(Sfx.CLICK)
                            arrangedWords = emptyList()
                            availableWords = words.shuffled()
                            result = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = accent),
                        modifier = Modifier.height(actionHeight)
                    ) { Text("Limpar", color = accent, fontSize = 15.sp * appliedScale) }

                    // SALVAR TESTE
                    if (canFinish) {
                        Button(
                            onClick = onFinishRequested,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (appliedContrast) accent else Color.White,
                                contentColor = if (appliedContrast) Color.Black else OliveDark
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(actionHeight)
                        ) { Text("Salvar Teste", fontWeight = FontWeight.Bold, fontSize = 16.sp * appliedScale) }
                    }

                    // VERIFICAR
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
                                } else {
                                    sfx.play(Sfx.WRONG_ANSWER)
                                }
                            }
                        },
                        containerColor = accent,
                        contentColor = accentText,
                        modifier = Modifier.height(actionHeight)
                    ) { Text("Verificar", fontSize = 15.sp * appliedScale, fontWeight = FontWeight.Medium) }
                }
            }
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
    content: Color,
    appliedScale: Float
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
            fontSize = 16.sp * appliedScale
        )
    }
}

@Composable
private fun AssistiveHint(textPrimary: Color, appliedScale: Float) {
    Text(
        "Toque nos botões para montar a frase",
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        textAlign = TextAlign.Center,
        color = textPrimary.copy(alpha = 0.85f),
        fontSize = 14.sp * appliedScale
    )
}

@Composable
private fun ResultMessageBox(isCorrect: Boolean, appliedContrast: Boolean, appliedScale: Float) {
    val bg = if (isCorrect) Color(0xFFDCFCE7) else if (appliedContrast) Color(0xFFFFF3CD) else Color(0xFFFFE4E6)
    val fg = if (isCorrect) Color(0xFF065F46) else if (appliedContrast) Color(0xFF222222) else Color(0xFF991B1B)
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
            fontSize = 14.sp * appliedScale,
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
    appliedContrast: Boolean,
    appliedDark: Boolean,
    appliedScale: Float,
    bgOverlay: Color,
    cardContainer: Color,
    titleColor: Color,
    primaryBtnContainer: Color,
    primaryBtnContent: Color,
    secondaryBtnContainer: Color,
    secondaryBtnContent: Color,
    onBackToHome: () -> Unit = {},
    onRestart: () -> Unit = {}
) {
    val accent = if (appliedContrast) HighContrastAccent else GreenDark

    val totalTries = results.sumOf { it.tries.size }
    val correctTries = results.sumOf { it.tries.count { tryItem -> tryItem.correct } }
    val successRate = if (totalTries > 0) (correctTries.toFloat() / totalTries.toFloat() * 100f) else 0f
    val avgTime = if (results.isNotEmpty()) results.map { it.timeElapsed }.average().toFloat() / 1000f else 0f

    ResultDialogContainer(
        appliedContrast = appliedContrast,
        appliedDark = appliedDark
    ) {
        ResultTitle(
            text = "Resultados Finais",
            appliedContrast = appliedContrast,
            appliedDark = appliedDark,
            appliedScale = appliedScale,
            accent = accent
        )
        Spacer(Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ResultMetricCard(
                title = "Taxa de acerto",
                value = String.format("%.0f", successRate),
                unit = "%",
                color = accent,
                icon = Icons.Default.CheckCircle,
                appliedContrast = appliedContrast,
                appliedDark = appliedDark,
                appliedScale = appliedScale
            )
            ResultMetricCard(
                title = "Tempo médio",
                value = String.format("%.1f", avgTime),
                unit = "s",
                color = accent,
                icon = Icons.Default.Timer,
                appliedContrast = appliedContrast,
                appliedDark = appliedDark,
                appliedScale = appliedScale
            )
        }
        Spacer(Modifier.height(16.dp))

        ResultSectionLabel(
            text = "Frases montadas",
            appliedContrast = appliedContrast,
            appliedDark = appliedDark,
            appliedScale = appliedScale
        )
        Spacer(Modifier.height(8.dp))



        results.forEachIndexed { idx, r ->
            ResultItemCard(
                appliedContrast = appliedContrast,
                appliedDark = appliedDark
            ) {
                ResultItemText(
                    text = "${idx + 1}. ${phrases.getOrNull(idx) ?: "—"}",
                    appliedContrast = appliedContrast,
                    appliedDark = appliedDark,
                    appliedScale = appliedScale,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16f
                )
                Spacer(Modifier.height(8.dp))

                if (r.tries.size > 1) {
                    ResultItemText(
                        text = "Tentativas: ${r.tries.size}",
                        appliedContrast = appliedContrast,
                        appliedDark = appliedDark,
                        appliedScale = appliedScale,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13f
                    )
                    Spacer(Modifier.height(6.dp))
                }

                r.tries.forEachIndexed { tryIdx, tryItem ->
                    val tryColor = if (tryItem.correct) accent else Color(0xFFD32F2F)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (tryItem.correct) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = tryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            ResultItemText(
                                text = if (r.tries.size > 1) "${tryIdx + 1}ª: ${tryItem.typedPhrase}" else tryItem.typedPhrase,
                                appliedContrast = appliedContrast,
                                appliedDark = appliedDark,
                                appliedScale = appliedScale,
                                fontSize = 14f
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = tryColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                formatTime(tryItem.elapsedMs),
                                color = tryColor,
                                fontSize = 12.sp * appliedScale,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (tryIdx < r.tries.size - 1) {
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = { sfx.play(Sfx.CLICK); onRestart() },
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = if (appliedContrast) Color.Black else Color.White
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) { Text("Reiniciar teste", fontSize = 16.sp * appliedScale, fontWeight = FontWeight.SemiBold) }

        Spacer(Modifier.height(8.dp))

        if (appliedContrast) {
            OutlinedButton(
                onClick = { sfx.play(Sfx.CLICK); onBackToHome() },
                border = BorderStroke(2.dp, accent),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = accent),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Voltar ao Início", fontSize = 16.sp * appliedScale, fontWeight = FontWeight.SemiBold) }
        } else {
            Button(
                onClick = { sfx.play(Sfx.CLICK); onBackToHome() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = secondaryBtnContainer,
                    contentColor = secondaryBtnContent
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Voltar ao Início", fontSize = 16.sp * appliedScale, fontWeight = FontWeight.SemiBold) }
        }
    }
}

/* ------------------------- Util ---------------------------- */
private fun formatTime(ms: Long): String {
    val seconds = ms / 1000
    val centiseconds = ((ms % 1000) / 10.0).roundToInt()
    return "$seconds.${centiseconds}s"
}
