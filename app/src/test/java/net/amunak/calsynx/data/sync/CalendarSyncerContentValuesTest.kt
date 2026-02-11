package net.amunak.calsynx.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarSyncerContentValuesTest {
	@Test
	fun durationClearsEndTime() {
		val timeFields = resolveEventTimeFields(
			source = sourceEvent(duration = "PT2H", endMillis = 123L)
		)

		assertEquals("PT2H", timeFields.duration)
		assertNull(timeFields.dtEnd)
	}

	@Test
	fun endTimeClearsDuration() {
		val timeFields = resolveEventTimeFields(
			source = sourceEvent(duration = null, endMillis = 456L)
		)

		assertEquals(456L, timeFields.dtEnd)
		assertNull(timeFields.duration)
	}

	@Test
	fun defaultDurationClearsEndTime() {
		val timeFields = resolveEventTimeFields(
			source = sourceEvent(duration = null, endMillis = null, allDay = true)
		)

		assertEquals("P1D", timeFields.duration)
		assertNull(timeFields.dtEnd)
	}

	private fun sourceEvent(
		duration: String?,
		endMillis: Long?,
		allDay: Boolean = false
	): SourceEvent {
		return SourceEvent(
			id = 1L,
			title = "Test",
			startMillis = 100L,
			endMillis = endMillis,
			duration = duration,
			allDay = allDay,
			timeZone = "UTC",
			endTimeZone = "UTC",
			rrule = null,
			exdate = null,
			exrule = null,
			rdate = null,
			originalId = null,
			originalInstanceTime = null,
			originalAllDay = null,
			status = null,
			location = null,
			description = null,
			availability = null,
			accessLevel = null,
			eventColor = null,
			organizer = null,
			ownerAccount = null
		)
	}
}
