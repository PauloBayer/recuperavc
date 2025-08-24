package com.whispercppdemo.ui.main

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
import com.whispercppdemo.media.decodeWaveFile
import com.whispercppdemo.recorder.Recorder
import com.whispercpp.whisper.WhisperContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

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
    var transcriptionResult by mutableStateOf("")
        private set
    var analysisResult by mutableStateOf<AnalysisResult?>(null)
        private set

    private val modelsPath = File(application.filesDir, "models")
    private val samplesPath = File(application.filesDir, "samples")
    private var recorder: Recorder = Recorder()
    private var whisperContext: com.whispercpp.whisper.WhisperContext? = null
    private var mediaPlayer: MediaPlayer? = null
    private var recordedFile: File? = null
    private var recordingStartTime: Long = 0
    
    private val expectedPhrase = "o rato roeu a roupa do rei de roma"
    
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
            canTranscribe = true
            isLoading = false
        } catch (e: Exception) {
            Log.w(LOG_TAG, e)
            isLoading = false
        }
    }

    private suspend fun copyAssets() = withContext(Dispatchers.IO) {
        modelsPath.mkdirs()
        samplesPath.mkdirs()
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
                
                withContext(Dispatchers.Main) {
                    transcriptionResult = cleanText
                    if (cleanText.isNotEmpty()) {
                        analysisResult = calculateAnalysis(cleanText, recordingDurationMs)
                    }
                    isProcessing = false
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

        canTranscribe = true
    }

    fun clearResults() {
        transcriptionResult = ""
        analysisResult = null
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
                recorder.startRecording(file) { e ->
                    viewModelScope.launch {
                        withContext(Dispatchers.Main) {
                            isRecording = false
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
        File.createTempFile("recording", "wav")
    }
    
    private fun boostProcessPriority() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            Log.d(LOG_TAG, "Boosted process priority for faster Whisper processing")
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to boost process priority", e)
        }
    }
    
    private fun restoreProcessPriority() {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
            Log.d(LOG_TAG, "Restored normal process priority")
        } catch (e: Exception) {
            Log.w(LOG_TAG, "Failed to restore process priority", e)
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
        val expectedWords = expectedPhrase.lowercase().split("\\s+".toRegex())
        val transcribedWords = transcribedText.split("\\s+".toRegex()).filter { it.isNotBlank() }
        
        
        val recordingDurationMinutes = recordingDurationMs / 60000.0
        val wpm = if (recordingDurationMinutes > 0) {
            (transcribedWords.size / recordingDurationMinutes).toInt()
        } else {
            0
        }
        
        val wer = calculateWER(expectedWords, transcribedWords)
        
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