package com.whispercpp.whisper

import android.content.res.AssetManager
import android.os.Build
import android.os.Process
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executors

private const val LOG_TAG = "LibWhisper"

class WhisperContext private constructor(private var ptr: Long) {
    private val scope: CoroutineScope = CoroutineScope(
        Executors.newFixedThreadPool(WhisperCpuConfig.preferredThreadCount).asCoroutineDispatcher()
    )

    suspend fun transcribeData(data: FloatArray, printTimestamp: Boolean = true): String = withContext(scope.coroutineContext) {
        require(ptr != 0L)
        
        if (data.size > 240000) {
            return@withContext transcribeDataChunked(data, printTimestamp)
        }
        
        return@withContext transcribeDataSingle(data, printTimestamp)
    }
    
    private suspend fun transcribeDataSingle(data: FloatArray, printTimestamp: Boolean): String {
        require(ptr != 0L)
        
        val startTime = System.currentTimeMillis()
        
        WhisperCpuConfig.setCpuGovernorToPerformance()
        
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            Log.d(LOG_TAG, "Set thread priority to URGENT_AUDIO")
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Could not set thread priority: ${e.message}")
        }
        
        val numThreads = WhisperCpuConfig.preferredThreadCount
        Log.d(LOG_TAG, "MAXIMUM PERFORMANCE: Using $numThreads threads")
        
        val memoryBefore = Runtime.getRuntime().freeMemory()
        System.gc()
        Runtime.getRuntime().gc()
        Thread.sleep(10)
        val memoryAfter = Runtime.getRuntime().freeMemory()
        Log.d(LOG_TAG, "Memory freed: ${(memoryAfter - memoryBefore) / 1024 / 1024}MB")
        
        Log.d(LOG_TAG, "SYSTEM INFO: ${getOptimizedSystemInfo()}")
        Log.d(LOG_TAG, "AUDIO DATA: ${data.size} samples (${data.size / 16000.0f} seconds of audio)")
        
        val transcribeStart = System.currentTimeMillis()
        Log.d(LOG_TAG, "STARTING TRANSCRIPTION with $numThreads threads")
        
        WhisperLib.fullTranscribe(ptr, numThreads, data)
        
