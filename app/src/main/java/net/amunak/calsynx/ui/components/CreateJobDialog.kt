package net.amunak.calsynx.ui.components

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
import androidx.compose.material3.Checkbox
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
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.res.stringResource
import net.amunak.calsynx.R
import net.amunak.calsynx.calendar.CalendarInfo
import net.amunak.calsynx.ui.PreviewData
import net.amunak.calsynx.ui.theme.CalsynxTheme

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CreateJobDialog(
	calendars: List<CalendarInfo>,
	jobs: List<net.amunak.calsynx.data.SyncJob>,
	initialJob: net.amunak.calsynx.data.SyncJob? = null,
	onDismiss: () -> Unit,
	onCreate: (Long, Long, Int, Int, Boolean, Int) -> Unit,
	onUpdate: (net.amunak.calsynx.data.SyncJob, Int, Int, Boolean, Int) -> Unit
) {
	var source by remember { mutableStateOf<CalendarInfo?>(null) }
	var target by remember { mutableStateOf<CalendarInfo?>(null) }
	var sourceExpanded by remember { mutableStateOf(false) }
	var targetExpanded by remember { mutableStateOf(false) }

	val isEdit = initialJob != null
	var pastDays by remember { mutableStateOf(initialJob?.windowPastDays ?: 7) }
	var futureDays by remember { mutableStateOf(initialJob?.windowFutureDays ?: 90) }
	var syncAllEvents by remember { mutableStateOf(initialJob?.syncAllEvents ?: false) }
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
					text = if (isEdit) {
						stringResource(R.string.title_edit_sync_job)
					} else {
						stringResource(R.string.dialog_create_sync_job)
					},
					style = MaterialTheme.typography.titleLarge
				)
				if (calendars.isEmpty()) {
					Text(stringResource(R.string.label_no_calendars_available))
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
							label = stringResource(R.string.label_source_calendar),
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
							label = stringResource(R.string.label_target_calendar),
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
						label = stringResource(R.string.label_past_days),
						value = pastDays,
						onValueChange = { pastDays = it.coerceIn(0, MAX_WINDOW_DAYS) },
						textFieldColors = textFieldColors,
						enabled = !syncAllEvents
					)

					NumberPickerRow(
						label = stringResource(R.string.label_future_days),
						value = futureDays,
						onValueChange = { futureDays = it.coerceIn(0, MAX_WINDOW_DAYS) },
						textFieldColors = textFieldColors,
						enabled = !syncAllEvents
					)

					Row(
						verticalAlignment = Alignment.CenterVertically,
						modifier = Modifier.padding(top = 4.dp)
					) {
						Checkbox(
							checked = syncAllEvents,
							onCheckedChange = { syncAllEvents = it }
						)
						Text(
							text = stringResource(R.string.label_sync_all_events),
							style = MaterialTheme.typography.bodySmall
						)
					}

					OptionPicker(
						label = stringResource(R.string.label_sync_frequency),
						options = frequencyOptions(),
						selected = frequencySelection,
						onSelected = { frequencySelection = it }
					)
					Text(
						text = stringResource(R.string.label_sync_window_hint),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)

					if (validationError != null && !isEdit) {
						Text(
							text = stringResource(validationError),
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
							text = stringResource(R.string.action_cancel),
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
									syncAllEvents,
									frequencySelection.minutes
								)
							} else {
								onCreate(
									source!!.id,
									target!!.id,
									pastDays,
									futureDays,
									syncAllEvents,
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
							text = if (isEdit) {
								stringResource(R.string.action_update)
							} else {
								stringResource(R.string.action_save)
							},
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
					name = selected?.displayName ?: stringResource(R.string.label_select),
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
				groupCalendars(
					calendars,
					onDeviceLabel = stringResource(R.string.text_on_device),
					externalLabel = stringResource(R.string.text_external)
				).forEach { (accountLabel, entries) ->
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
								Box(modifier = Modifier.fillMaxWidth()) {
									CalendarLabel(
										name = calendar.displayName,
										color = calendar.color
									)
									if (!calendar.isVisible) {
										Icon(
											imageVector = Icons.Default.VisibilityOff,
											contentDescription = stringResource(R.string.label_calendar_hidden),
											tint = MaterialTheme.colorScheme.onSurfaceVariant,
											modifier = Modifier
												.size(16.dp)
												.align(Alignment.CenterEnd)
										)
									}
								}
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
				Text(stringResource(selected.displayLabelRes))
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
						text = { Text(stringResource(option.displayLabelRes)) },
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
	textFieldColors: TextFieldColors,
	enabled: Boolean = true
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
			IconButton(
				onClick = { onValueChange((value - 1).coerceAtLeast(0)) },
				enabled = enabled
			) {
				Icon(
					Icons.Default.Remove,
					contentDescription = stringResource(R.string.label_decrease)
				)
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
				enabled = enabled,
				keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
				modifier = Modifier.weight(1f),
				suffix = { Text(stringResource(R.string.label_window_days_suffix)) },
				colors = textFieldColors
			)
			IconButton(onClick = { onValueChange(value + 1) }, enabled = enabled) {
				Icon(
					Icons.Default.Add,
					contentDescription = stringResource(R.string.label_increase)
				)
			}
		}
	}
}

private fun validateSelection(
	source: CalendarInfo?,
	target: CalendarInfo?,
	jobs: List<net.amunak.calsynx.data.SyncJob>,
	currentJobId: Long?
): Int? {
	if (source == null || target == null) return R.string.message_validation_select_both
	if (source.id == target.id) return R.string.message_validation_source_target_same
	if (jobs.any { it.targetCalendarId == source.id && it.id != currentJobId }) {
		return R.string.message_validation_source_is_target
	}
	if (jobs.any { it.sourceCalendarId == target.id && it.id != currentJobId }) {
		return R.string.message_validation_target_is_source
	}
	return null
}

interface DisplayOption {
	val displayLabelRes: Int
}

data class FrequencyOption(
	val minutes: Int,
	override val displayLabelRes: Int
) : DisplayOption

private fun frequencyOptions(): List<FrequencyOption> {
	return listOf(
		FrequencyOption(15, R.string.frequency_every_15_minutes),
		FrequencyOption(30, R.string.frequency_every_30_minutes),
		FrequencyOption(60, R.string.frequency_hourly),
		FrequencyOption(120, R.string.frequency_every_2_hours),
		FrequencyOption(240, R.string.frequency_every_4_hours),
		FrequencyOption(360, R.string.frequency_every_6_hours),
		FrequencyOption(720, R.string.frequency_every_12_hours),
		FrequencyOption(1440, R.string.frequency_daily),
		FrequencyOption(10080, R.string.frequency_weekly),
		FrequencyOption(43200, R.string.frequency_monthly)
	)
}

private const val MAX_WINDOW_DAYS = 3650

@Preview(showBackground = true)
@Composable
private fun CreateJobDialogPreview() {
	CalsynxTheme {
		CreateJobDialog(
			calendars = PreviewData.calendars(),
			jobs = PreviewData.jobs(),
			onDismiss = {},
			onCreate = { _, _, _, _, _, _ -> },
			onUpdate = { _, _, _, _, _ -> }
		)
	}
}
