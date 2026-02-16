package net.amunak.calsynx.data.ics

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.CalendarContract
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CalendarImportResult(
	val eventCount: Int,
	val reminderCount: Int,
	val attendeeCount: Int
)

class CalendarIcsImporter {
	fun importCalendar(
		resolver: ContentResolver,
		calendarId: Long,
		inputStream: InputStream
	): CalendarImportResult {
		val parser = IcsParser()
		val parsed = parser.parse(inputStream)
		var insertedEvents = 0
		var insertedReminders = 0
		var insertedAttendees = 0
		val sourceIdMap = HashMap<Long, Long>()
		val pendingOriginal = mutableListOf<OriginalReference>()

		parsed.forEach { event ->
			val values = ContentValues()
			event.standardValues.forEach { (column, value) ->
				putParsedValue(values, column, value)
			}
			event.eventColumns.forEach { (column, value) ->
				putParsedValue(values, column, value)
			}
			clearUnsafeColumns(values)
			ensureCalendarId(values, calendarId)

			val originalSourceId = event.originalSourceId
			val originalInstanceTime = event.originalInstanceTime
			val originalAllDay = event.originalAllDay
			values.remove(CalendarContract.Events.ORIGINAL_ID)
			values.remove(CalendarContract.Events.ORIGINAL_INSTANCE_TIME)
			values.remove(CalendarContract.Events.ORIGINAL_ALL_DAY)

			val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return@forEach
			val eventId = uri.lastPathSegment?.toLongOrNull() ?: return@forEach
			insertedEvents += 1

			if (event.sourceId != null) {
				sourceIdMap[event.sourceId] = eventId
			}
			if (originalSourceId != null) {
				pendingOriginal.add(
					OriginalReference(
						eventId = eventId,
						sourceOriginalId = originalSourceId,
						originalInstanceTime = originalInstanceTime,
						originalAllDay = originalAllDay
					)
				)
			}

			event.reminders.forEach { reminder ->
				val reminderValues = ContentValues().apply {
					put(CalendarContract.Reminders.EVENT_ID, eventId)
				}
				reminder.forEach { (column, value) ->
					putParsedValue(reminderValues, column, value)
				}
				clearUnsafeReminderColumns(reminderValues)
				resolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
				insertedReminders += 1
			}

			event.attendees.forEach { attendee ->
				val attendeeValues = ContentValues().apply {
					put(CalendarContract.Attendees.EVENT_ID, eventId)
				}
				attendee.forEach { (column, value) ->
					putParsedValue(attendeeValues, column, value)
				}
				clearUnsafeAttendeeColumns(attendeeValues)
				resolver.insert(CalendarContract.Attendees.CONTENT_URI, attendeeValues)
				insertedAttendees += 1
			}
		}

		pendingOriginal.forEach { original ->
			val targetOriginalId = sourceIdMap[original.sourceOriginalId] ?: return@forEach
			val update = ContentValues().apply {
				put(CalendarContract.Events.ORIGINAL_ID, targetOriginalId)
				if (original.originalInstanceTime != null) {
					put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, original.originalInstanceTime)
				}
				if (original.originalAllDay != null) {
					put(CalendarContract.Events.ORIGINAL_ALL_DAY, if (original.originalAllDay) 1 else 0)
				}
			}
			val uri = CalendarContract.Events.CONTENT_URI.buildUpon()
				.appendPath(original.eventId.toString())
				.build()
			resolver.update(uri, update, null, null)
		}

		return CalendarImportResult(insertedEvents, insertedReminders, insertedAttendees)
	}
}

private data class ParsedEvent(
	val sourceId: Long?,
	val originalSourceId: Long?,
	val originalInstanceTime: Long?,
	val originalAllDay: Boolean?,
	val standardValues: Map<String, String>,
	val eventColumns: Map<String, String>,
	val reminders: List<Map<String, String>>,
	val attendees: List<Map<String, String>>
)

private data class OriginalReference(
	val eventId: Long,
	val sourceOriginalId: Long,
	val originalInstanceTime: Long?,
	val originalAllDay: Boolean?
)

private class IcsParser {
	private val dateFormatter = DateTimeFormatter.BASIC_ISO_DATE
	private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

