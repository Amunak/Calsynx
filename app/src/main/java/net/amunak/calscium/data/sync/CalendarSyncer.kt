package net.amunak.calscium.data.sync

import android.content.ContentResolver
import android.content.ContentValues
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
	fun sync(
		job: SyncJob,
		window: SyncWindow = SyncWindow.default()
	): SyncResult {
		val sourceEvents = querySourceEvents(job.sourceCalendarId, window)
		if (sourceEvents.isEmpty()) {
			val deleted = deleteOrphans(job.targetCalendarId, emptySet())
			return SyncResult(created = 0, updated = 0, deleted = deleted)
		}

		val sourceIds = sourceEvents.mapTo(LinkedHashSet()) { it.id }
		val targetMap = queryTargetEvents(job.targetCalendarId, sourceIds)

		var created = 0
		var updated = 0

		sourceEvents.forEach { source ->
			val existingTargetId = targetMap[source.id]
			if (existingTargetId == null) {
				if (insertTargetEvent(job.targetCalendarId, source)) {
					created += 1
				}
			} else {
				if (updateTargetEvent(existingTargetId, source)) {
					updated += 1
				}
			}
		}

		val deleted = deleteOrphans(job.targetCalendarId, sourceIds)
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
		sourceIds: Set<Long>
	): Map<Long, Long> {
		if (sourceIds.isEmpty()) return emptyMap()

		val projection = arrayOf(
			CalendarContract.Events._ID,
			CalendarContract.Events.SYNC_DATA1
		)

		val result = HashMap<Long, Long>(sourceIds.size)
		val chunks = sourceIds.chunked(500)
		chunks.forEach { chunk ->
			val placeholders = chunk.joinToString(",") { "?" }
			val selection = buildString {
				append("${CalendarContract.Events.CALENDAR_ID} = ?")
				append(" AND ${CalendarContract.Events.SYNC_DATA1} IN ($placeholders)")
				append(" AND ${CalendarContract.Events.DELETED} = 0")
			}
			val args = ArrayList<String>(chunk.size + 1).apply {
				add(targetCalendarId.toString())
				chunk.forEach { add(it.toString()) }
			}
			val cursor = resolver.query(
				CalendarContract.Events.CONTENT_URI,
				projection,
				selection,
				args.toTypedArray(),
				null
			) ?: return@forEach

			cursor.use {
				val idIndex = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
				val syncIndex = it.getColumnIndexOrThrow(CalendarContract.Events.SYNC_DATA1)
				while (it.moveToNext()) {
					val targetId = it.getLong(idIndex)
					val sourceId = it.getString(syncIndex)?.toLongOrNull()
					if (sourceId != null) {
						result[sourceId] = targetId
					}
				}
			}
		}

		return result
	}

	private fun insertTargetEvent(targetCalendarId: Long, source: SourceEvent): Boolean {
		val values = toContentValues(targetCalendarId, source)
		val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
		return uri != null
	}

	private fun updateTargetEvent(targetEventId: Long, source: SourceEvent): Boolean {
		val values = toContentValues(null, source)
		val uri = CalendarContract.Events.CONTENT_URI.buildUpon()
			.appendPath(targetEventId.toString())
			.build()
		return resolver.update(uri, values, null, null) > 0
	}

	private fun deleteOrphans(targetCalendarId: Long, sourceIds: Set<Long>): Int {
		val projection = arrayOf(
			CalendarContract.Events._ID,
			CalendarContract.Events.SYNC_DATA1
		)
		val selection = buildString {
			append("${CalendarContract.Events.CALENDAR_ID} = ?")
			append(" AND ${CalendarContract.Events.SYNC_DATA1} IS NOT NULL")
			append(" AND ${CalendarContract.Events.DELETED} = 0")
		}
		val args = arrayOf(targetCalendarId.toString())
		val cursor = resolver.query(
			CalendarContract.Events.CONTENT_URI,
			projection,
			selection,
			args,
			null
		) ?: return 0

		val toDelete = ArrayList<Long>()
		cursor.use {
			val idIndex = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
			val syncIndex = it.getColumnIndexOrThrow(CalendarContract.Events.SYNC_DATA1)
			while (it.moveToNext()) {
				val targetId = it.getLong(idIndex)
				val sourceId = it.getString(syncIndex)?.toLongOrNull()
				if (sourceId != null && !sourceIds.contains(sourceId)) {
					toDelete.add(targetId)
				}
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
		source: SourceEvent
	): ContentValues {
		return ContentValues().apply {
			if (targetCalendarId != null) {
				put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
				put(CalendarContract.Events.SYNC_DATA1, source.id.toString())
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
