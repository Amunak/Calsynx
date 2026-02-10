package net.amunak.calscium.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.amunak.calscium.calendar.CalendarInfo
import net.amunak.calscium.data.DatabaseProvider
import net.amunak.calscium.data.SyncJob
import net.amunak.calscium.data.repository.CalendarRepository
import net.amunak.calscium.data.repository.SyncJobRepository
import net.amunak.calscium.data.sync.CalendarSyncer
import net.amunak.calscium.domain.CreateSyncJobUseCase
import net.amunak.calscium.domain.DeleteSyncJobUseCase
import net.amunak.calscium.domain.ObserveSyncJobsUseCase
import net.amunak.calscium.domain.RunManualSyncUseCase
import net.amunak.calscium.domain.UpdateLastSyncUseCase
import net.amunak.calscium.domain.UpdateSyncJobUseCase

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
	private val calendarSyncer = CalendarSyncer(app.contentResolver)
	private val observeSyncJobs = ObserveSyncJobsUseCase(syncJobRepository)
	private val createSyncJob = CreateSyncJobUseCase(syncJobRepository)
	private val updateSyncJob = UpdateSyncJobUseCase(syncJobRepository)
	private val deleteSyncJob = DeleteSyncJobUseCase(syncJobRepository)
	private val updateLastSync = UpdateLastSyncUseCase(syncJobRepository)
	private val runManualSync = RunManualSyncUseCase(
		calendarSyncer,
		updateLastSync
	)

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
				calendars.value = calendarRepository.getCalendars(app)
			} catch (e: SecurityException) {
				Log.e(TAG, "Calendar permission denied while refreshing calendars", e)
				calendars.value = emptyList()
				errorMessage.value = "Calendar permission denied."
			} catch (e: RuntimeException) {
				Log.e(TAG, "Failed to load calendars", e)
				errorMessage.value = "Failed to load calendars."
			} finally {
				isRefreshing.value = false
			}
		}
	}

	fun createJob(sourceId: Long, targetId: Long) {
		viewModelScope.launch(Dispatchers.IO) {
			createSyncJob(
				SyncJob(
					sourceCalendarId = sourceId,
					targetCalendarId = targetId
				)
			)
		}
	}

	fun setJobActive(job: SyncJob, isActive: Boolean) {
		viewModelScope.launch(Dispatchers.IO) {
			updateSyncJob(job.copy(isActive = isActive))
		}
	}

	fun deleteJob(job: SyncJob) {
		viewModelScope.launch(Dispatchers.IO) {
			deleteSyncJob(job)
		}
	}

	fun runManualSync(job: SyncJob) {
		viewModelScope.launch(Dispatchers.IO) {
			syncingJobIds.value = syncingJobIds.value + job.id
			errorMessage.value = null
			try {
				runManualSync.invoke(job)
			} catch (e: SecurityException) {
				Log.e(TAG, "Calendar permission denied during manual sync", e)
				errorMessage.value = "Calendar permission denied."
			} catch (e: RuntimeException) {
				Log.e(TAG, "Manual sync failed", e)
				errorMessage.value = "Sync failed."
			} finally {
				syncingJobIds.value = syncingJobIds.value - job.id
			}
		}
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