	fun parse(inputStream: InputStream): List<ParsedEvent> {
		val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
		val lines = unfoldLines(reader.readLines())
		val events = mutableListOf<ParsedEvent>()
		var current: MutableParsedEvent? = null

		for (line in lines) {
			if (line == "BEGIN:VEVENT") {
				current = MutableParsedEvent()
				continue
			}
			if (line == "END:VEVENT") {
				current?.let { events.add(it.toParsed()) }
				current = null
				continue
			}
			if (current == null) continue
			val parts = line.split(":", limit = 2)
			if (parts.size != 2) continue
			val namePart = parts[0]
			val value = unescapeValue(parts[1])
			val nameSegments = namePart.split(";")
			val name = nameSegments[0]
			val params = nameSegments.drop(1).associate {
				val (k, v) = it.split("=", limit = 2).let { seg -> seg[0] to seg.getOrElse(1) { "" } }
				k to v
			}

			when {
				name.startsWith("X-ANDROID-EVENT-") -> {
					val column = name.removePrefix("X-ANDROID-EVENT-")
					current.eventColumns[column] = value
				}
				name.startsWith("X-ANDROID-REMINDER-") -> {
					parseIndexedColumn(name, "X-ANDROID-REMINDER-", current.reminders, value)
				}
				name.startsWith("X-ANDROID-ATTENDEE-") -> {
					parseIndexedColumn(name, "X-ANDROID-ATTENDEE-", current.attendees, value)
				}
				else -> {
					current.standardValues[name] = value
					if (name == "DTSTART") {
						current.allDay = params["VALUE"] == "DATE"
						current.timeZoneId = params["TZID"]
					}
				}
			}
		}

		return events
	}

	private fun parseIndexedColumn(
		name: String,
		prefix: String,
		buckets: MutableList<MutableMap<String, String>>,
		value: String
	) {
		val remainder = name.removePrefix(prefix)
		val parts = remainder.split("-", limit = 2)
		if (parts.size != 2) return
		val index = parts[0].toIntOrNull() ?: return
		val column = parts[1]
		while (buckets.size <= index) {
			buckets.add(mutableMapOf())
		}
		buckets[index][column] = value
	}

	private fun unfoldLines(lines: List<String>): List<String> {
		if (lines.isEmpty()) return emptyList()
		val unfolded = mutableListOf<String>()
		var buffer = StringBuilder()
		lines.forEach { line ->
			if (line.startsWith(" ") || line.startsWith("\t")) {
				buffer.append(line.trimStart())
			} else {
				if (buffer.isNotEmpty()) {
					unfolded.add(buffer.toString())
				}
				buffer = StringBuilder(line)
			}
		}
		if (buffer.isNotEmpty()) {
			unfolded.add(buffer.toString())
		}
		return unfolded
	}

	private fun unescapeValue(value: String): String {
		return value.replace("\\n", "\n")
			.replace("\\,", ",")
			.replace("\\;", ";")
			.replace("\\\\", "\\")
	}

	private inner class MutableParsedEvent {
		val eventColumns = mutableMapOf<String, String>()
		val standardValues = mutableMapOf<String, String>()
		val reminders = mutableListOf<MutableMap<String, String>>()
		val attendees = mutableListOf<MutableMap<String, String>>()
		var allDay: Boolean? = null
		var timeZoneId: String? = null

		fun toParsed(): ParsedEvent {
			val sourceId = eventColumns[CalendarContract.Events._ID]?.toLongOrNull()
			val originalSourceId = eventColumns[CalendarContract.Events.ORIGINAL_ID]?.toLongOrNull()
			val originalInstanceTime = eventColumns[CalendarContract.Events.ORIGINAL_INSTANCE_TIME]?.toLongOrNull()
			val originalAllDay = eventColumns[CalendarContract.Events.ORIGINAL_ALL_DAY]?.toIntOrNull()?.let { it == 1 }
			val convertedStandard = mapStandardValues(standardValues, allDay, timeZoneId)
			return ParsedEvent(
				sourceId = sourceId,
				originalSourceId = originalSourceId,
				originalInstanceTime = originalInstanceTime,
				originalAllDay = originalAllDay,
				standardValues = convertedStandard,
				eventColumns = eventColumns.toMap(),
				reminders = reminders.map { it.toMap() },
				attendees = attendees.map { it.toMap() }
			)
		}
	}

