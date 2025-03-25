package com.anrlab.app

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import java.util.Collections

class ANRWatcher(
    private val samplingIntervalMs: Long = 100,
    private val anrThresholdMs: Long = 1000,
) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val samplingThread = HandlerThread("ANRSamplingThread").apply { start() }
    private val samplingHandler = Handler(samplingThread.looper)
    private val samples = Collections.synchronizedList(mutableListOf<StackSample>())
    @Volatile
    private var isMonitoring = false
    private var anrStartTime = 0L
    private val TAG = "ANRWatcher"

    data class StackSample(val timestamp: Long, val stackTrace: Array<StackTraceElement>)

    private val monitorRunnable = object : Runnable {
        override fun run() {
            if (!isMonitoring) return

            var isMainThreadResponsive = false
            // can we remove before adding?
            mainHandler.postAtFrontOfQueue { isMainThreadResponsive = true }

            Thread.sleep(anrThresholdMs)

            if (!isMainThreadResponsive) {
                if (anrStartTime == 0L) {
                    anrStartTime = SystemClock.elapsedRealtime()
                    Log.e(TAG, "ANR is Detected - starting the sample tracing")
                    startSampling()
                }
            } else if (anrStartTime > 0L) {
                val anrDuration = SystemClock.elapsedRealtime() - anrStartTime
                stopSampling(anrDuration)
                anrStartTime = 0L
                Log.e(TAG, "Main thread recovered- stopping the tracing and reporting the traces")
            }

            samplingHandler.postDelayed(this, anrThresholdMs)
        }
    }

    private val samplingRunnable = object : Runnable {
        override fun run() {
            if (anrStartTime == 0L) return

            try {
                val mainThreadStackTrace = Looper.getMainLooper().thread.stackTrace
                samples.add(StackSample(SystemClock.elapsedRealtime(), mainThreadStackTrace))
                samplingHandler.postDelayed(this, samplingIntervalMs)
            } catch (e: Exception) {
                Log.e(TAG, "Error occured while capturing sample- "+e)
            }
        }
    }


    fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true
        Log.d(TAG, "Start Monitoring")
        samplingHandler.post(monitorRunnable)
    }

    fun stopMonitoring() {
        isMonitoring = false
        samplingHandler.removeCallbacks(monitorRunnable)
    }

    private fun startSampling() {
        samples.clear()
        samplingHandler.post(samplingRunnable)
    }

    private fun stopSampling(anrDuration: Long) {
        samplingHandler.removeCallbacks(samplingRunnable)
        if (samples.isNotEmpty()) {
            Log.d(TAG, "ANR Duration: $anrDuration ms, Samples: ${samples.size}")
            val processedData = processStackSamples()
            Log.d(TAG, "ANR processed data- ${processedData.toString()}")
//            firebaseCrashlytics.recordException(Throwable("ANR detected - Duration: $anrDuration ms ${processedData.toString()}"))
        }
    }

    private fun processStackSamples(): LinkedHashMap<String, Int> {
        val methodFrequencyMap = HashMap<String, Int>()
        samples.forEach { sample ->
            val uniqueMethods = sample.stackTrace.map { "${it.className}.${it.methodName}(${it.fileName}:${it.lineNumber}" }.toSet()
            uniqueMethods.forEach { method ->
                methodFrequencyMap[method] = (methodFrequencyMap[method] ?: 0) + 1
            }
        }
        return methodFrequencyMap.entries
            .filter { it.key.contains("com.gojek") || it.key.contains("java.lang.Thread.sleep") }
            .sortedByDescending { it.value }
            .associate { it.toPair() } as LinkedHashMap<String, Int>
    }
}
