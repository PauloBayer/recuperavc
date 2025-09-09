package com.recuperavc.recorder

import kotlin.math.abs
import kotlin.math.sqrt

class SilenceDetector(
    private val silenceThreshold: Double = 0.02,
    private val silenceDurationMs: Long = 2000,
    private val minRecordingDurationMs: Long = 800
) {
    private var silenceStartTime: Long = 0
    private var isInSilence: Boolean = false
    private var recordingStartTime: Long = 0
    private var hasDetectedSound: Boolean = false
    
    fun startRecording() {
        recordingStartTime = System.currentTimeMillis()
        isInSilence = false
        silenceStartTime = 0
        hasDetectedSound = false
    }
    
    fun processSamples(samples: ShortArray): SilenceDetectionResult {
        val currentTime = System.currentTimeMillis()
        val rms = calculateRMS(samples)
        val isSilent = rms < silenceThreshold
        
        if (!isSilent) {
            hasDetectedSound = true
        }
        
        when {
            isSilent && !isInSilence -> {
                isInSilence = true
                silenceStartTime = currentTime
            }
            !isSilent && isInSilence -> {
                isInSilence = false
                silenceStartTime = 0
            }
            isSilent && isInSilence -> {
                val silenceDuration = currentTime - silenceStartTime
                val recordingDuration = currentTime - recordingStartTime
                
                if (silenceDuration >= silenceDurationMs && recordingDuration >= minRecordingDurationMs) {
                    return if (hasDetectedSound) {
                        SilenceDetectionResult.STOP_AND_PROCESS
                    } else {
                        SilenceDetectionResult.STOP_NO_AUDIO
                    }
                }
            }
        }
        
        return SilenceDetectionResult.CONTINUE
    }
    
    private fun calculateRMS(samples: ShortArray): Double {
        if (samples.isEmpty()) return 0.0
        
        val sum = samples.map { (it.toDouble() / Short.MAX_VALUE) * (it.toDouble() / Short.MAX_VALUE) }.sum()
        return sqrt(sum / samples.size)
    }
}

enum class SilenceDetectionResult {
    CONTINUE,
    STOP_AND_PROCESS,
    STOP_NO_AUDIO
}