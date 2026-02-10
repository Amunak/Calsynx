package net.amunak.calscium.data.sync

import android.content.ContentResolver
import android.content.ContentValues
import android.util.Log
import android.provider.CalendarContract
import net.amunak.calscium.data.SyncJob
import java.time.Instant
import java.time.temporal.ChronoUnit

data class SyncResult(
	val created: Int,
	val updated: Int,
	val deleted: Int
)

class CalendarSyncer(
	private val resolver: ContentResolver
) {
	private val propertyName = "calscium_source_key"

	fun sync(
		job: SyncJob,
		window: SyncWindow = SyncWindow.default()
	): SyncResult {
		val sourceEvents = querySourceEvents(job.sourceCalendarId, window)
		if (sourceEvents.isEmpty()) {
			val deleted = deleteOrphans(job.targetCalendarId, job.sourceCalendarId, emptySet<Long>())
			return SyncResult(created = 0, updated = 0, deleted = deleted)
		}

		val sourceIds = sourceEvents.mapTo(LinkedHashSet()) { it.id }
		val targetMap = queryTargetEvents(job.targetCalendarId, job.sourceCalendarId, sourceIds)

		var created = 0
		var updated = 0

		sourceEvents.forEach { source ->
			val existingTargetId = targetMap[source.id]
			if (existingTargetId == null) {
				if (insertTargetEvent(job.targetCalendarId, job.sourceCalendarId, source)) {
					created += 1
				}
			} else {
				if (updateTargetEvent(existingTargetId, job.sourceCalendarId, source)) {
					updated += 1
				}
			}
		}

		val deleted = deleteOrphans(job.targetCalendarId, job.sourceCalendarId, sourceIds)
		return SyncResult(created = created, updated = updated, deleted = deleted)
	}

	private fun querySourceEvents(
		sourceCalendarId: Long,
		window: SyncWindow
	): List<SourceEvent> {
		val projection = arrayOf(
			CalendarContract.Events._ID,
			CalendarContract.Events.TITLE,
			CalendarContract.Events.DTSTART,
			CalendarContract.Events.DTEND,
			CalendarContract.Events.ALL_DAY,
			CalendarContract.Events.EVENT_TIMEZONE,
			CalendarContract.Events.RRULE,
			CalendarContract.Events.EVENT_LOCATION,
			CalendarContract.Events.DESCRIPTION
		)
		val selection = buildString {
			append("${CalendarContract.Events.CALENDAR_ID} = ?")
			append(" AND ${CalendarContract.Events.DELETED} = 0")
			append(" AND (")
			append("${CalendarContract.Events.RRULE} IS NOT NULL")
			append(" OR (")
			append("${CalendarContract.Events.DTSTART} <= ?")
			append(" AND (${CalendarContract.Events.DTEND} IS NULL OR ${CalendarContract.Events.DTEND} >= ?)")
			append("))")
		}
		val selectionArgs = arrayOf(
			sourceCalendarId.toString(),
			window.endMillis.toString(),
			window.startMillis.toString()
		)
		val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

		val cursor = resolver.query(
			CalendarContract.Events.CONTENT_URI,
			projection,
			selection,
			selectionArgs,
			sortOrder
		) ?: return emptyList()

		return cursor.use {
			val idIndex = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
			val titleIndex = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
			val startIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
			val endIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
			val allDayIndex = it.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
			val tzIndex = it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_TIMEZONE)
			val rruleIndex = it.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
			val locationIndex = it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
			val descriptionIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)

			val events = ArrayList<SourceEvent>(it.count)
			while (it.moveToNext()) {
				events.add(
					SourceEvent(
						id = it.getLong(idIndex),
						title = it.getString(titleIndex),
						startMillis = it.getLong(startIndex),
						endMillis = if (it.isNull(endIndex)) null else it.getLong(endIndex),
						allDay = it.getInt(allDayIndex) == 1,
						timeZone = it.getString(tzIndex),
						rrule = it.getString(rruleIndex),
						location = it.getString(locationIndex),
						description = it.getString(descriptionIndex)
					)
				)
			}
			events
		}
	}

	private fun queryTargetEvents(
		targetCalendarId: Long,
		sourceCalendarId: Long,
		sourceIds: Set<Long>
	): Map<Long, Long> {
		if (sourceIds.isEmpty()) return emptyMap()

		val chunks = sourceIds.chunked(500)
		val targetMap = HashMap<Long, Long>(sourceIds.size)

		chunks.forEach { chunk ->
			val sourceKeys = chunk.map { buildSourceKey(sourceCalendarId, it) }
			val placeholders = sourceKeys.joinToString(",") { "?" }
			val selection = buildString {
				append("${CalendarContract.ExtendedProperties.NAME} = ?")
				append(" AND ${CalendarContract.ExtendedProperties.VALUE} IN ($placeholders)")
			}
			val args = ArrayList<String>(sourceKeys.size + 1).apply {
				add(propertyName)
				sourceKeys.forEach { add(it) }
			}
			val cursor = resolver.query(
				CalendarContract.ExtendedProperties.CONTENT_URI,
				arrayOf(
					CalendarContract.ExtendedProperties.EVENT_ID,
					CalendarContract.ExtendedProperties.VALUE
				),
				selection,
				args.toTypedArray(),
				null
			) ?: return@forEach

			val eventIds = ArrayList<Long>()
			val rows = ArrayList<Pair<Long, String>>()
			cursor.use {
				val eventIdIndex =
					it.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.EVENT_ID)
				val valueIndex =
					it.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.VALUE)
				while (it.moveToNext()) {
					val eventId = it.getLong(eventIdIndex)
					val value = it.getString(valueIndex) ?: continue
					eventIds.add(eventId)
					rows.add(eventId to value)
				}
			}

			val validEventIds = filterEventsByCalendar(eventIds, targetCalendarId)
			rows.forEach { (eventId, value) ->
				if (!validEventIds.contains(eventId)) return@forEach
				val sourceId = parseSourceId(sourceCalendarId, value) ?: return@forEach
				targetMap[sourceId] = eventId
			}
		}

		return targetMap
	}

	private fun insertTargetEvent(
		targetCalendarId: Long,
		sourceCalendarId: Long,
		source: SourceEvent
	): Boolean {
		val values = toContentValues(targetCalendarId, sourceCalendarId, source)
		val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values) ?: return false
		val eventId = uri.lastPathSegment?.toLongOrNull()
		if (eventId == null) {
			Log.w(TAG, "Insert returned invalid event uri: $uri")
			return false
		}
		return upsertSourceKey(eventId, buildSourceKey(sourceCalendarId, source.id))
	}

	private fun updateTargetEvent(
		targetEventId: Long,
		sourceCalendarId: Long,
		source: SourceEvent
	): Boolean {
		val values = toContentValues(null, sourceCalendarId, source)
		val uri = CalendarContract.Events.CONTENT_URI.buildUpon()
			.appendPath(targetEventId.toString())
			.build()
		val updated = resolver.update(uri, values, null, null) > 0
		if (!updated) return false
		return upsertSourceKey(targetEventId, buildSourceKey(sourceCalendarId, source.id))
	}

	private fun deleteOrphans(
		targetCalendarId: Long,
		sourceCalendarId: Long,
		sourceIds: Set<Long>
	): Int {
		val selection = buildString {
			append("${CalendarContract.ExtendedProperties.NAME} = ?")
			append(" AND ${CalendarContract.ExtendedProperties.VALUE} LIKE ?")
		}
		val args = arrayOf(propertyName, "${sourceCalendarId}:%")
		val cursor = resolver.query(
			CalendarContract.ExtendedProperties.CONTENT_URI,
			arrayOf(
				CalendarContract.ExtendedProperties.EVENT_ID,
				CalendarContract.ExtendedProperties.VALUE
			),
			selection,
			args,
			null
		) ?: return 0

		val toDelete = ArrayList<Long>()
		val eventIds = ArrayList<Long>()
		val rows = ArrayList<Pair<Long, String>>()
		cursor.use {
			val idIndex = it.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.EVENT_ID)
			val valueIndex = it.getColumnIndexOrThrow(CalendarContract.ExtendedProperties.VALUE)
			while (it.moveToNext()) {
				val eventId = it.getLong(idIndex)
				val value = it.getString(valueIndex) ?: continue
				eventIds.add(eventId)
				rows.add(eventId to value)
			}
		}

		val validEventIds = filterEventsByCalendar(eventIds, targetCalendarId)
		rows.forEach { (eventId, value) ->
			if (!validEventIds.contains(eventId)) return@forEach
			val sourceId = parseSourceId(sourceCalendarId, value) ?: return@forEach
			if (!sourceIds.contains(sourceId)) {
				toDelete.add(eventId)
			}
		}

		var deleted = 0
		toDelete.forEach { targetId ->
			val uri = CalendarContract.Events.CONTENT_URI.buildUpon()
				.appendPath(targetId.toString())
				.build()
			deleted += resolver.delete(uri, null, null)
		}
		return deleted
	}

	private fun toContentValues(
		targetCalendarId: Long?,
		sourceCalendarId: Long,
		source: SourceEvent
	): ContentValues {
		return ContentValues().apply {
			if (targetCalendarId != null) {
				put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
			}
			put(CalendarContract.Events.TITLE, source.title)
			put(CalendarContract.Events.DTSTART, source.startMillis)
			source.endMillis?.let { put(CalendarContract.Events.DTEND, it) }
				?: putNull(CalendarContract.Events.DTEND)
			put(CalendarContract.Events.ALL_DAY, if (source.allDay) 1 else 0)
			put(CalendarContract.Events.EVENT_TIMEZONE, source.timeZone)
			put(CalendarContract.Events.RRULE, source.rrule)
			put(CalendarContract.Events.EVENT_LOCATION, source.location)
			put(CalendarContract.Events.DESCRIPTION, source.description)
		}
	}

	private fun upsertSourceKey(eventId: Long, sourceKey: String): Boolean {
		val selection = buildString {
			append("${CalendarContract.ExtendedProperties.EVENT_ID} = ?")
			append(" AND ${CalendarContract.ExtendedProperties.NAME} = ?")
		}
		val args = arrayOf(eventId.toString(), propertyName)
		val cursor = resolver.query(
			CalendarContract.ExtendedProperties.CONTENT_URI,
			arrayOf(CalendarContract.ExtendedProperties._ID),
			selection,
			args,
			null
		)
		val existingId = cursor?.use {
			if (it.moveToFirst()) it.getLong(0) else null
		}

		val values = ContentValues().apply {
			put(CalendarContract.ExtendedProperties.EVENT_ID, eventId)
			put(CalendarContract.ExtendedProperties.NAME, propertyName)
			put(CalendarContract.ExtendedProperties.VALUE, sourceKey)
		}

		return if (existingId == null) {
			val uri = resolver.insert(CalendarContract.ExtendedProperties.CONTENT_URI, values)
			if (uri == null) {
				Log.w(TAG, "Failed to insert extended property for event $eventId")
				false
			} else {
				true
			}
		} else {
			val updated = resolver.update(
				CalendarContract.ExtendedProperties.CONTENT_URI,
				values,
				"${CalendarContract.ExtendedProperties._ID} = ?",
				arrayOf(existingId.toString())
			) > 0
			if (!updated) {
				Log.w(TAG, "Failed to update extended property for event $eventId")
			}
			updated
		}
	}

	private fun buildSourceKey(sourceCalendarId: Long, sourceEventId: Long): String {
		return "$sourceCalendarId:$sourceEventId"
	}

	private fun parseSourceId(sourceCalendarId: Long, value: String): Long? {
		val prefix = "$sourceCalendarId:"
		if (!value.startsWith(prefix)) return null
		return value.removePrefix(prefix).toLongOrNull()
	}

	private fun filterEventsByCalendar(
		eventIds: List<Long>,
		targetCalendarId: Long
	): Set<Long> {
		if (eventIds.isEmpty()) return emptySet()
		val validIds = HashSet<Long>(eventIds.size)
		eventIds.chunked(500).forEach { chunk ->
			val placeholders = chunk.joinToString(",") { "?" }
			val selection = buildString {
				append("${CalendarContract.Events._ID} IN ($placeholders)")
				append(" AND ${CalendarContract.Events.CALENDAR_ID} = ?")
				append(" AND ${CalendarContract.Events.DELETED} = 0")
			}
			val args = ArrayList<String>(chunk.size + 1).apply {
				chunk.forEach { add(it.toString()) }
				add(targetCalendarId.toString())
			}
			val cursor = resolver.query(
				CalendarContract.Events.CONTENT_URI,
				arrayOf(CalendarContract.Events._ID),
				selection,
				args.toTypedArray(),
				null
			) ?: return@forEach
			cursor.use {
				val idIndex = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
				while (it.moveToNext()) {
					validIds.add(it.getLong(idIndex))
				}
			}
		}
		return validIds
	}

	companion object {
		private const val TAG = "CalendarSyncer"
	}
}

data class SyncWindow(
	val startMillis: Long,
	val endMillis: Long
) {
	companion object {
		fun default(): SyncWindow {
			val now = Instant.now()
			val start = now.minus(7, ChronoUnit.DAYS).toEpochMilli()
			val end = now.plus(90, ChronoUnit.DAYS).toEpochMilli()
			return SyncWindow(start, end)
		}
	}
}

private data class SourceEvent(
	val id: Long,
	val title: String?,
	val startMillis: Long,
	val endMillis: Long?,
	val allDay: Boolean,
	val timeZone: String?,
	val rrule: String?,
	val location: String?,
	val description: String?
)
