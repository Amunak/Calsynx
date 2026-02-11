package net.amunak.calsynx.ui

import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.amunak.calsynx.R
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.ui.components.ToastMessage
import net.amunak.calsynx.ui.calendar.CalendarManagementScreen
import net.amunak.calsynx.ui.calendar.CalendarManagementViewModel
import net.amunak.calsynx.ui.calendar.CalendarDetailScreen
import net.amunak.calsynx.ui.theme.CalsynxTheme

enum class AppScreen {
	SyncJobs,
	CalendarManagement,
	CalendarDetail,
	SyncLogs
}

@Composable
fun CalsynxAppRoute() {
	val viewModel: SyncJobViewModel = viewModel()
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val calendarViewModel: CalendarManagementViewModel = viewModel()
	val calendarState by calendarViewModel.uiState.collectAsStateWithLifecycle()
	val logViewModel: net.amunak.calsynx.ui.logs.SyncLogViewModel = viewModel()
	val logState by logViewModel.uiState.collectAsStateWithLifecycle()
	val context = LocalContext.current
	var currentScreen by rememberSaveable { mutableStateOf(AppScreen.SyncJobs) }

	val permissionLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { result ->
		viewModel.onPermissionChanged(result.values.all { it })
	}

	LaunchedEffect(Unit) {
		viewModel.onPermissionChanged(hasCalendarPermissions(context))
	}

	CalsynxApp(
		uiState = uiState,
		calendarState = calendarState,
		currentScreen = currentScreen,
		onNavigate = { currentScreen = it },
		onRequestPermissions = {
			permissionLauncher.launch(
				arrayOf(
					Manifest.permission.READ_CALENDAR,
					Manifest.permission.WRITE_CALENDAR
				)
			)
		},
		onRefreshCalendars = viewModel::refreshCalendars,
		onCreateJob = { sourceId, targetId, pastDays, futureDays, syncAllEvents, frequencyMinutes ->
			viewModel.createJob(
				sourceId,
				targetId,
				pastDays,
				futureDays,
				syncAllEvents,
				frequencyMinutes
			)
			calendarViewModel.refreshCalendars()
		},
		onUpdateJob = { job, pastDays, futureDays, syncAllEvents, frequencyMinutes ->
			viewModel.updateJobOptions(job, pastDays, futureDays, syncAllEvents, frequencyMinutes)
			calendarViewModel.refreshCalendars()
		},
		onToggleActive = viewModel::setJobActive,
		onDeleteJob = { job ->
			viewModel.deleteJob(job)
			calendarViewModel.refreshCalendars()
		},
		onDeleteSyncedTargets = viewModel::deleteSyncedTargets,
		onManualSync = viewModel::runManualSync,
		onRefreshCalendarsManagement = calendarViewModel::refreshCalendars,
		onSelectCalendar = {
			calendarViewModel.selectCalendar(it)
			currentScreen = AppScreen.CalendarDetail
		},
		onClearCalendarSelection = calendarViewModel::clearSelection,
		onUpdateCalendarName = calendarViewModel::updateCalendarName,
		onUpdateCalendarColor = calendarViewModel::updateCalendarColor,
		onPurgeCalendar = calendarViewModel::purgeCalendar,
		onDeleteCalendar = calendarViewModel::deleteCalendar,
	onCreateCalendar = calendarViewModel::createCalendar,
	onCalendarToastShown = calendarViewModel::clearToast,
	onOpenLogs = { currentScreen = AppScreen.SyncLogs },
	logState = logState,
	onClearLogToast = logViewModel::clearToast,
	onClearLogs = logViewModel::clearLogs,
	onShareLogs = logViewModel::shareLogs,
	onRefreshLogs = logViewModel::loadLogs
)
}

