@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.recuperavc.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.key
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/* Cores no esquema do protótipo que a Nina fez */
private val Olive = Color(0xFF5E6F48)
private val OliveDark = Color(0xFF4C5C3A)
private val ChipLime = Color(0xFFB9E87A)
private val ChipLimeText = Color(0xFF2B3A11)
private val ChipMint = Color(0xFFDDF7B5)
private val ChipMintText = Color(0xFF2A3A13)
private val CardTint = Color(0x1AFFFFFF)

/* O status que cada palavra pode ter */

enum class WordStatus { POOL, SELECTED }

@Stable
data class Word(
    val id: Int,
    val text: String,
    val status: WordStatus,
    val selectedOrder: Int? = null
)

@Stable
data class SentenceUIState(
    val words: List<Word>,
    val correctOrder: List<String>,
    val isCorrect: Boolean? = null
)

@Composable
fun SentenceArrangeScreen(
    modifier: Modifier = Modifier,
    phrase: String,
    onResult: (Boolean) -> Unit = {},
    onBack: () -> Unit = {}
) {
    // Handle system back
    BackHandler(enabled = true) { onBack() }

    val correctOrder = rememberSaveable(phrase) { tokenize(phrase) }

    // Save/restore words including selectedOrder
    val wordsSaver = listSaver<SnapshotStateList<Word>, String>(
        save = { list ->
            list.map { "${it.id}|${it.text}|${it.status.name}|${it.selectedOrder ?: ""}" }
        },
        restore = { list ->
            list.map {
                val parts = it.split('|', limit = 4)
                val id = parts[0].toInt()
                val text = parts[1]
                val status = WordStatus.valueOf(parts[2])
                val ord = parts.getOrNull(3)?.toIntOrNull()
                Word(id, text, status, ord)
            }.toMutableStateList()
        }
    )

    val wordsState = rememberSaveable(phrase, saver = wordsSaver) {
        tokenize(phrase)
            .mapIndexed { index, text ->
                Word(id = index, text = text, status = WordStatus.POOL)
            }
            .shuffled()
            .toMutableStateList()
    }

    var nextOrder by rememberSaveable { mutableStateOf(0) }
    var result: Boolean? by rememberSaveable { mutableStateOf(null) }

    fun onWordClick(wordId: Int, newStatus: WordStatus) {
        val idx = wordsState.indexOfFirst { it.id == wordId }
        if (idx == -1) return
        val current = wordsState[idx]
        when (newStatus) {
            WordStatus.SELECTED -> {
                if (current.status != WordStatus.SELECTED) {
                    wordsState[idx] = current.copy(status = WordStatus.SELECTED, selectedOrder = nextOrder++)
                }
            }
            WordStatus.POOL -> {
                if (current.status != WordStatus.POOL) {
                    wordsState[idx] = current.copy(status = WordStatus.POOL, selectedOrder = null)
                }
            }
        }
        result = null
    }

    fun clearAll() {
        for (i in wordsState.indices) {
            val w = wordsState[i]
            if (w.status != WordStatus.POOL || w.selectedOrder != null) {
                wordsState[i] = w.copy(status = WordStatus.POOL, selectedOrder = null)
            }
        }
        wordsState.shuffle()
        nextOrder = 0
        result = null
    }

    fun check() {
        val selectedWords = wordsState
            .filter { it.status == WordStatus.SELECTED }
            .sortedBy { it.selectedOrder ?: Int.MAX_VALUE }
            .map { it.text }
        val ok = selectedWords == correctOrder
        result = ok
        onResult(ok)
    }

    val uiState = SentenceUIState(
        words = wordsState,
        correctOrder = correctOrder,
        isCorrect = result
    )

    SentenceArrangeContent(
        modifier = modifier.fillMaxSize(),
        state = uiState,
        onPoolClick = { wordId -> onWordClick(wordId, WordStatus.SELECTED) },
        onSelectedClick = { wordId -> onWordClick(wordId, WordStatus.POOL) },
        onClear = ::clearAll,
        onCheck = ::check,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SentenceArrangeContent(
    modifier: Modifier,
    state: SentenceUIState,
    onPoolClick: (Int) -> Unit,
    onSelectedClick: (Int) -> Unit,
    onClear: () -> Unit,
    onCheck: () -> Unit,
    onBack: () -> Unit
) {
    val chipShape = RoundedCornerShape(18.dp)

    // Área selecionada de acordo com o que foi clickado; Na pool fica o restante
    val selectedWords = state.words
        .filter { it.status == WordStatus.SELECTED }
        .sortedBy { it.selectedOrder ?: Int.MAX_VALUE }
    val poolWords = state.words.filter { it.status == WordStatus.POOL }

    Box(modifier = modifier.background(Olive)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("") }, // no title
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack, // ← arrow back
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
            bottomBar = {
                BottomAppBar(
                    containerColor = OliveDark,
                    contentColor = Color.White,
                    actions = { TextButton(onClick = onClear) { Text("Limpar", color = Color.White) } },
                    floatingActionButton = {
                        val canCheck = selectedWords.isNotEmpty()
                        ExtendedFloatingActionButton(
                            onClick = { if (canCheck) onCheck() },
                            containerColor = ChipLime,
                            contentColor = ChipLimeText,
                            modifier = Modifier.alpha(if (canCheck) 1f else 0.45f)
                        ) { Text("Verificar") }
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

                // Selected sentence area (no guideline)
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
                    ) {
                        if (selectedWords.isEmpty()) {
                            AssistiveHint()
                        } else {
                            selectedWords.forEach { word ->
                                key(word.id) {
                                    WordChip(
                                        text = word.text,
                                        onClick = { onSelectedClick(word.id) },
                                        shape = chipShape,
                                        container = ChipMint,
                                        content = ChipMintText
                                    )
                                }
                            }
                        }
                    }

                    // Result message (Correct/Incorrect)
                    AnimatedVisibility(
                        visible = state.isCorrect != null,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        ResultMessageBox(isCorrect = state.isCorrect == true)
                    }
                }

                Divider(color = Color.White.copy(alpha = .2f))

                Text(
                    text = "Toque para adicionar:",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )

                // Word pool area
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(220, easing = FastOutSlowInEasing))
                ) {
                    poolWords.forEach { word ->
                        key(word.id) {
                            WordChip(
                                text = word.text,
                                onClick = { onPoolClick(word.id) },
                                shape = chipShape,
                                container = ChipLime,
                                content = ChipLimeText
                            )
                        }
                    }
                }
                Spacer(Modifier.height(64.dp))
            }
        }
    }
}

/* Os componentes */

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

@Composable
private fun WordChip(
    text: String,
    onClick: () -> Unit,
    shape: RoundedCornerShape,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier
) {
    var pressed by remember(text) { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(120), label = "chipScale")
    val alpha by animateFloatAsState(if (pressed) 0.85f else 1f, tween(120), label = "chipAlpha")

    Surface(
        color = container,
        contentColor = content,
        shape = shape,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
        modifier = modifier
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

/** Tokenizador das palavras */
private fun tokenize(sentence: String): List<String> {
    val regex = Regex("""\p{L}+|\d+|[^\s\p{L}\d]""")
    return regex.findAll(sentence).map { it.value }.toList()
}

@Preview(showBackground = true, widthDp = 360, heightDp = 700)
@Composable
private fun SentenceArrangePreview() {
    val demo = "O rato roeu a roupa do rei de Roma"
    MaterialTheme {
        SentenceArrangeScreen(phrase = demo, onBack = {})
    }
}
