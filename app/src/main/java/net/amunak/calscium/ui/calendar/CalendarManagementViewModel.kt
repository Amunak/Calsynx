package net.amunak.calscium.ui.calendar

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
import net.amunak.calscium.calendar.CalendarInfo
import net.amunak.calscium.data.DatabaseProvider
import net.amunak.calscium.data.repository.CalendarRepository
import net.amunak.calscium.data.repository.SyncJobRepository
import net.amunak.calscium.ui.components.sanitizeCalendarName

data class CalendarRowUi(
	val calendar: CalendarInfo,
	val eventCount: Int,
	val syncedCount: Int,
	val incomingCalendars: List<CalendarInfo>,
	val outgoingCalendars: List<CalendarInfo>
)

data class CalendarManagementUiState(
	val calendars: List<CalendarRowUi> = emptyList(),
	val selectedCalendar: CalendarRowUi? = null,
	val errorMessage: String? = null,
	val toastMessage: String? = null,
	val isLoading: Boolean = false
)

class CalendarManagementViewModel(app: Application) : AndroidViewModel(app) {
	private val calendarRepository = CalendarRepository()
	private val syncJobRepository = SyncJobRepository(DatabaseProvider.get(app).syncJobDao())
	private val mappingDao = DatabaseProvider.get(app).eventMappingDao()

	private val _uiState = MutableStateFlow(CalendarManagementUiState())
	val uiState: StateFlow<CalendarManagementUiState> = _uiState.asStateFlow()

	fun refreshCalendars() {
		viewModelScope.launch(Dispatchers.IO) {
			_uiState.update { it.copy(isLoading = true, errorMessage = null) }
			try {
				val calendars = calendarRepository.getCalendars(getApplication(), onlyVisible = false)
				val calendarById = calendars.associateBy { it.id }
				val jobs = syncJobRepository.getAll()
				val rows = calendars.map { calendar ->
					val incoming = jobs.filter { it.targetCalendarId == calendar.id }
						.mapNotNull { job -> calendarById[job.sourceCalendarId] }
					val outgoing = jobs.filter { it.sourceCalendarId == calendar.id }
						.mapNotNull { job -> calendarById[job.targetCalendarId] }
					CalendarRowUi(
						calendar = calendar,
						eventCount = calendarRepository.countEvents(
							getApplication<Application>().contentResolver,
							calendar.id
						),
						syncedCount = mappingDao.countSyncedTargets(calendar.id),
						incomingCalendars = incoming,
						outgoingCalendars = outgoing
					)
				}
				_uiState.update { state ->
					val selectedId = state.selectedCalendar?.calendar?.id
					val selected = rows.firstOrNull { it.calendar.id == selectedId }
					state.copy(calendars = rows, selectedCalendar = selected, isLoading = false)
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "Failed to load calendars", e)
				_uiState.update {
					it.copy(isLoading = false, errorMessage = "Failed to load calendars.")
				}
			}
		}
	}

	fun selectCalendar(calendarId: Long) {
		val selection = _uiState.value.calendars.firstOrNull { it.calendar.id == calendarId }
		_uiState.update { it.copy(selectedCalendar = selection) }
	}

	fun clearSelection() {
		_uiState.update { it.copy(selectedCalendar = null) }
	}

	fun updateCalendarName(calendar: CalendarInfo, newName: String) {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val cleanName = sanitizeCalendarName(newName)
				val updated = calendarRepository.updateCalendarName(
					getApplication<Application>().contentResolver,
					calendar,
					cleanName
				)
				if (!updated) {
					Log.w(TAG, "Calendar name update failed for ${calendar.id}")
					postToast("Unable to rename calendar.")
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "Calendar name update failed", e)
				postToast("Unable to rename calendar.")
			}
			refreshCalendars()
		}
	}

	fun updateCalendarColor(calendar: CalendarInfo, color: Int) {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val updated = calendarRepository.updateCalendarColor(
					getApplication<Application>().contentResolver,
					calendar,
					color
				)
				if (!updated) {
					Log.w(TAG, "Calendar color update failed for ${calendar.id}")
					postToast("Unable to change calendar color.")
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "Calendar color update failed", e)
				postToast("Unable to change calendar color.")
			}
			refreshCalendars()
		}
	}

	fun purgeCalendar(calendar: CalendarInfo) {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val deleted = calendarRepository.purgeEvents(
					getApplication<Application>().contentResolver,
					calendar.id
				)
				Log.i(TAG, "Purged $deleted events for calendar ${calendar.id}")
			} catch (e: RuntimeException) {
				Log.e(TAG, "Calendar purge failed", e)
				postToast("Unable to purge calendar events.")
			}
			refreshCalendars()
		}
	}

	fun deleteCalendar(calendar: CalendarInfo) {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val deleted = calendarRepository.deleteCalendar(
					getApplication<Application>().contentResolver,
					calendar
				)
				if (!deleted) {
					Log.w(TAG, "Calendar delete failed for ${calendar.id}")
					postToast("Unable to delete calendar.")
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "Calendar delete failed", e)
				postToast("Unable to delete calendar.")
			}
			refreshCalendars()
		}
	}

	fun createCalendar(displayName: String, color: Int) {
		viewModelScope.launch(Dispatchers.IO) {
			val cleanName = sanitizeCalendarName(displayName)
			val accountName = calendarRepository.resolveLocalAccountName(getApplication())
			try {
				val uri = calendarRepository.createLocalCalendar(
					getApplication<Application>().contentResolver,
					cleanName,
					color,
					accountName
				)
				if (uri == null) {
					Log.w(TAG, "Calendar creation failed for $displayName")
					postToast("Unable to create calendar.")
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "Calendar creation failed", e)
				postToast("Unable to create calendar.")
			}
			refreshCalendars()
		}
	}

	fun clearToast() {
		_uiState.update { it.copy(toastMessage = null) }
	}

	private fun postToast(message: String) {
		_uiState.update { it.copy(toastMessage = message) }
	}

	companion object {
		private const val TAG = "CalendarManagement"
	}
}