        val transcribeTime = System.currentTimeMillis() - transcribeStart
        val processingTime = System.currentTimeMillis() - startTime
        Log.d(LOG_TAG, "PERFORMANCE BREAKDOWN:")
        Log.d(LOG_TAG, "  - Setup time: ${transcribeStart - startTime}ms")
        Log.d(LOG_TAG, "  - Transcription time: ${transcribeTime}ms")
        Log.d(LOG_TAG, "  - Total time: ${processingTime}ms")
        Log.d(LOG_TAG, "  - Audio length: ${data.size} samples")
        Log.d(LOG_TAG, "  - Performance: ${data.size.toFloat() / transcribeTime}ms samples/ms")
        val textCount = WhisperLib.getTextSegmentCount(ptr)
        val result = buildString {
            for (i in 0 until textCount) {
                if (printTimestamp) {
                    val textTimestamp = "[${toTimestamp(WhisperLib.getTextSegmentT0(ptr, i))} --> ${toTimestamp(WhisperLib.getTextSegmentT1(ptr, i))}]"
                    val textSegment = WhisperLib.getTextSegment(ptr, i)
                    append("$textTimestamp: $textSegment\n")
                } else {
                    append(WhisperLib.getTextSegment(ptr, i))
                }
            }
        }
        
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_DEFAULT)
        } catch (e: Exception) {
        }
        
        val totalTime = System.currentTimeMillis() - startTime
        Log.d(LOG_TAG, "PERFORMANCE: Total transcription time: ${totalTime}ms")
        
        return result
    }
    
    private suspend fun transcribeDataChunked(data: FloatArray, printTimestamp: Boolean): String {
        require(ptr != 0L)
        
        val chunkSize = 160000
        val overlap = 16000
        val chunks = mutableListOf<String>()
        
        Log.d(LOG_TAG, "CHUNKED PROCESSING: ${data.size} samples in chunks of $chunkSize")
        
        var start = 0
        while (start < data.size) {
            val end = minOf(start + chunkSize, data.size)
            val chunk = data.sliceArray(start until end)
            
            Log.d(LOG_TAG, "Processing chunk: $start-$end (${chunk.size} samples)")
            val chunkResult = transcribeDataSingle(chunk, false)
            
            if (chunkResult.isNotBlank()) {
                chunks.add(chunkResult.trim())
            }
            
            start += chunkSize - overlap
        }
        
        Log.d(LOG_TAG, "CHUNKED PROCESSING COMPLETE: ${chunks.size} chunks processed")
        return chunks.joinToString(" ")
    }

    suspend fun benchMemory(nthreads: Int): String = withContext(scope.coroutineContext) {
        return@withContext WhisperLib.benchMemcpy(nthreads)
    }

    suspend fun benchGgmlMulMat(nthreads: Int): String = withContext(scope.coroutineContext) {
        return@withContext WhisperLib.benchGgmlMulMat(nthreads)
    }

    suspend fun release() = withContext(scope.coroutineContext) {
        if (ptr != 0L) {
            WhisperLib.freeContext(ptr)
            ptr = 0
        }
    }

    protected fun finalize() {
        runBlocking {
            release()
        }
    }

    companion object {
        fun createContextFromFile(filePath: String): WhisperContext {
            Log.d(LOG_TAG, "CREATING OPTIMIZED CONTEXT from: $filePath")
            val startTime = System.currentTimeMillis()
            
            val ptr = WhisperLib.initContext(filePath)
            if (ptr == 0L) {
                throw java.lang.RuntimeException("Couldn't create context with path $filePath")
            }
            
            val loadTime = System.currentTimeMillis() - startTime
            Log.d(LOG_TAG, "MODEL LOADED in ${loadTime}ms with OPTIMIZED settings")
            Log.d(LOG_TAG, "System info: ${getSystemInfo()}")
            
            return WhisperContext(ptr)
        }

        fun createContextFromInputStream(stream: InputStream): WhisperContext {
            val ptr = WhisperLib.initContextFromInputStream(stream)

            if (ptr == 0L) {
                throw java.lang.RuntimeException("Couldn't create context from input stream")
            }
            return WhisperContext(ptr)
        }

        fun createContextFromAsset(assetManager: AssetManager, assetPath: String): WhisperContext {
            val ptr = WhisperLib.initContextFromAsset(assetManager, assetPath)

            if (ptr == 0L) {
                throw java.lang.RuntimeException("Couldn't create context from asset $assetPath")
            }
            return WhisperContext(ptr)
        }

        fun getSystemInfo(): String {
            return WhisperLib.getSystemInfo()
        }
        
        fun getOptimizedSystemInfo(): String {
            val systemInfo = WhisperLib.getSystemInfo()
            val cpuInfo = WhisperCpuConfig.preferredThreadCount
            val totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024 // MB
            val freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024 // MB
            val maxMemory = Runtime.getRuntime().maxMemory() / 1024 / 1024 // MB
            
            return """WHISPER OPTIMIZATION INFO:
                |Threads: $cpuInfo
                |Total Memory: ${totalMemory}MB
                |Free Memory: ${freeMemory}MB
                |Max Memory: ${maxMemory}MB
                |CPU ABI: ${Build.SUPPORTED_ABIS[0]}
                |Android Version: ${Build.VERSION.RELEASE}
                |Device: ${Build.MANUFACTURER} ${Build.MODEL}
                |
                |SYSTEM INFO:
                |$systemInfo
            """.trimMargin()
        }
    }
}

