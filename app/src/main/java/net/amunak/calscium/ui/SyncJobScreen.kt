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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import net.amunak.calscium.data.SyncJob
import net.amunak.calscium.ui.components.CreateJobDialog
import net.amunak.calscium.ui.components.SyncJobRow
import net.amunak.calscium.ui.theme.CalsciumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncJobScreen(
	uiState: SyncJobUiState,
	onRequestPermissions: () -> Unit,
	onRefreshCalendars: () -> Unit,
	onCreateJob: (Long, Long) -> Unit,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit,
	onManualSync: (SyncJob) -> Unit
) {
	var showCreateDialog by remember { mutableStateOf(false) }
	val calendarById = remember(uiState.calendars) {
		uiState.calendars.associateBy { it.id }
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
					Text("+")
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

			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(
					text = "Sync jobs",
					style = MaterialTheme.typography.titleMedium
				)
				OutlinedButton(
					onClick = onRefreshCalendars,
					enabled = !uiState.isRefreshing
				) {
					Text(if (uiState.isRefreshing) "Refreshing..." else "Refresh calendars")
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
			onSave = { sourceId, targetId ->
				onCreateJob(sourceId, targetId)
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
			onCreateJob = { _, _ -> },
			onToggleActive = { _, _ -> },
			onDeleteJob = {},
			onManualSync = {}
		)
	}
}
