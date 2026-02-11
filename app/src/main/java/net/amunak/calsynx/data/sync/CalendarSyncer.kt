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
	val targetCount: Int,
	val targetTotalCount: Int
)

class CalendarSyncer(
	private val resolver: ContentResolver,
	private val mappingDao: EventMappingDao,
	private val eventsUri: android.net.Uri = CalendarContract.Events.CONTENT_URI
) {
	suspend fun sync(
		job: SyncJob,
		window: SyncWindow = SyncWindow.fromJob(job)
	): SyncResult {
		val sourceEvents = querySourceEvents(job.sourceCalendarId, window, job.syncAllEvents)
		val mappings = mappingDao.getForJob(job.sourceCalendarId, job.targetCalendarId)
		val targetExists = fetchExistingTargetIds(mappings.map { it.targetEventId })
		val plan = buildSyncPlan(sourceEvents, mappings, targetExists)
		if (plan.missingMappingIds.isNotEmpty()) {
			mappingDao.deleteByIds(plan.missingMappingIds)
			Log.w(TAG, "Removed ${plan.missingMappingIds.size} mappings with missing target events.")
		}

		var created = 0
		var updated = 0

		if (sourceEvents.isNotEmpty()) {
			plan.createSources.forEach { source ->
				val targetId = insertTargetEvent(job.targetCalendarId, source)
				if (targetId != null) {
					upsertMapping(job, source.id, targetId, null)
					created += 1
				}
			}
			plan.updateTargets.forEach { (targetId, source) ->
				if (updateTargetEvent(targetId, source)) {
					updated += 1
				}
			}
		}

		val deleted = deleteTargets(plan.orphanTargetIds)
		if (plan.orphanMappingIds.isNotEmpty()) {
			mappingDao.deleteByIds(plan.orphanMappingIds)
		}
		val targetCount = mappingDao.countForJob(job.sourceCalendarId, job.targetCalendarId)
		val targetTotalCount = countEvents(job.targetCalendarId, window, job.syncAllEvents)
		return SyncResult(
			created = created,
			updated = updated,
			deleted = deleted,
			sourceCount = sourceEvents.size,
			targetCount = targetCount,
			targetTotalCount = targetTotalCount
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
			CalendarContract.Events.DURATION,
			CalendarContract.Events.ALL_DAY,
			CalendarContract.Events.EVENT_TIMEZONE,
			CalendarContract.Events.EVENT_END_TIMEZONE,
			CalendarContract.Events.RRULE,
			CalendarContract.Events.EXDATE,
			CalendarContract.Events.EXRULE,
			CalendarContract.Events.RDATE,
			CalendarContract.Events.ORIGINAL_ID,
			CalendarContract.Events.ORIGINAL_INSTANCE_TIME,
			CalendarContract.Events.ORIGINAL_ALL_DAY,
			CalendarContract.Events.STATUS,
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
			eventsUri,
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
			val durationIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DURATION)
			val allDayIndex = it.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
			val tzIndex = it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_TIMEZONE)
			val endTzIndex = it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_END_TIMEZONE)
			val rruleIndex = it.getColumnIndexOrThrow(CalendarContract.Events.RRULE)
			val exdateIndex = it.getColumnIndexOrThrow(CalendarContract.Events.EXDATE)
			val exruleIndex = it.getColumnIndexOrThrow(CalendarContract.Events.EXRULE)
			val rdateIndex = it.getColumnIndexOrThrow(CalendarContract.Events.RDATE)
			val originalIdIndex = it.getColumnIndexOrThrow(CalendarContract.Events.ORIGINAL_ID)
			val originalInstanceIndex =
				it.getColumnIndexOrThrow(CalendarContract.Events.ORIGINAL_INSTANCE_TIME)
			val originalAllDayIndex =
				it.getColumnIndexOrThrow(CalendarContract.Events.ORIGINAL_ALL_DAY)
			val statusIndex = it.getColumnIndexOrThrow(CalendarContract.Events.STATUS)
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
						duration = if (it.isNull(durationIndex)) null else it.getString(durationIndex),
						allDay = it.getInt(allDayIndex) == 1,
						timeZone = it.getString(tzIndex),
						endTimeZone = it.getString(endTzIndex),
						rrule = it.getString(rruleIndex),
						exdate = it.getString(exdateIndex),
						exrule = it.getString(exruleIndex),
						rdate = it.getString(rdateIndex),
						originalId = if (it.isNull(originalIdIndex)) null else it.getLong(originalIdIndex),
						originalInstanceTime = if (it.isNull(originalInstanceIndex)) {
							null
						} else {
							it.getLong(originalInstanceIndex)
						},
						originalAllDay = if (it.isNull(originalAllDayIndex)) {
							null
						} else {
							it.getInt(originalAllDayIndex) == 1
						},
						status = if (it.isNull(statusIndex)) null else it.getInt(statusIndex),
						location = it.getString(locationIndex),
						description = it.getString(descriptionIndex)
					)
				)
			}
			events
		}
	}

	private fun countEvents(
		calendarId: Long,
		window: SyncWindow,
		syncAllEvents: Boolean
	): Int {
		val projection = arrayOf(CalendarContract.Events._ID)
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
			arrayOf(calendarId.toString())
		} else {
			arrayOf(
				calendarId.toString(),
				window.endMillis.toString(),
				window.startMillis.toString()
			)
		}
		val cursor = resolver.query(
			eventsUri,
			projection,
			selection,
			selectionArgs,
			null
		) ?: return 0
		return cursor.use { it.count }
	}

	private fun insertTargetEvent(
		targetCalendarId: Long,
		source: SourceEvent
	): Long? {
		val values = toContentValues(targetCalendarId, source)
		val uri = resolver.insert(eventsUri, values)
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
		val values = toContentValues(null, source, isUpdate = true)
		val uri = eventsUri.buildUpon()
			.appendPath(targetEventId.toString())
			.build()
		val updated = resolver.update(uri, values, null, null) > 0
		if (!updated) {
			Log.w(TAG, "Failed to update target event $targetEventId")
		}
		return updated
	}

	private fun deleteTargets(targetIds: List<Long>): Int {
		var deleted = 0
		targetIds.forEach { targetId ->
			val uri = eventsUri.buildUpon()
				.appendPath(targetId.toString())
				.build()
			val result = resolver.delete(uri, null, null)
			if (result == 0) {
				Log.w(TAG, "Failed to delete target event $targetId")
			}
			deleted += result
		}
		return deleted
	}

	suspend fun deleteSyncedTargets(job: SyncJob): Int {
		val mappings = mappingDao.getForJob(job.sourceCalendarId, job.targetCalendarId)
		if (mappings.isEmpty()) return 0
		val targetIds = mappings.map { it.targetEventId }
		val deleted = deleteTargets(targetIds)
		mappingDao.deleteByIds(mappings.map { it.id })
		if (deleted < targetIds.size) {
			Log.w(
				TAG,
				"Deleted $deleted of ${targetIds.size} synced targets for job ${job.id}"
			)
		}
		return deleted
	}

	private fun toContentValues(
		targetCalendarId: Long?,
		source: SourceEvent,
		isUpdate: Boolean = false
	): ContentValues {
		return ContentValues().apply {
			if (targetCalendarId != null) {
				put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
			}
			put(CalendarContract.Events.TITLE, source.title)
			put(CalendarContract.Events.DTSTART, source.startMillis)
			if (!source.duration.isNullOrBlank()) {
				put(CalendarContract.Events.DURATION, source.duration)
				if (!isUpdate) {
					putNull(CalendarContract.Events.DTEND)
				}
			} else if (source.endMillis != null) {
				put(CalendarContract.Events.DTEND, source.endMillis)
				if (!isUpdate) {
					putNull(CalendarContract.Events.DURATION)
				}
			} else {
				val defaultDuration = if (source.allDay) "P1D" else "PT1H"
				if (isUpdate) {
					put(CalendarContract.Events.DURATION, defaultDuration)
				} else {
					putNull(CalendarContract.Events.DTEND)
					put(CalendarContract.Events.DURATION, defaultDuration)
				}
			}
			put(CalendarContract.Events.ALL_DAY, if (source.allDay) 1 else 0)
			put(CalendarContract.Events.EVENT_TIMEZONE, source.timeZone)
			put(CalendarContract.Events.EVENT_END_TIMEZONE, source.endTimeZone)
			put(CalendarContract.Events.RRULE, source.rrule)
			put(CalendarContract.Events.EXDATE, source.exdate)
			put(CalendarContract.Events.EXRULE, source.exrule)
			put(CalendarContract.Events.RDATE, source.rdate)
			if (source.originalId != null) {
				put(CalendarContract.Events.ORIGINAL_ID, source.originalId)
			}
			if (source.originalInstanceTime != null) {
				put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, source.originalInstanceTime)
			}
			if (source.originalAllDay != null) {
				put(CalendarContract.Events.ORIGINAL_ALL_DAY, if (source.originalAllDay) 1 else 0)
			}
			if (source.status != null) {
				put(CalendarContract.Events.STATUS, source.status)
			}
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
				eventsUri,
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

internal data class SyncPlan(
	val missingMappingIds: List<Long>,
	val createSources: List<SourceEvent>,
	val updateTargets: List<Pair<Long, SourceEvent>>,
	val orphanTargetIds: List<Long>,
	val orphanMappingIds: List<Long>
)

internal fun buildSyncPlan(
	sourceEvents: List<SourceEvent>,
	mappings: List<EventMapping>,
	existingTargetIds: Set<Long>
): SyncPlan {
	val mappingBySource = mappings.associateBy { it.sourceEventId }.toMutableMap()
	val missingTargets = mappings.filter { !existingTargetIds.contains(it.targetEventId) }
	if (missingTargets.isNotEmpty()) {
		missingTargets.forEach { mappingBySource.remove(it.sourceEventId) }
	}

	val createSources = ArrayList<SourceEvent>()
	val updateTargets = ArrayList<Pair<Long, SourceEvent>>()
	sourceEvents.forEach { source ->
		val mapping = mappingBySource[source.id]
		if (mapping == null) {
			createSources.add(source)
		} else {
			updateTargets.add(mapping.targetEventId to source)
		}
	}

	val sourceIds = sourceEvents.mapTo(HashSet()) { it.id }
	val orphanTargetIds = ArrayList<Long>()
	val orphanMappingIds = ArrayList<Long>()
	mappingBySource.values.forEach { mapping ->
		if (!sourceIds.contains(mapping.sourceEventId)) {
			orphanTargetIds.add(mapping.targetEventId)
			orphanMappingIds.add(mapping.id)
		}
	}

	return SyncPlan(
		missingMappingIds = missingTargets.map { it.id },
		createSources = createSources,
		updateTargets = updateTargets,
		orphanTargetIds = orphanTargetIds,
		orphanMappingIds = orphanMappingIds
	)
}

data class SyncWindow(
	val startMillis: Long,
	val endMillis: Long
) {
	companion object {
		fun fromJob(job: SyncJob, now: Instant = Instant.now()): SyncWindow {
			val pastDays = job.windowPastDays.coerceAtLeast(0)
			val futureDays = job.windowFutureDays.coerceAtLeast(0)
			val start = now.minus(pastDays.toLong(), ChronoUnit.DAYS).toEpochMilli()
			val end = now.plus(futureDays.toLong(), ChronoUnit.DAYS).toEpochMilli()
			return SyncWindow(start, end)
		}
	}
}

internal data class SourceEvent(
	val id: Long,
	val title: String?,
	val startMillis: Long,
	val endMillis: Long?,
	val duration: String?,
	val allDay: Boolean,
	val timeZone: String?,
	val endTimeZone: String?,
	val rrule: String?,
	val exdate: String?,
	val exrule: String?,
	val rdate: String?,
	val originalId: Long?,
	val originalInstanceTime: Long?,
	val originalAllDay: Boolean?,
	val status: Int?,
	val location: String?,
	val description: String?
)
