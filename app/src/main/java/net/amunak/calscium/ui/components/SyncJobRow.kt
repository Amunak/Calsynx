package net.amunak.calscium.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import net.amunak.calscium.data.SyncJob
import net.amunak.calscium.ui.formatters.formatLastSync
import net.amunak.calscium.ui.formatters.formatSyncCounts
import net.amunak.calscium.ui.theme.CalsciumTheme

@Composable
fun SyncJobRow(
	job: SyncJob,
	sourceName: String,
	targetName: String,
	isSyncing: Boolean,
	onToggleActive: (SyncJob, Boolean) -> Unit,
	onDeleteJob: (SyncJob) -> Unit,
	onManualSync: (SyncJob) -> Unit
) {
	Column(
		modifier = Modifier.fillMaxWidth()
	) {
		Row(
			modifier = Modifier.fillMaxWidth(),
			verticalAlignment = Alignment.CenterVertically
		) {
			Text(
				text = "$sourceName -> $targetName",
				style = MaterialTheme.typography.titleSmall,
				modifier = Modifier.weight(1f)
			)
			OutlinedButton(
				onClick = { onManualSync(job) },
				enabled = !isSyncing
			) {
				Text(if (isSyncing) "Syncing..." else "Sync now")
			}
		}
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = formatLastSync(job.lastSyncTimestamp),
			style = MaterialTheme.typography.bodySmall
		)
		Spacer(modifier = Modifier.height(4.dp))
		Text(
			text = formatSyncCounts(
				created = job.lastSyncCreated,
				updated = job.lastSyncUpdated,
				deleted = job.lastSyncDeleted,
				sourceCount = job.lastSyncSourceCount,
				targetCount = job.lastSyncTargetCount
			),
			style = MaterialTheme.typography.bodySmall
		)
		job.lastSyncError?.let { error ->
			Spacer(modifier = Modifier.height(4.dp))
			Text(
				text = "Last error: $error",
				color = MaterialTheme.colorScheme.error,
				style = MaterialTheme.typography.bodySmall
			)
		}
		Spacer(modifier = Modifier.height(8.dp))
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
			isSyncing = false,
			onToggleActive = { _, _ -> },
			onDeleteJob = {},
			onManualSync = {}
		)
	}
}
