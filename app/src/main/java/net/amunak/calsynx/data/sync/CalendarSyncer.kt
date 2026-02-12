package net.amunak.calsynx.data.sync

import android.content.ContentResolver
import android.content.ContentValues
import android.provider.CalendarContract
import android.util.Log
import net.amunak.calsynx.data.EventMapping
import net.amunak.calsynx.data.EventMappingDao
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.data.countEvents
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class SyncResult(
	val created: Int,
	val updated: Int,
	val deleted: Int,
	val sourceCount: Int,
	val targetCount: Int,
	val targetTotalCount: Int,
	val initialPairCount: Int = 0,
	val initialPairAttempted: Boolean = false
)

class CalendarSyncer(
	private val resolver: ContentResolver,
	private val mappingDao: EventMappingDao,
	private val eventsUri: android.net.Uri = CalendarContract.Events.CONTENT_URI
) {
	private val remindersUri = eventsUri.buildUpon().path("reminders").build()
	private val attendeesUri = eventsUri.buildUpon().path("attendees").build()

	suspend fun repairExistingMappings(
		job: SyncJob,
		window: SyncWindow = SyncWindow.fromJob(job)
	): Int {
		val sourceEvents = querySourceEvents(job.sourceCalendarId, window, job.syncAllEvents)
		if (sourceEvents.isEmpty()) return 0
		val existingMappings = mappingDao.getForJob(job.sourceCalendarId, job.targetCalendarId)
		val mappedSourceIds = existingMappings.map { it.sourceEventId }.toSet()
		val eligibleSources = sourceEvents.filter { it.id !in mappedSourceIds }
		if (eligibleSources.isEmpty()) return 0
		val targetEvents = queryTargetEvents(job.targetCalendarId, window, job.syncAllEvents)
		val mappedTargets = mappingDao.getTargetEventIdsForCalendar(job.targetCalendarId).toSet()
		val eligibleTargets = excludeMappedTargets(targetEvents, mappedTargets)
		val pairs = pairExistingEventsByTitleAndDate(eligibleSources, eligibleTargets)
		if (pairs.isEmpty()) return 0
		for ((sourceId, targetId) in pairs) {
			mappingDao.upsert(
				EventMapping(
					sourceEventId = sourceId,
					targetEventId = targetId,
					sourceCalendarId = job.sourceCalendarId,
					targetCalendarId = job.targetCalendarId
				)
			)
		}
		return pairs.size
	}

	suspend fun sync(
		job: SyncJob,
		window: SyncWindow = SyncWindow.fromJob(job)
	): SyncResult {
		val sourceEvents = querySourceEvents(job.sourceCalendarId, window, job.syncAllEvents)
		val mappings = mappingDao.getForJob(job.sourceCalendarId, job.targetCalendarId)
		val initialPairAttempted = job.pairExistingOnFirstSync &&
			job.lastSyncTimestamp == null &&
			mappings.isEmpty() &&
			sourceEvents.isNotEmpty()
		val seededMappings = if (initialPairAttempted) {
			val targetEvents = queryTargetEvents(job.targetCalendarId, window, job.syncAllEvents)
			val mappedTargets = mappingDao
				.getTargetEventIdsForCalendar(job.targetCalendarId)
				.toSet()
			val eligibleTargets = excludeMappedTargets(targetEvents, mappedTargets)
			seedInitialMappings(job, sourceEvents, eligibleTargets)
		} else {
			mappings
		}
		val initialPairCount = if (initialPairAttempted) seededMappings.size else 0
		val targetExists = fetchExistingTargetIds(seededMappings.map { it.targetEventId })
		val plan = buildSyncPlan(sourceEvents, seededMappings, targetExists)
		if (plan.missingMappingIds.isNotEmpty()) {
			mappingDao.deleteByIds(plan.missingMappingIds)
			Log.w(TAG, "Removed ${plan.missingMappingIds.size} mappings with missing target events.")
		}

		var created = 0
		var updated = 0

		if (sourceEvents.isNotEmpty()) {
			plan.createSources.forEach { source ->
				val targetId = insertTargetEvent(job, job.targetCalendarId, source)
				if (targetId != null) {
					upsertMapping(job, source.id, targetId, null)
					created += 1
				}
			}
			plan.updateTargets.forEach { (targetId, source) ->
				if (updateTargetEvent(job, targetId, source)) {
					updated += 1
				}
			}
		}

		val deleted = deleteTargets(plan.orphanTargetIds)
		if (plan.orphanMappingIds.isNotEmpty()) {
			mappingDao.deleteByIds(plan.orphanMappingIds)
		}
		var cleaned = 0
		if (job.deleteUnmappedTargets) {
			val mappedTargets = mappingDao
				.getTargetEventIdsForCalendar(job.targetCalendarId)
				.toSet()
			val targetEvents = queryTargetEvents(job.targetCalendarId, window, job.syncAllEvents)
			val unmappedTargets = excludeMappedTargets(targetEvents, mappedTargets)
			cleaned = deleteTargets(unmappedTargets.map { it.id })
		}
		val targetCount = mappingDao.countForJob(job.sourceCalendarId, job.targetCalendarId)
		val targetTotalCount = countEvents(job.targetCalendarId, window, job.syncAllEvents)
		return SyncResult(
			created = created,
			updated = updated,
			deleted = deleted + cleaned,
			sourceCount = sourceEvents.size,
			targetCount = targetCount,
			targetTotalCount = targetTotalCount,
			initialPairCount = initialPairCount,
			initialPairAttempted = initialPairAttempted
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
			CalendarContract.Events.DESCRIPTION,
			CalendarContract.Events.AVAILABILITY,
			CalendarContract.Events.ACCESS_LEVEL,
			CalendarContract.Events.EVENT_COLOR,
			CalendarContract.Events.ORGANIZER,
			CalendarContract.Events.OWNER_ACCOUNT
		)
		val querySpec = buildEventQuerySpec(
			calendarId = sourceCalendarId,
			window = window,
			syncAllEvents = syncAllEvents
		)
		val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

		val cursor = resolver.query(
			eventsUri,
			projection,
			querySpec.selection,
			querySpec.selectionArgs,
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
			val availabilityIndex = it.getColumnIndexOrThrow(CalendarContract.Events.AVAILABILITY)
			val accessIndex = it.getColumnIndexOrThrow(CalendarContract.Events.ACCESS_LEVEL)
			val colorIndex = it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_COLOR)
			val organizerIndex = it.getColumnIndexOrThrow(CalendarContract.Events.ORGANIZER)
			val ownerIndex = it.getColumnIndexOrThrow(CalendarContract.Events.OWNER_ACCOUNT)

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
						description = it.getString(descriptionIndex),
						availability = if (it.isNull(availabilityIndex)) null else it.getInt(availabilityIndex),
						accessLevel = if (it.isNull(accessIndex)) null else it.getInt(accessIndex),
						eventColor = if (it.isNull(colorIndex)) null else it.getInt(colorIndex),
						organizer = it.getString(organizerIndex),
						ownerAccount = it.getString(ownerIndex)
					)
				)
			}
			events
		}
	}

	private fun queryTargetEvents(
		targetCalendarId: Long,
		window: SyncWindow,
		syncAllEvents: Boolean
	): List<TargetEvent> {
		val projection = arrayOf(
			CalendarContract.Events._ID,
			CalendarContract.Events.TITLE,
			CalendarContract.Events.DTSTART,
			CalendarContract.Events.ALL_DAY
		)
		val querySpec = buildEventQuerySpec(
			calendarId = targetCalendarId,
			window = window,
			syncAllEvents = syncAllEvents
		)
		val sortOrder = "${CalendarContract.Events.DTSTART} ASC"
		val cursor = resolver.query(
			eventsUri,
			projection,
			querySpec.selection,
			querySpec.selectionArgs,
			sortOrder
		) ?: return emptyList()
		return cursor.use { it ->
			val idIndex = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
			val titleIndex = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
			val startIndex = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
			val allDayIndex = it.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
			val events = ArrayList<TargetEvent>(it.count)
			while (it.moveToNext()) {
				events.add(
					TargetEvent(
						id = it.getLong(idIndex),
						title = it.getString(titleIndex),
						startMillis = it.getLong(startIndex),
						allDay = it.getInt(allDayIndex) == 1
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
		val querySpec = buildEventQuerySpec(
			calendarId = calendarId,
			window = window,
			syncAllEvents = syncAllEvents
		)
		return countEvents(resolver, querySpec.selection, querySpec.selectionArgs)
	}

	private fun insertTargetEvent(
		job: SyncJob,
		targetCalendarId: Long,
		source: SourceEvent
	): Long? {
		val values = buildEventContentValues(job, targetCalendarId, source)
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
		applyReminders(job, source, eventId)
		applyAttendees(job, source, eventId)
		return eventId
	}

	private fun updateTargetEvent(
		job: SyncJob,
		targetEventId: Long,
		source: SourceEvent
	): Boolean {
		val values = buildEventContentValues(job, null, source)
		val uri = eventsUri.buildUpon()
			.appendPath(targetEventId.toString())
			.build()
		val updated = resolver.update(uri, values, null, null) > 0
		if (!updated) {
			Log.w(TAG, "Failed to update target event $targetEventId")
		} else {
			if (job.reminderResyncEnabled) {
				applyReminders(job, source, targetEventId)
			}
			applyAttendees(job, source, targetEventId)
		}
		return updated
	}

	private fun applyReminders(job: SyncJob, source: SourceEvent, targetEventId: Long) {
		when (ReminderMode.from(job.reminderMode)) {
			ReminderMode.COPY -> {
				val reminders = queryReminders(source.id)
				replaceReminders(targetEventId, reminders)
			}
			ReminderMode.NONE -> {
				deleteReminders(targetEventId)
			}
			ReminderMode.CUSTOM -> {
				if (source.allDay) {
					if (!job.reminderAllDayEnabled) {
						deleteReminders(targetEventId)
						return
					}
					val minutes = job.reminderAllDayMinutes.coerceAtLeast(0)
					replaceReminders(
						targetEventId,
						listOf(ReminderEntry(minutes, CalendarContract.Reminders.METHOD_ALERT))
					)
				} else {
					if (!job.reminderTimedEnabled) {
						deleteReminders(targetEventId)
						return
					}
					val minutes = job.reminderTimedMinutes.coerceAtLeast(0)
					replaceReminders(
						targetEventId,
						listOf(ReminderEntry(minutes, CalendarContract.Reminders.METHOD_ALERT))
					)
				}
			}
		}
	}

	private fun applyAttendees(job: SyncJob, source: SourceEvent, targetEventId: Long) {
		if (!job.copyAttendees) {
			deleteAttendees(targetEventId)
			return
		}
		val attendees = queryAttendees(source.id)
		replaceAttendees(targetEventId, attendees)
	}

	private suspend fun seedInitialMappings(
		job: SyncJob,
		sourceEvents: List<SourceEvent>,
		targetEvents: List<TargetEvent>
	): List<EventMapping> {
		val pairs = pairExistingEventsByTitleAndDate(sourceEvents, targetEvents)
		if (pairs.isEmpty()) return emptyList()
		val mappings = mutableListOf<EventMapping>()
		for ((sourceId, targetId) in pairs) {
			val mappingId = mappingDao.upsert(
				EventMapping(
					sourceEventId = sourceId,
					targetEventId = targetId,
					sourceCalendarId = job.sourceCalendarId,
					targetCalendarId = job.targetCalendarId
				)
			)
			val id = mappingId.takeIf { it > 0L } ?: 0L
			mappings.add(
				EventMapping(
					id = id,
					sourceEventId = sourceId,
					targetEventId = targetId,
					sourceCalendarId = job.sourceCalendarId,
					targetCalendarId = job.targetCalendarId
				)
			)
		}
		return mappings
	}

	private fun queryReminders(eventId: Long): List<ReminderEntry> {
		val cursor = resolver.query(
			remindersUri,
			arrayOf(CalendarContract.Reminders.MINUTES, CalendarContract.Reminders.METHOD),
			"${CalendarContract.Reminders.EVENT_ID} = ?",
			arrayOf(eventId.toString()),
			null
		) ?: return emptyList()
		return cursor.use {
			val minutesIndex = it.getColumnIndexOrThrow(CalendarContract.Reminders.MINUTES)
			val methodIndex = it.getColumnIndexOrThrow(CalendarContract.Reminders.METHOD)
			val reminders = ArrayList<ReminderEntry>(it.count)
			while (it.moveToNext()) {
				reminders.add(
					ReminderEntry(
						minutes = it.getInt(minutesIndex),
						method = it.getInt(methodIndex)
					)
				)
			}
			reminders
		}
	}

	private fun replaceReminders(eventId: Long, reminders: List<ReminderEntry>) {
		deleteReminders(eventId)
		reminders.forEach { reminder ->
			val values = ContentValues().apply {
				put(CalendarContract.Reminders.EVENT_ID, eventId)
				put(CalendarContract.Reminders.MINUTES, reminder.minutes)
				put(CalendarContract.Reminders.METHOD, reminder.method)
			}
			resolver.insert(remindersUri, values)
		}
	}

	private fun deleteReminders(eventId: Long) {
		resolver.delete(
			remindersUri,
			"${CalendarContract.Reminders.EVENT_ID} = ?",
			arrayOf(eventId.toString())
		)
	}

	private fun queryAttendees(eventId: Long): List<AttendeeEntry> {
		val cursor = resolver.query(
			attendeesUri,
			arrayOf(
				CalendarContract.Attendees.ATTENDEE_NAME,
				CalendarContract.Attendees.ATTENDEE_EMAIL,
				CalendarContract.Attendees.ATTENDEE_TYPE,
				CalendarContract.Attendees.ATTENDEE_RELATIONSHIP,
				CalendarContract.Attendees.ATTENDEE_STATUS,
				CalendarContract.Attendees.ATTENDEE_IDENTITY,
				CalendarContract.Attendees.ATTENDEE_ID_NAMESPACE
			),
			"${CalendarContract.Attendees.EVENT_ID} = ?",
			arrayOf(eventId.toString()),
			null
		) ?: return emptyList()
		return cursor.use {
			val nameIndex = it.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_NAME)
			val emailIndex = it.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_EMAIL)
			val typeIndex = it.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_TYPE)
			val relationshipIndex =
				it.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP)
			val statusIndex = it.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_STATUS)
			val identityIndex = it.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_IDENTITY)
			val namespaceIndex =
				it.getColumnIndexOrThrow(CalendarContract.Attendees.ATTENDEE_ID_NAMESPACE)
			val attendees = ArrayList<AttendeeEntry>(it.count)
			while (it.moveToNext()) {
				attendees.add(
					AttendeeEntry(
						name = it.getString(nameIndex),
						email = it.getString(emailIndex),
						type = it.getInt(typeIndex),
						relationship = it.getInt(relationshipIndex),
						status = it.getInt(statusIndex),
						identity = it.getString(identityIndex),
						namespace = it.getString(namespaceIndex)
					)
				)
			}
			attendees
		}
	}

	private fun replaceAttendees(eventId: Long, attendees: List<AttendeeEntry>) {
		deleteAttendees(eventId)
		attendees.forEach { attendee ->
			val values = ContentValues().apply {
				put(CalendarContract.Attendees.EVENT_ID, eventId)
				put(CalendarContract.Attendees.ATTENDEE_NAME, attendee.name)
				put(CalendarContract.Attendees.ATTENDEE_EMAIL, attendee.email)
				put(CalendarContract.Attendees.ATTENDEE_TYPE, attendee.type)
				put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP, attendee.relationship)
				put(CalendarContract.Attendees.ATTENDEE_STATUS, attendee.status)
				if (!attendee.identity.isNullOrBlank()) {
					put(CalendarContract.Attendees.ATTENDEE_IDENTITY, attendee.identity)
				}
				if (!attendee.namespace.isNullOrBlank()) {
					put(CalendarContract.Attendees.ATTENDEE_ID_NAMESPACE, attendee.namespace)
				}
			}
			resolver.insert(attendeesUri, values)
		}
	}

	private fun deleteAttendees(eventId: Long) {
		resolver.delete(
			attendeesUri,
			"${CalendarContract.Attendees.EVENT_ID} = ?",
			arrayOf(eventId.toString())
		)
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
	val description: String?,
	val availability: Int?,
	val accessLevel: Int?,
	val eventColor: Int?,
	val organizer: String?,
	val ownerAccount: String?
)

