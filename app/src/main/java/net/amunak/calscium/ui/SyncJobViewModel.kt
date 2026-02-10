package net.amunak.calscium.ui

import android.app.Application
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
import net.amunak.calscium.calendar.CalendarProvider
import net.amunak.calscium.data.DatabaseProvider
import net.amunak.calscium.data.SyncJob

data class SyncJobUiState(
	val jobs: List<SyncJob> = emptyList(),
	val calendars: List<CalendarInfo> = emptyList(),
	val hasCalendarPermission: Boolean = false,
	val isRefreshing: Boolean = false,
	val errorMessage: String? = null
)

class SyncJobViewModel(private val app: Application) : AndroidViewModel(app) {
	private val syncJobDao = DatabaseProvider.get(app).syncJobDao()

	private val calendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
	private val hasCalendarPermission = MutableStateFlow(false)
	private val isRefreshing = MutableStateFlow(false)
	private val errorMessage = MutableStateFlow<String?>(null)

	val uiState: StateFlow<SyncJobUiState> = combine(
		syncJobDao.getAllFlow(),
		calendars,
		hasCalendarPermission,
		isRefreshing,
		errorMessage
	) { jobs, calendars, hasPermission, refreshing, error ->
		SyncJobUiState(
			jobs = jobs,
			calendars = calendars,
			hasCalendarPermission = hasPermission,
			isRefreshing = refreshing,
			errorMessage = error
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
				calendars.value = CalendarProvider.getCalendars(app)
			} catch (e: SecurityException) {
				calendars.value = emptyList()
				errorMessage.value = "Calendar permission denied."
			} catch (e: RuntimeException) {
				errorMessage.value = "Failed to load calendars."
			} finally {
				isRefreshing.value = false
			}
		}
	}

	fun createJob(sourceId: Long, targetId: Long) {
		viewModelScope.launch(Dispatchers.IO) {
			syncJobDao.upsert(
				SyncJob(
					sourceCalendarId = sourceId,
					targetCalendarId = targetId
				)
			)
		}
	}

	fun setJobActive(job: SyncJob, isActive: Boolean) {
		viewModelScope.launch(Dispatchers.IO) {
			syncJobDao.upsert(job.copy(isActive = isActive))
		}
	}

	fun deleteJob(job: SyncJob) {
		viewModelScope.launch(Dispatchers.IO) {
			syncJobDao.delete(job)
		}
	}
}
