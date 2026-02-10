package net.amunak.calscium.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import net.amunak.calscium.calendar.CalendarInfo
import net.amunak.calscium.ui.PreviewData
import net.amunak.calscium.ui.theme.CalsciumTheme

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CreateJobDialog(
	calendars: List<CalendarInfo>,
	jobs: List<net.amunak.calscium.data.SyncJob>,
	initialJob: net.amunak.calscium.data.SyncJob? = null,
	onDismiss: () -> Unit,
	onCreate: (Long, Long, Int, Int, Int) -> Unit,
	onUpdate: (net.amunak.calscium.data.SyncJob, Int, Int, Int) -> Unit
) {
	var source by remember { mutableStateOf<CalendarInfo?>(null) }
	var target by remember { mutableStateOf<CalendarInfo?>(null) }
	var sourceExpanded by remember { mutableStateOf(false) }
	var targetExpanded by remember { mutableStateOf(false) }

	val isEdit = initialJob != null
	var pastDays by remember { mutableStateOf(initialJob?.windowPastDays ?: 7) }
	var futureDays by remember { mutableStateOf(initialJob?.windowFutureDays ?: 90) }
	var frequencySelection by remember {
		mutableStateOf(
			frequencyOptions().firstOrNull { it.minutes == initialJob?.frequencyMinutes }
				?: frequencyOptions().first { it.minutes == 360 }
		)
	}

	val validationError = remember(source, target, jobs) {
		validateSelection(source, target, jobs, initialJob?.id)
	}
	val canSave = validationError == null

	LaunchedEffect(calendars, initialJob) {
		if (isEdit) {
			val job = initialJob!!
			source = calendars.firstOrNull { it.id == job.sourceCalendarId }
			target = calendars.firstOrNull { it.id == job.targetCalendarId }
		}
	}

	BasicAlertDialog(
		onDismissRequest = onDismiss
	) {
		Surface(
			shape = MaterialTheme.shapes.large,
			color = MaterialTheme.colorScheme.surfaceContainerLow
		) {
			// Match dialog component colors with the rest of the app theme.
			val textFieldColors = OutlinedTextFieldDefaults.colors(
				focusedBorderColor = MaterialTheme.colorScheme.primary,
				unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
				focusedLabelColor = MaterialTheme.colorScheme.primary,
				cursorColor = MaterialTheme.colorScheme.primary
			)
			Column(
				modifier = Modifier.padding(20.dp),
				verticalArrangement = Arrangement.spacedBy(14.dp)
			) {
				Text(
					text = if (isEdit) "Edit sync job" else "Create sync job",
					style = MaterialTheme.typography.titleLarge
				)
				if (calendars.isEmpty()) {
					Text("No calendars available.")
				} else {
					// Keep edit and create dialogs visually consistent.
					if (isEdit) {
						Row(
							modifier = Modifier.fillMaxWidth(),
							verticalAlignment = Alignment.CenterVertically
						) {
							CalendarLabel(
								name = source?.displayName ?: "",
								color = source?.color,
								textColor = MaterialTheme.colorScheme.onSurfaceVariant,
								textStyle = MaterialTheme.typography.bodyMedium
							)
							Icon(
								imageVector = Icons.AutoMirrored.Filled.ArrowForward,
								contentDescription = null,
								tint = MaterialTheme.colorScheme.onSurfaceVariant,
								modifier = Modifier
									.padding(horizontal = 6.dp)
									.size(16.dp)
							)
							CalendarLabel(
								name = target?.displayName ?: "",
								color = target?.color,
								textColor = MaterialTheme.colorScheme.onSurfaceVariant,
								textStyle = MaterialTheme.typography.bodyMedium
							)
						}
					} else {
						CalendarPicker(
							label = "Source calendar",
							calendars = calendars,
							selected = source,
							expanded = sourceExpanded,
							onExpandedChange = { sourceExpanded = it },
							enabled = true,
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
							enabled = true,
							onSelected = { calendar ->
								target = calendar
								targetExpanded = false
							}
						)
					}

					NumberPickerRow(
						label = "Past days",
						value = pastDays,
						onValueChange = { pastDays = it.coerceIn(0, 365) },
						textFieldColors = textFieldColors
					)

					NumberPickerRow(
						label = "Future days",
						value = futureDays,
						onValueChange = { futureDays = it.coerceIn(0, 365) },
						textFieldColors = textFieldColors
					)

					OptionPicker(
						label = "Sync frequency",
						options = frequencyOptions(),
						selected = frequencySelection,
						onSelected = { frequencySelection = it }
					)

					if (validationError != null && !isEdit) {
						Text(
							text = validationError,
							color = MaterialTheme.colorScheme.error,
							style = MaterialTheme.typography.bodySmall
						)
					}
				}

				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					FilledTonalButton(
						onClick = onDismiss,
						colors = ButtonDefaults.filledTonalButtonColors(
							containerColor = MaterialTheme.colorScheme.secondaryContainer,
							contentColor = MaterialTheme.colorScheme.onSecondaryContainer
						)
					) {
						Icon(
							imageVector = Icons.Default.Close,
							contentDescription = null
						)
						Text(
							text = "Cancel",
							modifier = Modifier.padding(start = 6.dp)
						)
					}
					Button(
						onClick = {
							if (isEdit) {
								onUpdate(
									initialJob!!,
									pastDays,
									futureDays,
									frequencySelection.minutes
								)
							} else {
								onCreate(
									source!!.id,
									target!!.id,
									pastDays,
									futureDays,
									frequencySelection.minutes
								)
							}
						},
						enabled = canSave,
						colors = ButtonDefaults.buttonColors(
							containerColor = MaterialTheme.colorScheme.primaryContainer,
							contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
							disabledContainerColor = MaterialTheme.colorScheme.primaryContainer,
							disabledContentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
						)
					) {
						Icon(
							imageVector = Icons.Default.Check,
							contentDescription = null
						)
						Text(
							text = if (isEdit) "Update" else "Save",
							modifier = Modifier.padding(start = 6.dp)
						)
					}
				}
			}
		}
	}
}

