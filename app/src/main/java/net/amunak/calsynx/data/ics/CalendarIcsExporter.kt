package net.amunak.calsynx.data.ics

import android.content.ContentResolver
import android.provider.CalendarContract
import net.amunak.calsynx.calendar.CalendarInfo
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CalendarExportResult(
	val eventCount: Int,
	val reminderCount: Int,
	val attendeeCount: Int
)

class CalendarIcsExporter {
	fun exportCalendar(
		resolver: ContentResolver,
		calendar: CalendarInfo,
		outputStream: OutputStream
	): CalendarExportResult {
		var eventCount = 0
		var reminderCount = 0
		var attendeeCount = 0
		OutputStreamWriter(outputStream, StandardCharsets.UTF_8).use { writer ->
			val icsWriter = IcsWriter(writer)
			icsWriter.writeLine("BEGIN", "VCALENDAR")
			icsWriter.writeLine("VERSION", "2.0")
			icsWriter.writeLine("PRODID", "-//Calsynx//Calendar Export//EN")
			icsWriter.writeLine("CALSCALE", "GREGORIAN")
			icsWriter.writeLine("X-WR-CALNAME", calendar.displayName)
			icsWriter.writeLine("X-WR-CALID", calendar.id.toString())

			val selection = "${CalendarContract.Events.CALENDAR_ID} = ? AND ${CalendarContract.Events.DELETED} = 0"
			val args = arrayOf(calendar.id.toString())
			val cursor = resolver.query(
				CalendarContract.Events.CONTENT_URI,
				null,
				selection,
				args,
				"${CalendarContract.Events.DTSTART} ASC"
			) ?: return CalendarExportResult(0, 0, 0)
			cursor.use {
				val columnNames = it.columnNames
				val colIndex = columnNames.withIndex().associate { entry -> entry.value to entry.index }
				while (it.moveToNext()) {
					eventCount += 1
					val values = columnNames.associateWith { column ->
						if (it.isNull(colIndex.getValue(column))) null else it.getString(colIndex.getValue(column))
					}
					icsWriter.writeLine("BEGIN", "VEVENT")
					val uid = values[CalendarContract.Events.UID_2445]
						?: "calsynx-${calendar.id}-${values[CalendarContract.Events._ID] ?: eventCount}"
					icsWriter.writeLine("UID", uid)
					writeStandardFields(icsWriter, values)

					values.forEach { (column, value) ->
						if (value != null) {
							icsWriter.writeLine("X-ANDROID-EVENT-${column.uppercase()}", value)
						}
					}

					val eventId = values[CalendarContract.Events._ID]?.toLongOrNull()
					if (eventId != null) {
						val reminders = queryRows(
							resolver,
							CalendarContract.Reminders.CONTENT_URI,
							CalendarContract.Reminders.EVENT_ID,
							eventId
						)
						reminders.forEachIndexed { index, reminder ->
							reminderCount += 1
							reminder.forEach { (column, value) ->
								if (value != null) {
									icsWriter.writeLine(
										"X-ANDROID-REMINDER-$index-${column.uppercase()}",
										value
									)
								}
							}
						}

						val attendees = queryRows(
							resolver,
							CalendarContract.Attendees.CONTENT_URI,
							CalendarContract.Attendees.EVENT_ID,
							eventId
						)
						attendees.forEachIndexed { index, attendee ->
							attendeeCount += 1
							attendee.forEach { (column, value) ->
								if (value != null) {
									icsWriter.writeLine(
										"X-ANDROID-ATTENDEE-$index-${column.uppercase()}",
										value
									)
								}
							}
						}
					}

					icsWriter.writeLine("END", "VEVENT")
				}
			}
			icsWriter.writeLine("END", "VCALENDAR")
		}
		return CalendarExportResult(eventCount, reminderCount, attendeeCount)
	}
}