	private fun mapStandardValues(
		standard: Map<String, String>,
		allDay: Boolean?,
		timeZoneId: String?
	): Map<String, String> {
		val result = mutableMapOf<String, String>()
		standard["SUMMARY"]?.let { result[CalendarContract.Events.TITLE] = it }
		standard["DESCRIPTION"]?.let { result[CalendarContract.Events.DESCRIPTION] = it }
		standard["LOCATION"]?.let { result[CalendarContract.Events.EVENT_LOCATION] = it }
		standard["STATUS"]?.let { result[CalendarContract.Events.STATUS] = it }
		standard["RRULE"]?.let { result[CalendarContract.Events.RRULE] = it }
		standard["EXDATE"]?.let { result[CalendarContract.Events.EXDATE] = it }
		standard["EXRULE"]?.let { result[CalendarContract.Events.EXRULE] = it }
		standard["RDATE"]?.let { result[CalendarContract.Events.RDATE] = it }
		standard["DURATION"]?.let { result[CalendarContract.Events.DURATION] = it }
		standard["UID"]?.let { result[CalendarContract.Events.UID_2445] = it }
		standard["DTSTART"]?.let { value ->
			parseDateTime(value, allDay, timeZoneId)?.let { result[CalendarContract.Events.DTSTART] = it.toString() }
		}
		standard["DTEND"]?.let { value ->
			parseDateTime(value, allDay, timeZoneId)?.let { result[CalendarContract.Events.DTEND] = it.toString() }
		}
		allDay?.let { result[CalendarContract.Events.ALL_DAY] = if (it) "1" else "0" }
		timeZoneId?.let { result[CalendarContract.Events.EVENT_TIMEZONE] = it }
		return result
	}

	private fun parseDateTime(value: String, allDay: Boolean?, tzId: String?): Long? {
		return if (allDay == true) {
			LocalDate.parse(value, dateFormatter)
				.atStartOfDay(ZoneId.of(tzId ?: "UTC"))
				.toInstant()
				.toEpochMilli()
		} else {
			val trimmed = value.removeSuffix("Z")
			val zone = if (value.endsWith("Z")) ZoneId.of("UTC") else ZoneId.of(tzId ?: "UTC")
			LocalDateTime.parse(trimmed, dateTimeFormatter)
				.atZone(zone)
				.toInstant()
				.toEpochMilli()
		}
	}
}

private fun putParsedValue(values: ContentValues, column: String, rawValue: String) {
	if (rawValue.isBlank()) return
	val numericColumns = setOf(
		CalendarContract.Events.DTSTART,
		CalendarContract.Events.DTEND,
		CalendarContract.Events.ORIGINAL_ID,
		CalendarContract.Events.ORIGINAL_INSTANCE_TIME,
		CalendarContract.Events.LAST_DATE,
		CalendarContract.Events.EVENT_COLOR,
		CalendarContract.Events.ALL_DAY,
		CalendarContract.Events.STATUS,
		CalendarContract.Events.AVAILABILITY,
		CalendarContract.Events.ACCESS_LEVEL,
		CalendarContract.Reminders.MINUTES,
		CalendarContract.Reminders.METHOD
	)
	val value = rawValue.trim()
	if (numericColumns.contains(column)) {
		val numeric = value.toLongOrNull()
		if (numeric != null) {
			values.put(column, numeric)
			return
		}
	}
	values.put(column, value)
}

private fun clearUnsafeColumns(values: ContentValues) {
	val blocked = setOf(
		CalendarContract.Events._ID,
		CalendarContract.Events.CALENDAR_ID,
		CalendarContract.Events.DELETED,
		CalendarContract.Events.DIRTY,
		CalendarContract.Events.SYNC_DATA1,
		CalendarContract.Events.SYNC_DATA2,
		CalendarContract.Events.SYNC_DATA3,
		CalendarContract.Events.SYNC_DATA4,
		CalendarContract.Events.SYNC_DATA5,
		CalendarContract.Events.SYNC_DATA6,
		CalendarContract.Events.SYNC_DATA7,
		CalendarContract.Events.SYNC_DATA8,
		CalendarContract.Events.SYNC_DATA9,
		CalendarContract.Events.SYNC_DATA10
	)
	blocked.forEach { values.remove(it) }
}

private fun ensureCalendarId(values: ContentValues, calendarId: Long) {
	values.put(CalendarContract.Events.CALENDAR_ID, calendarId)
}

private fun clearUnsafeReminderColumns(values: ContentValues) {
	values.remove(CalendarContract.Reminders._ID)
	values.remove(CalendarContract.Reminders.EVENT_ID)
}

private fun clearUnsafeAttendeeColumns(values: ContentValues) {
	values.remove(CalendarContract.Attendees._ID)
	values.remove(CalendarContract.Attendees.EVENT_ID)
}
