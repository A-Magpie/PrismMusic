package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Robust, production-grade application logger for Prism Music.
 * Supports:
 * - Persistent logging to a rotating file in internal app storage.
 * - Dynamic runtime enable/disable from Settings.
 * - Crash / Uncaught exception logging.
 * - Thread-safe asynchronous disk writes.
 * - Export and clearing of logs.
 */
object AppLogger {

    private const val PREFS_NAME = "prism_logger_prefs"
    private const val KEY_LOGGING_ENABLED = "logging_enabled"
    private const val LOG_FILE_NAME = "prism_app.log"
    private const val LOG_OLD_FILE_NAME = "prism_app.log.old"
    private const val MAX_LOG_SIZE_BYTES = 3 * 1024 * 1024L // 3 MB max per file

    private val logScope = CoroutineScope(Dispatchers.IO)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    private var appContext: Context? = null
    private var prefs: SharedPreferences? = null

    private val _isLoggingEnabled = MutableStateFlow(true)
    val isLoggingEnabled: StateFlow<Boolean> = _isLoggingEnabled.asStateFlow()

    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

    fun init(context: Context) {
        val appCtx = context.applicationContext
        appContext = appCtx
        val sp = appCtx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        // Default to enabled so unexpected events are immediately traceable
        val enabled = sp.getBoolean(KEY_LOGGING_ENABLED, true)
        _isLoggingEnabled.value = enabled

        // Capture uncaught exceptions
        if (defaultExceptionHandler == null) {
            defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                e("CRASH", "Uncaught exception on thread ${thread.name}", throwable)
                defaultExceptionHandler?.uncaughtException(thread, throwable)
            }
        }

        i("AppLogger", "Logger initialized. Logging enabled = $enabled")
    }

    fun setLoggingEnabled(enabled: Boolean) {
        _isLoggingEnabled.value = enabled
        prefs?.edit()?.putBoolean(KEY_LOGGING_ENABLED, enabled)?.apply()
        i("AppLogger", "Logging toggled by user: $enabled")
    }

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        writeLog("DEBUG", tag, message, null)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        writeLog("INFO", tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
        writeLog("WARN", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        writeLog("ERROR", tag, message, throwable)
    }

    private fun writeLog(level: String, tag: String, message: String, throwable: Throwable?) {
        if (!_isLoggingEnabled.value) return
        val context = appContext ?: return

        logScope.launch {
            try {
                val logDir = File(context.filesDir, "logs")
                if (!logDir.exists()) logDir.mkdirs()

                val logFile = File(logDir, LOG_FILE_NAME)
                if (logFile.exists() && logFile.length() > MAX_LOG_SIZE_BYTES) {
                    val oldFile = File(logDir, LOG_OLD_FILE_NAME)
                    if (oldFile.exists()) oldFile.delete()
                    logFile.renameTo(oldFile)
                }

                val timestamp = synchronized(dateFormat) { dateFormat.format(Date()) }
                val threadName = Thread.currentThread().name

                val logEntry = buildString {
                    append(timestamp)
                    append(" [")
                    append(level)
                    append("] [")
                    append(threadName)
                    append("] [")
                    append(tag)
                    append("]: ")
                    append(message)
                    append("\n")
                    if (throwable != null) {
                        val sw = StringWriter()
                        throwable.printStackTrace(PrintWriter(sw))
                        append(sw.toString())
                        append("\n")
                    }
                }

                FileWriter(logFile, true).use { writer ->
                    writer.write(logEntry)
                }
            } catch (_: Exception) {}
        }
    }

    fun getLogFile(context: Context): File? {
        val logDir = File(context.filesDir, "logs")
        val logFile = File(logDir, LOG_FILE_NAME)
        return if (logFile.exists()) logFile else null
    }

    fun getLogText(context: Context, maxLines: Int = 1000): String {
        return try {
            val logFile = getLogFile(context) ?: return "No log file found."
            val lines = logFile.readLines()
            val start = (lines.size - maxLines).coerceAtLeast(0)
            lines.subList(start, lines.size).joinToString("\n")
        } catch (e: Exception) {
            "Error reading log: ${e.message}"
        }
    }

    fun clearLogs(context: Context): Boolean {
        return try {
            val logDir = File(context.filesDir, "logs")
            val logFile = File(logDir, LOG_FILE_NAME)
            val oldFile = File(logDir, LOG_OLD_FILE_NAME)
            if (logFile.exists()) logFile.delete()
            if (oldFile.exists()) oldFile.delete()
            i("AppLogger", "Logs cleared by user")
            true
        } catch (e: Exception) {
            false
        }
    }
}
