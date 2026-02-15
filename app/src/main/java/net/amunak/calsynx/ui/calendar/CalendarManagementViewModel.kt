package net.amunak.calsynx.ui.calendar

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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.amunak.calsynx.calendar.CalendarInfo
import net.amunak.calsynx.data.DatabaseProvider
import net.amunak.calsynx.data.ics.CalendarIcsExporter
import net.amunak.calsynx.data.ics.CalendarIcsImporter
import net.amunak.calsynx.data.repository.CalendarRepository
import net.amunak.calsynx.data.repository.SyncJobRepository
import net.amunak.calsynx.ui.components.sanitizeCalendarName
import net.amunak.calsynx.R
import android.content.ContentUris
import android.net.Uri

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
	private val icsExporter = CalendarIcsExporter()
	private val icsImporter = CalendarIcsImporter()
	private val refreshMutex = Mutex()

	private val _uiState = MutableStateFlow(CalendarManagementUiState())
	val uiState: StateFlow<CalendarManagementUiState> = _uiState.asStateFlow()

	init {
		// Refresh calendar stats whenever sync jobs change.
		viewModelScope.launch(Dispatchers.IO) {
			syncJobRepository.observeJobs().collect {
				refreshCalendarsInternal()
			}
		}
	}

	fun refreshCalendars() {
		viewModelScope.launch(Dispatchers.IO) {
			refreshCalendarsInternal()
		}
	}

	private suspend fun refreshCalendarsInternal() {
		refreshMutex.withLock {
			_uiState.update { it.copy(isLoading = true, errorMessage = null) }
			try {
				val calendars = calendarRepository.getCalendars(getApplication(), onlyVisible = false)
				val calendarById = calendars.associateBy { it.id }
				val jobs = syncJobRepository.getAll()
				val resolver = getApplication<Application>().contentResolver
				val eventCounts = calendarRepository.countEventsByCalendar(
					resolver,
					calendars.map { it.id }
				)
				val syncedCounts = mappingDao.countSyncedTargetsByCalendar()
					.associate { it.calendarId to it.count }
				val incomingByTarget = jobs.groupBy { it.targetCalendarId }
				val outgoingBySource = jobs.groupBy { it.sourceCalendarId }
				val rows = calendars.map { calendar ->
					val incoming = incomingByTarget[calendar.id].orEmpty()
						.mapNotNull { job -> calendarById[job.sourceCalendarId] }
					val outgoing = outgoingBySource[calendar.id].orEmpty()
						.mapNotNull { job -> calendarById[job.targetCalendarId] }
					CalendarRowUi(
						calendar = calendar,
						eventCount = eventCounts[calendar.id] ?: 0,
						syncedCount = syncedCounts[calendar.id] ?: 0,
						incomingCalendars = incoming,
						outgoingCalendars = outgoing
					)
				}
				_uiState.update { state ->
					val selectedId = state.selectedCalendar?.calendar?.id
					val selected = rows.firstOrNull { it.calendar.id == selectedId }
					state.copy(calendars = rows, selectedCalendar = selected, isLoading = false)
				}
			} catch (e: SecurityException) {
				Log.e(TAG, "Calendar permission denied while loading calendars", e)
				_uiState.update {
					it.copy(
						isLoading = false,
						errorMessage = getApplication<Application>()
							.getString(R.string.message_calendar_permission_denied)
					)
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "Failed to load calendars", e)
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
					postToast(
						getApplication<Application>()
							.getString(R.string.message_unable_rename_calendar)
					)
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "Calendar name update failed", e)
				postToast(
					getApplication<Application>()
						.getString(R.string.message_unable_rename_calendar)
				)
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
					postToast(
						getApplication<Application>()
							.getString(R.string.message_unable_change_color)
					)
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "Calendar color update failed", e)
				postToast(
					getApplication<Application>()
						.getString(R.string.message_unable_change_color)
				)
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
				postToast(
					getApplication<Application>()
						.getString(R.string.message_unable_purge_calendar)
				)
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
					postToast(
						getApplication<Application>()
							.getString(R.string.message_unable_delete_calendar)
					)
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "Calendar delete failed", e)
				postToast(
					getApplication<Application>()
						.getString(R.string.message_unable_delete_calendar)
				)
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
					postToast(
						getApplication<Application>()
							.getString(R.string.message_unable_create_calendar)
					)
				}
			} catch (e: RuntimeException) {
				Log.e(TAG, "Calendar creation failed", e)
				postToast(
					getApplication<Application>()
						.getString(R.string.message_unable_create_calendar)
				)
			}
			refreshCalendars()
		}
	}

	fun exportCalendar(calendar: CalendarInfo, uri: Uri) {
		viewModelScope.launch(Dispatchers.IO) {
			try {
				val resolver = getApplication<Application>().contentResolver
				val stream = resolver.openOutputStream(uri)
				if (stream == null) {
					postToast(getApplication<Application>().getString(R.string.message_export_calendar_failed))
					return@launch
				}
				val result = stream.use { output ->
					icsExporter.exportCalendar(resolver, calendar, output)
				}
				postToast(
					getApplication<Application>().getString(
						R.string.message_export_calendar_success,
						result.eventCount
					)
				)
			} catch (e: RuntimeException) {
				Log.e(TAG, "Calendar export failed for ${calendar.id}", e)
				postToast(getApplication<Application>().getString(R.string.message_export_calendar_failed))
			}
		}
	}

	fun importCalendar(uri: Uri, name: String, color: Int) {
		viewModelScope.launch(Dispatchers.IO) {
			val resolver = getApplication<Application>().contentResolver
			try {
				val cleanName = sanitizeCalendarName(name).ifBlank {
					getApplication<Application>().getString(R.string.dialog_calendar_name_default)
				}
				val accountName = calendarRepository.resolveLocalAccountName(getApplication())
				val calendarUri = calendarRepository.createLocalCalendar(
					resolver,
					cleanName,
					color,
					accountName
				)
				if (calendarUri == null) {
					postToast(getApplication<Application>().getString(R.string.message_import_calendar_failed))
					return@launch
				}
				val calendarId = ContentUris.parseId(calendarUri)
				val stream = resolver.openInputStream(uri)
				if (stream == null) {
					postToast(getApplication<Application>().getString(R.string.message_import_calendar_failed))
					return@launch
				}
				val result = stream.use { input ->
					icsImporter.importCalendar(resolver, calendarId, input)
				}
				postToast(
					getApplication<Application>().getString(
						R.string.message_import_calendar_success,
						result.eventCount
					)
				)
			} catch (e: RuntimeException) {
				Log.e(TAG, "Calendar import failed", e)
				postToast(getApplication<Application>().getString(R.string.message_import_calendar_failed))
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
