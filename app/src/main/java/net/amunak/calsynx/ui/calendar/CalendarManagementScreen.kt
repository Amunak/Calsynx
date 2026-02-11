package net.amunak.calsynx.ui.calendar

import android.provider.CalendarContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.amunak.calsynx.calendar.CalendarInfo
import net.amunak.calsynx.ui.PreviewData
import net.amunak.calsynx.ui.components.groupCalendars
import net.amunak.calsynx.ui.components.sanitizeCalendarName
import net.amunak.calsynx.ui.components.TooltipIconButton
import net.amunak.calsynx.ui.components.ScrollIndicator
import androidx.compose.ui.res.stringResource
import net.amunak.calsynx.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarManagementScreen(
	state: CalendarManagementUiState,
	onBack: () -> Unit,
	onRefresh: () -> Unit,
	onSelectCalendar: (Long) -> Unit,
	onCreateCalendar: (String, Int) -> Unit
) {
	var showCreateDialog by remember { mutableStateOf(false) }

	LaunchedEffect(Unit) {
		onRefresh()
	}

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(stringResource(R.string.title_calendar_management)) },
				navigationIcon = {
					TooltipIconButton(
						tooltip = stringResource(R.string.action_back),
						onClick = onBack
					) {
						Icon(
							Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.action_back)
						)
					}
				}
			)
		},
		floatingActionButton = {
			ExtendedFloatingActionButton(onClick = { showCreateDialog = true }) {
				Icon(Icons.Default.Add, contentDescription = null)
				Text(
					text = stringResource(R.string.label_create_calendar),
					modifier = Modifier.padding(start = 6.dp)
				)
			}
		}
	) { padding ->
		Surface(
			modifier = Modifier.fillMaxSize(),
			color = MaterialTheme.colorScheme.surfaceContainerLowest
		) {
			val listState = rememberLazyListState()
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
					.padding(16.dp)
			) {
				state.errorMessage?.let { message ->
					Text(
						text = message,
						color = MaterialTheme.colorScheme.error,
						style = MaterialTheme.typography.bodySmall
					)
					Spacer(modifier = Modifier.height(8.dp))
				}
				if (state.isLoading) {
					Text(
						text = stringResource(R.string.label_loading_calendars),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				if (!state.isLoading && state.calendars.isEmpty()) {
					Text(
						text = stringResource(R.string.label_no_calendars_available),
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				val grouped = groupCalendars(
					state.calendars.map { it.calendar },
					onDeviceLabel = stringResource(R.string.text_on_device),
					externalLabel = stringResource(R.string.text_external)
				)
				Box(
					modifier = Modifier
						.fillMaxWidth()
						.weight(1f)
				) {
					LazyColumn(
						state = listState,
						contentPadding = PaddingValues(bottom = 96.dp),
						verticalArrangement = Arrangement.spacedBy(12.dp)
					) {
						grouped.forEach { (group, calendars) ->
							item {
								Text(
									text = group,
									style = MaterialTheme.typography.titleSmall,
									color = MaterialTheme.colorScheme.onSurfaceVariant
								)
							}
							items(
								items = calendars,
								key = { it.id }
							) { calendar ->
								val row = state.calendars.firstOrNull { it.calendar.id == calendar.id }
								if (row != null) {
									CalendarRowCard(
										row = row,
										onClick = { onSelectCalendar(calendar.id) }
									)
								}
							}
						}
					}
					ScrollIndicator(
						state = listState,
						modifier = Modifier
							.align(Alignment.CenterEnd)
							.padding(end = 6.dp)
					)
				}
			}
		}
	}

	if (showCreateDialog) {
		CreateCalendarDialog(
			onDismiss = { showCreateDialog = false },
			onCreate = { name, color ->
				onCreateCalendar(name, color)
				showCreateDialog = false
			}
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDetailScreen(
	state: CalendarManagementUiState,
	onBack: () -> Unit,
	onUpdateName: (CalendarInfo, String) -> Unit,
	onUpdateColor: (CalendarInfo, Int) -> Unit,
	onPurge: (CalendarInfo) -> Unit,
	onDelete: (CalendarInfo) -> Unit
) {
	val row = state.selectedCalendar ?: return
	val calendar = row.calendar
	val calendarTitle = sanitizeCalendarName(calendar.displayName)
	val sourceCalendars = remember(row) { row.incomingCalendars }
	val targetCalendars = remember(row) { row.outgoingCalendars }
	val accessLevel = calendar.accessLevel ?: CalendarContract.Calendars.CAL_ACCESS_EDITOR
	val canEditCalendar = accessLevel >= CalendarContract.Calendars.CAL_ACCESS_EDITOR
	val canWriteEvents = accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
	val canDeleteCalendar = accessLevel >= CalendarContract.Calendars.CAL_ACCESS_OWNER
	val hasActions = canEditCalendar || canWriteEvents || canDeleteCalendar

	var showRenameDialog by remember { mutableStateOf(false) }
	var showColorDialog by remember { mutableStateOf(false) }
	var showPurgeDialog by remember { mutableStateOf(false) }
	var showDeleteDialog by remember { mutableStateOf(false) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(calendarTitle) },
				navigationIcon = {
					TooltipIconButton(
						tooltip = stringResource(R.string.action_back),
						onClick = onBack
					) {
						Icon(
							Icons.AutoMirrored.Filled.ArrowBack,
							contentDescription = stringResource(R.string.action_back)
						)
					}
				}
			)
		}
	) { padding ->
		Surface(
			modifier = Modifier.fillMaxSize(),
			color = MaterialTheme.colorScheme.surfaceContainerLowest
		) {
			Column(
				modifier = Modifier
					.fillMaxSize()
					.padding(padding)
					.padding(16.dp),
				verticalArrangement = Arrangement.spacedBy(12.dp)
			) {
				CalendarMetaSection(
					row = row,
					sourceCalendars = sourceCalendars,
					targetCalendars = targetCalendars
				)

				ElevatedCard(
					colors = CardDefaults.elevatedCardColors(
						containerColor = MaterialTheme.colorScheme.surface
					),
					modifier = Modifier.fillMaxWidth()
				) {
					Column(
						modifier = Modifier
							.fillMaxWidth()
							.padding(16.dp),
						verticalArrangement = Arrangement.spacedBy(10.dp)
					) {
						Text(
							text = stringResource(R.string.label_actions),
							style = MaterialTheme.typography.titleSmall
						)
						if (canEditCalendar) {
							ActionRow(
								icon = Icons.Default.Edit,
								label = stringResource(R.string.action_rename_calendar),
								onClick = { showRenameDialog = true }
							)
							ActionRow(
								icon = Icons.Default.Palette,
								label = stringResource(R.string.action_change_color),
								onClick = { showColorDialog = true }
							)
						}
						if (canWriteEvents) {
							ActionRow(
								icon = Icons.Default.Warning,
								label = stringResource(R.string.action_purge_events),
								onClick = { showPurgeDialog = true }
							)
						}
						if (canDeleteCalendar) {
							ActionRow(
								icon = Icons.Default.Delete,
								label = stringResource(R.string.action_delete_calendar),
								onClick = { showDeleteDialog = true }
							)
						}
						if (!hasActions) {
							Text(
								text = stringResource(R.string.label_no_actions_available),
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						} else if (!canEditCalendar || !canWriteEvents || !canDeleteCalendar) {
							Text(
								text = stringResource(R.string.label_permissions_limited),
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant
							)
						}
					}
				}
			}
		}
	}

	if (showRenameDialog) {
		RenameCalendarDialog(
			initialName = sanitizeCalendarName(calendar.displayName),
			onDismiss = { showRenameDialog = false },
			onSave = { newName ->
				onUpdateName(calendar, newName)
				showRenameDialog = false
			}
		)
	}

	if (showColorDialog) {
		ColorPickerDialog(
			onDismiss = { showColorDialog = false },
			onSelect = { color ->
				onUpdateColor(calendar, color)
				showColorDialog = false
			}
		)
	}

	if (showPurgeDialog) {
		ConfirmDialog(
			title = stringResource(R.string.title_purge_calendar),
			message = stringResource(R.string.message_calendar_purge, calendarTitle),
			confirmLabel = stringResource(R.string.action_purge_events),
			onDismiss = { showPurgeDialog = false },
			onConfirm = {
				onPurge(calendar)
				showPurgeDialog = false
			}
		)
	}

	if (showDeleteDialog) {
		ConfirmDialog(
			title = stringResource(R.string.title_delete_calendar),
			message = stringResource(R.string.message_calendar_delete, calendarTitle),
			confirmLabel = stringResource(R.string.action_delete),
			onDismiss = { showDeleteDialog = false },
			onConfirm = {
				onDelete(calendar)
				showDeleteDialog = false
			}
		)
	}
}

@Preview(showBackground = true)
@Composable
private fun CalendarManagementScreenPreview() {
	CalendarManagementScreen(
		state = CalendarManagementUiState(
			calendars = listOf(
				CalendarRowUi(
					calendar = PreviewData.calendars().first(),
					eventCount = 12,
					syncedCount = 5,
					incomingCalendars = listOf(PreviewData.calendars().first()),
					outgoingCalendars = PreviewData.calendars()
				)
			),
			isLoading = false
		),
		onBack = {},
		onRefresh = {},
		onSelectCalendar = {},
		onCreateCalendar = { _, _ -> }
	)
}

@Preview(showBackground = true)
@Composable
private fun CalendarDetailScreenPreview() {
	val calendar = PreviewData.calendars().first()
	CalendarDetailScreen(
		state = CalendarManagementUiState(
			selectedCalendar = CalendarRowUi(
				calendar = calendar,
				eventCount = 12,
				syncedCount = 5,
				incomingCalendars = listOf(calendar),
				outgoingCalendars = PreviewData.calendars()
			)
		),
		onBack = {},
		onUpdateName = { _, _ -> },
		onUpdateColor = { _, _ -> },
		onPurge = {},
		onDelete = {}
	)
}