internal data class TargetEvent(
	val id: Long,
	val title: String?,
	val startMillis: Long,
	val allDay: Boolean
)

private data class ReminderEntry(
	val minutes: Int,
	val method: Int
)

private data class AttendeeEntry(
	val name: String?,
	val email: String?,
	val type: Int,
	val relationship: Int,
	val status: Int,
	val identity: String?,
	val namespace: String?
)

internal data class EventTimeFields(
	val dtEnd: Long?,
	val duration: String?
)

internal data class EventQuerySpec(
	val selection: String,
	val selectionArgs: Array<String>
)

internal fun pairExistingEventsByTitleAndDate(
	sources: List<SourceEvent>,
	targets: List<TargetEvent>,
	zoneId: ZoneId = ZoneId.systemDefault()
): List<Pair<Long, Long>> {
	val sourceGroups = sources
		.mapNotNull { event ->
			val title = normalizeEventTitle(event.title) ?: return@mapNotNull null
			val date = Instant.ofEpochMilli(event.startMillis).atZone(zoneId).toLocalDate()
			Triple(title, date, event.allDay) to event
		}
		.groupBy({ it.first }, { it.second })
	val targetGroups = targets
		.mapNotNull { event ->
			val title = normalizeEventTitle(event.title) ?: return@mapNotNull null
			val date = Instant.ofEpochMilli(event.startMillis).atZone(zoneId).toLocalDate()
			Triple(title, date, event.allDay) to event
		}
		.groupBy({ it.first }, { it.second })

	val pairs = mutableListOf<Pair<Long, Long>>()
	for ((key, sourceEvents) in sourceGroups) {
		val targetEvents = targetGroups[key] ?: continue
		val sortedSources = sourceEvents.sortedBy { it.startMillis }
		val sortedTargets = targetEvents.sortedBy { it.startMillis }
		val limit = minOf(sortedSources.size, sortedTargets.size)
		for (index in 0 until limit) {
			pairs.add(sortedSources[index].id to sortedTargets[index].id)
		}
	}
	return pairs
}

