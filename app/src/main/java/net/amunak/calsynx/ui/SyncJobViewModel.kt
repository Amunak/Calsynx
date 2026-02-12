package net.amunak.calsynx.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.amunak.calsynx.calendar.CalendarInfo
import net.amunak.calsynx.data.DatabaseProvider
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.data.repository.CalendarRepository
import net.amunak.calsynx.data.repository.SyncJobRepository
import net.amunak.calsynx.data.sync.CalendarSyncer
import net.amunak.calsynx.data.sync.SyncJobScheduler
import net.amunak.calsynx.ui.logs.SyncLogStore
import net.amunak.calsynx.ui.formatters.formatJobLabel
import net.amunak.calsynx.domain.CreateSyncJobUseCase
import net.amunak.calsynx.domain.DeleteSyncJobUseCase
import net.amunak.calsynx.domain.ObserveSyncJobsUseCase
import net.amunak.calsynx.domain.RunManualSyncUseCase
import net.amunak.calsynx.domain.UpdateSyncErrorUseCase
import net.amunak.calsynx.domain.UpdateSyncStatsUseCase
import net.amunak.calsynx.domain.UpdateSyncJobUseCase
import net.amunak.calsynx.R

data class SyncJobUiState(
	val jobs: List<SyncJob> = emptyList(),
	val calendars: List<CalendarInfo> = emptyList(),
	val hasCalendarPermission: Boolean = false,
	val isRefreshing: Boolean = false,
	val syncingJobIds: Set<Long> = emptySet(),
	val errorMessage: String? = null
)

class SyncJobViewModel(private val app: Application) : AndroidViewModel(app) {
	private val syncJobRepository = SyncJobRepository(DatabaseProvider.get(app).syncJobDao())
	private val calendarRepository = CalendarRepository()
	private val calendarSyncer = CalendarSyncer(
		app.contentResolver,
		DatabaseProvider.get(app).eventMappingDao()
	)
	private val observeSyncJobs = ObserveSyncJobsUseCase(syncJobRepository)
	private val createSyncJob = CreateSyncJobUseCase(syncJobRepository)
	private val updateSyncJob = UpdateSyncJobUseCase(syncJobRepository)
	private val deleteSyncJob = DeleteSyncJobUseCase(
		syncJobRepository,
		DatabaseProvider.get(app).eventMappingDao()
	)
	private val updateSyncStats = UpdateSyncStatsUseCase(syncJobRepository)
	private val updateSyncError = UpdateSyncErrorUseCase(syncJobRepository)
	private val runManualSync = RunManualSyncUseCase(
		calendarSyncer,
		updateSyncStats
	)
	private val scheduler = SyncJobScheduler(app)
	private val logStore = SyncLogStore(app)

	private val calendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
	private val hasCalendarPermission = MutableStateFlow(false)
	private val isRefreshing = MutableStateFlow(false)
	private val syncingJobIds = MutableStateFlow<Set<Long>>(emptySet())
	private val errorMessage = MutableStateFlow<String?>(null)

	private val auxState = combine(
		calendars,
		hasCalendarPermission,
		isRefreshing,
		syncingJobIds,
		errorMessage
	) { calendars, hasPermission, refreshing, syncingIds, error ->
		AuxState(
			calendars = calendars,
			hasCalendarPermission = hasPermission,
			isRefreshing = refreshing,
			syncingJobIds = syncingIds,
			errorMessage = error
		)
	}

