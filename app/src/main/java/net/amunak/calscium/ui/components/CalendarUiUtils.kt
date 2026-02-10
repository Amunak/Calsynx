package net.amunak.calscium.ui.components

import net.amunak.calscium.calendar.CalendarInfo

fun groupCalendars(calendars: List<CalendarInfo>): Map<String, List<CalendarInfo>> {
	return calendars.groupBy { calendar ->
		val name = calendar.accountName ?: "On device"
		val type = calendar.accountType?.takeIf { it.isNotBlank() }
		if (type != null) "$name · $type" else name
	}
}