internal fun excludeMappedTargets(
	targets: List<TargetEvent>,
	mappedTargetIds: Set<Long>
): List<TargetEvent> {
	if (mappedTargetIds.isEmpty()) return targets
	return targets.filter { it.id !in mappedTargetIds }
}

private fun normalizeEventTitle(title: String?): String? {
	val cleaned = title?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
	return cleaned.takeIf { it.isNotBlank() }?.lowercase()
}

internal fun buildEventQuerySpec(
	calendarId: Long,
	window: SyncWindow,
	syncAllEvents: Boolean
): EventQuerySpec {
	if (syncAllEvents) {
		return EventQuerySpec(
			selection = buildString {
				append("${CalendarContract.Events.CALENDAR_ID} = ?")
				append(" AND ${CalendarContract.Events.DELETED} = 0")
			},
			selectionArgs = arrayOf(calendarId.toString())
		)
	}
	return EventQuerySpec(
		selection = buildString {
			append("${CalendarContract.Events.CALENDAR_ID} = ?")
			append(" AND ${CalendarContract.Events.DELETED} = 0")
			append(" AND (")
			append("${CalendarContract.Events.RRULE} IS NOT NULL")
			append(" OR (")
			append("${CalendarContract.Events.DTSTART} <= ?")
			append(" AND (${CalendarContract.Events.DTEND} IS NULL OR ${CalendarContract.Events.DTEND} >= ?)")
			append("))")
		},
		selectionArgs = arrayOf(
			calendarId.toString(),
			window.endMillis.toString(),
			window.startMillis.toString()
		)
	)
}

