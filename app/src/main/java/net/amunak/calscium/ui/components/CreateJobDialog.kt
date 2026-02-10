package net.amunak.calscium.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateJobDialog(
	calendars: List<CalendarInfo>,
	onDismiss: () -> Unit,
	onSave: (Long, Long) -> Unit
) {
	var source by remember { mutableStateOf<CalendarInfo?>(null) }
	var target by remember { mutableStateOf<CalendarInfo?>(null) }
	var sourceExpanded by remember { mutableStateOf(false) }
	var targetExpanded by remember { mutableStateOf(false) }

	val canSave = source != null && target != null && source?.id != target?.id

	AlertDialog(
		onDismissRequest = onDismiss,
		title = { Text("Create sync job") },
		text = {
			Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
				if (calendars.isEmpty()) {
					Text("No calendars available.")
				} else {
					ExposedDropdownMenuBox(
						expanded = sourceExpanded,
						onExpandedChange = { sourceExpanded = it }
					) {
						OutlinedTextField(
							value = source?.displayName ?: "",
							onValueChange = {},
							label = { Text("Source calendar") },
							readOnly = true,
							trailingIcon = {
								ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded)
							},
							modifier = Modifier
								.menuAnchor(MenuAnchorType.PrimaryNotEditable)
								.fillMaxWidth()
						)
						ExposedDropdownMenu(
							expanded = sourceExpanded,
							onDismissRequest = { sourceExpanded = false }
						) {
							calendars.forEach { calendar ->
								DropdownMenuItem(
									text = { Text(calendar.displayName) },
									onClick = {
										source = calendar
										sourceExpanded = false
									}
								)
							}
						}
					}

					ExposedDropdownMenuBox(
						expanded = targetExpanded,
						onExpandedChange = { targetExpanded = it }
					) {
						OutlinedTextField(
							value = target?.displayName ?: "",
							onValueChange = {},
							label = { Text("Target calendar") },
							readOnly = true,
							trailingIcon = {
								ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetExpanded)
							},
							modifier = Modifier
								.menuAnchor(MenuAnchorType.PrimaryNotEditable)
								.fillMaxWidth()
						)
						ExposedDropdownMenu(
							expanded = targetExpanded,
							onDismissRequest = { targetExpanded = false }
						) {
							calendars.forEach { calendar ->
								DropdownMenuItem(
									text = { Text(calendar.displayName) },
									onClick = {
										target = calendar
										targetExpanded = false
									}
								)
							}
						}
					}

					if (source != null && target != null && source?.id == target?.id) {
						Text(
							text = "Source and target must be different.",
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

@Preview(showBackground = true)
@Composable
private fun CreateJobDialogPreview() {
	CalsciumTheme {
		CreateJobDialog(
			calendars = PreviewData.calendars,
			onDismiss = {},
			onSave = { _, _ -> }
		)
	}
}
