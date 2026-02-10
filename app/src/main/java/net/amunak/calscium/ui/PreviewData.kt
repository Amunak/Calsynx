package net.amunak.calscium.ui

import net.amunak.calscium.calendar.CalendarInfo
import net.amunak.calscium.data.SyncJob

object PreviewData {
	val calendars = listOf(
		CalendarInfo(
			id = 1L,
			displayName = "Work",
			accountName = "work@example.com",
			accountType = "com.google",
			ownerAccount = "work@example.com",
			color = null
		),
		CalendarInfo(
			id = 2L,
			displayName = "Personal",
			accountName = "me@example.com",
			accountType = "com.google",
			ownerAccount = "me@example.com",
			color = null
		)
	)

	val jobs = listOf(
		SyncJob(
			id = 1L,
			sourceCalendarId = 1L,
			targetCalendarId = 2L,
			windowPastDays = 7,
			windowFutureDays = 90,
			frequencyMinutes = 240,
			lastSyncTimestamp = System.currentTimeMillis() - 3_600_000L,
			isActive = true
		),
		SyncJob(
			id = 2L,
			sourceCalendarId = 2L,
			targetCalendarId = 1L,
			windowPastDays = 14,
			windowFutureDays = 60,
			frequencyMinutes = 1440,
			lastSyncTimestamp = null,
			isActive = false
		)
	)
}
