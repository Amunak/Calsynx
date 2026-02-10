package net.amunak.calscium.data.repository

import android.content.Context
import net.amunak.calscium.calendar.CalendarInfo
import net.amunak.calscium.calendar.CalendarProvider

class CalendarRepository(
	private val calendarProvider: CalendarProvider = CalendarProvider
) {
	fun getCalendars(context: Context, onlyVisible: Boolean = true): List<CalendarInfo> {
		return calendarProvider.getCalendars(context, onlyVisible)
	}
}
