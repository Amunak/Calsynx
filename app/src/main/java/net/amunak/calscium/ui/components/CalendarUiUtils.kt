package net.amunak.calscium.ui.components

import net.amunak.calscium.calendar.CalendarInfo
import android.provider.CalendarContract

fun groupCalendars(calendars: List<CalendarInfo>): Map<String, List<CalendarInfo>> {
	return calendars.groupBy { calendar ->
		if (calendar.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL) {
			return@groupBy "On device"
		}
		val name = calendar.accountName ?: "On device"
		val type = calendar.accountType?.takeIf { it.isNotBlank() }
		if (type != null) "$name · $type" else name
	}
}

fun sanitizeCalendarName(name: String): String {
	return name.replace(Regex("[\\r\\n]+"), " ").replace(Regex("\\s+"), " ").trim()
}
