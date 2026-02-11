package net.amunak.calsynx.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.R
import net.amunak.calsynx.ui.formatters.formatLastSync
import net.amunak.calsynx.ui.formatters.formatSyncCounts
import net.amunak.calsynx.ui.formatters.formatFrequency
import net.amunak.calsynx.ui.theme.CalsynxTheme
import net.amunak.calsynx.ui.components.sanitizeCalendarName
import androidx.compose.ui.res.stringResource

@Composable
fun SyncJobRow(
	job: SyncJob,
	sourceName: String,
	targetName: String,
	sourceColor: Int?,
	targetColor: Int?,
	isMissingCalendar: Boolean,
	isSyncing: Boolean,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit,
	onDeleteSyncedTargets: (SyncJob) -> Unit,
	onEditJob: (SyncJob) -> Unit,
	onManualSync: (SyncJob) -> Unit
) {
	var menuExpanded by remember { mutableStateOf(false) }
	var showDeleteConfirm by remember { mutableStateOf(false) }
	var showDeleteSyncedConfirm by remember { mutableStateOf(false) }
	val pulse = remember { Animatable(0f) }
	var pulseTrigger by remember { mutableStateOf(0) }
	val resources = LocalContext.current.resources

	// Pulse on click so short syncs still show feedback.
	LaunchedEffect(pulseTrigger) {
		if (pulseTrigger == 0) return@LaunchedEffect
		pulse.snapTo(0f)
		pulse.animateTo(1f, animationSpec = tween(durationMillis = 180))
		pulse.animateTo(0f, animationSpec = tween(durationMillis = 240))
	}
	val baseColor = when {
		isMissingCalendar -> MaterialTheme.colorScheme.errorContainer
		!job.isActive -> MaterialTheme.colorScheme.surfaceVariant
		else -> MaterialTheme.colorScheme.surface
	}
	val animatedBaseColor by animateColorAsState(
		targetValue = baseColor,
		animationSpec = tween(durationMillis = 220),
		label = stringResource(R.string.label_sync_job_base_color)
	)
	val cardColor = lerp(
		start = animatedBaseColor,
		stop = MaterialTheme.colorScheme.primaryContainer,
		fraction = pulse.value * 0.35f
	)
	val displaySourceName = sanitizeCalendarName(sourceName)
	val displayTargetName = sanitizeCalendarName(targetName)
	ElevatedCard(
		modifier = Modifier.fillMaxWidth(),
		colors = CardDefaults.elevatedCardColors(
			containerColor = cardColor
		)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp)
		) {
			Column {
				Row(verticalAlignment = Alignment.CenterVertically) {
					CalendarDot(color = sourceColor)
					Text(
						text = displaySourceName,
						style = MaterialTheme.typography.titleMedium
					)
					Icon(
						imageVector = Icons.AutoMirrored.Filled.ArrowForward,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier
							.padding(horizontal = 6.dp)
							.size(16.dp)
					)
					CalendarDot(color = targetColor)
					Text(
						text = displayTargetName,
						style = MaterialTheme.typography.titleMedium
					)
				}
				Spacer(modifier = Modifier.height(6.dp))
				Text(
					text = formatLastSync(resources, job.lastSyncTimestamp, isSyncing),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			Spacer(modifier = Modifier.height(12.dp))
			Text(
				text = formatSyncCounts(
					resources,
					created = job.lastSyncCreated,
					updated = job.lastSyncUpdated,
					deleted = job.lastSyncDeleted,
					sourceCount = job.lastSyncSourceCount,
					targetCount = job.lastSyncTargetCount
				),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			if (job.lastSyncUnpairedTargetCount > 0) {
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = stringResource(
						R.string.text_unpaired_target,
						job.lastSyncUnpairedTargetCount
					),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
			job.lastSyncError?.let { error ->
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = stringResource(R.string.label_last_error, error),
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodySmall
				)
			}
			Spacer(modifier = Modifier.height(12.dp))
			if (isMissingCalendar) {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.End
				) {
					FilledTonalButton(
						onClick = { showDeleteConfirm = true },
						colors = ButtonDefaults.filledTonalButtonColors(
							containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
							contentColor = MaterialTheme.colorScheme.onSurface
						)
					) {
						Icon(imageVector = Icons.Default.Delete, contentDescription = null)
						Text(
							text = stringResource(R.string.action_delete),
							modifier = Modifier.padding(start = 6.dp)
						)
					}
				}
			} else {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(10.dp),
					verticalAlignment = Alignment.CenterVertically
				) {
					FilledTonalButton(
						onClick = { onToggleActive(job, !job.isActive) },
						modifier = Modifier.weight(1f),
						colors = ButtonDefaults.filledTonalButtonColors(
							containerColor = MaterialTheme.colorScheme.secondaryContainer,
							contentColor = MaterialTheme.colorScheme.onSecondaryContainer
						)
					) {
						val icon = if (job.isActive) Icons.Default.Pause else Icons.Default.PlayArrow
						val label = if (job.isActive) {
							stringResource(R.string.action_pause)
						} else {
							stringResource(R.string.action_resume)
						}
						Icon(imageVector = icon, contentDescription = null)
						Text(text = label, modifier = Modifier.padding(start = 6.dp))
					}
					FilledTonalButton(
						onClick = {
							pulseTrigger += 1
							onManualSync(job)
						},
						enabled = !isSyncing,
						modifier = Modifier.weight(1f),
						colors = ButtonDefaults.filledTonalButtonColors(
							containerColor = MaterialTheme.colorScheme.primaryContainer,
							contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
							disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
							disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
						)
					) {
						Icon(
							imageVector = Icons.Default.Sync,
							contentDescription = null
						)
						Text(
							text = stringResource(R.string.action_sync),
							modifier = Modifier.padding(start = 6.dp)
						)
					}
					Box {
						IconButton(onClick = { menuExpanded = true }) {
							Icon(
								imageVector = Icons.Default.MoreVert,
								contentDescription = stringResource(R.string.label_job_actions)
							)
						}
						DropdownMenu(
							expanded = menuExpanded,
							onDismissRequest = { menuExpanded = false }
						) {
							DropdownMenuItem(
								text = { Text(stringResource(R.string.action_edit)) },
								leadingIcon = {
									Icon(Icons.Default.Edit, contentDescription = null)
								},
								onClick = {
									menuExpanded = false
									onEditJob(job)
								}
							)
							DropdownMenuItem(
								text = { Text(stringResource(R.string.action_purge_synced)) },
								leadingIcon = {
									Icon(Icons.Default.LayersClear, contentDescription = null)
								},
								onClick = {
									menuExpanded = false
									showDeleteSyncedConfirm = true
								}
							)
							DropdownMenuItem(
								text = { Text(stringResource(R.string.action_delete)) },
								leadingIcon = {
									Icon(Icons.Default.Delete, contentDescription = null)
								},
								onClick = {
									menuExpanded = false
									showDeleteConfirm = true
								}
							)
						}
					}
				}
			}
			Spacer(modifier = Modifier.height(8.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(
					text = formatFrequency(resources, job.frequencyMinutes),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
				)
				Text(
					text = if (job.syncAllEvents) {
						stringResource(R.string.text_sync_window_all)
					} else {
						stringResource(
							R.string.text_sync_window,
							job.windowPastDays,
							job.windowFutureDays
						)
					},
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
				)
			}
		}
	}

	if (showDeleteConfirm) {
		AlertDialog(
			onDismissRequest = { showDeleteConfirm = false },
			title = { Text(stringResource(R.string.title_delete_sync_job)) },
			text = {
				Text(
					stringResource(
						R.string.message_delete_sync_job,
						displaySourceName,
						displayTargetName
					)
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						showDeleteConfirm = false
						onDeleteJob(job)
					}
				) {
					Text(stringResource(R.string.action_delete))
				}
			},
			dismissButton = {
				TextButton(onClick = { showDeleteConfirm = false }) {
					Text(stringResource(R.string.action_cancel))
				}
			}
		)
	}

	if (showDeleteSyncedConfirm) {
		AlertDialog(
			onDismissRequest = { showDeleteSyncedConfirm = false },
			title = { Text(stringResource(R.string.title_purge_synced_events)) },
			text = {
				Text(
					stringResource(
						R.string.message_purge_synced_events,
						displaySourceName,
						displayTargetName
					)
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						showDeleteSyncedConfirm = false
						onDeleteSyncedTargets(job)
					}
				) {
					Text(stringResource(R.string.action_delete))
				}
			},
			dismissButton = {
				TextButton(onClick = { showDeleteSyncedConfirm = false }) {
					Text(stringResource(R.string.action_cancel))
				}
			}
		)
	}
}

@Composable
private fun CalendarDot(color: Int?) {
	if (color == null) return
	Box(
		modifier = Modifier
			.padding(end = 6.dp)
			.size(10.dp)
			.background(Color(color), CircleShape)
	)
}

@Preview(showBackground = true)
@Composable
private fun SyncJobRowPreview() {
	CalsynxTheme {
		SyncJobRow(
			job = SyncJob(
				id = 1L,
				sourceCalendarId = 1L,
				targetCalendarId = 2L,
				lastSyncTimestamp = System.currentTimeMillis() - 86_400_000L,
				isActive = true
			),
			sourceName = stringResource(R.string.preview_calendar_work),
			targetName = stringResource(R.string.preview_calendar_personal),
			sourceColor = 0xFF3F51B5.toInt(),
			targetColor = 0xFFFF9800.toInt(),
			isMissingCalendar = false,
			isSyncing = false,
			onToggleActive = { _, _ -> },
			onDeleteJob = {},
			onDeleteSyncedTargets = {},
			onEditJob = {},
			onManualSync = {}
		)
	}
}
