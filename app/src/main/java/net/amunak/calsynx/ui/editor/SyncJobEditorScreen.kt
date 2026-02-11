package net.amunak.calsynx.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import net.amunak.calsynx.R
import net.amunak.calsynx.calendar.CalendarInfo
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.ui.components.CalendarLabel
import net.amunak.calsynx.ui.components.groupCalendars
import net.amunak.calsynx.ui.components.sanitizeCalendarName

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncJobEditorScreen(
	state: SyncJobEditorUiState,
	onClose: () -> Unit,
	onSave: (Long, Long, Int, Int, Boolean, Int) -> Unit
) {
	val isEdit = state.job != null
	var source by rememberSaveable(state.job?.id) { mutableStateOf<CalendarInfo?>(null) }
	var target by rememberSaveable(state.job?.id) { mutableStateOf<CalendarInfo?>(null) }
	var sourceExpanded by rememberSaveable { mutableStateOf(false) }
	var targetExpanded by rememberSaveable { mutableStateOf(false) }
	var pastDays by rememberSaveable(state.job?.id) { mutableStateOf(state.job?.windowPastDays ?: 7) }
	var futureDays by rememberSaveable(state.job?.id) { mutableStateOf(state.job?.windowFutureDays ?: 90) }
	var syncAllEvents by rememberSaveable(state.job?.id) { mutableStateOf(state.job?.syncAllEvents ?: false) }
	var frequencySelection by rememberSaveable(state.job?.id) {
		mutableStateOf(
			frequencyOptions().firstOrNull { it.minutes == state.job?.frequencyMinutes }
				?: frequencyOptions().first { it.minutes == 360 }
		)
	}

	LaunchedEffect(state.calendars, state.job?.id) {
		if (state.job != null) {
			source = state.calendars.firstOrNull { it.id == state.job.sourceCalendarId }
			target = state.calendars.firstOrNull { it.id == state.job.targetCalendarId }
		}
	}

	val validationError = remember(source, target, state.jobs, state.job?.id) {
		validateSelection(source, target, state.jobs, state.job?.id)
	}
	val canSave = validationError == null && !state.isLoading && state.saveState != SaveState.Saving

	Scaffold(
		topBar = {
			TopAppBar(
				title = {
					Text(
						text = if (isEdit) {
							stringResource(R.string.title_edit_sync_job)
						} else {
							stringResource(R.string.title_create_sync_job)
						}
					)
				},
				navigationIcon = {
					IconButton(onClick = onClose) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.action_back)
						)
					}
				},
				actions = {
					IconButton(
						onClick = {
							onSave(
								source?.id ?: return@IconButton,
								target?.id ?: return@IconButton,
								pastDays,
								futureDays,
								syncAllEvents,
								frequencySelection.minutes
							)
						},
						enabled = canSave
					) {
						Icon(
							imageVector = Icons.Default.Check,
							contentDescription = stringResource(R.string.action_save)
						)
					}
				}
			)
		}
	) { padding ->
		LazyColumn(
			modifier = Modifier
				.fillMaxSize()
				.padding(padding),
			contentPadding = PaddingValues(
				start = 16.dp,
				end = 16.dp,
				top = 16.dp,
				bottom = 24.dp
			),
			verticalArrangement = Arrangement.spacedBy(16.dp)
		) {
			item {
				EditorSection(title = stringResource(R.string.label_section_calendars)) {
					if (state.calendars.isEmpty()) {
						Text(
							text = stringResource(R.string.label_no_calendars_available),
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					} else if (isEdit) {
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
							Text(
								text = "→",
								modifier = Modifier.padding(horizontal = 8.dp),
								color = MaterialTheme.colorScheme.onSurfaceVariant
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
							calendars = state.calendars,
							selected = source,
							expanded = sourceExpanded,
							onExpandedChange = { sourceExpanded = it },
							onSelected = { calendar ->
								source = calendar
								sourceExpanded = false
							}
						)
						Spacer(modifier = Modifier.height(12.dp))
						CalendarPicker(
							label = stringResource(R.string.label_target_calendar),
							calendars = state.calendars,
							selected = target,
							expanded = targetExpanded,
							onExpandedChange = { targetExpanded = it },
							onSelected = { calendar ->
								target = calendar
								targetExpanded = false
							}
						)
					}
					validationError?.let { errorRes ->
						Spacer(modifier = Modifier.height(8.dp))
						Text(
							text = stringResource(errorRes),
							color = MaterialTheme.colorScheme.error,
							style = MaterialTheme.typography.bodySmall
						)
					}
				}
			}

			item {
				EditorSection(title = stringResource(R.string.label_section_window)) {
					NumberPickerRow(
						label = stringResource(R.string.label_past_days),
						value = pastDays,
						onValueChange = { pastDays = it.coerceIn(0, MAX_WINDOW_DAYS) },
						enabled = !syncAllEvents
					)
					Spacer(modifier = Modifier.height(12.dp))
					NumberPickerRow(
						label = stringResource(R.string.label_future_days),
						value = futureDays,
						onValueChange = { futureDays = it.coerceIn(0, MAX_WINDOW_DAYS) },
						enabled = !syncAllEvents
					)
					Spacer(modifier = Modifier.height(8.dp))
					Row(
						verticalAlignment = Alignment.CenterVertically
					) {
						SyncCheckbox(
							checked = syncAllEvents,
							onCheckedChange = { syncAllEvents = it }
						)
						Text(
							text = stringResource(R.string.label_sync_all_events),
							style = MaterialTheme.typography.bodySmall
						)
					}
					Spacer(modifier = Modifier.height(8.dp))
					Text(
						text = stringResource(R.string.label_sync_window_hint),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}

			item {
				EditorSection(title = stringResource(R.string.label_section_frequency)) {
					OptionPicker(
						label = stringResource(R.string.label_sync_frequency),
						options = frequencyOptions(),
						selected = frequencySelection,
						onSelected = { frequencySelection = it }
					)
				}
			}

			state.errorMessage?.let { message ->
				item {
					Text(
						text = message,
						color = MaterialTheme.colorScheme.error,
						style = MaterialTheme.typography.bodySmall
					)
				}
			}
		}
	}
}

@Composable
private fun EditorSection(
	title: String,
	content: @Composable ColumnScope.() -> Unit
) {
	Column(
		modifier = Modifier.fillMaxWidth(),
		verticalArrangement = Arrangement.spacedBy(8.dp),
		content = {
			Text(
				text = title,
				style = MaterialTheme.typography.titleSmall
			)
			content()
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
		CalendarDropdown(
			calendars = calendars,
			selected = selected,
			expanded = expanded,
			onExpandedChange = onExpandedChange,
			onSelected = onSelected
		)
	}
}

@Composable
private fun CalendarDropdown(
	calendars: List<CalendarInfo>,
	selected: CalendarInfo?,
	expanded: Boolean,
	onExpandedChange: (Boolean) -> Unit,
	onSelected: (CalendarInfo) -> Unit
) {
	val grouped = groupCalendars(
		calendars,
		onDeviceLabel = stringResource(R.string.text_on_device),
		externalLabel = stringResource(R.string.text_external)
	)
	SyncDropdown(
		selectedLabel = selected?.displayName ?: stringResource(R.string.label_select),
		isExpanded = expanded,
		onExpandedChange = onExpandedChange
	) {
		grouped.forEach { (accountLabel, entries) ->
			SyncDropdownHeader(text = accountLabel)
			entries.forEach { calendar ->
				SyncDropdownItem(
					label = sanitizeCalendarName(calendar.displayName),
					color = calendar.color,
					isHidden = !calendar.isVisible,
					onClick = { onSelected(calendar) }
				)
			}
		}
	}
}

private fun validateSelection(
	source: CalendarInfo?,
	target: CalendarInfo?,
	jobs: List<SyncJob>,
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

private const val MAX_WINDOW_DAYS = 3650
