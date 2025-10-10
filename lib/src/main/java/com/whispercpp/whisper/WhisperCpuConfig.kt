package com.whispercpp.whisper

import android.util.Log
import java.io.BufferedReader
import java.io.FileReader

object WhisperCpuConfig {
    val preferredThreadCount: Int
        get() = getOptimalThreadCount()
    
    fun setCpuGovernorToPerformance() {
        try {
            val cpuCount = Runtime.getRuntime().availableProcessors()
            for (i in 0 until cpuCount) {
                try {
                    val governorFile = java.io.File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_governor")
                    if (governorFile.exists() && governorFile.canWrite()) {
                        governorFile.writeText("performance")
                        Log.d("WhisperCpuConfig", "Set CPU $i governor to performance")
                    }
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            Log.d("WhisperCpuConfig", "Could not set CPU governor to performance: ${e.message}")
        }
    }
    
    private fun getOptimalThreadCount(): Int {
        val totalCores = Runtime.getRuntime().availableProcessors()
        val highPerfCores = CpuInfo.getHighPerfCpuCount()
        
        return when {
            totalCores >= 8 -> (totalCores - 1).coerceAtLeast(6)
            
            totalCores >= 4 -> totalCores
            
            else -> totalCores.coerceAtLeast(2)
        }
    }
}

private class CpuInfo(private val lines: List<String>) {
    private fun getHighPerfCpuCount(): Int = try {
        getHighPerfCpuCountByFrequencies()
    } catch (e: Exception) {
        Log.d(LOG_TAG, "Couldn't read CPU frequencies", e)
        getHighPerfCpuCountByVariant()
    }

    private fun getHighPerfCpuCountByFrequencies(): Int =
        getCpuValues(property = "processor") { getMaxCpuFrequency(it.toInt()) }
            .countDroppingMin()

    private fun getHighPerfCpuCountByVariant(): Int =
        getCpuValues(property = "CPU variant") { it.substringAfter("0x").toInt(radix = 16) }
            .also { Log.d(LOG_TAG, "Binned cpu variants (variant, count): ${it.binnedValues()}") }
            .countKeepingMin()

    private fun List<Int>.binnedValues() = groupingBy { it }.eachCount()

    private fun getCpuValues(property: String, mapper: (String) -> Int) = lines
        .asSequence()
        .filter { it.startsWith(property) }
        .map { mapper(it.substringAfter(':').trim()) }
        .sorted()
        .toList()


    private fun List<Int>.countDroppingMin(): Int {
        val min = min()
        return count { it > min }
    }

    private fun List<Int>.countKeepingMin(): Int {
        val min = min()
        return count { it == min }
    }

    companion object {
        private const val LOG_TAG = "WhisperCpuConfig"

        fun getHighPerfCpuCount(): Int = try {
            readCpuInfo().getHighPerfCpuCount()
        } catch (e: Exception) {
            Log.d(LOG_TAG, "Couldn't read CPU info", e)
            (Runtime.getRuntime().availableProcessors() - 2).coerceAtLeast(2)
        }

        private fun readCpuInfo() = CpuInfo(
            BufferedReader(FileReader("/proc/cpuinfo"))
                .useLines { it.toList() }
        )

        private fun getMaxCpuFrequency(cpuIndex: Int): Int {
            val path = "/sys/devices/system/cpu/cpu${cpuIndex}/cpufreq/cpuinfo_max_freq"
            val maxFreq = BufferedReader(FileReader(path)).use { it.readLine() }
            return maxFreq.toInt()
        }
    }
}