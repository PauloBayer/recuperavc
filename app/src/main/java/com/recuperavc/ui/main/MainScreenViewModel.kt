package com.recuperavc.ui.main

import android.app.Application
import android.content.Context
import android.media.MediaPlayer
import android.os.Process
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.recuperavc.media.decodeWaveFile
import com.recuperavc.recorder.Recorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import com.recuperavc.library.PhraseManager
import com.recuperavc.data.db.DbProvider
import com.recuperavc.models.AudioFile
import com.recuperavc.models.AudioReport
import com.recuperavc.models.AudioReportGroup
import com.recuperavc.models.CoherenceReport
import com.recuperavc.models.CoherenceReportGroup
import com.recuperavc.models.Phrase
import com.recuperavc.models.enums.PhraseType
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.delay

private const val LOG_TAG = "MainScreenViewModel"

data class AnalysisResult(
    val wpm: Int,
    val wer: Double
)

class MainScreenViewModel(private val application: Application) : ViewModel() {
    var canTranscribe by mutableStateOf(false)
        private set
    var isRecording by mutableStateOf(false)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isProcessing by mutableStateOf(false)
        private set
    var isCancelling by mutableStateOf(false)
        private set
    var transcriptionResult by mutableStateOf("")
        private set
    var analysisResult by mutableStateOf<AnalysisResult?>(null)
        private set

    var phraseText by mutableStateOf("")
        private set

    private var currentPhrase: Phrase? = null

    var sessionCount by mutableStateOf(0)
        private set
    private val minAudiosRequired = 3
    private data class SessionItem(
        val audioId: UUID,
        val phraseId: UUID?,
        val phraseText: String,
        val wpm: Int,
        val wer: Double
    )
    private val sessionItems = mutableListOf<SessionItem>()

    data class SessionSummaryItem(val phrase: String, val wpm: Int, val wer: Double)
    data class SessionSummary(val avgWpm: Float, val avgWer: Float, val items: List<SessionSummaryItem>)
    var sessionSummary by mutableStateOf<SessionSummary?>(null)
        private set

    private val modelsPath = File(application.filesDir, "models")
    private val samplesPath = File(application.filesDir, "samples")
    private val recordingsPath = File(application.filesDir, "recordings")
    private var recorder: Recorder = Recorder()
    private var whisperContext: com.whispercpp.whisper.WhisperContext? = null
    private var mediaPlayer: MediaPlayer? = null
    private var recordedFile: File? = null
    private var recordingStartTime: Long = 0
    
    private val phraseManager = PhraseManager(application)
    
    private val highPerformanceDispatcher = Dispatchers.Default.limitedParallelism(
        Runtime.getRuntime().availableProcessors().coerceAtLeast(4)
    )

    init {
        viewModelScope.launch {
            loadData()
        }
    }

    private suspend fun loadData() {
        try {
            copyAssets()
            loadBaseModel()
            loadNewPhrase()
            canTranscribe = true
            isLoading = false
        } catch (e: Exception) {
            Log.w(LOG_TAG, e)
            isLoading = false
        }
    }
    fun loadNewPhrase() {
        viewModelScope.launch {
            val nextType = when (sessionCount % 3) {
                0 -> PhraseType.SHORT
                1 -> PhraseType.MEDIUM
                else -> PhraseType.BIG
            }
            val phrase = phraseManager.getNextPhrase(nextType)
            currentPhrase = phrase
            phraseText = phrase.description
        }
    }


    private suspend fun copyAssets() = withContext(Dispatchers.IO) {
        modelsPath.mkdirs()
        samplesPath.mkdirs()
        recordingsPath.mkdirs()
        application.copyData("samples", samplesPath)
    }

    private suspend fun loadBaseModel() = withContext(Dispatchers.IO) {
        val models = application.assets.list("models/")
        if (models != null) {
            whisperContext = com.whispercpp.whisper.WhisperContext.createContextFromAsset(application.assets, "models/" + models[0])
        }
    }


    private suspend fun readAudioSamples(file: File): FloatArray = withContext(Dispatchers.IO) {
        stopPlayback()
        startPlayback(file)
        return@withContext decodeWaveFile(file)
    }

