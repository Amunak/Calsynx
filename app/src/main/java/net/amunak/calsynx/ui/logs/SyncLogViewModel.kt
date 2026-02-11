package net.amunak.calsynx.ui.logs

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.amunak.calsynx.R
import net.amunak.calsynx.data.DatabaseProvider
import net.amunak.calsynx.data.repository.CalendarRepository
import net.amunak.calsynx.data.repository.SyncJobRepository
import net.amunak.calsynx.ui.formatters.resolveJobCalendarNames
import java.io.File
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class SyncLogUiState(
	val lines: List<String> = emptyList(),
	val toastMessage: String? = null,
	val isLoading: Boolean = false
)

class SyncLogViewModel(app: Application) : AndroidViewModel(app) {
	private val logStore = SyncLogStore(app)
	private val jobRepository = SyncJobRepository(DatabaseProvider.get(app).syncJobDao())
	private val calendarRepository = CalendarRepository()
	private val _uiState = MutableStateFlow(SyncLogUiState())
	val uiState: StateFlow<SyncLogUiState> = _uiState.asStateFlow()

	fun loadLogs() {
		viewModelScope.launch(Dispatchers.IO) {
			_uiState.update { it.copy(isLoading = true) }
			val lines = logStore.readLines(MAX_LOG_LINES)
			_uiState.update { it.copy(lines = lines, isLoading = false) }
		}
	}

	fun clearLogs() {
		viewModelScope.launch(Dispatchers.IO) {
			logStore.clear()
			_uiState.update { it.copy(lines = emptyList()) }
		}
	}

	fun shareLogs() {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val context = getApplication<Application>()
				val file = buildShareFile(context)
				val uri = FileProvider.getUriForFile(
					context,
					"${context.packageName}.fileprovider",
					file
				)
				val intent = Intent(Intent.ACTION_SEND).apply {
					type = "text/plain"
					putExtra(Intent.EXTRA_STREAM, uri)
					addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
				}
				val chooser = Intent.createChooser(intent, context.getString(R.string.label_share_logs))
				chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				context.startActivity(chooser)
			} catch (e: RuntimeException) {
				Log.e(TAG, "Unable to share sync logs", e)
				_uiState.update {
					it.copy(toastMessage = getApplication<Application>().getString(R.string.message_share_logs_failed))
				}
			}
		}
	}

	fun clearToast() {
		_uiState.update { it.copy(toastMessage = null) }
	}

	private suspend fun buildShareFile(context: Context): File {
		val cacheDir = File(context.cacheDir, "logs")
		if (!cacheDir.exists()) {
			cacheDir.mkdirs()
		}
		val file = File(cacheDir, "sync-log-${System.currentTimeMillis()}.txt")
		val header = StringBuilder().apply {
			appendLine("Calsynx sync logs")
			appendLine("App version: ${getAppVersion(context)}")
			appendLine("Exported: ${ZonedDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}")
			appendLine()
			appendLine("Jobs:")
			appendLine(buildJobSummary(context))
			appendLine()
			appendLine("Recent log entries:")
		}.toString()
		val lines = logStore.readLines(MAX_LOG_LINES)
		file.writeText(header + lines.joinToString("\n"))
		return file
	}

	private suspend fun buildJobSummary(context: Context): String {
		return try {
			val calendars = calendarRepository.getCalendars(context, onlyVisible = false)
				val calendarById = calendars.associateBy { it.id }
				val jobs = jobRepository.getAll()
			if (jobs.isEmpty()) {
				"- None"
			} else {
					jobs.joinToString("\n") { job ->
						val names = resolveJobCalendarNames(job, calendarById)
						val status = if (job.isActive) "active" else "paused"
						val window = if (job.syncAllEvents) {
							"all events"
						} else {
							"${job.windowPastDays}d back, ${job.windowFutureDays}d ahead"
						}
						"- ${job.id}: ${names.sourceName} → ${names.targetName} ($status, every ${job.frequencyMinutes}m, window=$window)"
					}
			}
		} catch (e: RuntimeException) {
			"- Unable to load jobs: ${e.message ?: "unknown error"}"
		}
	}

	private fun getAppVersion(context: Context): String {
		return try {
			// Use PackageManager to avoid BuildConfig when buildConfig generation is disabled.
			val packageInfo = if (Build.VERSION.SDK_INT >= 33) {
				context.packageManager.getPackageInfo(
					context.packageName,
					PackageManager.PackageInfoFlags.of(0)
				)
			} else {
				context.packageManager.getPackageInfo(context.packageName, 0)
			}
			val versionName = packageInfo.versionName ?: "unknown"
			val versionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
			"$versionName ($versionCode)"
		} catch (e: RuntimeException) {
			"unknown"
		}
	}

	companion object {
		private const val TAG = "SyncLogViewModel"
		private const val MAX_LOG_LINES = 200
	}
}
