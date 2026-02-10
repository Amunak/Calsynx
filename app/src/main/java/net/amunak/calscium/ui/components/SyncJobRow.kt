package net.amunak.calscium.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import net.amunak.calscium.data.SyncJob
import net.amunak.calscium.ui.formatters.formatLastSync
import net.amunak.calscium.ui.formatters.formatSyncCounts
import net.amunak.calscium.ui.formatters.formatFrequency
import net.amunak.calscium.ui.theme.CalsciumTheme

@Composable
fun SyncJobRow(
	job: SyncJob,
	sourceName: String,
	targetName: String,
	sourceColor: Int?,
	targetColor: Int?,
	isSyncing: Boolean,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit,
	onEditJob: (SyncJob) -> Unit,
	onManualSync: (SyncJob) -> Unit
) {
	var menuExpanded by remember { mutableStateOf(false) }
	var showDeleteConfirm by remember { mutableStateOf(false) }

	ElevatedCard(
		modifier = Modifier.fillMaxWidth()
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
						text = sourceName,
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
						text = targetName,
						style = MaterialTheme.typography.titleMedium
					)
				}
				Spacer(modifier = Modifier.height(6.dp))
				Text(
					text = "Last sync: " + formatLastSync(job.lastSyncTimestamp)
						.removePrefix("Last sync: "),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}

			Spacer(modifier = Modifier.height(12.dp))
			Text(
				text = formatSyncCounts(
					created = job.lastSyncCreated,
					updated = job.lastSyncUpdated,
					deleted = job.lastSyncDeleted,
					sourceCount = job.lastSyncSourceCount,
					targetCount = job.lastSyncTargetCount
				),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			job.lastSyncError?.let { error ->
				Spacer(modifier = Modifier.height(4.dp))
				Text(
					text = "Last error: $error",
					color = MaterialTheme.colorScheme.error,
					style = MaterialTheme.typography.bodySmall
				)
			}
			Spacer(modifier = Modifier.height(12.dp))
			Surface(
				shape = MaterialTheme.shapes.medium,
				color = MaterialTheme.colorScheme.surfaceVariant
			) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(horizontal = 8.dp, vertical = 6.dp),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					FilledTonalButton(
						onClick = { onToggleActive(job, !job.isActive) },
						colors = ButtonDefaults.filledTonalButtonColors(
							containerColor = MaterialTheme.colorScheme.surface,
							contentColor = MaterialTheme.colorScheme.onSurface
						)
					) {
						val icon = if (job.isActive) Icons.Default.Pause else Icons.Default.PlayArrow
						val label = if (job.isActive) "Pause sync" else "Resume sync"
						Icon(imageVector = icon, contentDescription = null)
						Text(text = label, modifier = Modifier.padding(start = 6.dp))
					}
					Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
						FilledTonalButton(
							onClick = { onManualSync(job) },
							enabled = !isSyncing,
							modifier = Modifier.widthIn(min = 110.dp),
							colors = ButtonDefaults.filledTonalButtonColors(
								containerColor = MaterialTheme.colorScheme.surface,
								contentColor = MaterialTheme.colorScheme.onSurface,
								disabledContainerColor = MaterialTheme.colorScheme.surface,
								disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
							)
						) {
							Icon(
								imageVector = Icons.Default.Sync,
								contentDescription = null
							)
							Text(
								text = if (isSyncing) "Syncing" else "Sync now",
								modifier = Modifier.padding(start = 6.dp)
							)
						}
						Box {
							IconButton(onClick = { menuExpanded = true }) {
								Icon(
									imageVector = Icons.Default.MoreVert,
									contentDescription = "Job actions"
								)
							}
							DropdownMenu(
								expanded = menuExpanded,
								onDismissRequest = { menuExpanded = false }
							) {
								DropdownMenuItem(
									text = { Text("Edit") },
									leadingIcon = {
										Icon(Icons.Default.Edit, contentDescription = null)
									},
									onClick = {
										menuExpanded = false
										onEditJob(job)
									}
								)
								DropdownMenuItem(
									text = { Text("Delete") },
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
			}
			Spacer(modifier = Modifier.height(8.dp))
			Row(
				modifier = Modifier.fillMaxWidth(),
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				Text(
					text = formatFrequency(job.frequencyMinutes),
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
				)
				Text(
					text = "${job.windowPastDays}d back · ${job.windowFutureDays}d ahead",
					style = MaterialTheme.typography.labelSmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
				)
			}
		}
	}

	if (showDeleteConfirm) {
		AlertDialog(
			onDismissRequest = { showDeleteConfirm = false },
			title = { Text("Delete sync job?") },
			text = {
				Text(
					"Delete the sync between \"$sourceName\" and \"$targetName\"? " +
						"This removes the job, forgets previous mappings, and stops syncing."
				)
			},
			confirmButton = {
				TextButton(
					onClick = {
						showDeleteConfirm = false
						onDeleteJob(job)
					}
				) {
					Text("Delete")
				}
			},
			dismissButton = {
				TextButton(onClick = { showDeleteConfirm = false }) {
					Text("Cancel")
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
	CalsciumTheme {
		SyncJobRow(
			job = SyncJob(
				id = 1L,
				sourceCalendarId = 1L,
				targetCalendarId = 2L,
				lastSyncTimestamp = System.currentTimeMillis() - 86_400_000L,
				isActive = true
			),
			sourceName = "Work",
			targetName = "Personal",
			sourceColor = 0xFF3F51B5.toInt(),
			targetColor = 0xFFFF9800.toInt(),
			isSyncing = false,
			onToggleActive = { _, _ -> },
			onDeleteJob = {},
			onEditJob = {},
			onManualSync = {}
		)
	}
}
