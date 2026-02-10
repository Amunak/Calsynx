package net.amunak.calscium.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import net.amunak.calscium.data.SyncJob
import net.amunak.calscium.ui.components.SyncFrequencyOption
import net.amunak.calscium.ui.components.SyncWindowOption
import net.amunak.calscium.ui.components.CreateJobDialog
import net.amunak.calscium.ui.components.SyncJobRow
import net.amunak.calscium.ui.theme.CalsciumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncJobScreen(
	uiState: SyncJobUiState,
	onRequestPermissions: () -> Unit,
	onRefreshCalendars: () -> Unit,
	onCreateJob: (Long, Long, SyncWindowOption, SyncFrequencyOption) -> Unit,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit,
	onManualSync: (SyncJob) -> Unit
) {
	var showCreateDialog by remember { mutableStateOf(false) }
	val calendarById = remember(uiState.calendars) {
		uiState.calendars.associateBy { it.id }
	}
	LaunchedEffect(showCreateDialog) {
		if (showCreateDialog) {
			onRefreshCalendars()
		}
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text("Calscium") }
			)
		},
		floatingActionButton = {
			if (uiState.hasCalendarPermission) {
				FloatingActionButton(onClick = { showCreateDialog = true }) {
					Icon(
						imageVector = Icons.Default.Add,
						contentDescription = "Add sync job"
					)
				}
			}
		}
	) { padding ->
		Column(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.padding(16.dp)
		) {
			if (!uiState.hasCalendarPermission) {
				Text(
					text = "Calendar access is required to list calendars and sync events.",
					style = MaterialTheme.typography.bodyMedium
				)
				Spacer(modifier = Modifier.height(12.dp))
				Button(onClick = onRequestPermissions) {
					Text("Grant permissions")
				}
				return@Column
			}

			Text(
				text = "Sync jobs",
				style = MaterialTheme.typography.titleMedium
			)
			Spacer(modifier = Modifier.height(4.dp))
			Text(
				text = "Manual sync is available until background scheduling is enabled.",
				style = MaterialTheme.typography.bodySmall
			)

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
					text = "No sync jobs yet. Tap + to create one.",
					style = MaterialTheme.typography.bodyMedium
				)
			} else {
				LazyColumn(
					modifier = Modifier.fillMaxWidth(),
					verticalArrangement = Arrangement.spacedBy(12.dp)
				) {
					items(uiState.jobs, key = { it.id }) { job ->
						val sourceName =
							calendarById[job.sourceCalendarId]?.displayName
								?: "Unknown (${job.sourceCalendarId})"
						val targetName =
							calendarById[job.targetCalendarId]?.displayName
								?: "Unknown (${job.targetCalendarId})"
						SyncJobRow(
							job = job,
							sourceName = sourceName,
							targetName = targetName,
							isSyncing = uiState.syncingJobIds.contains(job.id),
							onToggleActive = onToggleActive,
							onDeleteJob = onDeleteJob,
							onManualSync = onManualSync
						)
					}
				}
			}
		}
	}

	if (showCreateDialog) {
		CreateJobDialog(
			calendars = uiState.calendars,
			jobs = uiState.jobs,
			onDismiss = { showCreateDialog = false },
			onSave = { sourceId, targetId, window, frequency ->
				onCreateJob(sourceId, targetId, window, frequency)
				showCreateDialog = false
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
				jobs = PreviewData.jobs,
				calendars = PreviewData.calendars,
				hasCalendarPermission = true
			),
			onRequestPermissions = {},
			onRefreshCalendars = {},
			onCreateJob = { _, _, _, _ -> },
			onToggleActive = { _, _ -> },
			onDeleteJob = {},
			onManualSync = {}
		)
	}
}
