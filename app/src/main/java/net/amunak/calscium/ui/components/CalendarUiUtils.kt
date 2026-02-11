package net.amunak.calscium.ui.components

import net.amunak.calscium.calendar.CalendarInfo
import android.provider.CalendarContract

fun groupCalendars(
	calendars: List<CalendarInfo>,
	onDeviceLabel: String,
	externalLabel: String
): Map<String, List<CalendarInfo>> {
	return calendars.groupBy { calendar ->
		if (calendar.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL) {
			return@groupBy onDeviceLabel
		}
		val name = calendar.accountName ?: externalLabel
		val type = calendar.accountType?.takeIf { it.isNotBlank() }
		if (type != null) "$name · $type" else name
	}
}

fun sanitizeCalendarName(name: String): String {
	return name.replace(Regex("[\\r\\n]+"), " ").replace(Regex("\\s+"), " ").trim()
}
