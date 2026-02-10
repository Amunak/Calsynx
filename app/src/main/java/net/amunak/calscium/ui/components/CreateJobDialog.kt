package net.amunak.calscium.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
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
	onSave: (Long, Long, SyncWindowOption, SyncFrequencyOption) -> Unit
) {
	var source by remember { mutableStateOf<CalendarInfo?>(null) }
	var target by remember { mutableStateOf<CalendarInfo?>(null) }
	var sourceExpanded by remember { mutableStateOf(false) }
	var targetExpanded by remember { mutableStateOf(false) }

	var windowSelection by remember { mutableStateOf(SyncWindowOption.DEFAULT) }
	var frequencySelection by remember { mutableStateOf(SyncFrequencyOption.DEFAULT) }

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

					HorizontalDivider()

					OptionPicker(
						label = "Sync window",
						options = SyncWindowOption.values().toList(),
						selected = windowSelection,
						onSelected = { windowSelection = it }
					)

					OptionPicker(
						label = "Sync frequency",
						options = SyncFrequencyOption.values().toList(),
						selected = frequencySelection,
						onSelected = { frequencySelection = it }
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
				onClick = {
					onSave(
						source!!.id,
						target!!.id,
						windowSelection,
						frequencySelection
					)
				},
				enabled = canSave
			) {
				Text("Save")
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
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

@Composable
private fun <T> OptionPicker(
	label: String,
	options: List<T>,
	selected: T,
	onSelected: (T) -> Unit
) where T : Enum<T>, T : DisplayOption {
	var expanded by remember { mutableStateOf(false) }
	Column {
		Text(
			text = label,
			style = MaterialTheme.typography.labelMedium
		)
		Box {
			OutlinedButton(
				onClick = { expanded = true },
				modifier = Modifier.fillMaxWidth()
			) {
				Text(selected.displayLabel)
			}
			DropdownMenu(
				expanded = expanded,
				onDismissRequest = { expanded = false }
			) {
				options.forEach { option ->
					DropdownMenuItem(
						text = { Text(option.displayLabel) },
						onClick = {
							onSelected(option)
							expanded = false
						}
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

interface DisplayOption {
	val displayLabel: String
}

enum class SyncWindowOption(
	val pastDays: Int,
	val futureDays: Int,
	override val displayLabel: String
) : DisplayOption {
	DEFAULT(7, 90, "Past 7d, Next 90d"),
	SHORT(3, 30, "Past 3d, Next 30d"),
	MEDIUM(14, 60, "Past 14d, Next 60d"),
	LONG(30, 180, "Past 30d, Next 180d")
}

enum class SyncFrequencyOption(
	val minutes: Int,
	override val displayLabel: String
) : DisplayOption {
	MINUTES_5(5, "Every 5 minutes"),
	MINUTES_15(15, "Every 15 minutes"),
	MINUTES_60(60, "Hourly"),
	HOURS_6(360, "Every 6 hours"),
	DAILY(1440, "Daily"),
	WEEKLY(10080, "Weekly"),
	MONTHLY(43200, "Monthly");

	companion object {
		val DEFAULT = HOURS_6
	}
}

@Preview(showBackground = true)
@Composable
private fun CreateJobDialogPreview() {
	CalsciumTheme {
		CreateJobDialog(
			calendars = PreviewData.calendars,
			jobs = PreviewData.jobs,
			onDismiss = {},
			onSave = { _, _, _, _ -> }
		)
	}
}
