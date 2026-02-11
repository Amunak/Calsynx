package net.amunak.calsynx.ui.formatters

import net.amunak.calsynx.calendar.CalendarInfo
import net.amunak.calsynx.data.SyncJob
import org.junit.Assert.assertEquals
import org.junit.Test

class JobLabelFormatterTest {
	@Test
	fun formatJobLabelUsesCalendarNames() {
		val calendars = listOf(
			CalendarInfo(
				id = 10L,
				displayName = "Work",
				accountName = null,
				accountType = null,
				ownerAccount = null,
				color = null,
				accessLevel = null,
				isVisible = true,
				isSynced = true
			),
			CalendarInfo(
				id = 20L,
				displayName = "Personal",
				accountName = null,
				accountType = null,
				ownerAccount = null,
				color = null,
				accessLevel = null,
				isVisible = true,
				isSynced = true
			)
		)
		val job = SyncJob(
			id = 7L,
			sourceCalendarId = 10L,
			targetCalendarId = 20L
		)

		val label = formatJobLabel(job, calendars)

		assertEquals("job 7 (Work → Personal)", label)
	}

	@Test
	fun resolveJobCalendarNamesUsesFallbacks() {
		val job = SyncJob(
			id = 2L,
			sourceCalendarId = 1L,
			targetCalendarId = 2L
		)

		val names = resolveJobCalendarNames(job, emptyMap())

		assertEquals("Unknown (1)", names.sourceName)
		assertEquals("Unknown (2)", names.targetName)
	}
}
