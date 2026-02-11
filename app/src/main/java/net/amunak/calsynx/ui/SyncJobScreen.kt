package net.amunak.calsynx.ui

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.stringResource
import net.amunak.calsynx.R
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.ui.components.CreateJobDialog
import net.amunak.calsynx.ui.components.SyncJobRow
import net.amunak.calsynx.ui.components.sanitizeCalendarName
import net.amunak.calsynx.ui.theme.CalsynxTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncJobScreen(
	uiState: SyncJobUiState,
	onRequestPermissions: () -> Unit,
	onRefreshCalendars: () -> Unit,
	onCreateJob: (Long, Long, Int, Int, Boolean, Int) -> Unit,
	onUpdateJob: (SyncJob, Int, Int, Boolean, Int) -> Unit,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit,
	onDeleteSyncedTargets: (SyncJob) -> Unit,
	onManualSync: (SyncJob) -> Unit,
	onOpenCalendarManagement: () -> Unit,
	onOpenLogs: () -> Unit
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
				title = {
					Row(
						verticalAlignment = Alignment.CenterVertically
					) {
						Icon(
							painter = androidx.compose.ui.res.painterResource(R.drawable.ic_app_logo),
							contentDescription = null,
							tint = MaterialTheme.colorScheme.primary
						)
						Text(
							text = stringResource(R.string.app_name),
							modifier = Modifier.padding(start = 8.dp)
						)
					}
				},
				actions = {
					if (uiState.hasCalendarPermission) {
						IconButton(onClick = onOpenLogs) {
							Icon(
								imageVector = Icons.Default.BugReport,
								contentDescription = stringResource(R.string.label_view_logs)
							)
						}
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
			if (!uiState.hasCalendarPermission) {
				Column(
					modifier = Modifier
						.fillMaxSize()
						.padding(padding)
						.padding(16.dp)
						.verticalScroll(rememberScrollState())
				) {
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
				}
			} else {
				LazyColumn(
					modifier = Modifier
						.fillMaxSize()
						.padding(padding),
					verticalArrangement = Arrangement.spacedBy(12.dp),
					contentPadding = PaddingValues(
						start = 16.dp,
						end = 16.dp,
						top = 16.dp,
						bottom = 96.dp
					)
				) {
					item {
						Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
							Text(
								text = stringResource(R.string.label_sync_jobs),
								style = MaterialTheme.typography.titleMedium
							)
							uiState.errorMessage?.let { message ->
								Text(
									text = message,
									color = MaterialTheme.colorScheme.error,
									style = MaterialTheme.typography.bodySmall
								)
							}
						}
					}
					if (uiState.jobs.isEmpty()) {
						item {
							Text(
								text = stringResource(R.string.label_no_sync_jobs),
								style = MaterialTheme.typography.bodyMedium
							)
						}
					} else {
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
								onDeleteSyncedTargets = onDeleteSyncedTargets,
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
			onCreate = { sourceId, targetId, pastDays, futureDays, syncAllEvents, frequencyMinutes ->
				onCreateJob(sourceId, targetId, pastDays, futureDays, syncAllEvents, frequencyMinutes)
				showDialog = false
			},
			onUpdate = { job, pastDays, futureDays, syncAllEvents, frequencyMinutes ->
				onUpdateJob(job, pastDays, futureDays, syncAllEvents, frequencyMinutes)
				showDialog = false
			}
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun SyncJobScreenPreview() {
	CalsynxTheme {
		SyncJobScreen(
			uiState = SyncJobUiState(
				jobs = PreviewData.jobs(),
				calendars = PreviewData.calendars(),
				hasCalendarPermission = true
			),
			onRequestPermissions = {},
			onRefreshCalendars = {},
			onCreateJob = { _, _, _, _, _, _ -> },
			onUpdateJob = { _, _, _, _, _ -> },
			onToggleActive = { _, _ -> },
			onDeleteJob = {},
			onDeleteSyncedTargets = {},
			onManualSync = {},
			onOpenCalendarManagement = {},
			onOpenLogs = {}
		)
	}
}
