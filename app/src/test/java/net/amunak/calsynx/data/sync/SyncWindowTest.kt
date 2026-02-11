package net.amunak.calsynx.data.sync

import net.amunak.calsynx.data.SyncJob
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class SyncWindowTest {
	@Test
	fun fromJob_usesProvidedClockAndDays() {
		val now = Instant.parse("2026-02-11T10:15:30Z")
		val job = SyncJob(
			id = 1L,
			sourceCalendarId = 10L,
			targetCalendarId = 20L,
			windowPastDays = 7,
			windowFutureDays = 30
		)

		val window = SyncWindow.fromJob(job, now)

		val expectedStart = now.minus(7, ChronoUnit.DAYS).toEpochMilli()
		val expectedEnd = now.plus(30, ChronoUnit.DAYS).toEpochMilli()
		assertEquals(expectedStart, window.startMillis)
		assertEquals(expectedEnd, window.endMillis)
	}

	@Test
	fun fromJob_clampsNegativeDays() {
		val now = Instant.parse("2026-02-11T10:15:30Z")
		val job = SyncJob(
			id = 2L,
			sourceCalendarId = 10L,
			targetCalendarId = 20L,
			windowPastDays = -4,
			windowFutureDays = -1
		)

		val window = SyncWindow.fromJob(job, now)

		assertEquals(now.toEpochMilli(), window.startMillis)
		assertEquals(now.toEpochMilli(), window.endMillis)
	}
}
