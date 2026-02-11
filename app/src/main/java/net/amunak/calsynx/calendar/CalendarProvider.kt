package net.amunak.calsynx.calendar

import android.content.ContentResolver
import android.content.Context
import android.provider.CalendarContract

data class CalendarInfo(
	val id: Long,
	val displayName: String,
	val accountName: String?,
	val accountType: String?,
	val ownerAccount: String?,
	val color: Int?,
	val accessLevel: Int?,
	val isVisible: Boolean,
	val isSynced: Boolean
)

object CalendarProvider {
	private val projection = arrayOf(
		CalendarContract.Calendars._ID,
		CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
		CalendarContract.Calendars.ACCOUNT_NAME,
		CalendarContract.Calendars.ACCOUNT_TYPE,
		CalendarContract.Calendars.OWNER_ACCOUNT,
		CalendarContract.Calendars.CALENDAR_COLOR,
		CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
		CalendarContract.Calendars.VISIBLE,
		CalendarContract.Calendars.SYNC_EVENTS
	)

	fun getCalendars(
		context: Context,
		onlyVisible: Boolean = true
	): List<CalendarInfo> {
		val resolver = context.contentResolver
		val selection = if (onlyVisible) {
			"${CalendarContract.Calendars.VISIBLE} = 1"
		} else {
			null
		}

		return resolver.query(
			CalendarContract.Calendars.CONTENT_URI,
			projection,
			selection,
			null,
			CalendarContract.Calendars.CALENDAR_DISPLAY_NAME + " ASC"
		)?.use { cursor ->
			val idIndex = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
			val nameIndex =
				cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
			val accountNameIndex =
				cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
			val accountTypeIndex =
				cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE)
			val ownerIndex =
				cursor.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)
			val colorIndex =
				cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_COLOR)
			val accessIndex =
				cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)
			val visibleIndex =
				cursor.getColumnIndexOrThrow(CalendarContract.Calendars.VISIBLE)
			val syncIndex =
				cursor.getColumnIndexOrThrow(CalendarContract.Calendars.SYNC_EVENTS)

			val calendars = ArrayList<CalendarInfo>(cursor.count)
			while (cursor.moveToNext()) {
				calendars.add(
					CalendarInfo(
						id = cursor.getLong(idIndex),
						displayName = cursor.getString(nameIndex),
						accountName = cursor.getString(accountNameIndex),
						accountType = cursor.getString(accountTypeIndex),
						ownerAccount = cursor.getString(ownerIndex),
						color = if (cursor.isNull(colorIndex)) null else cursor.getInt(colorIndex),
						accessLevel = if (cursor.isNull(accessIndex)) null else cursor.getInt(accessIndex),
						isVisible = cursor.getInt(visibleIndex) == 1,
						isSynced = cursor.getInt(syncIndex) == 1
					)
				)
			}
			calendars
		} ?: emptyList()
	}
}
