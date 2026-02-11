package net.amunak.calscium.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import net.amunak.calscium.R
import net.amunak.calscium.data.SyncJob
import net.amunak.calscium.ui.components.CreateJobDialog
import net.amunak.calscium.ui.components.SyncJobRow
import net.amunak.calscium.ui.components.sanitizeCalendarName
import net.amunak.calscium.ui.theme.CalsciumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncJobScreen(
	uiState: SyncJobUiState,
	onRequestPermissions: () -> Unit,
	onRefreshCalendars: () -> Unit,
	onCreateJob: (Long, Long, Int, Int, Int) -> Unit,
	onUpdateJob: (SyncJob, Int, Int, Int) -> Unit,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit,
	onManualSync: (SyncJob) -> Unit,
	onOpenCalendarManagement: () -> Unit
) {
	var showDialog by remember { mutableStateOf(false) }
	var editingJob by remember { mutableStateOf<SyncJob?>(null) }
	val calendarById = remember(uiState.calendars) {
		uiState.calendars.associateBy { it.id }
	}
	LaunchedEffect(showDialog) {
		if (showDialog) {
			onRefreshCalendars()
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.app_name)) },
				actions = {
					if (uiState.hasCalendarPermission) {
						IconButton(onClick = onOpenCalendarManagement) {
							Icon(
								imageVector = Icons.Default.Settings,
								contentDescription = stringResource(R.string.label_calendar_management)
							)
						}
					}
				}
			)
		},
		floatingActionButton = {
			if (uiState.hasCalendarPermission) {
				FloatingActionButton(onClick = {
					editingJob = null
					showDialog = true
				}) {
					Icon(
						imageVector = Icons.Default.Add,
						contentDescription = stringResource(R.string.action_add_sync_job)
					)
				}
			}
		}
	) { padding ->
		Surface(
			modifier = Modifier.fillMaxSize(),
			color = MaterialTheme.colorScheme.surfaceContainerLowest
		) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
					.padding(16.dp)
			) {
			if (!uiState.hasCalendarPermission) {
				Text(
					text = stringResource(R.string.label_calendar_access_required),
					style = MaterialTheme.typography.bodyMedium
				)
				Spacer(modifier = Modifier.height(12.dp))
				Button(onClick = onRequestPermissions) {
					Icon(
						imageVector = Icons.Default.CalendarToday,
						contentDescription = null
					)
					Text(
						text = stringResource(R.string.action_grant_permissions),
						modifier = Modifier.padding(start = 6.dp)
					)
				}
				return@Column
			}

			Text(
				text = stringResource(R.string.label_sync_jobs),
				style = MaterialTheme.typography.titleMedium
			)
			Spacer(modifier = Modifier.height(4.dp))
			Surface(
				modifier = Modifier.fillMaxWidth(),
				shape = MaterialTheme.shapes.large,
				tonalElevation = 2.dp,
				color = MaterialTheme.colorScheme.primaryContainer
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(14.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					Icon(
						imageVector = Icons.Default.Sync,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onPrimaryContainer
					)
					Column(modifier = Modifier.padding(start = 8.dp)) {
						Text(
							text = stringResource(R.string.label_manual_sync_only),
							style = MaterialTheme.typography.titleSmall,
							color = MaterialTheme.colorScheme.onPrimaryContainer
						)
						Text(
							text = stringResource(R.string.message_background_scheduling_later),
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onPrimaryContainer
						)
					}
				}
			}

			uiState.errorMessage?.let { message ->
				Spacer(modifier = Modifier.height(8.dp))
				Text(
					text = message,
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodySmall
				)
			}

			Spacer(modifier = Modifier.height(12.dp))

			if (uiState.jobs.isEmpty()) {
				Text(
					text = stringResource(R.string.label_no_sync_jobs),
					style = MaterialTheme.typography.bodyMedium
				)
			} else {
				LazyColumn(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(12.dp),
					contentPadding = PaddingValues(bottom = 96.dp)
				) {
					items(uiState.jobs, key = { it.id }) { job ->
						val sourceName =
							sanitizeCalendarName(
								calendarById[job.sourceCalendarId]?.displayName
									?: stringResource(
										R.string.message_unknown_calendar,
										job.sourceCalendarId
									)
							)
						val targetName =
							sanitizeCalendarName(
								calendarById[job.targetCalendarId]?.displayName
									?: stringResource(
										R.string.message_unknown_calendar,
										job.targetCalendarId
									)
							)
						SyncJobRow(
							job = job,
							sourceName = sourceName,
							targetName = targetName,
							sourceColor = calendarById[job.sourceCalendarId]?.color,
							targetColor = calendarById[job.targetCalendarId]?.color,
							isMissingCalendar = calendarById[job.sourceCalendarId] == null
								|| calendarById[job.targetCalendarId] == null,
							isSyncing = uiState.syncingJobIds.contains(job.id),
							onToggleActive = onToggleActive,
							onDeleteJob = onDeleteJob,
							onEditJob = {
								editingJob = job
								showDialog = true
							},
							onManualSync = onManualSync
						)
					}
				}
			}
			}
		}
	}

	if (showDialog) {
		CreateJobDialog(
			calendars = uiState.calendars,
			jobs = uiState.jobs,
			initialJob = editingJob,
			onDismiss = { showDialog = false },
			onCreate = { sourceId, targetId, pastDays, futureDays, frequencyMinutes ->
				onCreateJob(sourceId, targetId, pastDays, futureDays, frequencyMinutes)
				showDialog = false
			},
			onUpdate = { job, pastDays, futureDays, frequencyMinutes ->
				onUpdateJob(job, pastDays, futureDays, frequencyMinutes)
				showDialog = false
			}
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun SyncJobScreenPreview() {
	CalsciumTheme {
		SyncJobScreen(
			uiState = SyncJobUiState(
				jobs = PreviewData.jobs(),
				calendars = PreviewData.calendars(),
				hasCalendarPermission = true
			),
			onRequestPermissions = {},
			onRefreshCalendars = {},
			onCreateJob = { _, _, _, _, _ -> },
			onUpdateJob = { _, _, _, _ -> },
			onToggleActive = { _, _ -> },
			onDeleteJob = {},
			onManualSync = {},
			onOpenCalendarManagement = {}
		)
	}
}
