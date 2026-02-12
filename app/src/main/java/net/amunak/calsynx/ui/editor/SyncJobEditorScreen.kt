package net.amunak.calsynx.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import net.amunak.calsynx.R
import net.amunak.calsynx.calendar.CalendarInfo
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.data.sync.AvailabilityMode
import net.amunak.calsynx.data.sync.ReminderMode
import net.amunak.calsynx.ui.components.CalendarLabel
import net.amunak.calsynx.ui.components.ScrollIndicator
import net.amunak.calsynx.ui.components.TooltipIconButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncJobEditorScreen(
	state: SyncJobEditorUiState,
	onClose: () -> Unit,
	onSave: (
		Long,
		Long,
		Int,
		Int,
		Boolean,
		Int,
		AvailabilityMode,
		Boolean,
		Boolean,
		Boolean,
		Boolean,
		ReminderMode,
		Int,
		Int,
		Boolean,
		Boolean,
		Boolean,
		Boolean,
		Boolean
	) -> Unit
) {
	val isEdit = state.job != null
	var source by rememberSaveable(state.job?.id) { mutableStateOf<CalendarInfo?>(null) }
	var target by rememberSaveable(state.job?.id) { mutableStateOf<CalendarInfo?>(null) }
	var sourceExpanded by rememberSaveable { mutableStateOf(false) }
	var targetExpanded by rememberSaveable { mutableStateOf(false) }
	var pastDays by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.windowPastDays ?: DEFAULT_PAST_DAYS)
	}
	var futureDays by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.windowFutureDays ?: DEFAULT_FUTURE_DAYS)
	}
	var syncAllEvents by rememberSaveable(state.job?.id) { mutableStateOf(state.job?.syncAllEvents ?: false) }
	var availabilityModeValue by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.availabilityMode ?: AvailabilityMode.COPY.value)
	}
	var copyEventColor by rememberSaveable(state.job?.id) { mutableStateOf(state.job?.copyEventColor ?: false) }
	var copyPrivacy by rememberSaveable(state.job?.id) { mutableStateOf(state.job?.copyPrivacy ?: true) }
	var copyAttendees by rememberSaveable(state.job?.id) { mutableStateOf(state.job?.copyAttendees ?: false) }
	var copyOrganizer by rememberSaveable(state.job?.id) { mutableStateOf(state.job?.copyOrganizer ?: false) }
	var reminderModeValue by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.reminderMode ?: ReminderMode.COPY.value)
	}
	var reminderAllDayDays by rememberSaveable(state.job?.id) {
		val minutes = state.job?.reminderAllDayMinutes
		val days = if (minutes == null) {
			DEFAULT_ALL_DAY_REMINDER_DAYS
		} else {
			deriveAllDayReminderDays(minutes)
		}
		mutableStateOf(days)
	}
	var reminderAllDayTimeMinutes by rememberSaveable(state.job?.id) {
		val minutes = state.job?.reminderAllDayMinutes
		val timeMinutes = if (minutes == null) {
			DEFAULT_ALL_DAY_REMINDER_TIME_MINUTES
		} else {
			deriveAllDayReminderTimeMinutes(minutes)
		}
		mutableStateOf(timeMinutes)
	}
	var reminderTimedMinutes by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.reminderTimedMinutes ?: 60)
	}
	var reminderAllDayEnabled by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.reminderAllDayEnabled ?: true)
	}
	var reminderTimedEnabled by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.reminderTimedEnabled ?: true)
	}
	var reminderResyncEnabled by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.reminderResyncEnabled ?: true)
	}
	var pairExistingOnFirstSync by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.pairExistingOnFirstSync ?: false)
	}
	var deleteUnmappedTargets by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.deleteUnmappedTargets ?: false)
	}
	var showAdvanced by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.let(::jobUsesAdvancedOptions) ?: false)
	}
	var frequencyMinutes by rememberSaveable(state.job?.id) {
		mutableStateOf(state.job?.frequencyMinutes ?: DEFAULT_FREQUENCY_MINUTES)
	}
	var showDiscardDialog by remember { mutableStateOf(false) }
	val resources = LocalContext.current.resources
	val listState = rememberLazyListState()

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
	val initialSnapshot = remember(state.job?.id) { EditorSnapshot.fromJob(state.job) }
	val currentSnapshot = EditorSnapshot.fromState(
		sourceId = source?.id,
		targetId = target?.id,
		pastDays = pastDays,
		futureDays = futureDays,
		syncAllEvents = syncAllEvents,
		frequencyMinutes = frequencyMinutes,
		availabilityModeValue = availabilityModeValue,
		copyEventColor = copyEventColor,
		copyPrivacy = copyPrivacy,
		copyAttendees = copyAttendees,
		copyOrganizer = copyOrganizer,
		reminderModeValue = reminderModeValue,
		reminderAllDayMinutes = computeAllDayReminderMinutes(
			reminderAllDayDays,
			reminderAllDayTimeMinutes
		),
		reminderTimedMinutes = reminderTimedMinutes,
		reminderAllDayEnabled = reminderAllDayEnabled,
		reminderTimedEnabled = reminderTimedEnabled,
		reminderResyncEnabled = reminderResyncEnabled,
		pairExistingOnFirstSync = pairExistingOnFirstSync,
		deleteUnmappedTargets = deleteUnmappedTargets
	)
	val hasUnsavedChanges = initialSnapshot != currentSnapshot
	val requestClose = {
		if (hasUnsavedChanges) {
			showDiscardDialog = true
		} else {
			onClose()
		}
	}

	BackHandler(enabled = hasUnsavedChanges) {
		showDiscardDialog = true
	}

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
					TooltipIconButton(
						tooltip = stringResource(R.string.action_back),
						onClick = requestClose
					) {
						Icon(
							imageVector = Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.action_back)
						)
					}
				},
				actions = {
					TextButton(
						onClick = {
							onSave(
								source?.id ?: return@TextButton,
								target?.id ?: return@TextButton,
								pastDays,
								futureDays,
								syncAllEvents,
								frequencyMinutes,
								AvailabilityMode.from(availabilityModeValue),
								copyEventColor,
								copyPrivacy,
								copyAttendees,
								copyOrganizer,
								ReminderMode.from(reminderModeValue),
								computeAllDayReminderMinutes(
									reminderAllDayDays,
									reminderAllDayTimeMinutes
								),
								reminderTimedMinutes.coerceAtLeast(0),
								reminderAllDayEnabled,
								reminderTimedEnabled,
								reminderResyncEnabled,
								pairExistingOnFirstSync,
								deleteUnmappedTargets
							)
						},
						enabled = canSave
					) {
						Icon(
							imageVector = Icons.Default.Check,
							contentDescription = null,
							modifier = Modifier.padding(end = 6.dp)
						)
						Text(
							text = if (isEdit) {
								stringResource(R.string.action_update)
							} else {
								stringResource(R.string.action_create)
							}
						)
					}
				}
			)
		}
	) { padding ->
		Box(modifier = Modifier.fillMaxSize()) {
			LazyColumn(
				state = listState,
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
							},
							jobs = state.jobs,
							currentJobId = state.job?.id,
							currentSource = source,
							currentTarget = target,
							mode = CalendarPickMode.Source
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
							},
							jobs = state.jobs,
							currentJobId = state.job?.id,
							currentSource = source,
							currentTarget = target,
							mode = CalendarPickMode.Target
						)
					}
					validationError?.let { errorRes ->
						Spacer(modifier = Modifier.height(8.dp))
						SyncInlineMessage(
							message = stringResource(errorRes),
							icon = Icons.Default.Warning,
							tint = MaterialTheme.colorScheme.error
						)
					}
					Spacer(modifier = Modifier.height(8.dp))
					SyncCheckboxRow(
						checked = pairExistingOnFirstSync,
						label = stringResource(R.string.label_pair_existing_events),
						onCheckedChange = { pairExistingOnFirstSync = it },
						enabled = !isEdit
					)
					SyncInlineMessage(
						message = stringResource(
							if (isEdit) {
								R.string.message_pair_existing_events_disabled
							} else {
								R.string.message_pair_existing_events
							}
						),
						startIndent = CHECKBOX_MESSAGE_INDENT
					)
					SyncCheckboxRow(
						checked = deleteUnmappedTargets,
						label = stringResource(R.string.label_delete_unmapped_targets),
						onCheckedChange = { deleteUnmappedTargets = it }
					)
					SyncInlineMessage(
						message = stringResource(R.string.message_delete_unmapped_targets),
						icon = Icons.Default.Warning,
						tint = MaterialTheme.colorScheme.onSurfaceVariant,
						startIndent = CHECKBOX_MESSAGE_INDENT
					)
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
					SyncCheckboxRow(
						checked = syncAllEvents,
						label = stringResource(R.string.label_sync_all_events),
						onCheckedChange = { syncAllEvents = it }
					)
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
					val selectedOption = frequencyOptions().firstOrNull { it.minutes == frequencyMinutes }
						?: frequencyOptions().first()
					OptionPicker(
						label = stringResource(R.string.label_sync_frequency),
						options = frequencyOptions(),
						selected = selectedOption,
						onSelected = { frequencyMinutes = it.minutes }
					)
				}
			}

			item {
				AdvancedDisclosureSection(
					title = stringResource(R.string.label_section_advanced),
					expanded = showAdvanced,
					onExpandedChange = { showAdvanced = it }
				) {
					AdvancedSubsection(title = stringResource(R.string.label_section_copy_options)) {
						val availabilityMode = AvailabilityMode.from(availabilityModeValue)
						OptionPicker(
							label = stringResource(R.string.label_availability),
							options = availabilityOptions(),
							selected = availabilityOptionFor(availabilityMode),
							onSelected = { availabilityModeValue = it.mode.value }
						)
						Spacer(modifier = Modifier.height(8.dp))
						SyncCheckboxRow(
							checked = copyPrivacy,
							label = stringResource(R.string.label_copy_privacy),
							onCheckedChange = { copyPrivacy = it }
						)
						SyncCheckboxRow(
							checked = copyEventColor,
							label = stringResource(R.string.label_copy_event_color),
							onCheckedChange = { copyEventColor = it }
						)
						SyncCheckboxRow(
							checked = copyOrganizer,
							label = stringResource(R.string.label_copy_organizer_owner),
							onCheckedChange = { copyOrganizer = it }
						)
						SyncCheckboxRow(
							checked = copyAttendees,
							label = stringResource(R.string.label_copy_attendees),
							onCheckedChange = { copyAttendees = it }
						)
						SyncInlineMessage(
							message = stringResource(R.string.message_copy_attendees_warning),
							icon = Icons.Default.Warning,
							tint = MaterialTheme.colorScheme.onSurfaceVariant,
							startIndent = CHECKBOX_MESSAGE_INDENT
						)
					}
					AdvancedSubsection(title = stringResource(R.string.label_reminder_mode)) {
						val reminderMode = ReminderMode.from(reminderModeValue)
						OptionPicker(
							label = stringResource(R.string.label_reminder_mode_choice),
							options = reminderOptions(),
							selected = reminderOptionFor(reminderMode),
							onSelected = { reminderModeValue = it.mode.value }
						)
						Spacer(modifier = Modifier.height(8.dp))
						SyncCheckboxRow(
							checked = reminderResyncEnabled,
							label = stringResource(R.string.label_reminder_resync),
							onCheckedChange = { reminderResyncEnabled = it }
						)
						if (reminderMode == ReminderMode.CUSTOM) {
							Spacer(modifier = Modifier.height(8.dp))
							NumberPickerRow(
								label = stringResource(R.string.label_reminder_all_day_days),
								value = reminderAllDayDays,
								onValueChange = { reminderAllDayDays = it.coerceAtLeast(0) },
								suffix = stringResource(R.string.label_days_suffix_short),
								enabled = reminderAllDayEnabled
							)
							Spacer(modifier = Modifier.height(8.dp))
							TimePickerRow(
								label = stringResource(R.string.label_reminder_all_day_time),
								timeMinutes = reminderAllDayTimeMinutes,
								onTimeChange = { reminderAllDayTimeMinutes = it },
								enabled = reminderAllDayEnabled
							)
							Spacer(modifier = Modifier.height(8.dp))
							if (reminderAllDayEnabled) {
								SyncInlineMessage(
									message = formatReminderOffset(
										resources = resources,
										minutesBefore = computeAllDayReminderMinutes(
											reminderAllDayDays,
											reminderAllDayTimeMinutes
										)
									),
									startIndent = CHECKBOX_MESSAGE_INDENT
								)
							}
							Spacer(modifier = Modifier.height(8.dp))
							SyncCheckboxRow(
								checked = !reminderAllDayEnabled,
								label = stringResource(R.string.label_reminder_all_day_none),
								onCheckedChange = { reminderAllDayEnabled = !it }
							)
							Spacer(modifier = Modifier.height(12.dp))
							NumberPickerRow(
								label = stringResource(R.string.label_reminder_timed_minutes),
								value = reminderTimedMinutes,
								onValueChange = { reminderTimedMinutes = it.coerceAtLeast(0) },
								suffix = stringResource(R.string.label_minutes_suffix_short),
								enabled = reminderTimedEnabled
							)
							Spacer(modifier = Modifier.height(8.dp))
							SyncCheckboxRow(
								checked = !reminderTimedEnabled,
								label = stringResource(R.string.label_reminder_timed_none),
								onCheckedChange = { reminderTimedEnabled = !it }
							)
						}
					}
				}
			}

			state.errorMessage?.let { message ->
				item {
					SyncInlineMessage(
						message = message,
						icon = Icons.Default.Warning,
						tint = MaterialTheme.colorScheme.error
					)
				}
			}
		}
		ScrollIndicator(
			state = listState,
			modifier = Modifier
				.align(Alignment.CenterEnd)
				.padding(top = padding.calculateTopPadding())
				.padding(
					bottom = WindowInsets.navigationBars
						.asPaddingValues()
						.calculateBottomPadding()
				)
				.padding(end = 2.dp)
		)
	}
	}

	if (showDiscardDialog) {
		AlertDialog(
			onDismissRequest = { showDiscardDialog = false },
			title = { Text(stringResource(R.string.title_discard_changes)) },
			text = { Text(stringResource(R.string.message_discard_changes)) },
			confirmButton = {
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.SpaceBetween,
					verticalAlignment = Alignment.CenterVertically
				) {
					TextButton(
						onClick = {
							showDiscardDialog = false
							onClose()
						}
					) {
						Text(
							text = stringResource(R.string.action_discard),
							color = MaterialTheme.colorScheme.error
						)
					}
					TextButton(onClick = { showDiscardDialog = false }) {
						Text(stringResource(R.string.action_continue_editing))
					}
				}
			}
		)
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
	if (jobs.any {
			it.sourceCalendarId == source.id &&
				it.targetCalendarId == target.id &&
				it.id != currentJobId
		}) {
		return R.string.message_validation_duplicate_job
	}
	return null
}

