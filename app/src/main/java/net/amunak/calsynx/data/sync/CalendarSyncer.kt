package net.amunak.calsynx.data.sync

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.CalendarContract
import android.util.Log
import net.amunak.calsynx.data.EventMapping
import net.amunak.calsynx.data.EventMappingDao
import net.amunak.calsynx.data.SyncJob
import java.time.Instant
import java.time.temporal.ChronoUnit

data class SyncResult(
	val created: Int,
	val updated: Int,
	val deleted: Int,
	val sourceCount: Int,
	val targetCount: Int
)

class CalendarSyncer(
	private val resolver: ContentResolver,
	private val mappingDao: EventMappingDao
) {
	suspend fun sync(
		job: SyncJob,
		window: SyncWindow = SyncWindow.fromJob(job)
	): SyncResult {
		val sourceEvents = querySourceEvents(job.sourceCalendarId, window, job.syncAllEvents)
		if (sourceEvents.isEmpty()) {
			val mappings = mappingDao.getForJob(job.sourceCalendarId, job.targetCalendarId)
			val deleted = deleteOrphans(job, mappings, emptySet<Long>())
			val targetCount = mappingDao.countForJob(job.sourceCalendarId, job.targetCalendarId)
			return SyncResult(
				created = 0,
				updated = 0,
				deleted = deleted,
				sourceCount = 0,
				targetCount = targetCount
			)
		}

		val sourceIds = sourceEvents.mapTo(LinkedHashSet()) { it.id }
		val mappings = mappingDao.getForJob(job.sourceCalendarId, job.targetCalendarId)
		val targetExists = fetchExistingTargetIds(mappings.map { it.targetEventId })
		val mappingBySource = mappings.associateBy { it.sourceEventId }.toMutableMap()
		val missingTargets = mappings.filter { !targetExists.contains(it.targetEventId) }
		if (missingTargets.isNotEmpty()) {
			mappingDao.deleteByIds(missingTargets.map { it.id })
			missingTargets.forEach { mappingBySource.remove(it.sourceEventId) }
			Log.w(TAG, "Removed ${missingTargets.size} mappings with missing target events.")
		}

		var created = 0
		var updated = 0

		sourceEvents.forEach { source ->
			val existingMapping = mappingBySource[source.id]
			val existingTargetId = existingMapping?.targetEventId
			if (existingTargetId == null) {
				val targetId = insertTargetEvent(job.targetCalendarId, source)
				if (targetId != null) {
					upsertMapping(job, source.id, targetId, existingMapping?.id)
					created += 1
				}
			} else {
				if (updateTargetEvent(existingTargetId, source)) {
					updated += 1
				}
			}
		}

		val deleted = deleteOrphans(job, mappingBySource.values, sourceIds)
		val targetCount = mappingDao.countForJob(job.sourceCalendarId, job.targetCalendarId)
		return SyncResult(
			created = created,
			updated = updated,
			deleted = deleted,
			sourceCount = sourceEvents.size,
			targetCount = targetCount
		)
	}

	private fun querySourceEvents(
		sourceCalendarId: Long,
		window: SyncWindow,
		syncAllEvents: Boolean
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
		val selection = if (syncAllEvents) {
			buildString {
				append("${CalendarContract.Events.CALENDAR_ID} = ?")
				append(" AND ${CalendarContract.Events.DELETED} = 0")
			}
		} else {
			buildString {
				append("${CalendarContract.Events.CALENDAR_ID} = ?")
				append(" AND ${CalendarContract.Events.DELETED} = 0")
				append(" AND (")
				append("${CalendarContract.Events.RRULE} IS NOT NULL")
				append(" OR (")
				append("${CalendarContract.Events.DTSTART} <= ?")
				append(" AND (${CalendarContract.Events.DTEND} IS NULL OR ${CalendarContract.Events.DTEND} >= ?)")
				append("))")
			}
		}
		val selectionArgs = if (syncAllEvents) {
			arrayOf(sourceCalendarId.toString())
		} else {
			arrayOf(
				sourceCalendarId.toString(),
				window.endMillis.toString(),
				window.startMillis.toString()
			)
		}
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

	private fun insertTargetEvent(
		targetCalendarId: Long,
		source: SourceEvent
	): Long? {
		val values = toContentValues(targetCalendarId, source)
		val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
		if (uri == null) {
			Log.w(TAG, "Insert returned null uri for target calendar $targetCalendarId")
			return null
		}
		val eventId = uri.lastPathSegment?.toLongOrNull()
		if (eventId == null) {
			Log.w(TAG, "Insert returned invalid event uri: $uri")
			return null
		}
		return eventId
	}

	private fun updateTargetEvent(
		targetEventId: Long,
		source: SourceEvent
	): Boolean {
		val values = toContentValues(null, source)
		val uri = CalendarContract.Events.CONTENT_URI.buildUpon()
			.appendPath(targetEventId.toString())
			.build()
		val updated = resolver.update(uri, values, null, null) > 0
		if (!updated) {
			Log.w(TAG, "Failed to update target event $targetEventId")
		}
		return updated
	}

	private suspend fun deleteOrphans(
		job: SyncJob,
		mappings: Collection<EventMapping>,
		sourceIds: Set<Long>
	): Int {
		val toDelete = ArrayList<Long>()
		val mappingIds = ArrayList<Long>()
		mappings.forEach { mapping ->
			if (!sourceIds.contains(mapping.sourceEventId)) {
				toDelete.add(mapping.targetEventId)
				mappingIds.add(mapping.id)
			}
		}

		var deleted = 0
		toDelete.forEach { targetId ->
			val uri = CalendarContract.Events.CONTENT_URI.buildUpon()
				.appendPath(targetId.toString())
				.build()
			val result = resolver.delete(uri, null, null)
			if (result == 0) {
				Log.w(TAG, "Failed to delete target event $targetId")
			}
			deleted += result
		}
		if (mappingIds.isNotEmpty()) {
			mappingDao.deleteByIds(mappingIds)
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
			}
			put(CalendarContract.Events.TITLE, source.title)
			put(CalendarContract.Events.DTSTART, source.startMillis)
			if (source.endMillis != null) {
				put(CalendarContract.Events.DTEND, source.endMillis)
				putNull(CalendarContract.Events.DURATION)
			} else {
				putNull(CalendarContract.Events.DTEND)
				val defaultDuration = if (source.allDay) "P1D" else "PT1H"
				put(CalendarContract.Events.DURATION, defaultDuration)
			}
			put(CalendarContract.Events.ALL_DAY, if (source.allDay) 1 else 0)
			put(CalendarContract.Events.EVENT_TIMEZONE, source.timeZone)
			put(CalendarContract.Events.RRULE, source.rrule)
			put(CalendarContract.Events.EVENT_LOCATION, source.location)
			put(CalendarContract.Events.DESCRIPTION, source.description)
		}
	}

	private suspend fun upsertMapping(
		job: SyncJob,
		sourceEventId: Long,
		targetEventId: Long,
		existingId: Long?
	) {
		mappingDao.upsert(
			EventMapping(
				id = existingId ?: 0L,
				sourceEventId = sourceEventId,
				targetEventId = targetEventId,
				sourceCalendarId = job.sourceCalendarId,
				targetCalendarId = job.targetCalendarId
			)
		)
	}

	private fun fetchExistingTargetIds(targetIds: List<Long>): Set<Long> {
		if (targetIds.isEmpty()) return emptySet()
		val existing = HashSet<Long>(targetIds.size)
		targetIds.chunked(500).forEach { chunk ->
			val placeholders = chunk.joinToString(",") { "?" }
			val selection = "${CalendarContract.Events._ID} IN ($placeholders)"
			val cursor = resolver.query(
				CalendarContract.Events.CONTENT_URI,
				arrayOf(CalendarContract.Events._ID),
				selection,
				chunk.map { it.toString() }.toTypedArray(),
				null
			) ?: return@forEach
			cursor.use {
				val idIndex = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
				while (it.moveToNext()) {
					existing.add(it.getLong(idIndex))
				}
			}
		}
		return existing
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
		fun fromJob(job: SyncJob): SyncWindow {
			val now = Instant.now()
			val pastDays = job.windowPastDays.coerceAtLeast(0)
			val futureDays = job.windowFutureDays.coerceAtLeast(0)
			val start = now.minus(pastDays.toLong(), ChronoUnit.DAYS).toEpochMilli()
			val end = now.plus(futureDays.toLong(), ChronoUnit.DAYS).toEpochMilli()
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
