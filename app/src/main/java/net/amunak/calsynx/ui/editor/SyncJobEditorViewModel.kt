package net.amunak.calsynx.ui.editor

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.amunak.calsynx.R
import net.amunak.calsynx.calendar.CalendarInfo
import net.amunak.calsynx.data.DatabaseProvider
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.data.repository.CalendarRepository
import net.amunak.calsynx.data.repository.SyncJobRepository
import net.amunak.calsynx.data.sync.SyncJobScheduler
import net.amunak.calsynx.data.sync.AvailabilityMode
import net.amunak.calsynx.data.sync.ReminderMode
import net.amunak.calsynx.domain.CreateSyncJobUseCase
import net.amunak.calsynx.domain.UpdateSyncJobUseCase

enum class SaveState {
	Idle,
	Saving,
	Success,
	Error
}

data class SyncJobEditorUiState(
	val calendars: List<CalendarInfo> = emptyList(),
	val jobs: List<SyncJob> = emptyList(),
	val job: SyncJob? = null,
	val isLoading: Boolean = false,
	val errorMessage: String? = null,
	val saveState: SaveState = SaveState.Idle
)

class SyncJobEditorViewModel(app: Application) : AndroidViewModel(app) {
	private val syncJobRepository = SyncJobRepository(DatabaseProvider.get(app).syncJobDao())
	private val calendarRepository = CalendarRepository()
	private val createSyncJob = CreateSyncJobUseCase(syncJobRepository)
	private val updateSyncJob = UpdateSyncJobUseCase(syncJobRepository)
	private val scheduler = SyncJobScheduler(app)

	private val _uiState = MutableStateFlow(SyncJobEditorUiState())
	val uiState: StateFlow<SyncJobEditorUiState> = _uiState.asStateFlow()

	private var loaded = false

	fun load(jobId: Long?) {
		if (loaded) return
		loaded = true
		viewModelScope.launch(Dispatchers.IO) {
			_uiState.update { it.copy(isLoading = true, errorMessage = null) }
			try {
				val calendars = calendarRepository.getCalendars(getApplication(), onlyVisible = false)
				val jobs = syncJobRepository.getAll()
				val job = jobId?.let { syncJobRepository.getById(it) }
				_uiState.update {
					it.copy(
						calendars = calendars,
						jobs = jobs,
						job = job,
						isLoading = false
					)
				}
			} catch (e: SecurityException) {
				Log.e(TAG, "Calendar permission denied while loading editor data", e)
				_uiState.update {
					it.copy(
						isLoading = false,
						errorMessage = getApplication<Application>()
							.getString(R.string.message_calendar_permission_denied)
					)
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "Failed to load editor data", e)
				_uiState.update {
					it.copy(
						isLoading = false,
						errorMessage = getApplication<Application>()
							.getString(R.string.message_failed_load_calendars)
					)
				}
			}
		}
	}

