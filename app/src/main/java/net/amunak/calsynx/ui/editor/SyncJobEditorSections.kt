package net.amunak.calsynx.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.amunak.calsynx.R
import net.amunak.calsynx.calendar.CalendarInfo
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.ui.PreviewData
import net.amunak.calsynx.ui.components.groupCalendars
import net.amunak.calsynx.ui.components.sanitizeCalendarName
import androidx.compose.ui.res.stringResource

@Composable
fun EditorSection(
	title: String,
	content: @Composable ColumnScope.() -> Unit
) {
	Surface(
		shape = MaterialTheme.shapes.large,
		color = MaterialTheme.colorScheme.surfaceContainerLow,
		modifier = Modifier.fillMaxWidth()
	) {
		Column(
			modifier = Modifier
				.fillMaxWidth()
				.padding(14.dp),
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
}

@Composable
fun AdvancedDisclosureSection(
	title: String,
	expanded: Boolean,
	onExpandedChange: (Boolean) -> Unit,
	content: @Composable ColumnScope.() -> Unit
) {
	EditorSection(title = title) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable { onExpandedChange(!expanded) },
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Text(
				text = if (expanded) {
					stringResource(R.string.label_hide_advanced)
				} else {
					stringResource(R.string.label_show_advanced)
				},
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Icon(
				imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
				contentDescription = null,
				tint = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		AnimatedVisibility(
			visible = expanded,
			enter = expandVertically(),
			exit = shrinkVertically()
		) {
			Column(
				verticalArrangement = Arrangement.spacedBy(12.dp),
				modifier = Modifier.padding(top = 8.dp)
			) {
				content()
			}
		}
	}
}

@Composable
fun AdvancedSubsection(
	title: String,
	content: @Composable ColumnScope.() -> Unit
) {
	Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
		Text(
			text = title,
			style = MaterialTheme.typography.titleSmall,
			color = MaterialTheme.colorScheme.onSurface
		)
		content()
	}
}

@Composable
internal fun CalendarPicker(
	label: String,
	calendars: List<CalendarInfo>,
	selected: CalendarInfo?,
	expanded: Boolean,
	onExpandedChange: (Boolean) -> Unit,
	onSelected: (CalendarInfo) -> Unit,
	jobs: List<SyncJob>,
	currentJobId: Long?,
	currentSource: CalendarInfo?,
	currentTarget: CalendarInfo?,
	mode: CalendarPickMode
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
			onSelected = onSelected,
			jobs = jobs,
			currentJobId = currentJobId,
			currentSource = currentSource,
			currentTarget = currentTarget,
			mode = mode
		)
	}
}

@Composable
internal fun CalendarDropdown(
	calendars: List<CalendarInfo>,
	selected: CalendarInfo?,
	expanded: Boolean,
	onExpandedChange: (Boolean) -> Unit,
	onSelected: (CalendarInfo) -> Unit,
	jobs: List<SyncJob>,
	currentJobId: Long?,
	currentSource: CalendarInfo?,
	currentTarget: CalendarInfo?,
	mode: CalendarPickMode
) {
	val grouped = groupCalendars(
		calendars,
		onDeviceLabel = stringResource(R.string.text_on_device),
		externalLabel = stringResource(R.string.text_external)
	)
	SyncDropdown(
		selectedLabel = sanitizeCalendarName(
			selected?.displayName ?: stringResource(R.string.label_select)
		),
		isExpanded = expanded,
		onExpandedChange = onExpandedChange
	) {
		grouped.forEach { (accountLabel, entries) ->
			SyncDropdownHeader(text = accountLabel)
			entries.forEach { calendar ->
				val enabled = isCalendarSelectable(
					calendar = calendar,
					mode = mode,
					source = currentSource,
					target = currentTarget,
					jobs = jobs,
					currentJobId = currentJobId
				)
				SyncDropdownItem(
					label = sanitizeCalendarName(calendar.displayName),
					color = calendar.color,
					isHidden = !calendar.isVisible,
					enabled = enabled,
					onClick = { onSelected(calendar) }
				)
			}
		}
	}
}

internal enum class CalendarPickMode {
	Source,
	Target
}

internal fun isCalendarSelectable(
	calendar: CalendarInfo,
	mode: CalendarPickMode,
	source: CalendarInfo?,
	target: CalendarInfo?,
	jobs: List<SyncJob>,
	currentJobId: Long?
): Boolean {
	if (mode == CalendarPickMode.Source && target?.id == calendar.id) return false
	if (mode == CalendarPickMode.Target && source?.id == calendar.id) return false
	val conflictingSource = jobs.any { it.targetCalendarId == calendar.id && it.id != currentJobId }
	val conflictingTarget = jobs.any { it.sourceCalendarId == calendar.id && it.id != currentJobId }
	if (mode == CalendarPickMode.Source && conflictingSource) return false
	if (mode == CalendarPickMode.Target && conflictingTarget) return false
	if (mode == CalendarPickMode.Source && target != null) {
		val duplicateJob = jobs.any {
			it.sourceCalendarId == calendar.id &&
				it.targetCalendarId == target.id &&
				it.id != currentJobId
		}
		if (duplicateJob) return false
	}
	if (mode == CalendarPickMode.Target && source != null) {
		val duplicateJob = jobs.any {
			it.sourceCalendarId == source.id &&
				it.targetCalendarId == calendar.id &&
				it.id != currentJobId
		}
		if (duplicateJob) return false
	}
	return true
}

@Preview(showBackground = true)
@Composable
private fun EditorSectionPreview() {
	EditorSection(title = "Section title") {
		Text(text = "Section content")
	}
}

@Preview(showBackground = true)
@Composable
private fun AdvancedDisclosurePreview() {
	val expandedState: MutableState<Boolean> = remember { mutableStateOf(true) }
	AdvancedDisclosureSection(
		title = "Advanced",
		expanded = expandedState.value,
		onExpandedChange = { expandedState.value = it }
	) {
		Text(text = "Advanced option")
	}
}

@Preview(showBackground = true)
@Composable
private fun AdvancedSubsectionPreview() {
	AdvancedSubsection(title = "Copy options") {
		Text(text = "Option")
	}
}

@Preview(showBackground = true)
@Composable
private fun CalendarPickerPreview() {
	val calendars = PreviewData.calendars()
	val selection = remember { mutableStateOf<CalendarInfo?>(calendars.first()) }
	CalendarPicker(
		label = "Calendar",
		calendars = calendars,
		selected = selection.value,
		expanded = false,
		onExpandedChange = {},
		onSelected = { selection.value = it },
		jobs = PreviewData.jobs(),
		currentJobId = null,
		currentSource = selection.value,
		currentTarget = calendars.last(),
		mode = CalendarPickMode.Source
	)
}
