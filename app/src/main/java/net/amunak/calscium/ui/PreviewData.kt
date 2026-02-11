package net.amunak.calscium.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.amunak.calscium.R
import net.amunak.calscium.calendar.CalendarInfo
import net.amunak.calscium.data.SyncJob

object PreviewData {
	@Composable
	fun calendars(): List<CalendarInfo> {
		return listOf(
			CalendarInfo(
				id = 1L,
				displayName = stringResource(R.string.preview_calendar_work),
				accountName = "work@example.com",
				accountType = "com.google",
				ownerAccount = "work@example.com",
				color = null,
				accessLevel = null,
				isVisible = true,
				isSynced = true
			),
			CalendarInfo(
				id = 2L,
				displayName = stringResource(R.string.preview_calendar_personal),
				accountName = "me@example.com",
				accountType = "com.google",
				ownerAccount = "me@example.com",
				color = null,
				accessLevel = null,
				isVisible = true,
				isSynced = true
			)
		)
	}

	fun jobs(): List<SyncJob> {
		return listOf(
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
}
