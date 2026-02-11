package net.amunak.calsynx.ui.editor

import android.text.format.DateFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerRow(
	label: String,
	timeMinutes: Int,
	onTimeChange: (Int) -> Unit,
	enabled: Boolean = true
) {
	var showDialog by remember { mutableStateOf(false) }
	val context = LocalContext.current
	val timeLabel = remember(timeMinutes) { formatTimeLabel(context, timeMinutes) }
	Column {
		Text(
			text = label,
			style = MaterialTheme.typography.labelMedium
		)
		OutlinedButton(
			onClick = { showDialog = true },
			enabled = enabled,
			modifier = Modifier.fillMaxWidth()
		) {
			Text(text = timeLabel)
		}
	}
	if (showDialog) {
		val initialHour = (timeMinutes / 60).coerceIn(0, 23)
		val initialMinute = (timeMinutes % 60).coerceIn(0, 59)
		val state = rememberTimePickerState(
			initialHour = initialHour,
			initialMinute = initialMinute,
			is24Hour = DateFormat.is24HourFormat(context)
		)
		AlertDialog(
			onDismissRequest = { showDialog = false },
			confirmButton = {
				TextButton(
					onClick = {
						onTimeChange(state.hour * 60 + state.minute)
						showDialog = false
					}
				) {
					Text(text = context.getString(android.R.string.ok))
				}
			},
			dismissButton = {
				TextButton(onClick = { showDialog = false }) {
					Text(text = context.getString(android.R.string.cancel))
				}
			},
			text = { TimePicker(state = state) }
		)
	}
}

private fun formatTimeLabel(context: android.content.Context, timeMinutes: Int): String {
	val calendar = Calendar.getInstance().apply {
		set(Calendar.HOUR_OF_DAY, (timeMinutes / 60).coerceIn(0, 23))
		set(Calendar.MINUTE, (timeMinutes % 60).coerceIn(0, 59))
	}
	return DateFormat.getTimeFormat(context).format(calendar.time)
}

@Preview(showBackground = true)
@Composable
private fun TimePickerRowPreview() {
	Column {
		TimePickerRow(
			label = "Notify at",
			timeMinutes = 20 * 60,
			onTimeChange = {},
			enabled = true
		)
		Spacer(modifier = Modifier.height(12.dp))
		TimePickerRow(
			label = "Notify at",
			timeMinutes = 9 * 60,
			onTimeChange = {},
			enabled = false
		)
	}
}