    private suspend fun stopPlayback() = withContext(Dispatchers.Main) {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private suspend fun startPlayback(file: File) = withContext(Dispatchers.Main) {
        mediaPlayer = MediaPlayer.create(application, file.absolutePath.toUri())
        mediaPlayer?.start()
    }

    private suspend fun transcribeAudio(file: File, recordingDurationMs: Long) {
        if (!canTranscribe) {
            return
        }

        canTranscribe = false
        isProcessing = true

        try {
            withContext(highPerformanceDispatcher) {
                boostProcessPriority()
                
                val data = readAudioSamples(file)
                val rawText = whisperContext?.transcribeData(data)?.trim() ?: ""
                
                val cleanText = extractCleanText(rawText).lowercase()
                
                val analysis = if (cleanText.isNotEmpty()) calculateAnalysis(cleanText, recordingDurationMs) else null
                withContext(Dispatchers.Main) {
                    transcriptionResult = cleanText
                    analysisResult = analysis
                    isProcessing = false
                }
                if (analysis != null) {
                    persistResults(file, recordingDurationMs, cleanText, analysis)
                }
                
                restoreProcessPriority()
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, e)
            withContext(Dispatchers.Main) {
                isProcessing = false
            }
            restoreProcessPriority()
        }

        viewModelScope.launch {
            delay(800)
            clearResults()
            loadNewPhrase()
        }
        canTranscribe = true
    }

    private suspend fun persistResults(
        file: File,
        recordingDurationMs: Long,
        transcribedText: String,
        analysis: AnalysisResult
    ) {
        withContext(Dispatchers.IO) {
            val db = DbProvider.db(application)

            val phraseDao = db.phraseDao()
            val ensuredPhrase: Phrase = currentPhrase
                ?: phraseDao.getAll().firstOrNull { it.description == phraseText }
                ?: run {
                    val inferredType = when (sessionCount % 3) {
                        0 -> PhraseType.SHORT
                        1 -> PhraseType.MEDIUM
                        else -> PhraseType.BIG
                    }
                    val created = Phrase(description = phraseText, type = inferredType)
                    phraseDao.upsert(created)
                    created
                }
            val phraseId: UUID = ensuredPhrase.id

            // 1) AudioFile
            val audioId = UUID.randomUUID()
            val audio = AudioFile(
                id = audioId,
                path = file.absolutePath,
                fileType = "wav",
                fileName = file.name,
                audioDuration = recordingDurationMs.toInt(),
                recordedAt = Instant.now(),
                phraseId = phraseId
            )
            db.audioFileDao().upsert(audio)

            // Track this attempt in-memory for the session
            sessionItems.add(
                SessionItem(
                    audioId = audioId,
                    phraseId = phraseId,
                    phraseText = phraseText,
                    wpm = analysis.wpm,
                    wer = analysis.wer
                )
            )

            // 2) AudioReport
            val reportId = UUID.randomUUID()
            val report = AudioReport(
                id = reportId,
                averageWordsPerMinute = analysis.wpm.toFloat(),
                averageWordErrorRate = analysis.wer.toFloat(),
                allTestsDescription = "wpm=${analysis.wpm};wer=${String.format("%.1f", analysis.wer)};text=$transcribedText",
                mainAudioFileId = audioId
            )
            db.audioReportDao().upsert(report)
            db.audioReportDao().link(
                AudioReportGroup(
                    idAudioReport = reportId,
                    idAudioFile = audioId
                )
            )

            // 3) CoherenceReport
            sessionCount = sessionItems.size
            val coherenceId = UUID.randomUUID()
            val coherence = CoherenceReport(
                id = coherenceId,
                averageErrorsPerTry = analysis.wer.toFloat(),
                averageTimePerTry = recordingDurationMs / 1000.0f,
                allTestsDescription = "score=${String.format("%.1f", 100 - analysis.wer)};expected=$phraseText;transcribed=$transcribedText",
                phraseId = phraseId
            )
            db.coherenceReportDao().upsert(coherence)
            db.coherenceReportDao().link(
                CoherenceReportGroup(
                    idPhrase = phraseId,
                    idCoherenceReport = coherenceId
                )
            )
        }
    }

    fun clearResults() {
        transcriptionResult = ""
        analysisResult = null
    }

    fun finishSession(onFinished: (saved: Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            val saved = if (sessionItems.size >= minAudiosRequired) {
                val snapshot = sessionItems.toList()
                val avgWpm = snapshot.map { it.wpm }.average().toFloat()
                val avgWer = snapshot.map { it.wer }.average().toFloat()
                val mainFileId = sessionItems.firstOrNull()?.audioId
                val desc = org.json.JSONObject().apply {
                    put("count", snapshot.size)
                    put("avgWpm", avgWpm)
                    put("avgWer", avgWer)
                    val arr = org.json.JSONArray()
                    snapshot.forEach { it ->
                        arr.put(
                            org.json.JSONObject().apply {
                                put("fileId", it.audioId.toString())
                                put("phrase", it.phraseText)
                                put("wpm", it.wpm)
                                put("wer", it.wer)
                            }
                        )
                    }
                    put("attempts", arr)
                }.toString()
                val report = AudioReport(
                    id = UUID.randomUUID(),
                    averageWordsPerMinute = avgWpm,
                    averageWordErrorRate = avgWer,
                    allTestsDescription = desc,
                    mainAudioFileId = mainFileId
                )
                val db = DbProvider.db(application)
                db.audioReportDao().insertWithFiles(report, snapshot.map { it.audioId })
                val items = snapshot.map { SessionSummaryItem(it.phraseText, it.wpm, it.wer) }
                sessionSummary = SessionSummary(avgWpm, avgWer, items)
                true
            } else {
                false
            }
            sessionItems.clear()
            sessionCount = 0
            onFinished(saved)
        }
    }

    fun discardSession() {
        sessionItems.clear()
        sessionCount = 0
    }

    fun dismissSummary() {
        sessionSummary = null
        clearResults()
        loadNewPhrase()
    }

    fun cancelRecording() {
        viewModelScope.launch {
            isCancelling = true
            recorder.stopRecording()
            isRecording = false
            isCancelling = false
        }
    }

    fun toggleRecord() = viewModelScope.launch {
        try {
            if (isRecording) {
                recorder.stopRecording()
                isRecording = false
                val recordingDuration = System.currentTimeMillis() - recordingStartTime
                recordedFile?.let { transcribeAudio(it, recordingDuration) }
            } else {
                stopPlayback()
                transcriptionResult = ""
                analysisResult = null
                val file = getTempFileForRecording()
                recorder.startRecording(file, { e ->
                    viewModelScope.launch {
                        withContext(Dispatchers.Main) {
                            isRecording = false
                        }
                    }
                }) { shouldProcess ->
                    viewModelScope.launch {
                        withContext(Dispatchers.Main) {
                            recorder.stopRecording()
                            isRecording = false
                            if (shouldProcess) {
                                val recordingDuration = System.currentTimeMillis() - recordingStartTime
                                recordedFile?.let { transcribeAudio(it, recordingDuration) }
                            } else {
                                Log.d(LOG_TAG, "Gravação cancelada - nenhum áudio detectado")
                            }
                        }
                    }
                }
                isRecording = true
                recordingStartTime = System.currentTimeMillis()
                recordedFile = file
            }
        } catch (e: Exception) {
            Log.w(LOG_TAG, e)
            isRecording = false
        }
    }

    private suspend fun getTempFileForRecording() = withContext(Dispatchers.IO) {
        recordingsPath.mkdirs()
        val raw = phraseText.lowercase()
        val safe = raw.replace("[^a-z0-9]+".toRegex(), "_").trim('_')
        val short = if (safe.length > 16) safe.substring(0, 16) else safe
        val fmt = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
        val ts = java.time.LocalDateTime.now().format(fmt)
        File(recordingsPath, "rec_${ts}_${short}.wav")
    }
    
    private fun boostProcessPriority() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        } catch (e: Exception) {
        }
    }
    
    private fun restoreProcessPriority() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
        } catch (e: Exception) {
        }
    }
    
    private fun extractCleanText(whisperOutput: String): String {
        var cleanText = whisperOutput.replace("\\[.*?\\]".toRegex(), "")
        
        cleanText = cleanText.replace(":", "").trim()
        
        cleanText = cleanText.replace("\\s+".toRegex(), " ")
            .replace(".", "")
            .replace(",", "")
            .replace("!", "")
            .replace("?", "")
            .trim()
        
        
        return cleanText
    }

    private fun calculateAnalysis(transcribedText: String, recordingDurationMs: Long): AnalysisResult {
        val cleanExpectedText = phraseText.lowercase()
            .replace(Regex("[.,!?;:\"'()\\[\\]{}]"), "")
            .replace("\\s+".toRegex(), " ")
            .trim()
        val expectedWords = cleanExpectedText.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val transcribedWords = transcribedText.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        
        Log.d(LOG_TAG, "Análise: $expectedWords vs $transcribedWords")

        val recordingDurationMinutes = recordingDurationMs / 60000.0
        val wpm = if (recordingDurationMinutes > 0) {
            (transcribedWords.size / recordingDurationMinutes).toInt()
        } else {
            0
        }

        val wer = calculateWER(expectedWords, transcribedWords)
        
        Log.d(LOG_TAG, "Precisão: ${String.format("%.1f", 100 - wer)}%")

        return AnalysisResult(wpm, wer)
    }
    
    private fun calculateWER(expected: List<String>, transcribed: List<String>): Double {
        if (expected.isEmpty()) return 0.0
        
        val dp = Array(expected.size + 1) { IntArray(transcribed.size + 1) }
        
        for (i in 0..expected.size) dp[i][0] = i
        for (j in 0..transcribed.size) dp[0][j] = j
        
        for (i in 1..expected.size) {
            for (j in 1..transcribed.size) {
                dp[i][j] = if (expected[i - 1].equals(transcribed[j - 1], ignoreCase = true)) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(
                        dp[i - 1][j],
                        dp[i][j - 1],
                        dp[i - 1][j - 1]
                    )
                }
            }
        }
        
        val editDistance = dp[expected.size][transcribed.size]
        return (editDistance.toDouble() / expected.size) * 100
    }

    override fun onCleared() {
        runBlocking {
            whisperContext?.release()
            whisperContext = null
            stopPlayback()
        }
    }

    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                MainScreenViewModel(application)
            }
        }
    }
}

private suspend fun Context.copyData(
    assetDirName: String,
    destDir: File
) = withContext(Dispatchers.IO) {
    assets.list(assetDirName)?.forEach { name ->
        val assetPath = "$assetDirName/$name"
        Log.v(LOG_TAG, "Processing $assetPath...")
        val destination = File(destDir, name)
        Log.v(LOG_TAG, "Copying $assetPath to $destination...")
        assets.open(assetPath).use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        Log.v(LOG_TAG, "Copied $assetPath to $destination")
    }
}
