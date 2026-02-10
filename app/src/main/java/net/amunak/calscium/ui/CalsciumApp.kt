package net.amunak.calscium.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.amunak.calscium.data.SyncJob
import net.amunak.calscium.ui.theme.CalsciumTheme

@Composable
fun CalsciumAppRoute() {
	val viewModel: SyncJobViewModel = viewModel()
	val uiState by viewModel.uiState.collectAsStateWithLifecycle()
	val context = LocalContext.current

	val permissionLauncher = rememberLauncherForActivityResult(
		ActivityResultContracts.RequestMultiplePermissions()
	) { result ->
		viewModel.onPermissionChanged(result.values.all { it })
	}

	LaunchedEffect(Unit) {
		viewModel.onPermissionChanged(hasCalendarPermissions(context))
	}

	CalsciumApp(
		uiState = uiState,
		onRequestPermissions = {
			permissionLauncher.launch(
				arrayOf(
					Manifest.permission.READ_CALENDAR,
					Manifest.permission.WRITE_CALENDAR
				)
			)
		},
		onRefreshCalendars = viewModel::refreshCalendars,
		onCreateJob = viewModel::createJob,
		onUpdateJob = viewModel::updateJobOptions,
		onToggleActive = viewModel::setJobActive,
		onDeleteJob = viewModel::deleteJob,
		onManualSync = viewModel::runManualSync
	)
}

@Composable
fun CalsciumApp(
	uiState: SyncJobUiState,
	onRequestPermissions: () -> Unit,
	onRefreshCalendars: () -> Unit,
	onCreateJob: (Long, Long, Int, Int, Int) -> Unit,
	onUpdateJob: (SyncJob, Int, Int, Int) -> Unit,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit,
	onManualSync: (SyncJob) -> Unit
) {
	SyncJobScreen(
		uiState = uiState,
		onRequestPermissions = onRequestPermissions,
		onRefreshCalendars = onRefreshCalendars,
		onCreateJob = onCreateJob,
		onUpdateJob = onUpdateJob,
		onToggleActive = onToggleActive,
		onDeleteJob = onDeleteJob,
		onManualSync = onManualSync
	)
}

@Preview(showBackground = true)
@Composable
private fun CalsciumAppPreview() {
	CalsciumTheme {
		CalsciumApp(
			uiState = SyncJobUiState(
				jobs = PreviewData.jobs,
				calendars = PreviewData.calendars,
				hasCalendarPermission = true
			),
			onRequestPermissions = {},
			onRefreshCalendars = {},
			onCreateJob = { _, _, _, _, _ -> },
			onUpdateJob = { _, _, _, _ -> },
			onToggleActive = { _, _ -> },
			onDeleteJob = {},
			onManualSync = {}
		)
	}
}
