package net.amunak.calsynx.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import net.amunak.calsynx.R
import net.amunak.calsynx.ui.components.CalendarLabel

@Composable
fun SyncDropdown(
	selectedLabel: String,
	isExpanded: Boolean,
	onExpandedChange: (Boolean) -> Unit,
	content: @Composable () -> Unit
) {
	Box {
		OutlinedButton(
			onClick = { onExpandedChange(true) },
			modifier = Modifier.fillMaxWidth()
		) {
			Text(selectedLabel)
			Icon(
				imageVector = Icons.Default.ArrowDropDown,
				contentDescription = null,
				modifier = Modifier.padding(start = 6.dp)
			)
		}
		DropdownMenu(
			expanded = isExpanded,
			onDismissRequest = { onExpandedChange(false) }
		) {
			content()
		}
	}
}

@Composable
fun SyncDropdownHeader(text: String) {
	DropdownMenuItem(
		text = {
			Text(
				text = text,
				style = MaterialTheme.typography.labelSmall
			)
		},
		enabled = false,
		onClick = {}
	)
}

@Composable
fun SyncDropdownItem(
	label: String,
	color: Int?,
	isHidden: Boolean,
	onClick: () -> Unit
) {
	DropdownMenuItem(
		text = {
			Box(modifier = Modifier.fillMaxWidth()) {
				CalendarLabel(
					name = label,
					color = color
				)
				if (isHidden) {
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
		onClick = onClick
	)
}

@Composable
fun <T> OptionPicker(
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
fun NumberPickerRow(
	label: String,
	value: Int,
	onValueChange: (Int) -> Unit,
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
				suffix = { Text(stringResource(R.string.label_window_days_suffix)) }
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

@Composable
fun SyncCheckbox(
	checked: Boolean,
	onCheckedChange: (Boolean) -> Unit
) {
	Checkbox(
		checked = checked,
		onCheckedChange = onCheckedChange
	)
}

interface DisplayOption {
	val displayLabelRes: Int
}

data class FrequencyOption(
	val minutes: Int,
	override val displayLabelRes: Int
) : DisplayOption

fun frequencyOptions(): List<FrequencyOption> {
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
