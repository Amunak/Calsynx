package net.amunak.calscium.data.repository

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import net.amunak.calscium.calendar.CalendarInfo
import net.amunak.calscium.calendar.CalendarProvider
import java.util.TimeZone

class CalendarRepository(
	private val calendarProvider: CalendarProvider = CalendarProvider
) {
	fun getCalendars(context: Context, onlyVisible: Boolean = true): List<CalendarInfo> {
		return calendarProvider.getCalendars(context, onlyVisible)
	}

	fun countEvents(resolver: ContentResolver, calendarId: Long): Int {
		val projection = arrayOf(CalendarContract.Events._ID)
		val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DELETED} = 0"
		val args = arrayOf(calendarId.toString())
		val cursor = resolver.query(
			CalendarContract.Events.CONTENT_URI,
			projection,
			selection,
			args,
			null
		) ?: return 0
		return cursor.use { it.count }
	}

	fun purgeEvents(resolver: ContentResolver, calendarId: Long): Int {
		val selection = "${CalendarContract.Events.CALENDAR_ID} = ?"
		val args = arrayOf(calendarId.toString())
		return resolver.delete(CalendarContract.Events.CONTENT_URI, selection, args)
	}

	fun updateCalendarName(
		resolver: ContentResolver,
		calendar: CalendarInfo,
		newName: String
	): Boolean {
		val values = ContentValues().apply {
			put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, newName)
		}
		return updateCalendar(resolver, calendar, values)
	}

	fun updateCalendarColor(
		resolver: ContentResolver,
		calendar: CalendarInfo,
		color: Int
	): Boolean {
		val values = ContentValues().apply {
			put(CalendarContract.Calendars.CALENDAR_COLOR, color)
		}
		return updateCalendar(resolver, calendar, values)
	}

	fun deleteCalendar(
		resolver: ContentResolver,
		calendar: CalendarInfo
	): Boolean {
		val uri = asSyncAdapter(
			CalendarContract.Calendars.CONTENT_URI,
			calendar.accountName,
			calendar.accountType
		)
		val deleted = resolver.delete(uri, null, null)
		if (deleted <= 0) {
			Log.w(TAG, "Failed to delete calendar ${calendar.id}")
		}
		return deleted > 0
	}

	fun createLocalCalendar(
		resolver: ContentResolver,
		displayName: String,
		color: Int,
		accountName: String
	): Uri? {
		val values = ContentValues().apply {
			put(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
			put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
			put(CalendarContract.Calendars.NAME, displayName)
			put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, displayName)
			put(CalendarContract.Calendars.CALENDAR_COLOR, color)
			put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
			put(CalendarContract.Calendars.OWNER_ACCOUNT, accountName)
			put(CalendarContract.Calendars.VISIBLE, 1)
			put(CalendarContract.Calendars.SYNC_EVENTS, 1)
			put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
		}
		val uri = asSyncAdapter(
			CalendarContract.Calendars.CONTENT_URI,
			accountName,
			CalendarContract.ACCOUNT_TYPE_LOCAL
		)
		return resolver.insert(uri, values)
	}

	fun resolveLocalAccountName(context: Context): String {
		val existing = calendarProvider.getCalendars(context, onlyVisible = false)
			.firstOrNull { it.accountType == CalendarContract.ACCOUNT_TYPE_LOCAL }
			?.accountName
		return existing?.takeIf { it.isNotBlank() } ?: LOCAL_ACCOUNT_NAME
	}

	private fun updateCalendar(
		resolver: ContentResolver,
		calendar: CalendarInfo,
		values: ContentValues
	): Boolean {
		val uri = asSyncAdapter(
			CalendarContract.Calendars.CONTENT_URI.buildUpon()
				.appendPath(calendar.id.toString())
				.build(),
			calendar.accountName,
			calendar.accountType
		)
		val updated = resolver.update(uri, values, null, null)
		if (updated <= 0) {
			Log.w(TAG, "Failed to update calendar ${calendar.id}")
		}
		return updated > 0
	}

	private fun asSyncAdapter(
		uri: Uri,
		accountName: String?,
		accountType: String?
	): Uri {
		if (accountName.isNullOrBlank() || accountType.isNullOrBlank()) return uri
		return uri.buildUpon()
			.appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
			.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, accountName)
			.appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, accountType)
			.build()
	}

	companion object {
		private const val TAG = "CalendarRepository"
		private const val LOCAL_ACCOUNT_NAME = "Local"
	}
}
