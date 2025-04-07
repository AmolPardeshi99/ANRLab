package com.anrlab.app.cleanup//package com.anrlab.app
//
//import android.os.Looper
//import java.io.Serializable
//
///**
// * Error thrown when an ANR is detected. Contains the stack trace of the frozen UI thread.
// *
// * Note: In an ANRError, all the "Caused by" are not really the cause of the exception. Each "Caused
// * by" is the stack trace of a running thread. The main thread always comes first.
// */
//class ANRError
//private constructor(
//  st: ThreadInfo.ThreadStackTrace,
//  /** The minimum duration, in ms, for which the main thread has been blocked. May be more. */
//  duration: Long,
//) : Error("Application Not Responding for at least $duration ms.", st) {
//
//  private data class ThreadInfo(val name: String, val stackTrace: Array<StackTraceElement>) :
//    Serializable {
//    // Custom equals and hashCode for Array handling
//    override fun equals(other: Any?): Boolean {
//      if (this === other) return true
//      if (other !is ThreadInfo) return false
//      return name == other.name && stackTrace.contentEquals(other.stackTrace)
//    }
//
//    override fun hashCode(): Int {
//      var result = name.hashCode()
//      result = 31 * result + stackTrace.contentHashCode()
//      return result
//    }
//
//    inner class ThreadStackTrace(other: ThreadStackTrace? = null) : Throwable(name, other) {
//      override fun fillInStackTrace(): Throwable {
//        stackTrace = this@ThreadInfo.stackTrace
//        return this
//      }
//    }
//
//    companion object {
//      private const val serialVersionUID: Long = 1L
//    }
//  }
//
//  override fun fillInStackTrace(): Throwable {
//    stackTrace = emptyArray()
//    return this
//  }
//
//  companion object {
//
//    @JvmStatic
//    fun new(
//      duration: Long,
//      prefix: String? = null,
//      logThreadsWithoutStackTrace: Boolean = false,
//    ): ANRError {
//      val mainThread = Looper.getMainLooper().thread
//
//      // Using TreeMap with custom comparator through sortedMapOf
//      val stackTraces =
//        sortedMapOf<Thread, Array<StackTraceElement>>(
//          compareBy<Thread> { it != mainThread }.thenBy { it.name }
//        )
//
//      Thread.getAllStackTraces().forEach { (thread, stack) ->
//        if (shouldIncludeThread(thread, stack, prefix, logThreadsWithoutStackTrace)) {
//          stackTraces[thread] = stack
//        }
//      }
//
//      // Ensure main thread is included
//      if (!stackTraces.containsKey(mainThread)) {
//        stackTraces[mainThread] = mainThread.stackTrace
//      }
//
//      var threadStackTrace: ThreadInfo.ThreadStackTrace? = null
//      stackTraces.forEach { (thread, stack) ->
//        threadStackTrace =
//          ThreadInfo(getThreadTitle(thread), stack).ThreadStackTrace(threadStackTrace)
//      }
//
//      return threadStackTrace?.let { ANRError(it, duration) }
//        ?: throw IllegalStateException("Failed to create ANRError - no threads found")
//    }
//
//    private fun shouldIncludeThread(
//      thread: Thread,
//      stack: Array<StackTraceElement>,
//      prefix: String?,
//      logThreadsWithoutStackTrace: Boolean,
//    ): Boolean {
//      val mainThread = Looper.getMainLooper().thread
//      return thread == mainThread ||
//        (prefix != null &&
//          thread.name.startsWith(prefix) &&
//          (logThreadsWithoutStackTrace || stack.isNotEmpty()))
//    }
//
//    @JvmStatic
//    fun newMainOnly(duration: Long): ANRError {
//      val mainThread = Looper.getMainLooper().thread
//      val mainStackTrace = mainThread.stackTrace
//
//      return ANRError(
//        ThreadInfo(getThreadTitle(mainThread), mainStackTrace).ThreadStackTrace(),
//        duration,
//      )
//    }
//
//    private fun getThreadTitle(thread: Thread): String = "${thread.name} (state = ${thread.state})"
//  }
//}
