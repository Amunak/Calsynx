package net.amunak.calsynx.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import net.amunak.calsynx.R
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.ui.components.SyncJobRow
import net.amunak.calsynx.ui.components.ScrollIndicator
import net.amunak.calsynx.ui.components.ScreenSurface
import net.amunak.calsynx.ui.components.sanitizeCalendarName
import net.amunak.calsynx.ui.components.TooltipIconButton
import net.amunak.calsynx.ui.components.WarningCard
import net.amunak.calsynx.ui.components.rememberNavBarPadding
import androidx.compose.material3.TextButton
import android.os.Build
import android.os.PowerManager
import android.content.Intent
import android.provider.Settings
import android.net.Uri
import net.amunak.calsynx.ui.editor.SyncJobEditorActivity
import net.amunak.calsynx.ui.theme.CalsynxTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncJobScreen(
	uiState: SyncJobUiState,
	onRequestPermissions: () -> Unit,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit,
	onDeleteSyncedTargets: (SyncJob) -> Unit,
	onManualSync: (SyncJob) -> Unit,
	onOpenCalendarManagement: () -> Unit,
	onOpenLogs: () -> Unit
) {
	val context = LocalContext.current
	val lifecycleOwner = LocalLifecycleOwner.current
	val calendarById = remember(uiState.calendars) {
		uiState.calendars.associateBy { it.id }
	}
	val listState = rememberLazyListState()
	var resumeTick by remember { mutableIntStateOf(0) }
	DisposableEffect(lifecycleOwner) {
		val observer = LifecycleEventObserver { _, event ->
			if (event == Lifecycle.Event.ON_RESUME) {
				resumeTick += 1
			}
		}
		lifecycleOwner.lifecycle.addObserver(observer)
		onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
	}
	val shouldShowBatteryWarning = remember(uiState.hasCalendarPermission, resumeTick) {
		if (!uiState.hasCalendarPermission) {
			false
		} else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
			false
		} else {
			val powerManager = context.getSystemService(PowerManager::class.java)
			powerManager == null || !powerManager.isIgnoringBatteryOptimizations(context.packageName)
		}
	}

	val bottomPadding = if (uiState.hasCalendarPermission) 96.dp else 16.dp
	val navBar = rememberNavBarPadding()
	val navBarBottom = navBar.bottom
	val navBarEnd = navBar.end
	val contentBottomPadding = bottomPadding + navBarBottom

	Scaffold(
		contentWindowInsets = WindowInsets(0, 0, 0, 0),
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
						TooltipIconButton(
							tooltip = stringResource(R.string.label_view_logs),
							onClick = onOpenLogs
						) {
							Icon(
								imageVector = Icons.Default.BugReport,
								contentDescription = stringResource(R.string.label_view_logs)
							)
						}
						TooltipIconButton(
							tooltip = stringResource(R.string.label_calendar_management),
							onClick = onOpenCalendarManagement
						) {
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
				val fabBottomPadding = navBar.bottom + 12.dp
				ExtendedFloatingActionButton(
					onClick = { context.startActivity(SyncJobEditorActivity.newIntent(context)) },
					modifier = Modifier
						.padding(bottom = fabBottomPadding, end = navBarEnd)
				) {
					Icon(
						imageVector = Icons.Default.Add,
						contentDescription = stringResource(R.string.action_add_sync_job)
					)
					Text(
						text = stringResource(R.string.action_add_sync_job),
						modifier = Modifier.padding(start = 6.dp)
					)
				}
			}
		}
	) { padding ->
		ScreenSurface {
			Box(modifier = Modifier.fillMaxSize()) {
				LazyColumn(
					state = listState,
					modifier = Modifier
						.fillMaxSize()
						.padding(padding),
					verticalArrangement = Arrangement.spacedBy(12.dp),
					contentPadding = PaddingValues(
						start = 16.dp,
						end = 16.dp,
						top = 16.dp,
						bottom = contentBottomPadding
					)
				) {
					if (!uiState.hasCalendarPermission) {
						item {
							WarningCard(
								title = stringResource(R.string.label_calendar_access_title),
								message = stringResource(R.string.label_calendar_access_required)
							) {}
						}
						item {
							Box(
								modifier = Modifier
									.fillMaxWidth()
									.heightIn(min = 240.dp),
								contentAlignment = Alignment.Center
							) {
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
						}
					} else {
						if (shouldShowBatteryWarning) {
							item {
								WarningCard(
									title = stringResource(R.string.label_battery_optimization_title),
									message = stringResource(R.string.message_battery_optimization_warning)
								) {
									TextButton(
										onClick = {
											val intent = Intent(
												Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
												Uri.parse("package:${context.packageName}")
											)
											context.startActivity(intent)
										}
									) {
										Text(stringResource(R.string.action_allow_background_sync))
									}
								}
							}
						}
						item {
							Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
								Text(
									text = stringResource(R.string.label_sync_jobs),
									style = MaterialTheme.typography.titleMedium
								)
								uiState.errorMessage?.let { message ->
									Row(verticalAlignment = Alignment.CenterVertically) {
										Icon(
											imageVector = Icons.Default.Warning,
											contentDescription = null,
											tint = MaterialTheme.colorScheme.error,
											modifier = Modifier
												.size(16.dp)
												.padding(end = 6.dp)
										)
										Text(
											text = message,
											color = MaterialTheme.colorScheme.error,
											style = MaterialTheme.typography.bodySmall
										)
									}
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
										context.startActivity(
											SyncJobEditorActivity.newIntent(context, job.id)
										)
									},
									onManualSync = onManualSync
								)
							}
						}
					}
				}
				ScrollIndicator(
					state = listState,
					modifier = Modifier
						.align(Alignment.CenterEnd)
						.padding(top = padding.calculateTopPadding())
						.padding(bottom = navBarBottom)
						.padding(end = navBarEnd + 2.dp)
				)
			}
		}
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
			onToggleActive = { _, _ -> },
			onDeleteJob = {},
			onDeleteSyncedTargets = {},
			onManualSync = {},
			onOpenCalendarManagement = {},
			onOpenLogs = {}
		)
	}
}
