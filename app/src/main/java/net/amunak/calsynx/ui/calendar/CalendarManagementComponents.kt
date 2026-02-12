package net.amunak.calsynx.ui.calendar

import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.amunak.calsynx.calendar.CalendarInfo
import net.amunak.calsynx.R
import net.amunak.calsynx.ui.PreviewData
import net.amunak.calsynx.ui.components.CalendarLabel
import androidx.compose.ui.res.stringResource

@Composable
fun CalendarRowCard(
	row: CalendarRowUi,
	onClick: () -> Unit
) {
	val incomingCalendar = row.incomingCalendars.firstOrNull()
	val outgoingCalendars = row.outgoingCalendars
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
			Row(
				modifier = Modifier.fillMaxWidth(),
				verticalAlignment = Alignment.CenterVertically,
				horizontalArrangement = Arrangement.SpaceBetween
			) {
				CalendarLabel(
					name = row.calendar.displayName,
					color = row.calendar.color
				)
				if (!row.calendar.isVisible) {
					Icon(
						imageVector = Icons.Default.VisibilityOff,
						contentDescription = stringResource(R.string.label_calendar_hidden),
						tint = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.size(16.dp)
					)
				}
			}
			Spacer(modifier = Modifier.height(6.dp))
			Text(
				text = stringResource(R.string.text_events, row.eventCount),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			if (row.incomingCalendars.isNotEmpty() && row.syncedCount > 0) {
				Text(
					text = stringResource(R.string.text_synced_entries, row.syncedCount),
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
			if (incomingCalendar != null) {
				CalendarLinkRow(
					label = stringResource(R.string.text_synced_input),
					calendars = row.incomingCalendars
				)
			}
			if (outgoingCalendars.isNotEmpty()) {
				CalendarLinkRow(
					label = stringResource(R.string.text_synced_output),
					calendars = outgoingCalendars
				)
			}
		}
	}
}

@Composable
fun CalendarMetaSection(
	row: CalendarRowUi,
	sourceCalendars: List<CalendarInfo>,
	targetCalendars: List<CalendarInfo>
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
			text = stringResource(R.string.text_type_label, typeLabel),
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Text(
			text = if (row.incomingCalendars.isNotEmpty() && row.syncedCount > 0) {
				stringResource(R.string.text_events_synced, row.eventCount, row.syncedCount)
			} else {
				stringResource(R.string.text_events, row.eventCount)
			},
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
	if (sourceCalendars.isNotEmpty()) {
		CalendarLinkRow(
			label = stringResource(R.string.text_synced_input),
			calendars = sourceCalendars
		)
	}
	if (targetCalendars.isNotEmpty()) {
		CalendarLinkRow(
			label = stringResource(R.string.text_synced_output),
			calendars = targetCalendars
		)
	}
	AccountDetailsSection(calendar = calendar)
}
}

@Composable
fun AccountDetailsSection(calendar: CalendarInfo) {
	val accountLabel = accountLabel(calendar)
	val ownerLabel = calendar.ownerAccount?.takeIf { it.isNotBlank() }
	val accessLabel = calendar.accessLevel?.toString()
	Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
		Text(
			text = stringResource(R.string.text_account_label, accountLabel),
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		if (ownerLabel != null) {
			Text(
				text = stringResource(R.string.text_owner_label, ownerLabel),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		if (accessLabel != null) {
			Text(
				text = stringResource(R.string.text_access_level_label, accessLabel),
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
	}
}

@Composable
fun ActionRow(
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
private fun CalendarLinkRow(
	label: String,
	calendars: List<CalendarInfo>
) {
	val uniqueCalendars = calendars.distinctBy { it.id }
	if (uniqueCalendars.isEmpty()) return
	Row(
		modifier = Modifier.fillMaxWidth(),
		verticalAlignment = Alignment.CenterVertically
	) {
		Text(
			text = label,
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant,
			modifier = Modifier.padding(end = 6.dp)
		)
		Row(
			modifier = Modifier
				.weight(1f)
				.horizontalScroll(rememberScrollState()),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			uniqueCalendars.forEach { calendar ->
				CalendarLabel(
					name = calendar.displayName,
					color = calendar.color,
					textStyle = MaterialTheme.typography.bodySmall,
					textColor = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
	}
}

@Composable
fun calendarTypeLabel(calendar: CalendarInfo): String {
	return when {
		calendar.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL ->
			stringResource(R.string.text_on_device)
		calendar.accountType.isNullOrBlank() -> stringResource(R.string.text_external)
		else -> calendar.accountType
	}
}

@Composable
fun accountLabel(calendar: CalendarInfo): String {
	if (calendar.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL) {
		return stringResource(R.string.text_on_device)
	}
	val name = calendar.accountName?.takeIf { it.isNotBlank() }
		?: stringResource(R.string.text_external)
	val type = calendar.accountType?.takeIf { it.isNotBlank() }
	return if (type != null) "$name · $type" else name
}

@Preview(showBackground = true)
@Composable
private fun CalendarRowCardPreview() {
	val calendar = PreviewData.calendars().first()
	val row = CalendarRowUi(
		calendar = calendar,
		eventCount = 12,
		syncedCount = 5,
		incomingCalendars = listOf(calendar),
		outgoingCalendars = PreviewData.calendars()
	)
	CalendarRowCard(row = row, onClick = {})
}

@Preview(showBackground = true)
@Composable
private fun CalendarMetaSectionPreview() {
	val calendar = PreviewData.calendars().first()
	val row = CalendarRowUi(
		calendar = calendar,
		eventCount = 12,
		syncedCount = 5,
		incomingCalendars = listOf(calendar),
		outgoingCalendars = PreviewData.calendars()
	)
	CalendarMetaSection(
		row = row,
		sourceCalendars = listOf(calendar),
		targetCalendars = PreviewData.calendars()
	)
}

@Preview(showBackground = true)
@Composable
private fun AccountDetailsSectionPreview() {
	AccountDetailsSection(calendar = PreviewData.calendars().first())
}

@Preview(showBackground = true)
@Composable
private fun ActionRowPreview() {
	ActionRow(
		icon = androidx.compose.material.icons.Icons.Default.Edit,
		label = stringResource(R.string.action_rename_calendar),
		onClick = {}
	)
}