private fun jobUsesAdvancedOptions(job: SyncJob): Boolean {
	return job.availabilityMode != AvailabilityMode.COPY.value ||
		!job.copyPrivacy ||
		job.copyEventColor ||
		job.copyAttendees ||
		job.copyOrganizer ||
		job.reminderMode != ReminderMode.COPY.value ||
		!job.reminderAllDayEnabled ||
		!job.reminderTimedEnabled ||
		!job.reminderResyncEnabled ||
		job.reminderAllDayMinutes != defaultAllDayReminderMinutes() ||
		job.reminderTimedMinutes != DEFAULT_TIMED_REMINDER_MINUTES ||
		job.pairExistingOnFirstSync ||
		job.deleteUnmappedTargets
}

private data class EditorSnapshot(
	val sourceId: Long?,
	val targetId: Long?,
	val pastDays: Int,
	val futureDays: Int,
	val syncAllEvents: Boolean,
	val frequencyMinutes: Int,
	val availabilityModeValue: Int,
	val copyEventColor: Boolean,
	val copyPrivacy: Boolean,
	val copyAttendees: Boolean,
	val copyOrganizer: Boolean,
	val reminderModeValue: Int,
	val reminderAllDayMinutes: Int,
	val reminderTimedMinutes: Int,
	val reminderAllDayEnabled: Boolean,
	val reminderTimedEnabled: Boolean,
	val reminderResyncEnabled: Boolean,
	val pairExistingOnFirstSync: Boolean,
	val deleteUnmappedTargets: Boolean
) {
	companion object {
		fun fromJob(job: SyncJob?): EditorSnapshot {
			return EditorSnapshot(
				sourceId = job?.sourceCalendarId,
				targetId = job?.targetCalendarId,
				pastDays = job?.windowPastDays ?: DEFAULT_PAST_DAYS,
				futureDays = job?.windowFutureDays ?: DEFAULT_FUTURE_DAYS,
				syncAllEvents = job?.syncAllEvents ?: false,
				frequencyMinutes = job?.frequencyMinutes ?: DEFAULT_FREQUENCY_MINUTES,
				availabilityModeValue = job?.availabilityMode ?: AvailabilityMode.COPY.value,
				copyEventColor = job?.copyEventColor ?: false,
				copyPrivacy = job?.copyPrivacy ?: true,
				copyAttendees = job?.copyAttendees ?: false,
				copyOrganizer = job?.copyOrganizer ?: false,
				reminderModeValue = job?.reminderMode ?: ReminderMode.COPY.value,
				reminderAllDayMinutes = job?.reminderAllDayMinutes
					?: computeAllDayReminderMinutes(
						DEFAULT_ALL_DAY_REMINDER_DAYS,
						DEFAULT_ALL_DAY_REMINDER_TIME_MINUTES
					),
				reminderTimedMinutes = job?.reminderTimedMinutes ?: DEFAULT_TIMED_REMINDER_MINUTES,
				reminderAllDayEnabled = job?.reminderAllDayEnabled ?: true,
				reminderTimedEnabled = job?.reminderTimedEnabled ?: true,
				reminderResyncEnabled = job?.reminderResyncEnabled ?: true,
				pairExistingOnFirstSync = job?.pairExistingOnFirstSync ?: false,
				deleteUnmappedTargets = job?.deleteUnmappedTargets ?: false
			)
		}

		fun fromState(
			sourceId: Long?,
			targetId: Long?,
			pastDays: Int,
			futureDays: Int,
			syncAllEvents: Boolean,
			frequencyMinutes: Int,
			availabilityModeValue: Int,
			copyEventColor: Boolean,
			copyPrivacy: Boolean,
			copyAttendees: Boolean,
			copyOrganizer: Boolean,
			reminderModeValue: Int,
			reminderAllDayMinutes: Int,
			reminderTimedMinutes: Int,
			reminderAllDayEnabled: Boolean,
			reminderTimedEnabled: Boolean,
			reminderResyncEnabled: Boolean,
			pairExistingOnFirstSync: Boolean,
			deleteUnmappedTargets: Boolean
		): EditorSnapshot {
			return EditorSnapshot(
				sourceId = sourceId,
				targetId = targetId,
				pastDays = pastDays,
				futureDays = futureDays,
				syncAllEvents = syncAllEvents,
				frequencyMinutes = frequencyMinutes,
				availabilityModeValue = availabilityModeValue,
				copyEventColor = copyEventColor,
				copyPrivacy = copyPrivacy,
				copyAttendees = copyAttendees,
				copyOrganizer = copyOrganizer,
				reminderModeValue = reminderModeValue,
				reminderAllDayMinutes = reminderAllDayMinutes,
				reminderTimedMinutes = reminderTimedMinutes,
				reminderAllDayEnabled = reminderAllDayEnabled,
				reminderTimedEnabled = reminderTimedEnabled,
				reminderResyncEnabled = reminderResyncEnabled,
				pairExistingOnFirstSync = pairExistingOnFirstSync,
				deleteUnmappedTargets = deleteUnmappedTargets
			)
		}
	}
}

private const val MAX_WINDOW_DAYS = 3650
private const val DEFAULT_PAST_DAYS = 7
private const val DEFAULT_FUTURE_DAYS = 90
private const val DEFAULT_FREQUENCY_MINUTES = 360
private const val DEFAULT_TIMED_REMINDER_MINUTES = 60
private const val DEFAULT_ALL_DAY_REMINDER_DAYS = 0
private const val DEFAULT_ALL_DAY_REMINDER_TIME_MINUTES = 1200
private val CHECKBOX_MESSAGE_INDENT = 32.dp

private fun defaultAllDayReminderMinutes(): Int {
	return computeAllDayReminderMinutes(
		DEFAULT_ALL_DAY_REMINDER_DAYS,
		DEFAULT_ALL_DAY_REMINDER_TIME_MINUTES
	)
}
