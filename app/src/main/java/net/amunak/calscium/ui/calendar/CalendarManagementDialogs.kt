package net.amunak.calscium.ui.calendar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import net.amunak.calscium.R
import net.amunak.calscium.ui.components.sanitizeCalendarName

@Composable
	fun CreateCalendarDialog(
		onDismiss: () -> Unit,
		onCreate: (String, Int) -> Unit
	) {
		val colorRows = remember { defaultCalendarColorRows() }
		val defaultColor = colorRows.first().first()
		val defaultName = stringResource(R.string.dialog_calendar_name_default)
		var name by remember(defaultName) { mutableStateOf(defaultName) }
	var selectedColor by remember { mutableStateOf(defaultColor) }

	AlertDialog(
		onDismissRequest = onDismiss,
		shape = MaterialTheme.shapes.large,
		containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
		title = { Text(stringResource(R.string.title_create_calendar)) },
		text = {
			val textFieldColors = OutlinedTextFieldDefaults.colors(
				focusedBorderColor = MaterialTheme.colorScheme.primary,
				unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
				focusedLabelColor = MaterialTheme.colorScheme.primary,
				cursorColor = MaterialTheme.colorScheme.primary
			)
			Column(
				verticalArrangement = Arrangement.spacedBy(12.dp),
				modifier = Modifier.verticalScroll(rememberScrollState())
			) {
				OutlinedTextField(
					value = name,
					onValueChange = { name = sanitizeCalendarName(it) },
					label = { Text(stringResource(R.string.label_calendar_name)) },
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
				onClick = { onCreate(name.trim(), selectedColor) },
				enabled = name.isNotBlank()
			) {
				Text(stringResource(R.string.action_create))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(R.string.action_cancel))
			}
		}
	)
}

@Composable
fun RenameCalendarDialog(
	initialName: String,
	onDismiss: () -> Unit,
	onSave: (String) -> Unit
) {
	var name by remember { mutableStateOf(initialName) }
	AlertDialog(
		onDismissRequest = onDismiss,
		shape = MaterialTheme.shapes.large,
		containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
		title = { Text(stringResource(R.string.title_rename_calendar)) },
		text = {
			val textFieldColors = OutlinedTextFieldDefaults.colors(
				focusedBorderColor = MaterialTheme.colorScheme.primary,
				unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
				focusedLabelColor = MaterialTheme.colorScheme.primary,
				cursorColor = MaterialTheme.colorScheme.primary
			)
			OutlinedTextField(
				value = name,
				onValueChange = { name = sanitizeCalendarName(it) },
				label = { Text(stringResource(R.string.label_calendar_name)) },
				modifier = Modifier.fillMaxWidth(),
				colors = textFieldColors
			)
		},
		confirmButton = {
			Button(onClick = { onSave(name.trim()) }, enabled = name.isNotBlank()) {
				Text(stringResource(R.string.action_save))
			}
		},
		dismissButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(R.string.action_cancel))
			}
		}
	)
}

@Composable
fun ColorPickerDialog(
	onDismiss: () -> Unit,
	onSelect: (Int) -> Unit
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		shape = MaterialTheme.shapes.large,
		containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
		title = { Text(stringResource(R.string.title_select_color)) },
		text = {
			ColorPickerRow(
				selectedColor = null,
				onSelect = { onSelect(it) }
			)
		},
		confirmButton = {
			TextButton(onClick = onDismiss) {
				Text(stringResource(R.string.action_close))
			}
		}
	)
}

@Composable
fun ColorPickerRow(
	selectedColor: Int?,
	onSelect: (Int) -> Unit
) {
	val colorRows = remember { defaultCalendarColorRows() }
	Column(
		verticalArrangement = Arrangement.spacedBy(6.dp),
		modifier = Modifier.verticalScroll(rememberScrollState())
	) {
		colorRows.forEach { row ->
			Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
				row.forEach { color ->
					val isSelected = selectedColor == color
					Surface(
						color = Color(color),
						shape = MaterialTheme.shapes.small,
						modifier = Modifier
							.size(28.dp)
							.clickable { onSelect(color) },
						tonalElevation = if (isSelected) 6.dp else 0.dp
					) {}
				}
			}
		}
	}
}

@Composable
fun ConfirmDialog(
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
				Text(stringResource(R.string.action_cancel))
			}
		}
	)
}

