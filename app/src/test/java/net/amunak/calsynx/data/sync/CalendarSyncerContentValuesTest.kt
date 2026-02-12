package net.amunak.calsynx.data.sync

import android.provider.CalendarContract
import net.amunak.calsynx.data.SyncJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

	@Test
	fun disabledCopyOptionsClearFields() {
		val job = SyncJob(
			sourceCalendarId = 1L,
			targetCalendarId = 2L,
			copyPrivacy = false,
			copyEventColor = false,
			copyOrganizer = false
		)
		val values = buildEventContentValues(
			job = job,
			targetCalendarId = 2L,
			source = sourceEvent(
				duration = null,
				endMillis = 123L,
				accessLevel = CalendarContract.Events.ACCESS_PRIVATE,
				eventColor = 0xFFAA33.toInt(),
				organizer = "organizer@example.com"
			)
		)

		assertTrue(values.containsKey(CalendarContract.Events.ACCESS_LEVEL))
		assertTrue(values.containsKey(CalendarContract.Events.EVENT_COLOR))
		assertTrue(values.containsKey(CalendarContract.Events.ORGANIZER))
		assertNull(values.getAsInteger(CalendarContract.Events.ACCESS_LEVEL))
		assertNull(values.getAsInteger(CalendarContract.Events.EVENT_COLOR))
		assertNull(values.getAsString(CalendarContract.Events.ORGANIZER))
	}

	@Test
	fun copyOrganizerClearsWhenMissing() {
		val job = SyncJob(
			sourceCalendarId = 1L,
			targetCalendarId = 2L,
			copyOrganizer = true
		)
		val values = buildEventContentValues(
			job = job,
			targetCalendarId = 2L,
			source = sourceEvent(duration = null, endMillis = 123L, organizer = null)
		)

		assertTrue(values.containsKey(CalendarContract.Events.ORGANIZER))
		assertNull(values.getAsString(CalendarContract.Events.ORGANIZER))
	}

	private fun sourceEvent(
		duration: String?,
		endMillis: Long?,
		allDay: Boolean = false,
		accessLevel: Int? = null,
		eventColor: Int? = null,
		organizer: String? = null
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
			accessLevel = accessLevel,
			eventColor = eventColor,
			organizer = organizer,
			ownerAccount = null
		)
	}
}
