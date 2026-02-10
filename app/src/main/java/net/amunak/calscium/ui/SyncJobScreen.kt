package net.amunak.calscium.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import net.amunak.calscium.calendar.CalendarInfo
import net.amunak.calscium.data.SyncJob

@Composable
fun CalsciumApp() {
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

	SyncJobScreen(
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
		onToggleActive = viewModel::setJobActive,
		onDeleteJob = viewModel::deleteJob
	)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SyncJobScreen(
	uiState: SyncJobUiState,
	onRequestPermissions: () -> Unit,
	onRefreshCalendars: () -> Unit,
	onCreateJob: (Long, Long) -> Unit,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit
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
							onToggleActive = onToggleActive,
							onDeleteJob = onDeleteJob
						)
					}
				}
			}
		}
	}

	if (showCreateDialog) {
		CreateJobDialog(
			calendars = uiState.calendars,
			onDismiss = { showCreateDialog = false },
			onSave = { sourceId, targetId ->
				onCreateJob(sourceId, targetId)
				showCreateDialog = false
			}
		)
	}
}

@Composable
private fun SyncJobRow(
	job: SyncJob,
	sourceName: String,
	targetName: String,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit
) {
	Column(
		modifier = Modifier.fillMaxWidth()
	) {
		Text(
			text = "$sourceName → $targetName",
			style = MaterialTheme.typography.titleSmall
		)
		Spacer(modifier = Modifier.height(4.dp))
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
				Text("Active")
				Switch(
					checked = job.isActive,
					onCheckedChange = { onToggleActive(job, it) }
				)
			}
			TextButton(onClick = { onDeleteJob(job) }) {
				Text("Delete")
			}
		}
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateJobDialog(
	calendars: List<CalendarInfo>,
	onDismiss: () -> Unit,
	onSave: (Long, Long) -> Unit
) {
	var source by remember { mutableStateOf<CalendarInfo?>(null) }
	var target by remember { mutableStateOf<CalendarInfo?>(null) }
	var sourceExpanded by remember { mutableStateOf(false) }
	var targetExpanded by remember { mutableStateOf(false) }

	val canSave = source != null && target != null && source?.id != target?.id

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Create sync job") },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				if (calendars.isEmpty()) {
					Text("No calendars available.")
				} else {
					ExposedDropdownMenuBox(
						expanded = sourceExpanded,
						onExpandedChange = { sourceExpanded = it }
					) {
						OutlinedTextField(
							value = source?.displayName ?: "",
							onValueChange = {},
							label = { Text("Source calendar") },
							readOnly = true,
							trailingIcon = {
								ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded)
							},
							modifier = Modifier
								.menuAnchor(MenuAnchorType.PrimaryNotEditable)
								.fillMaxWidth()
						)
						ExposedDropdownMenu(
							expanded = sourceExpanded,
							onDismissRequest = { sourceExpanded = false }
						) {
							calendars.forEach { calendar ->
								androidx.compose.material3.DropdownMenuItem(
									text = { Text(calendar.displayName) },
									onClick = {
										source = calendar
										sourceExpanded = false
									}
								)
							}
						}
					}

					ExposedDropdownMenuBox(
						expanded = targetExpanded,
						onExpandedChange = { targetExpanded = it }
					) {
						OutlinedTextField(
							value = target?.displayName ?: "",
							onValueChange = {},
							label = { Text("Target calendar") },
							readOnly = true,
							trailingIcon = {
								ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded)
							},
							modifier = Modifier
								.menuAnchor(MenuAnchorType.PrimaryNotEditable)
								.fillMaxWidth()
						)
						ExposedDropdownMenu(
							expanded = targetExpanded,
							onDismissRequest = { targetExpanded = false }
						) {
							calendars.forEach { calendar ->
								androidx.compose.material3.DropdownMenuItem(
									text = { Text(calendar.displayName) },
									onClick = {
										target = calendar
										targetExpanded = false
									}
								)
							}
						}
					}

					if (source != null && target != null && source?.id == target?.id) {
						Text(
							text = "Source and target must be different.",
							color = MaterialTheme.colorScheme.error,
							style = MaterialTheme.typography.bodySmall
						)
					}
				}
			}
		},
		confirmButton = {
			Button(
				onClick = { onSave(source!!.id, target!!.id) },
				enabled = canSave
			) {
				Text("Save")
			}
		},
		dismissButton = {
			OutlinedButton(onClick = onDismiss) {
				Text("Cancel")
			}
		}
	)
}

private fun hasCalendarPermissions(context: Context): Boolean {
	val readGranted = ContextCompat.checkSelfPermission(
		context,
		Manifest.permission.READ_CALENDAR
	) == PackageManager.PERMISSION_GRANTED
	val writeGranted = ContextCompat.checkSelfPermission(
		context,
		Manifest.permission.WRITE_CALENDAR
	) == PackageManager.PERMISSION_GRANTED
	return readGranted && writeGranted
}