private fun defaultCalendarColorRows(): List<List<Int>> {
	return listOf(
		listOf(0xFFB71C1C, 0xFFC62828, 0xFFD32F2F, 0xFFE53935, 0xFFEF5350, 0xFFFFCDD2),
		listOf(0xFF880E4F, 0xFFAD1457, 0xFFC2185B, 0xFFD81B60, 0xFFEC407A, 0xFFF8BBD0),
		listOf(0xFF4A148C, 0xFF6A1B9A, 0xFF7B1FA2, 0xFF8E24AA, 0xFFAB47BC, 0xFFE1BEE7),
		listOf(0xFF311B92, 0xFF4527A0, 0xFF512DA8, 0xFF5E35B1, 0xFF7E57C2, 0xFFD1C4E9),
		listOf(0xFF1A237E, 0xFF283593, 0xFF303F9F, 0xFF3949AB, 0xFF5C6BC0, 0xFFC5CAE9),
		listOf(0xFF0D47A1, 0xFF1565C0, 0xFF1976D2, 0xFF1E88E5, 0xFF42A5F5, 0xFFBBDEFB),
		listOf(0xFF01579B, 0xFF0277BD, 0xFF0288D1, 0xFF039BE5, 0xFF29B6F6, 0xFFB3E5FC),
		listOf(0xFF006064, 0xFF00838F, 0xFF0097A7, 0xFF00ACC1, 0xFF26C6DA, 0xFFB2EBF2),
		listOf(0xFF004D40, 0xFF00695C, 0xFF00796B, 0xFF00897B, 0xFF26A69A, 0xFFB2DFDB),
		listOf(0xFF1B5E20, 0xFF2E7D32, 0xFF388E3C, 0xFF43A047, 0xFF66BB6A, 0xFFC8E6C9),
		listOf(0xFF33691E, 0xFF558B2F, 0xFF689F38, 0xFF7CB342, 0xFF9CCC65, 0xFFDCEDC8),
		listOf(0xFF827717, 0xFF9E9D24, 0xFFAFB42B, 0xFFC0CA33, 0xFFD4E157, 0xFFF0F4C3),
		listOf(0xFFF57F17, 0xFFF9A825, 0xFFFBC02D, 0xFFFDD835, 0xFFFFEE58, 0xFFFFF9C4),
		listOf(0xFFFF6F00, 0xFFFF8F00, 0xFFFFA000, 0xFFFFB300, 0xFFFFCA28, 0xFFFFECB3),
		listOf(0xFFE65100, 0xFFEF6C00, 0xFFF57C00, 0xFFFB8C00, 0xFFFF9800, 0xFFFFE0B2),
		listOf(0xFFBF360C, 0xFFD84315, 0xFFE64A19, 0xFFF4511E, 0xFFFF7043, 0xFFFFCCBC),
		listOf(0xFF3E2723, 0xFF4E342E, 0xFF5D4037, 0xFF6D4C41, 0xFF8D6E63, 0xFFD7CCC8),
		listOf(0xFF212121, 0xFF424242, 0xFF616161, 0xFF757575, 0xFF9E9E9E, 0xFFEEEEEE),
		listOf(0xFF263238, 0xFF37474F, 0xFF455A64, 0xFF546E7A, 0xFF78909C, 0xFFCFD8DC)
	).map { row -> row.map { it.toInt() } }
}

@Preview(showBackground = true)
@Composable
private fun CreateCalendarDialogPreview() {
	CreateCalendarDialog(onDismiss = {}, onCreate = { _, _ -> })
}

@Preview(showBackground = true)
@Composable
private fun RenameCalendarDialogPreview() {
	RenameCalendarDialog(
		initialName = stringResource(R.string.preview_calendar_work),
		onDismiss = {},
		onSave = {}
	)
}

@Preview(showBackground = true)
@Composable
private fun ColorPickerDialogPreview() {
	ColorPickerDialog(onDismiss = {}, onSelect = {})
}

@Preview(showBackground = true)
@Composable
private fun ConfirmDialogPreview() {
	ConfirmDialog(
		title = stringResource(R.string.title_delete_calendar),
		message = stringResource(
			R.string.message_calendar_delete,
			stringResource(R.string.preview_calendar_work)
		),
		confirmLabel = stringResource(R.string.action_delete),
		onDismiss = {},
		onConfirm = {}
	)
}