internal fun resolveEventTimeFields(source: SourceEvent): EventTimeFields {
	return if (!source.duration.isNullOrBlank()) {
		EventTimeFields(dtEnd = null, duration = source.duration)
	} else if (source.endMillis != null) {
		EventTimeFields(dtEnd = source.endMillis, duration = null)
	} else {
		val defaultDuration = if (source.allDay) "P1D" else "PT1H"
		EventTimeFields(dtEnd = null, duration = defaultDuration)
	}
}

internal fun buildEventContentValues(
	job: SyncJob,
	targetCalendarId: Long?,
	source: SourceEvent
): ContentValues {
	return ContentValues().apply {
		if (targetCalendarId != null) {
			put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
		}
		put(CalendarContract.Events.TITLE, source.title)
		put(CalendarContract.Events.DTSTART, source.startMillis)
		val timeFields = resolveEventTimeFields(source)
		if (timeFields.duration == null) {
			putNull(CalendarContract.Events.DURATION)
		} else {
			put(CalendarContract.Events.DURATION, timeFields.duration)
		}
		if (timeFields.dtEnd == null) {
			putNull(CalendarContract.Events.DTEND)
		} else {
			put(CalendarContract.Events.DTEND, timeFields.dtEnd)
		}
		put(CalendarContract.Events.ALL_DAY, if (source.allDay) 1 else 0)
		put(CalendarContract.Events.EVENT_TIMEZONE, source.timeZone)
		put(CalendarContract.Events.EVENT_END_TIMEZONE, source.endTimeZone)
		put(CalendarContract.Events.RRULE, source.rrule)
		put(CalendarContract.Events.EXDATE, source.exdate)
		put(CalendarContract.Events.EXRULE, source.exrule)
		put(CalendarContract.Events.RDATE, source.rdate)
		val availabilityMode = AvailabilityMode.from(job.availabilityMode)
		val forcedAvailability = AvailabilityMode.toAvailabilityValue(availabilityMode)
		if (forcedAvailability != null) {
			put(CalendarContract.Events.AVAILABILITY, forcedAvailability)
		} else if (availabilityMode == AvailabilityMode.COPY) {
			if (source.availability == null) {
				putNull(CalendarContract.Events.AVAILABILITY)
			} else {
				put(CalendarContract.Events.AVAILABILITY, source.availability)
			}
		}
		if (job.copyPrivacy) {
			if (source.accessLevel == null) {
				putNull(CalendarContract.Events.ACCESS_LEVEL)
			} else {
				put(CalendarContract.Events.ACCESS_LEVEL, source.accessLevel)
			}
		} else {
			putNull(CalendarContract.Events.ACCESS_LEVEL)
		}
		if (job.copyEventColor) {
			if (source.eventColor == null) {
				putNull(CalendarContract.Events.EVENT_COLOR)
			} else {
				put(CalendarContract.Events.EVENT_COLOR, source.eventColor)
			}
		} else {
			putNull(CalendarContract.Events.EVENT_COLOR)
		}
		if (job.copyOrganizer) {
			if (source.organizer.isNullOrBlank()) {
				putNull(CalendarContract.Events.ORGANIZER)
			} else {
				put(CalendarContract.Events.ORGANIZER, source.organizer)
			}
		} else {
			putNull(CalendarContract.Events.ORGANIZER)
		}
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
