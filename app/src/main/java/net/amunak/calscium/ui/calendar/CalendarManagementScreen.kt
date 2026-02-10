package net.amunak.calscium.ui.calendar

import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import net.amunak.calscium.calendar.CalendarInfo
import net.amunak.calscium.ui.components.CalendarLabel
import net.amunak.calscium.ui.components.groupCalendars

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
				title = { Text("Calendar Management") },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
					}
				}
			)
		},
		floatingActionButton = {
			ExtendedFloatingActionButton(onClick = { showCreateDialog = true }) {
				Icon(Icons.Default.Add, contentDescription = null)
				Text(text = "Create calendar", modifier = Modifier.padding(start = 6.dp))
			}
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
						text = "Loading calendars...",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
				if (!state.isLoading && state.calendars.isEmpty()) {
					Text(
						text = "No calendars available.",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}

				// Keep account sections visually distinct while using the same card style.
				val grouped = groupCalendars(state.calendars.map { it.calendar })
				LazyColumn(
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
	val calendarById = remember(state.calendars) {
		state.calendars.associate { it.calendar.id to it.calendar }
	}

	var showRenameDialog by remember { mutableStateOf(false) }
	var showColorDialog by remember { mutableStateOf(false) }
	var showPurgeDialog by remember { mutableStateOf(false) }
	var showDeleteDialog by remember { mutableStateOf(false) }

	Scaffold(
		topBar = {
			TopAppBar(
				title = { Text(calendar.displayName) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
					sourceNames = row.incomingJobs.mapNotNull { job ->
						calendarById[job.sourceCalendarId]?.displayName
					}
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
						Text("Actions", style = MaterialTheme.typography.titleSmall)
						ActionRow(
							icon = Icons.Default.Edit,
							label = "Rename calendar",
							onClick = { showRenameDialog = true }
						)
						ActionRow(
							icon = Icons.Default.Palette,
							label = "Change color",
							onClick = { showColorDialog = true }
						)
						ActionRow(
							icon = Icons.Default.Warning,
							label = "Purge all events",
							onClick = { showPurgeDialog = true }
						)
						ActionRow(
							icon = Icons.Default.Delete,
							label = "Delete calendar",
							onClick = { showDeleteDialog = true }
						)
					}
				}
			}
		}
	}

	if (showRenameDialog) {
		RenameCalendarDialog(
			initialName = calendar.displayName,
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
			title = "Purge all events?",
			message = "This will delete all events in \"${calendar.displayName}\" and cannot be undone.",
			confirmLabel = "Purge",
			onDismiss = { showPurgeDialog = false },
			onConfirm = {
				onPurge(calendar)
				showPurgeDialog = false
			}
		)
	}

	if (showDeleteDialog) {
		ConfirmDialog(
			title = "Delete calendar?",
			message = "Delete \"${calendar.displayName}\" and all its events. Any sync mappings to this calendar will be lost.",
			confirmLabel = "Delete",
			onDismiss = { showDeleteDialog = false },
			onConfirm = {
				onDelete(calendar)
				showDeleteDialog = false
			}
		)
	}
}

@Composable
private fun CalendarRowCard(
	row: CalendarRowUi,
	onClick: () -> Unit
) {
	ElevatedCard(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick),
		colors = CardDefaults.elevatedCardColors(
			containerColor = MaterialTheme.colorScheme.surface
		)
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp)
		) {
			CalendarLabel(
				name = row.calendar.displayName,
				color = row.calendar.color
			)
			Spacer(modifier = Modifier.height(6.dp))
			Text(
				text = "${row.eventCount} events · ${row.syncedCount} synced",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			if (row.incomingJobs.isNotEmpty()) {
				Text(
					text = "Incoming syncs: ${row.incomingJobs.size}",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

@Composable
private fun CalendarMetaSection(
	row: CalendarRowUi,
	sourceNames: List<String>
) {
	val calendar = row.calendar
	val typeLabel = calendarTypeLabel(calendar)
	Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
		Row(verticalAlignment = Alignment.CenterVertically) {
			CalendarLabel(
				name = calendar.displayName,
				color = calendar.color,
				textStyle = MaterialTheme.typography.titleMedium,
				maxLines = 2
			)
		}
		Text(
			text = "Type: $typeLabel",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Text(
			text = "Events: ${row.eventCount} · Synced: ${row.syncedCount}",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		if (row.incomingJobs.isNotEmpty()) {
			val sources = sourceNames.distinct().sorted()
			val sourceLabel = if (sources.isNotEmpty()) {
				sources.joinToString()
			} else {
				"Unknown"
			}
			Text(
				text = "Sources: $sourceLabel",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Text(
				text = "Synced into by: ${row.incomingJobs.size} job(s)",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		} else {
			Text(
				text = "No jobs sync into this calendar.",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
private fun ActionRow(
	icon: androidx.compose.ui.graphics.vector.ImageVector,
	label: String,
	onClick: () -> Unit
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(vertical = 8.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		Icon(icon, contentDescription = null)
		Text(
			text = label,
			style = MaterialTheme.typography.bodyMedium,
			modifier = Modifier.padding(start = 12.dp)
		)
	}
}

@Composable
private fun CreateCalendarDialog(
	onDismiss: () -> Unit,
	onCreate: (String, Int) -> Unit
) {
	var name by remember { mutableStateOf(TextFieldValue("New calendar")) }
	var selectedColor by remember { mutableStateOf(defaultCalendarColors().first()) }

	AlertDialog(
		onDismissRequest = onDismiss,
		shape = MaterialTheme.shapes.large,
		containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
		title = { Text("Create calendar") },
		text = {
			val textFieldColors = OutlinedTextFieldDefaults.colors(
				focusedBorderColor = MaterialTheme.colorScheme.primary,
				unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
				focusedLabelColor = MaterialTheme.colorScheme.primary,
				cursorColor = MaterialTheme.colorScheme.primary
			)
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				OutlinedTextField(
					value = name,
					onValueChange = { name = it },
					label = { Text("Calendar name") },
					modifier = Modifier.fillMaxWidth(),
					colors = textFieldColors
				)
				ColorPickerRow(
					selectedColor = selectedColor,
					onSelect = { selectedColor = it }
				)
			}
		},
		confirmButton = {
			Button(
				onClick = { onCreate(name.text.trim(), selectedColor) },
				enabled = name.text.isNotBlank()
			) {
				Text("Create")
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
private fun RenameCalendarDialog(
	initialName: String,
	onDismiss: () -> Unit,
	onSave: (String) -> Unit
) {
	var name by remember { mutableStateOf(TextFieldValue(initialName)) }
	AlertDialog(
		onDismissRequest = onDismiss,
		shape = MaterialTheme.shapes.large,
		containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
		title = { Text("Rename calendar") },
		text = {
			val textFieldColors = OutlinedTextFieldDefaults.colors(
				focusedBorderColor = MaterialTheme.colorScheme.primary,
				unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
				focusedLabelColor = MaterialTheme.colorScheme.primary,
				cursorColor = MaterialTheme.colorScheme.primary
			)
			OutlinedTextField(
				value = name,
				onValueChange = { name = it },
				label = { Text("Calendar name") },
				modifier = Modifier.fillMaxWidth(),
				colors = textFieldColors
			)
		},
		confirmButton = {
			Button(
				onClick = { onSave(name.text.trim()) },
				enabled = name.text.isNotBlank()
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
private fun ColorPickerDialog(
	onDismiss: () -> Unit,
	onSelect: (Int) -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		shape = MaterialTheme.shapes.large,
		containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
		title = { Text("Select color") },
		text = {
			ColorPickerRow(
				selectedColor = null,
				onSelect = { onSelect(it) }
			)
		},
		confirmButton = {
			TextButton(onClick = onDismiss) {
				Text("Close")
			}
		}
	)
}

@Composable
private fun ColorPickerRow(
	selectedColor: Int?,
	onSelect: (Int) -> Unit
) {
	Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
		defaultCalendarColors().forEach { color ->
			Surface(
				color = Color(color),
				shape = MaterialTheme.shapes.small,
				modifier = Modifier
					.size(32.dp)
					.clickable(onClick = { onSelect(color) }),
				tonalElevation = if (selectedColor == color) 6.dp else 0.dp
			) {}
		}
	}
}

@Composable
private fun ConfirmDialog(
	title: String,
	message: String,
	confirmLabel: String,
	onDismiss: () -> Unit,
	onConfirm: () -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		shape = MaterialTheme.shapes.large,
		containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
		title = { Text(title) },
		text = { Text(message) },
		confirmButton = {
			Button(onClick = onConfirm) {
				Text(confirmLabel)
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text("Cancel")
			}
		}
	)
}

private fun calendarTypeLabel(calendar: CalendarInfo): String {
	return when {
		calendar.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL -> "On device"
		calendar.accountType.isNullOrBlank() -> "External"
		else -> calendar.accountType
	}
}

private fun defaultCalendarColors(): List<Int> {
	return listOf(
		0xFFE53935.toInt(),
		0xFF8E24AA.toInt(),
		0xFF3949AB.toInt(),
		0xFF1E88E5.toInt(),
		0xFF00897B.toInt(),
		0xFF43A047.toInt(),
		0xFFF4511E.toInt(),
		0xFF6D4C41.toInt()
	)
}
