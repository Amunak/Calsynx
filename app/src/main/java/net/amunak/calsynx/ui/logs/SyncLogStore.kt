package net.amunak.calsynx.ui.logs

import android.content.Context
import java.io.File

class SyncLogStore(private val context: Context) {
	private val logDir = File(context.filesDir, "logs")
	private val logFile = File(logDir, LOG_FILE_NAME)

	fun append(line: String) {
		ensureDir()
		logFile.appendText("${timestamp()} $line\n")
		trimIfNeeded()
	}

	fun readLines(maxLines: Int): List<String> {
		if (!logFile.exists()) return emptyList()
		val lines = logFile.readLines()
		return if (lines.size <= maxLines) lines else lines.takeLast(maxLines)
	}

	fun clear() {
		if (logFile.exists()) {
			logFile.writeText("")
		}
	}

	fun file(): File {
		ensureDir()
		return logFile
	}

	private fun ensureDir() {
		if (!logDir.exists()) {
			logDir.mkdirs()
		}
	}

	private fun trimIfNeeded() {
		if (!logFile.exists()) return
		val lines = logFile.readLines()
		if (lines.size <= MAX_LINES) return
		logFile.writeText(lines.takeLast(MAX_LINES).joinToString("\n") + "\n")
	}

	private fun timestamp(): String {
		return java.time.ZonedDateTime.now().format(
			java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
		)
	}

	companion object {
		private const val LOG_FILE_NAME = "sync.log"
		private const val MAX_LINES = 1000
	}
}