@Composable
private fun CalendarPicker(
	label: String,
	calendars: List<CalendarInfo>,
	selected: CalendarInfo?,
	expanded: Boolean,
	onExpandedChange: (Boolean) -> Unit,
	enabled: Boolean,
	onSelected: (CalendarInfo) -> Unit
) {
	Column {
		Text(
			text = label,
			style = MaterialTheme.typography.labelMedium
		)
		Box {
			OutlinedButton(
				onClick = { if (enabled) onExpandedChange(true) },
				modifier = Modifier.fillMaxWidth(),
				enabled = enabled
			) {
				CalendarLabel(
					name = selected?.displayName ?: "Select",
					color = selected?.color
				)
				Icon(
					imageVector = Icons.Default.ArrowDropDown,
					contentDescription = null,
					modifier = Modifier.padding(start = 6.dp)
				)
			}
			DropdownMenu(
				expanded = expanded,
				onDismissRequest = { onExpandedChange(false) }
			) {
				groupCalendars(calendars).forEach { (accountLabel, entries) ->
					DropdownMenuItem(
						text = {
							Text(
								text = accountLabel,
								style = MaterialTheme.typography.labelSmall
							)
						},
						enabled = false,
						onClick = {}
					)
					entries.forEach { calendar ->
						DropdownMenuItem(
							text = {
								CalendarLabel(
									name = calendar.displayName,
									color = calendar.color
								)
							},
							onClick = { onSelected(calendar) }
						)
					}
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
) where T : DisplayOption {
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
				Icon(
					imageVector = Icons.Default.ArrowDropDown,
					contentDescription = null,
					modifier = Modifier.padding(start = 6.dp)
				)
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

@Composable
private fun NumberPickerRow(
	label: String,
	value: Int,
	onValueChange: (Int) -> Unit,
	textFieldColors: TextFieldColors
) {
	var textValue by remember(value) { mutableStateOf(value.toString()) }
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Text(
			text = label,
			style = MaterialTheme.typography.labelMedium
		)
		Row(
			modifier = Modifier.fillMaxWidth(),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			IconButton(onClick = { onValueChange((value - 1).coerceAtLeast(0)) }) {
				Icon(Icons.Default.Remove, contentDescription = "Decrease")
			}
			OutlinedTextField(
				value = textValue,
				onValueChange = { raw ->
					val digits = raw.filter { it.isDigit() }
					textValue = digits
					val number = digits.toIntOrNull()
					if (number != null) {
						onValueChange(number)
					}
				},
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
				modifier = Modifier.weight(1f),
				suffix = { Text("days") },
				colors = textFieldColors
			)
			IconButton(onClick = { onValueChange(value + 1) }) {
				Icon(Icons.Default.Add, contentDescription = "Increase")
			}
		}
	}
}

private fun validateSelection(
	source: CalendarInfo?,
	target: CalendarInfo?,
	jobs: List<net.amunak.calscium.data.SyncJob>,
	currentJobId: Long?
): String? {
	if (source == null || target == null) return "Select both calendars."
	if (source.id == target.id) return "Source and target must be different."
	if (jobs.any { it.targetCalendarId == target.id && it.id != currentJobId }) {
		return "Target calendar is already used by another job."
	}
	if (jobs.any { it.targetCalendarId == source.id && it.id != currentJobId }) {
		return "Source calendar is already a target in another job."
	}
	return null
}

interface DisplayOption {
	val displayLabel: String
}

data class FrequencyOption(
	val minutes: Int,
	override val displayLabel: String
) : DisplayOption

private fun frequencyOptions(): List<FrequencyOption> {
	return listOf(
		FrequencyOption(5, "Every 5 minutes"),
		FrequencyOption(15, "Every 15 minutes"),
		FrequencyOption(30, "Every 30 minutes"),
		FrequencyOption(60, "Hourly"),
		FrequencyOption(120, "Every 2 hours"),
		FrequencyOption(240, "Every 4 hours"),
		FrequencyOption(360, "Every 6 hours"),
		FrequencyOption(720, "Every 12 hours"),
		FrequencyOption(1440, "Daily"),
		FrequencyOption(10080, "Weekly"),
		FrequencyOption(43200, "Monthly")
	)
}

private fun groupCalendars(calendars: List<CalendarInfo>): Map<String, List<CalendarInfo>> {
	return calendars.groupBy { calendar ->
		val name = calendar.accountName ?: "On device"
		val type = calendar.accountType?.takeIf { it.isNotBlank() }
		if (type != null) "$name · $type" else name
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
			onCreate = { _, _, _, _, _ -> },
			onUpdate = { _, _, _, _ -> }
		)
	}
}