private class WhisperLib {
    companion object {
        init {
            Log.d(LOG_TAG, "Primary ABI: ${Build.SUPPORTED_ABIS[0]}")
            var loadVfpv4 = false
            var loadV8fp16 = false
            if (isArmEabiV7a()) {
                // armeabi-v7a needs runtime detection support
                val cpuInfo = cpuInfo()
                cpuInfo?.let {
                    Log.d(LOG_TAG, "CPU info: $cpuInfo")
                    if (cpuInfo.contains("vfpv4")) {
                        Log.d(LOG_TAG, "CPU supports vfpv4")
                        loadVfpv4 = true
                    }
                }
            } else if (isArmEabiV8a()) {
                // ARMv8.2a needs runtime detection support
                val cpuInfo = cpuInfo()
                cpuInfo?.let {
                    Log.d(LOG_TAG, "CPU info: $cpuInfo")
                    if (cpuInfo.contains("fphp")) {
                        Log.d(LOG_TAG, "CPU supports fp16 arithmetic")
                        loadV8fp16 = true
                    }
                }
            }

            if (loadVfpv4 && tryLoad("whisper_vfpv4")) {
                Log.d(LOG_TAG, "Loading libwhisper_vfpv4.so")
                System.loadLibrary("whisper_vfpv4")
            } else if (loadV8fp16 && tryLoad("whisper_v8fp16_va")) {
                Log.d(LOG_TAG, "Loading libwhisper_v8fp16_va.so")
                System.loadLibrary("whisper_v8fp16_va")
            } else if (tryLoad("whisper_v8fp16")) {
                Log.d(LOG_TAG, "Loading libwhisper_v8fp16_va.so")
                System.loadLibrary("whisper_v8fp16_va")
            } else {
                Log.d(LOG_TAG, "Loading libwhisper.so")
                System.loadLibrary("whisper")
            }
        }

        external fun initContextFromInputStream(inputStream: InputStream): Long
        external fun initContextFromAsset(assetManager: AssetManager, assetPath: String): Long
        external fun initContext(modelPath: String): Long
        external fun freeContext(contextPtr: Long)
        external fun fullTranscribe(contextPtr: Long, numThreads: Int, audioData: FloatArray)
        external fun getTextSegmentCount(contextPtr: Long): Int
        external fun getTextSegment(contextPtr: Long, index: Int): String
        external fun getTextSegmentT0(contextPtr: Long, index: Int): Long
        external fun getTextSegmentT1(contextPtr: Long, index: Int): Long
        external fun getSystemInfo(): String
        external fun benchMemcpy(nthread: Int): String
        external fun benchGgmlMulMat(nthread: Int): String
    }
}

private fun tryLoad(name: String): Boolean =
    try { System.loadLibrary(name); true } catch (e: UnsatisfiedLinkError) {
        android.util.Log.w("LibWhisper", "Missing $name, falling back", e); false
    }

private fun toTimestamp(t: Long, comma: Boolean = false): String {
    var msec = t * 10
    val hr = msec / (1000 * 60 * 60)
    msec -= hr * (1000 * 60 * 60)
    val min = msec / (1000 * 60)
    msec -= min * (1000 * 60)
    val sec = msec / 1000
    msec -= sec * 1000

    val delimiter = if (comma) "," else "."
    return String.format("%02d:%02d:%02d%s%03d", hr, min, sec, delimiter, msec)
}

private fun isArmEabiV7a(): Boolean {
    return Build.SUPPORTED_ABIS[0].equals("armeabi-v7a")
}

private fun isArmEabiV8a(): Boolean {
    return Build.SUPPORTED_ABIS[0].equals("arm64-v8a")
}

private fun cpuInfo(): String? {
    return try {
        File("/proc/cpuinfo").inputStream().bufferedReader().use {
            it.readText()
        }
    } catch (e: Exception) {
        Log.w(LOG_TAG, "Couldn't read /proc/cpuinfo", e)
        null
    }
}