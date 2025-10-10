package com.recuperavc.recorder

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.recuperavc.media.encodeWaveFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class Recorder {
    private val scope: CoroutineScope = CoroutineScope(
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    )
    private var recorder: AudioRecordThread? = null

    suspend fun startRecording(outputFile: File, onError: (Exception) -> Unit, onAutoStop: ((Boolean) -> Unit)? = null) = withContext(scope.coroutineContext) {
        recorder = AudioRecordThread(outputFile, onError, onAutoStop)
        recorder?.start()
    }

    suspend fun stopRecording() = withContext(scope.coroutineContext) {
        recorder?.stopRecording()
        @Suppress("BlockingMethodInNonBlockingContext")
        recorder?.join()
        recorder = null
    }
}

private class AudioRecordThread(
    private val outputFile: File,
    private val onError: (Exception) -> Unit,
    private val onAutoStop: ((Boolean) -> Unit)? = null
) :
    Thread("AudioRecorder") {
    private var quit = AtomicBoolean(false)
    private val silenceDetector = SilenceDetector()

    @SuppressLint("MissingPermission")
    override fun run() {
        try {
            val bufferSize = AudioRecord.getMinBufferSize(
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ) * 4
            val buffer = ShortArray(bufferSize / 2)

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            try {
                audioRecord.startRecording()
                silenceDetector.startRecording()

                val allData = mutableListOf<Short>()

                while (!quit.get()) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        val currentBuffer = buffer.sliceArray(0 until read)
                        for (i in 0 until read) {
                            allData.add(buffer[i])
                        }
                        
                        if (onAutoStop != null) {
                            when (silenceDetector.processSamples(currentBuffer)) {
                                SilenceDetectionResult.STOP_AND_PROCESS -> {
                                    quit.set(true)
                                    onAutoStop.invoke(true)
                                }
                                SilenceDetectionResult.STOP_NO_AUDIO -> {
                                    quit.set(true)
                                    onAutoStop.invoke(false)
                                }
                                SilenceDetectionResult.CONTINUE -> {
                                }
                            }
                        }
                    } else {
                        throw java.lang.RuntimeException("audioRecord.read returned $read")
                    }
                }

                audioRecord.stop()
                encodeWaveFile(outputFile, allData.toShortArray())
            } finally {
                audioRecord.release()
            }
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun stopRecording() {
        quit.set(true)
    }
}