private fun writeStandardFields(
	writer: IcsWriter,
	values: Map<String, String?>
) {
	val title = values[CalendarContract.Events.TITLE]
	val description = values[CalendarContract.Events.DESCRIPTION]
	val location = values[CalendarContract.Events.EVENT_LOCATION]
	val status = values[CalendarContract.Events.STATUS]
	val rrule = values[CalendarContract.Events.RRULE]
	val exdate = values[CalendarContract.Events.EXDATE]
	val exrule = values[CalendarContract.Events.EXRULE]
	val rdate = values[CalendarContract.Events.RDATE]
	val duration = values[CalendarContract.Events.DURATION]
	val allDay = values[CalendarContract.Events.ALL_DAY] == "1"
	val startMillis = values[CalendarContract.Events.DTSTART]?.toLongOrNull()
	val endMillis = values[CalendarContract.Events.DTEND]?.toLongOrNull()
	val tzId = values[CalendarContract.Events.EVENT_TIMEZONE]
	val endTzId = values[CalendarContract.Events.EVENT_END_TIMEZONE]

	if (!title.isNullOrBlank()) writer.writeLine("SUMMARY", title)
	if (!description.isNullOrBlank()) writer.writeLine("DESCRIPTION", description)
	if (!location.isNullOrBlank()) writer.writeLine("LOCATION", location)
	if (!status.isNullOrBlank()) writer.writeLine("STATUS", status)
	if (!rrule.isNullOrBlank()) writer.writeLine("RRULE", rrule)
	if (!exdate.isNullOrBlank()) writer.writeLine("EXDATE", exdate)
	if (!exrule.isNullOrBlank()) writer.writeLine("EXRULE", exrule)
	if (!rdate.isNullOrBlank()) writer.writeLine("RDATE", rdate)
	if (!duration.isNullOrBlank()) writer.writeLine("DURATION", duration)
	if (startMillis != null) {
		val params = if (allDay) mapOf("VALUE" to "DATE") else timeZoneParam(tzId)
		writer.writeLine("DTSTART", formatDateTime(startMillis, tzId, allDay), params)
	}
	if (endMillis != null) {
		val params = if (allDay) mapOf("VALUE" to "DATE") else timeZoneParam(endTzId ?: tzId)
		writer.writeLine("DTEND", formatDateTime(endMillis, endTzId ?: tzId, allDay), params)
	}
}

private fun timeZoneParam(tzId: String?): Map<String, String> {
	if (tzId.isNullOrBlank() || tzId == "UTC") return emptyMap()
	return mapOf("TZID" to tzId)
}

private fun formatDateTime(millis: Long, tzId: String?, allDay: Boolean): String {
	val zone = ZoneId.of(tzId ?: "UTC")
	return if (allDay) {
		Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
			.format(DateTimeFormatter.BASIC_ISO_DATE)
	} else {
		val formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
		val zoned = Instant.ofEpochMilli(millis).atZone(zone)
		val base = formatter.format(zoned)
		if (zone.id == "UTC") "${base}Z" else base
	}
}

private fun queryRows(
	resolver: ContentResolver,
	uri: android.net.Uri,
	eventIdColumn: String,
	eventId: Long
): List<Map<String, String?>> {
	val cursor = resolver.query(
		uri,
		null,
		"$eventIdColumn = ?",
		arrayOf(eventId.toString()),
		null
	) ?: return emptyList()
	return cursor.use {
		val columnNames = it.columnNames
		val colIndex = columnNames.withIndex().associate { entry -> entry.value to entry.index }
		val results = mutableListOf<Map<String, String?>>()
		while (it.moveToNext()) {
			val values = columnNames.associateWith { column ->
				if (it.isNull(colIndex.getValue(column))) null else it.getString(colIndex.getValue(column))
			}
			results.add(values)
		}
		results
	}
}

private class IcsWriter(private val writer: OutputStreamWriter) {
	fun writeLine(name: String, value: String, params: Map<String, String> = emptyMap()) {
		val escaped = escapeValue(value)
		val paramText = if (params.isEmpty()) {
			""
		} else {
			params.entries.joinToString(separator = "", prefix = ";") { "${it.key}=${it.value}" }
		}
		writeFolded("$name$paramText:$escaped")
	}

	private fun writeFolded(line: String) {
		if (line.length <= 75) {
			writer.write(line)
			writer.write("\r\n")
			return
		}
		var index = 0
		while (index < line.length) {
			val end = minOf(index + 75, line.length)
			val chunk = line.substring(index, end)
			writer.write(chunk)
			writer.write("\r\n")
			if (end < line.length) {
				writer.write(" ")
			}
			index = end
		}
	}

	private fun escapeValue(value: String): String {
		return buildString(value.length) {
			value.forEach { ch ->
				when (ch) {
					'\\' -> append("\\\\")
					';' -> append("\\;")
					',' -> append("\\,")
					'\n' -> append("\\n")
					'\r' -> Unit
					else -> append(ch)
				}
			}
		}
	}
}
