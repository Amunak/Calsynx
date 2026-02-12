package net.amunak.calsynx.data

import android.content.ContentResolver
import android.provider.CalendarContract

fun countEvents(
	resolver: ContentResolver,
	selection: String,
	selectionArgs: Array<String>
): Int {
	val projection = arrayOf(CalendarContract.Events._ID)
	val cursor = resolver.query(
		CalendarContract.Events.CONTENT_URI,
		projection,
		selection,
		selectionArgs,
		null
	) ?: return 0
	return cursor.use { it.count }
}
