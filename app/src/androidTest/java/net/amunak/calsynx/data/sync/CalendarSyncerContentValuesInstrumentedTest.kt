package net.amunak.calsynx.data.sync

import android.provider.CalendarContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import net.amunak.calsynx.data.SyncJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarSyncerContentValuesInstrumentedTest {
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

	@Test
	fun remapsOriginalIdForExceptions() {
		val job = SyncJob(
			sourceCalendarId = 1L,
			targetCalendarId = 2L
		)
		val values = buildEventContentValues(
			job = job,
			targetCalendarId = 2L,
			source = sourceEvent(
				duration = null,
				endMillis = 123L,
				originalId = 10L,
				originalInstanceTime = 111L,
				originalAllDay = false
			),
			targetOriginalId = 99L
		)

		assertEquals(99L, values.getAsLong(CalendarContract.Events.ORIGINAL_ID))
		assertEquals(111L, values.getAsLong(CalendarContract.Events.ORIGINAL_INSTANCE_TIME))
		assertEquals(0, values.getAsInteger(CalendarContract.Events.ORIGINAL_ALL_DAY))
	}

	@Test
	fun clearsOriginalIdWhenMissingMapping() {
		val job = SyncJob(
			sourceCalendarId = 1L,
			targetCalendarId = 2L
		)
		val values = buildEventContentValues(
			job = job,
			targetCalendarId = 2L,
			source = sourceEvent(
				duration = null,
				endMillis = 123L,
				originalId = 10L,
				originalInstanceTime = 111L,
				originalAllDay = true
			),
			targetOriginalId = null
		)

		assertTrue(values.containsKey(CalendarContract.Events.CALENDAR_ID))
		assertNull(values.getAsLong(CalendarContract.Events.ORIGINAL_ID))
		assertNull(values.getAsLong(CalendarContract.Events.ORIGINAL_INSTANCE_TIME))
		assertNull(values.getAsInteger(CalendarContract.Events.ORIGINAL_ALL_DAY))
	}

	private fun sourceEvent(
		duration: String?,
		endMillis: Long?,
		allDay: Boolean = false,
		accessLevel: Int? = null,
		eventColor: Int? = null,
		organizer: String? = null,
		originalId: Long? = null,
		originalInstanceTime: Long? = null,
		originalAllDay: Boolean? = null
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
			originalId = originalId,
			originalInstanceTime = originalInstanceTime,
			originalAllDay = originalAllDay,
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