	val uiState: StateFlow<SyncJobUiState> = combine(
		observeSyncJobs(),
		auxState
	) { jobs, aux ->
		SyncJobUiState(
			jobs = jobs,
			calendars = aux.calendars,
			hasCalendarPermission = aux.hasCalendarPermission,
			isRefreshing = aux.isRefreshing,
			syncingJobIds = aux.syncingJobIds,
			errorMessage = aux.errorMessage
		)
	}.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SyncJobUiState())

	init {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				scheduler.scheduleAll(syncJobRepository.getAll())
			} catch (e: RuntimeException) {
				Log.e(TAG, "Failed to schedule background sync jobs", e)
			}
		}
	}

	fun onPermissionChanged(hasPermission: Boolean) {
		hasCalendarPermission.value = hasPermission
		if (hasPermission) {
			refreshCalendars()
		} else {
			calendars.value = emptyList()
		}
	}

	fun refreshCalendars() {
		if (!hasCalendarPermission.value) return
		viewModelScope.launch(Dispatchers.IO) {
			isRefreshing.value = true
			errorMessage.value = null
			try {
				calendars.value = calendarRepository.getCalendars(app, onlyVisible = false)
			} catch (e: SecurityException) {
				Log.e(TAG, "Calendar permission denied while refreshing calendars", e)
				calendars.value = emptyList()
				errorMessage.value = app.getString(R.string.message_calendar_permission_denied)
			} catch (e: RuntimeException) {
				Log.e(TAG, "Failed to load calendars", e)
				errorMessage.value = app.getString(R.string.message_failed_load_calendars)
			} finally {
				isRefreshing.value = false
			}
		}
	}

	fun createJob(
		sourceId: Long,
		targetId: Long,
		pastDays: Int,
		futureDays: Int,
		syncAllEvents: Boolean,
		frequencyMinutes: Int
	) {
		viewModelScope.launch(Dispatchers.IO) {
			if (sourceId == targetId) {
				Log.w(
					TAG,
					"Rejected sync job due to identical calendars: source=$sourceId target=$targetId"
				)
				errorMessage.value = app.getString(R.string.message_validation_source_target_same)
				return@launch
			}
			val jobs = syncJobRepository.getAll()
			val sourceConflict = jobs.any { it.targetCalendarId == sourceId }
			val targetConflict = jobs.any { it.sourceCalendarId == targetId }
			if (sourceConflict || targetConflict) {
				Log.w(
					TAG,
					"Rejected sync job due to calendar conflict: source=$sourceId target=$targetId"
				)
				errorMessage.value = if (sourceConflict) {
					app.getString(R.string.message_validation_source_is_target)
				} else {
					app.getString(R.string.message_validation_target_is_source)
				}
				return@launch
			}
			createSyncJob(
				SyncJob(
					sourceCalendarId = sourceId,
					targetCalendarId = targetId,
					windowPastDays = pastDays,
					windowFutureDays = futureDays,
					syncAllEvents = syncAllEvents,
					frequencyMinutes = frequencyMinutes
				)
			).also { id ->
				val job = SyncJob(
					id = id,
					sourceCalendarId = sourceId,
					targetCalendarId = targetId,
					windowPastDays = pastDays,
					windowFutureDays = futureDays,
					syncAllEvents = syncAllEvents,
					frequencyMinutes = frequencyMinutes
				)
				scheduler.schedule(job)
				scheduler.enqueueImmediate(job)
			}
		}
	}

	fun updateJobOptions(
		job: SyncJob,
		pastDays: Int,
		futureDays: Int,
		syncAllEvents: Boolean,
		frequencyMinutes: Int
	) {
		viewModelScope.launch(Dispatchers.IO) {
			val updated = job.copy(
				windowPastDays = pastDays,
				windowFutureDays = futureDays,
				syncAllEvents = syncAllEvents,
				frequencyMinutes = frequencyMinutes
			)
			updateSyncJob(updated)
			scheduler.schedule(updated)
		}
	}

	fun setJobActive(job: SyncJob, isActive: Boolean) {
		viewModelScope.launch(Dispatchers.IO) {
			val updated = job.copy(isActive = isActive)
			updateSyncJob(updated)
			scheduler.schedule(updated)
		}
	}

	fun deleteJob(job: SyncJob) {
		viewModelScope.launch(Dispatchers.IO) {
			deleteSyncJob(job)
			scheduler.cancel(job.id)
		}
	}

	fun deleteSyncedTargets(job: SyncJob) {
		viewModelScope.launch(Dispatchers.IO) {
			errorMessage.value = null
			try {
				val deleted = calendarSyncer.deleteSyncedTargets(job)
				logStore.append("Purged $deleted synced events for ${jobLabel(job)}")
				if (deleted == 0) {
					Log.i(TAG, "No synced targets to delete for job ${job.id}")
				}
			} catch (e: SecurityException) {
				Log.e(TAG, "Calendar permission denied while deleting synced targets", e)
				errorMessage.value = app.getString(R.string.message_calendar_permission_denied)
			} catch (e: RuntimeException) {
				Log.e(TAG, "Failed to delete synced targets for job ${job.id}", e)
				errorMessage.value = app.getString(R.string.message_purge_synced_failed)
			}
		}
	}

	fun runManualSync(job: SyncJob) {
		viewModelScope.launch(Dispatchers.IO) {
			syncingJobIds.value = syncingJobIds.value + job.id
			errorMessage.value = null
			try {
				val result = runManualSync.invoke(job)
				logStore.append(formatSyncSummary("Manual sync", jobLabel(job), result))
				if (result.initialPairAttempted) {
					logStore.append(
						"Initial sync paired ${result.initialPairCount} existing target events for ${jobLabel(job)}"
					)
				}
				logStore.append("Manual sync completed for ${jobLabel(job)}")
			} catch (e: SecurityException) {
				Log.e(TAG, "Calendar permission denied during manual sync", e)
				logStore.append("Manual sync permission denied for ${jobLabel(job)}")
				val message = app.getString(R.string.message_calendar_permission_denied)
				updateSyncError(job, message)
				errorMessage.value = message
			} catch (e: RuntimeException) {
				Log.e(TAG, "Manual sync failed", e)
				logStore.append("Manual sync failed for ${jobLabel(job)}: ${e.message ?: "unknown error"}")
				val message = app.getString(R.string.message_sync_error)
				updateSyncError(job, message)
				errorMessage.value = message
			} finally {
				syncingJobIds.value = syncingJobIds.value - job.id
			}
		}
	}

	private fun jobLabel(job: SyncJob): String {
		return formatJobLabel(job, calendars.value)
	}

	private fun formatSyncSummary(prefix: String, label: String, result: net.amunak.calsynx.data.sync.SyncResult): String {
		return "$prefix summary for $label: " +
			"created=${result.created}, updated=${result.updated}, deleted=${result.deleted}, " +
			"source=${result.sourceCount}, targets=${result.targetCount}, totalTargets=${result.targetTotalCount}"
	}

	companion object {
		private const val TAG = "SyncJobViewModel"
	}
}

private data class AuxState(
	val calendars: List<CalendarInfo>,
	val hasCalendarPermission: Boolean,
	val isRefreshing: Boolean,
	val syncingJobIds: Set<Long>,
	val errorMessage: String?
)