@Composable
fun CalsynxApp(
	uiState: SyncJobUiState,
	calendarState: net.amunak.calsynx.ui.calendar.CalendarManagementUiState,
	currentScreen: AppScreen,
	onNavigate: (AppScreen) -> Unit,
	onRequestPermissions: () -> Unit,
	onRefreshCalendars: () -> Unit,
	onCreateJob: (Long, Long, Int, Int, Boolean, Int) -> Unit,
	onUpdateJob: (SyncJob, Int, Int, Boolean, Int) -> Unit,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit,
	onDeleteSyncedTargets: (SyncJob) -> Unit,
	onManualSync: (SyncJob) -> Unit,
	onRefreshCalendarsManagement: () -> Unit,
	onSelectCalendar: (Long) -> Unit,
	onClearCalendarSelection: () -> Unit,
	onUpdateCalendarName: (net.amunak.calsynx.calendar.CalendarInfo, String) -> Unit,
	onUpdateCalendarColor: (net.amunak.calsynx.calendar.CalendarInfo, Int) -> Unit,
	onPurgeCalendar: (net.amunak.calsynx.calendar.CalendarInfo) -> Unit,
	onDeleteCalendar: (net.amunak.calsynx.calendar.CalendarInfo) -> Unit,
	onCreateCalendar: (String, Int) -> Unit,
	onCalendarToastShown: () -> Unit,
	onOpenLogs: () -> Unit,
	logState: net.amunak.calsynx.ui.logs.SyncLogUiState,
	onClearLogToast: () -> Unit,
	onClearLogs: () -> Unit,
	onShareLogs: () -> Unit,
	onRefreshLogs: () -> Unit
) {
	ToastMessage(
		message = calendarState.toastMessage,
		onShown = onCalendarToastShown
	)
	ToastMessage(
		message = logState.toastMessage,
		onShown = onClearLogToast
	)
	val screenOrder = listOf(
		AppScreen.SyncJobs,
		AppScreen.CalendarManagement,
		AppScreen.CalendarDetail,
		AppScreen.SyncLogs
	)

	BackHandler(enabled = currentScreen != AppScreen.SyncJobs) {
		when (currentScreen) {
			AppScreen.CalendarDetail -> {
				onClearCalendarSelection()
				onNavigate(AppScreen.CalendarManagement)
			}
			AppScreen.CalendarManagement -> onNavigate(AppScreen.SyncJobs)
			AppScreen.SyncLogs -> onNavigate(AppScreen.SyncJobs)
			AppScreen.SyncJobs -> Unit
		}
	}

	AnimatedContent(
		targetState = currentScreen,
		label = stringResource(R.string.label_app_navigation),
		transitionSpec = {
			val targetIndex = screenOrder.indexOf(targetState)
			val initialIndex = screenOrder.indexOf(initialState)
			val direction = if (targetIndex >= initialIndex) 1 else -1
			slideInHorizontally { it * direction } + fadeIn() togetherWith
				slideOutHorizontally { -it * direction } + fadeOut()
		}
	) { screen ->
		when (screen) {
			AppScreen.SyncJobs -> {
				SyncJobScreen(
					uiState = uiState,
					onRequestPermissions = onRequestPermissions,
					onRefreshCalendars = onRefreshCalendars,
					onCreateJob = onCreateJob,
					onUpdateJob = onUpdateJob,
					onToggleActive = onToggleActive,
					onDeleteJob = onDeleteJob,
					onDeleteSyncedTargets = onDeleteSyncedTargets,
					onManualSync = onManualSync,
					onOpenCalendarManagement = { onNavigate(AppScreen.CalendarManagement) },
					onOpenLogs = onOpenLogs
				)
			}
			AppScreen.CalendarManagement -> {
				CalendarManagementScreen(
					state = calendarState,
					onBack = { onNavigate(AppScreen.SyncJobs) },
					onRefresh = onRefreshCalendarsManagement,
					onSelectCalendar = onSelectCalendar,
					onCreateCalendar = onCreateCalendar
				)
			}
			AppScreen.CalendarDetail -> {
				CalendarDetailScreen(
					state = calendarState,
					onBack = {
						onClearCalendarSelection()
						onNavigate(AppScreen.CalendarManagement)
					},
					onUpdateName = onUpdateCalendarName,
					onUpdateColor = onUpdateCalendarColor,
					onPurge = onPurgeCalendar,
					onDelete = onDeleteCalendar
				)
			}
			AppScreen.SyncLogs -> {
				net.amunak.calsynx.ui.logs.SyncLogScreen(
					state = logState,
					onBack = { onNavigate(AppScreen.SyncJobs) },
					onClearLogs = onClearLogs,
					onShareLogs = onShareLogs,
					onRefresh = onRefreshLogs
				)
			}
		}
	}
}

@Preview(showBackground = true)
@Composable
private fun CalsynxAppPreview() {
	CalsynxTheme {
		CalsynxApp(
			uiState = SyncJobUiState(
				jobs = PreviewData.jobs(),
				calendars = PreviewData.calendars(),
				hasCalendarPermission = true
			),
			calendarState = net.amunak.calsynx.ui.calendar.CalendarManagementUiState(),
			currentScreen = AppScreen.SyncJobs,
			onNavigate = {},
			onRequestPermissions = {},
			onRefreshCalendars = {},
			onCreateJob = { _, _, _, _, _, _ -> },
			onUpdateJob = { _, _, _, _, _ -> },
			onToggleActive = { _, _ -> },
			onDeleteJob = {},
			onDeleteSyncedTargets = {},
			onManualSync = {},
			onRefreshCalendarsManagement = {},
			onSelectCalendar = {},
			onClearCalendarSelection = {},
			onUpdateCalendarName = { _, _ -> },
			onUpdateCalendarColor = { _, _ -> },
			onPurgeCalendar = {},
			onDeleteCalendar = {},
			onCreateCalendar = { _, _ -> },
			onCalendarToastShown = {},
			onOpenLogs = {},
			logState = net.amunak.calsynx.ui.logs.SyncLogUiState(),
			onClearLogToast = {},
			onClearLogs = {},
			onShareLogs = {},
			onRefreshLogs = {}
		)
	}
}
