package net.amunak.calsynx.data.sync

import android.provider.CalendarContract
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarQuerySpecTest {
	@Test
	fun recurringEventsIncludedRegardlessOfStartTime() {
		val window = SyncWindow(
			startMillis = 1_000L,
			endMillis = 2_000L
		)

		val query = buildEventQuerySpec(
			calendarId = 42L,
			window = window,
			syncAllEvents = false
		)

		assertTrue(query.selection.contains("${CalendarContract.Events.RRULE} IS NOT NULL"))
		assertEquals(
			arrayOf("42", "2000", "1000").toList(),
			query.selectionArgs.toList()
		)
	}
}
