package net.amunak.calsynx.data.repository

import android.provider.CalendarContract
import net.amunak.calsynx.calendar.CalendarInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarRepositoryTest {
	@Test
	fun buildCalendarDeleteUriIncludesSyncAdapterParams() {
		val repository = CalendarRepository()
		val calendar = CalendarInfo(
			id = 42L,
			displayName = "Local",
			accountName = "Offline Calendar",
			accountType = CalendarContract.ACCOUNT_TYPE_LOCAL,
			ownerAccount = "Offline Calendar",
			color = null,
			accessLevel = null,
			isVisible = true,
			isSynced = true
		)

		val uri = repository.buildCalendarDeleteUri(calendar)

		assertEquals(calendar.accountName, uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME))
		assertEquals(calendar.accountType, uri.getQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE))
		assertEquals("true", uri.getQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER))
	}

	@Test
	fun buildDeleteSelectionUsesCalendarId() {
		val repository = CalendarRepository()

		val (selection, args) = repository.buildDeleteSelection(42L)

		assertTrue(selection.contains(CalendarContract.Calendars._ID))
		assertEquals(1, args.size)
		assertEquals("42", args[0])
	}
}
