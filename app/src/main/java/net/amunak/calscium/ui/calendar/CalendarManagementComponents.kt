package net.amunak.calscium.ui.calendar

import android.provider.CalendarContract
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.amunak.calscium.calendar.CalendarInfo
import net.amunak.calscium.ui.PreviewData
import net.amunak.calscium.ui.components.CalendarLabel

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
			CalendarLabel(
				name = row.calendar.displayName,
				color = row.calendar.color
			)
			Spacer(modifier = Modifier.height(6.dp))
			Text(
				text = "${row.eventCount} events",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			if (row.incomingCalendars.isNotEmpty() && row.syncedCount > 0) {
				Text(
					text = "Synced entries: ${row.syncedCount}",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
			if (incomingCalendar != null) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						text = "Synced (input):",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(end = 6.dp)
					)
					CalendarLabel(
						name = incomingCalendar.displayName,
						color = incomingCalendar.color,
						textStyle = MaterialTheme.typography.bodySmall,
						textColor = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
			if (outgoingCalendars.isNotEmpty()) {
				val target = outgoingCalendars.first()
				val extraCount = outgoingCalendars.size - 1
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						text = "Synced (output):",
						style = MaterialTheme.typography.bodySmall,
						color = MaterialTheme.colorScheme.onSurfaceVariant,
						modifier = Modifier.padding(end = 6.dp)
					)
					CalendarLabel(
						name = target.displayName,
						color = target.color,
						textStyle = MaterialTheme.typography.bodySmall,
						textColor = MaterialTheme.colorScheme.onSurfaceVariant
					)
					if (extraCount > 0) {
						Text(
							text = " +$extraCount",
							style = MaterialTheme.typography.bodySmall,
							color = MaterialTheme.colorScheme.onSurfaceVariant
						)
					}
				}
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
			text = "Type: $typeLabel",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		Text(
			text = if (row.incomingCalendars.isNotEmpty() && row.syncedCount > 0) {
				"Events: ${row.eventCount} · Synced: ${row.syncedCount}"
			} else {
				"Events: ${row.eventCount}"
			},
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		val source = sourceCalendars.firstOrNull()
		if (source != null) {
			Row(verticalAlignment = Alignment.CenterVertically) {
				Text(
					text = "Synced (input):",
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					modifier = Modifier.padding(end = 6.dp)
				)
				CalendarLabel(
					name = source.displayName,
					color = source.color,
					textStyle = MaterialTheme.typography.bodySmall,
					textColor = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
		}
		if (targetCalendars.isNotEmpty()) {
			Text(
				text = "Synced (output):",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
			Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
				targetCalendars.distinctBy { it.id }.forEach { target ->
					CalendarLabel(
						name = target.displayName,
						color = target.color,
						textStyle = MaterialTheme.typography.bodySmall,
						textColor = MaterialTheme.colorScheme.onSurfaceVariant
					)
				}
			}
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
			text = "Account: $accountLabel",
			style = MaterialTheme.typography.bodySmall,
			color = MaterialTheme.colorScheme.onSurfaceVariant
		)
		if (ownerLabel != null) {
			Text(
				text = "Owner: $ownerLabel",
				style = MaterialTheme.typography.bodySmall,
				color = MaterialTheme.colorScheme.onSurfaceVariant
			)
		}
		if (accessLabel != null) {
			Text(
				text = "Access level: $accessLabel",
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

fun calendarTypeLabel(calendar: CalendarInfo): String {
	return when {
		calendar.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL -> "On device"
		calendar.accountType.isNullOrBlank() -> "External"
		else -> calendar.accountType
	}
}

fun accountLabel(calendar: CalendarInfo): String {
	if (calendar.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL) return "On device"
	val name = calendar.accountName?.takeIf { it.isNotBlank() } ?: "External"
	val type = calendar.accountType?.takeIf { it.isNotBlank() }
	return if (type != null) "$name · $type" else name
}

@Preview(showBackground = true)
@Composable
private fun CalendarRowCardPreview() {
	val calendar = PreviewData.calendars.first()
	val row = CalendarRowUi(
		calendar = calendar,
		eventCount = 12,
		syncedCount = 5,
		incomingCalendars = listOf(calendar),
		outgoingCalendars = PreviewData.calendars
	)
	CalendarRowCard(row = row, onClick = {})
}

@Preview(showBackground = true)
@Composable
private fun CalendarMetaSectionPreview() {
	val calendar = PreviewData.calendars.first()
	val row = CalendarRowUi(
		calendar = calendar,
		eventCount = 12,
		syncedCount = 5,
		incomingCalendars = listOf(calendar),
		outgoingCalendars = PreviewData.calendars
	)
	CalendarMetaSection(
		row = row,
		sourceCalendars = listOf(calendar),
		targetCalendars = PreviewData.calendars
	)
}

@Preview(showBackground = true)
@Composable
private fun AccountDetailsSectionPreview() {
	AccountDetailsSection(calendar = PreviewData.calendars.first())
}

@Preview(showBackground = true)
@Composable
private fun ActionRowPreview() {
	ActionRow(
		icon = androidx.compose.material.icons.Icons.Default.Edit,
		label = "Rename calendar",
		onClick = {}
	)
}