	fun save(
		sourceId: Long,
		targetId: Long,
		pastDays: Int,
		futureDays: Int,
		syncAllEvents: Boolean,
		frequencyMinutes: Int,
		availabilityMode: AvailabilityMode,
		copyEventColor: Boolean,
		copyPrivacy: Boolean,
		copyAttendees: Boolean,
		copyOrganizer: Boolean,
		reminderMode: ReminderMode,
		reminderAllDayMinutes: Int,
		reminderTimedMinutes: Int,
		reminderAllDayEnabled: Boolean,
		reminderTimedEnabled: Boolean,
		reminderResyncEnabled: Boolean,
		pairExistingOnFirstSync: Boolean,
		deleteUnmappedTargets: Boolean
	) {
		viewModelScope.launch(Dispatchers.IO) {
			_uiState.update { it.copy(saveState = SaveState.Saving, errorMessage = null) }
			try {
				val jobs = syncJobRepository.getAll()
				val validationError = validateSyncJobSelection(
					sourceId,
					targetId,
					jobs,
					_uiState.value.job?.id
				)
				if (validationError != null) {
					_uiState.update {
						it.copy(
							saveState = SaveState.Error,
							errorMessage = getApplication<Application>().getString(validationError)
						)
					}
					return@launch
				}

				val existing = _uiState.value.job
				if (existing == null) {
					val id = createSyncJob(
						SyncJob(
							sourceCalendarId = sourceId,
							targetCalendarId = targetId,
							windowPastDays = pastDays,
							windowFutureDays = futureDays,
							syncAllEvents = syncAllEvents,
							frequencyMinutes = frequencyMinutes,
							availabilityMode = availabilityMode.value,
							copyEventColor = copyEventColor,
							copyPrivacy = copyPrivacy,
							copyAttendees = copyAttendees,
							copyOrganizer = copyOrganizer,
							reminderMode = reminderMode.value,
							reminderAllDayMinutes = reminderAllDayMinutes,
							reminderTimedMinutes = reminderTimedMinutes,
							reminderAllDayEnabled = reminderAllDayEnabled,
							reminderTimedEnabled = reminderTimedEnabled,
							reminderResyncEnabled = reminderResyncEnabled,
							pairExistingOnFirstSync = pairExistingOnFirstSync,
							deleteUnmappedTargets = deleteUnmappedTargets
						)
					)
					val job = SyncJob(
						id = id,
						sourceCalendarId = sourceId,
						targetCalendarId = targetId,
						windowPastDays = pastDays,
						windowFutureDays = futureDays,
						syncAllEvents = syncAllEvents,
						frequencyMinutes = frequencyMinutes,
						availabilityMode = availabilityMode.value,
						copyEventColor = copyEventColor,
						copyPrivacy = copyPrivacy,
						copyAttendees = copyAttendees,
						copyOrganizer = copyOrganizer,
						reminderMode = reminderMode.value,
						reminderAllDayMinutes = reminderAllDayMinutes,
						reminderTimedMinutes = reminderTimedMinutes,
						reminderAllDayEnabled = reminderAllDayEnabled,
						reminderTimedEnabled = reminderTimedEnabled,
						reminderResyncEnabled = reminderResyncEnabled,
						pairExistingOnFirstSync = pairExistingOnFirstSync,
						deleteUnmappedTargets = deleteUnmappedTargets
					)
					scheduler.schedule(job)
					scheduler.enqueueImmediate(job)
				} else {
					val updated = existing.copy(
						windowPastDays = pastDays,
						windowFutureDays = futureDays,
						syncAllEvents = syncAllEvents,
						frequencyMinutes = frequencyMinutes,
						availabilityMode = availabilityMode.value,
						copyEventColor = copyEventColor,
						copyPrivacy = copyPrivacy,
						copyAttendees = copyAttendees,
						copyOrganizer = copyOrganizer,
						reminderMode = reminderMode.value,
						reminderAllDayMinutes = reminderAllDayMinutes,
						reminderTimedMinutes = reminderTimedMinutes,
						reminderAllDayEnabled = reminderAllDayEnabled,
						reminderTimedEnabled = reminderTimedEnabled,
						reminderResyncEnabled = reminderResyncEnabled,
						pairExistingOnFirstSync = pairExistingOnFirstSync,
						deleteUnmappedTargets = deleteUnmappedTargets
					)
					updateSyncJob(updated)
					scheduler.schedule(updated)
				}
				_uiState.update { it.copy(saveState = SaveState.Success) }
			} catch (e: RuntimeException) {
				Log.e(TAG, "Failed to save sync job", e)
				_uiState.update {
					it.copy(
						saveState = SaveState.Error,
						errorMessage = getApplication<Application>()
							.getString(R.string.message_sync_job_save_failed)
					)
				}
			}
		}
	}

	companion object {
		private const val TAG = "SyncJobEditorViewModel"
	}
}
