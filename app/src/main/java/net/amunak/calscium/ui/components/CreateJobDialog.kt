package net.amunak.calscium.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import net.amunak.calscium.calendar.CalendarInfo
import net.amunak.calscium.ui.PreviewData
import net.amunak.calscium.ui.theme.CalsciumTheme

@Composable
fun CreateJobDialog(
	calendars: List<CalendarInfo>,
	jobs: List<net.amunak.calscium.data.SyncJob>,
	onDismiss: () -> Unit,
	onSave: (Long, Long) -> Unit
) {
	var source by remember { mutableStateOf<CalendarInfo?>(null) }
	var target by remember { mutableStateOf<CalendarInfo?>(null) }
	var sourceExpanded by remember { mutableStateOf(false) }
	var targetExpanded by remember { mutableStateOf(false) }

	val validationError = remember(source, target, jobs) {
		validateSelection(source, target, jobs)
	}
	val canSave = validationError == null

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Create sync job") },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				if (calendars.isEmpty()) {
					Text("No calendars available.")
				} else {
					CalendarPicker(
						label = "Source calendar",
						calendars = calendars,
						selected = source,
						expanded = sourceExpanded,
						onExpandedChange = { sourceExpanded = it },
						onSelected = { calendar ->
							source = calendar
							sourceExpanded = false
						}
					)

					CalendarPicker(
						label = "Target calendar",
						calendars = calendars,
						selected = target,
						expanded = targetExpanded,
						onExpandedChange = { targetExpanded = it },
						onSelected = { calendar ->
							target = calendar
							targetExpanded = false
						}
					)

					if (source != null && target != null && source?.id == target?.id) {
						Text(
							text = "Source and target must be different.",
							color = MaterialTheme.colorScheme.error,
							style = MaterialTheme.typography.bodySmall
						)
					} else if (validationError != null) {
						Text(
							text = validationError,
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

@Composable
private fun CalendarPicker(
	label: String,
	calendars: List<CalendarInfo>,
	selected: CalendarInfo?,
	expanded: Boolean,
	onExpandedChange: (Boolean) -> Unit,
	onSelected: (CalendarInfo) -> Unit
) {
	Column {
		Text(
			text = label,
			style = MaterialTheme.typography.labelMedium
		)
		Box {
			OutlinedButton(
				onClick = { onExpandedChange(true) },
				modifier = Modifier.fillMaxWidth()
			) {
				Text(selected?.displayName ?: "Select")
			}
			DropdownMenu(
				expanded = expanded,
				onDismissRequest = { onExpandedChange(false) }
			) {
				calendars.forEach { calendar ->
					DropdownMenuItem(
						text = { Text(calendar.displayName) },
						onClick = { onSelected(calendar) }
					)
				}
			}
		}
	}
}

private fun validateSelection(
	source: CalendarInfo?,
	target: CalendarInfo?,
	jobs: List<net.amunak.calscium.data.SyncJob>
): String? {
	if (source == null || target == null) return "Select both calendars."
	if (source.id == target.id) return "Source and target must be different."
	if (jobs.any { it.targetCalendarId == target.id }) {
		return "Target calendar is already used by another job."
	}
	if (jobs.any { it.targetCalendarId == source.id }) {
		return "Source calendar is already a target in another job."
	}
	return null
}

@Preview(showBackground = true)
@Composable
private fun CreateJobDialogPreview() {
	CalsciumTheme {
		CreateJobDialog(
			calendars = PreviewData.calendars,
			jobs = PreviewData.jobs,
			onDismiss = {},
			onSave = { _, _ -> }
		)
	}
}
