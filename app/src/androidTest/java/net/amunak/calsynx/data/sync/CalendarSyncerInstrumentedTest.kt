package net.amunak.calsynx.data.sync

import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import net.amunak.calsynx.data.EventMapping
import net.amunak.calsynx.data.EventMappingDao
import net.amunak.calsynx.data.SyncJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarSyncerInstrumentedTest {
	private lateinit var resolver: android.content.ContentResolver
	private val eventsUri = Uri.parse("content://${FakeCalendarProvider.AUTHORITY}/events")

	@Before
	fun setUp() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		resolver = context.contentResolver
		resolver.delete(eventsUri, null, null)
	}

	@Test
	fun syncCopiesDurationAndEndTime() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 10L
		val targetId = 20L

		val durationEventId = insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Duration",
				startMillis = 1_000L,
				duration = "PT90M"
			)
		)
		val endEventId = insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "End",
				startMillis = 5_000L,
				endMillis = 7_000L
			)
		)

		val result = syncer.sync(syncJob(sourceId, targetId), SyncWindow(0L, 10_000L))
		assertEquals(2, result.created)
		assertEquals(2, result.sourceCount)

		val targets = queryEvents(resolver, targetId)
		val durationTarget = targets.first { it.getAsString(CalendarContract.Events.DURATION) == "PT90M" }
		val endTarget = targets.first { it.getAsLong(CalendarContract.Events.DTEND) == 7_000L }
		assertEquals(null, durationTarget.getAsLong(CalendarContract.Events.DTEND))
		assertEquals(null, endTarget.getAsString(CalendarContract.Events.DURATION))
		assertNotNull(durationEventId)
		assertNotNull(endEventId)
	}

	@Test
	fun syncHandlesSplitSeriesWithoutDupes() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 11L
		val targetId = 21L

		val originalId = insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Series",
				startMillis = 1_000L,
				endMillis = 2_000L,
				rrule = "FREQ=DAILY;COUNT=5"
			)
		)

		syncer.sync(syncJob(sourceId, targetId), SyncWindow(0L, 100_000L))

		val splitStart = 3_000L
		updateEvent(
			resolver,
			originalId,
			ContentValues().apply {
				put(CalendarContract.Events.EXDATE, splitStart.toString())
			}
		)
		insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Series (split)",
				startMillis = splitStart,
				endMillis = 4_000L,
				rrule = "FREQ=DAILY;COUNT=3",
				originalId = originalId,
				originalInstanceTime = splitStart
			)
		)

		val result = syncer.sync(syncJob(sourceId, targetId), SyncWindow(0L, 100_000L))
		assertEquals(1, result.created)
		assertEquals(1, result.updated)

		val targets = queryEvents(resolver, targetId)
		assertEquals(2, targets.size)
		assertTrue(targets.any { it.getAsString(CalendarContract.Events.EXDATE) == splitStart.toString() })
		assertTrue(targets.any { it.getAsLong(CalendarContract.Events.ORIGINAL_ID) == originalId })
	}

	@Test
	fun syncRecreatesMissingTargetMapping() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 12L
		val targetId = 22L

		val sourceEventId = insertEvent(
			resolver,
			sourceId,
			eventValues(title = "Single", startMillis = 10_000L, endMillis = 11_000L)
		)
		mappingDao.upsert(
			EventMapping(
				id = 1L,
				sourceEventId = sourceEventId,
				targetEventId = 9999L,
				sourceCalendarId = sourceId,
				targetCalendarId = targetId
			)
		)

		val result = syncer.sync(syncJob(sourceId, targetId), SyncWindow(0L, 100_000L))
		assertEquals(1, result.created)
		assertEquals(1, mappingDao.countForJob(sourceId, targetId))
		assertEquals(1, queryEvents(resolver, targetId).size)
	}

	@Test
	fun syncRespectsWindowFiltering() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 13L
		val targetId = 23L

		insertEvent(
			resolver,
			sourceId,
			eventValues(title = "In window", startMillis = 1_000L, endMillis = 2_000L)
		)
		insertEvent(
			resolver,
			sourceId,
			eventValues(title = "Out window", startMillis = 50_000L, endMillis = 60_000L)
		)

		val result = syncer.sync(syncJob(sourceId, targetId), SyncWindow(0L, 10_000L))
		assertEquals(1, result.created)
		assertEquals(1, queryEvents(resolver, targetId).size)
	}

	@Test
	fun syncIncludesRecurringSeriesOutsideWindowStart() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 15L
		val targetId = 25L

		insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Series far in past",
				startMillis = 1_000L,
				endMillis = 2_000L,
				rrule = "FREQ=DAILY;COUNT=30"
			)
		)

		val result = syncer.sync(syncJob(sourceId, targetId), SyncWindow(1_000_000L, 2_000_000L))
		assertEquals(1, result.created)

		val targetEvents = queryEvents(resolver, targetId)
		assertEquals(1, targetEvents.size)
		val target = targetEvents.first()
		assertEquals("Series far in past", target.getAsString(CalendarContract.Events.TITLE))
		assertEquals(1_000L, target.getAsLong(CalendarContract.Events.DTSTART))
		assertEquals(2_000L, target.getAsLong(CalendarContract.Events.DTEND))
		assertEquals("FREQ=DAILY;COUNT=30", target.getAsString(CalendarContract.Events.RRULE))
	}

	@Test
	fun syncCopiesVariedFieldsAcrossEvents() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 16L
		val targetId = 26L

		insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Series detail",
				startMillis = 10_000L,
				endMillis = 12_000L,
				duration = null,
				rrule = "FREQ=WEEKLY;COUNT=4",
				exdate = "20260215T090000Z",
				exrule = "FREQ=DAILY;COUNT=2",
				rdate = "20260220T090000Z",
				location = "Office",
				description = "Weekly sync",
				status = CalendarContract.Events.STATUS_CONFIRMED,
				allDay = false,
				timeZone = "UTC",
				endTimeZone = "UTC"
			)
		)
		insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Override detail",
				startMillis = 50_000L,
				endMillis = null,
				duration = "PT2H",
				rrule = null,
				exdate = null,
				exrule = null,
				rdate = null,
				location = "Remote",
				description = "Override",
				status = CalendarContract.Events.STATUS_TENTATIVE,
				allDay = true,
				timeZone = "Europe/Paris",
				endTimeZone = "Europe/Paris",
				originalId = 1234L,
				originalInstanceTime = 49_000L,
				originalAllDay = true
			)
		)

		val result = syncer.sync(syncJob(sourceId, targetId), SyncWindow(0L, 100_000L))
		assertEquals(2, result.created)

		val targets = queryEvents(resolver, targetId)
		assertEquals(2, targets.size)
		val series = targets.first { it.getAsString(CalendarContract.Events.TITLE) == "Series detail" }
		assertEquals(10_000L, series.getAsLong(CalendarContract.Events.DTSTART))
		assertEquals(12_000L, series.getAsLong(CalendarContract.Events.DTEND))
		assertEquals(null, series.getAsString(CalendarContract.Events.DURATION))
		assertEquals("FREQ=WEEKLY;COUNT=4", series.getAsString(CalendarContract.Events.RRULE))
		assertEquals("20260215T090000Z", series.getAsString(CalendarContract.Events.EXDATE))
		assertEquals("FREQ=DAILY;COUNT=2", series.getAsString(CalendarContract.Events.EXRULE))
		assertEquals("20260220T090000Z", series.getAsString(CalendarContract.Events.RDATE))
		assertEquals("Office", series.getAsString(CalendarContract.Events.EVENT_LOCATION))
		assertEquals("Weekly sync", series.getAsString(CalendarContract.Events.DESCRIPTION))
		assertEquals(CalendarContract.Events.STATUS_CONFIRMED, series.getAsInteger(CalendarContract.Events.STATUS))
		assertEquals(0, series.getAsInteger(CalendarContract.Events.ALL_DAY))
		assertEquals("UTC", series.getAsString(CalendarContract.Events.EVENT_TIMEZONE))
		assertEquals("UTC", series.getAsString(CalendarContract.Events.EVENT_END_TIMEZONE))

		val override = targets.first { it.getAsString(CalendarContract.Events.TITLE) == "Override detail" }
		assertEquals(50_000L, override.getAsLong(CalendarContract.Events.DTSTART))
		assertEquals(null, override.getAsLong(CalendarContract.Events.DTEND))
		assertEquals("PT2H", override.getAsString(CalendarContract.Events.DURATION))
		assertEquals(null, override.getAsString(CalendarContract.Events.RRULE))
		assertEquals("Remote", override.getAsString(CalendarContract.Events.EVENT_LOCATION))
		assertEquals("Override", override.getAsString(CalendarContract.Events.DESCRIPTION))
		assertEquals(CalendarContract.Events.STATUS_TENTATIVE, override.getAsInteger(CalendarContract.Events.STATUS))
		assertEquals(1, override.getAsInteger(CalendarContract.Events.ALL_DAY))
		assertEquals("Europe/Paris", override.getAsString(CalendarContract.Events.EVENT_TIMEZONE))
		assertEquals("Europe/Paris", override.getAsString(CalendarContract.Events.EVENT_END_TIMEZONE))
		assertEquals(1234L, override.getAsLong(CalendarContract.Events.ORIGINAL_ID))
		assertEquals(49_000L, override.getAsLong(CalendarContract.Events.ORIGINAL_INSTANCE_TIME))
		assertEquals(1, override.getAsInteger(CalendarContract.Events.ORIGINAL_ALL_DAY))
	}

	@Test
	fun deleteSyncedTargetsRemovesMappedEvents() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 14L
		val targetId = 24L

		insertEvent(
			resolver,
			sourceId,
			eventValues(title = "Cleanup", startMillis = 1_000L, endMillis = 2_000L)
		)
		syncer.sync(syncJob(sourceId, targetId), SyncWindow(0L, 10_000L))
		assertEquals(1, queryEvents(resolver, targetId).size)
		assertEquals(1, mappingDao.countForJob(sourceId, targetId))

		val deleted = syncer.deleteSyncedTargets(syncJob(sourceId, targetId))
		assertEquals(1, deleted)
		assertEquals(0, queryEvents(resolver, targetId).size)
		assertEquals(0, mappingDao.countForJob(sourceId, targetId))
	}

	private fun syncJob(sourceId: Long, targetId: Long): SyncJob {
		return SyncJob(
			id = 1L,
			sourceCalendarId = sourceId,
			targetCalendarId = targetId,
			windowPastDays = 7,
			windowFutureDays = 7
		)
	}

	private fun insertEvent(
		resolver: android.content.ContentResolver,
		calendarId: Long,
		values: ContentValues
	): Long {
		values.put(CalendarContract.Events.CALENDAR_ID, calendarId)
		val uri = resolver.insert(eventsUri, values)
		return requireNotNull(uri?.lastPathSegment).toLong()
	}

	private fun updateEvent(
		resolver: android.content.ContentResolver,
		eventId: Long,
		values: ContentValues
	) {
		val uri = ContentUris.withAppendedId(eventsUri, eventId)
		resolver.update(uri, values, null, null)
	}

	private fun queryEvents(
		resolver: android.content.ContentResolver,
		calendarId: Long
	): List<ContentValues> {
		val projection = arrayOf(
			CalendarContract.Events._ID,
			CalendarContract.Events.CALENDAR_ID,
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
			CalendarContract.Events.ORIGINAL_ID,
			CalendarContract.Events.ORIGINAL_INSTANCE_TIME,
			CalendarContract.Events.ORIGINAL_ALL_DAY,
			CalendarContract.Events.STATUS,
			CalendarContract.Events.EVENT_LOCATION,
			CalendarContract.Events.DESCRIPTION,
			CalendarContract.Events.RDATE
		)
		val cursor = resolver.query(
			eventsUri,
			projection,
			"${CalendarContract.Events.CALENDAR_ID} = ?",
			arrayOf(calendarId.toString()),
			null
		) ?: return emptyList()
		val results = ArrayList<ContentValues>(cursor.count)
		cursor.use {
			while (it.moveToNext()) {
				val row = ContentValues()
				projection.forEachIndexed { index, column ->
					if (it.isNull(index)) {
						row.putNull(column)
					} else {
						when (it.getType(index)) {
							Cursor.FIELD_TYPE_INTEGER -> row.put(column, it.getLong(index))
							Cursor.FIELD_TYPE_STRING -> row.put(column, it.getString(index))
							else -> row.put(column, it.getString(index))
						}
					}
				}
				results.add(row)
			}
		}
		return results
	}

	private fun eventValues(
		title: String,
		startMillis: Long,
		endMillis: Long? = null,
		duration: String? = null,
		rrule: String? = null,
		exdate: String? = null,
		exrule: String? = null,
		rdate: String? = null,
		location: String? = null,
		description: String? = null,
		status: Int? = null,
		allDay: Boolean = false,
		timeZone: String = "UTC",
		endTimeZone: String = "UTC",
		originalId: Long? = null,
		originalInstanceTime: Long? = null,
		originalAllDay: Boolean? = null
	): ContentValues {
		return ContentValues().apply {
			put(CalendarContract.Events.TITLE, title)
			put(CalendarContract.Events.DTSTART, startMillis)
			if (endMillis != null) {
				put(CalendarContract.Events.DTEND, endMillis)
			}
			if (duration != null) {
				put(CalendarContract.Events.DURATION, duration)
			}
			put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
			put(CalendarContract.Events.EVENT_TIMEZONE, timeZone)
			put(CalendarContract.Events.EVENT_END_TIMEZONE, endTimeZone)
			if (rrule != null) {
				put(CalendarContract.Events.RRULE, rrule)
			}
			if (exdate != null) {
				put(CalendarContract.Events.EXDATE, exdate)
			}
			if (exrule != null) {
				put(CalendarContract.Events.EXRULE, exrule)
			}
			if (rdate != null) {
				put(CalendarContract.Events.RDATE, rdate)
			}
			if (location != null) {
				put(CalendarContract.Events.EVENT_LOCATION, location)
			}
			if (description != null) {
				put(CalendarContract.Events.DESCRIPTION, description)
			}
			if (status != null) {
				put(CalendarContract.Events.STATUS, status)
			}
			if (originalId != null) {
				put(CalendarContract.Events.ORIGINAL_ID, originalId)
			}
			if (originalInstanceTime != null) {
				put(CalendarContract.Events.ORIGINAL_INSTANCE_TIME, originalInstanceTime)
			}
			if (originalAllDay != null) {
				put(CalendarContract.Events.ORIGINAL_ALL_DAY, if (originalAllDay) 1 else 0)
			}
		}
	}
}

private class InMemoryEventMappingDao : EventMappingDao {
	private val mappings = mutableListOf<EventMapping>()
	private var nextId = 1L

	override suspend fun getForJob(sourceCalendarId: Long, targetCalendarId: Long): List<EventMapping> {
		return mappings.filter { it.sourceCalendarId == sourceCalendarId && it.targetCalendarId == targetCalendarId }
	}

	override suspend fun upsert(mapping: EventMapping): Long {
		val id = if (mapping.id == 0L) nextId++ else mapping.id
		val updated = mapping.copy(id = id)
		val index = mappings.indexOfFirst { it.id == id }
		if (index >= 0) {
			mappings[index] = updated
		} else {
			mappings.add(updated)
		}
		return id
	}

	override suspend fun deleteByIds(ids: List<Long>) {
		mappings.removeAll { it.id in ids }
	}

	override suspend fun deleteByJob(sourceCalendarId: Long, targetCalendarId: Long) {
		mappings.removeAll { it.sourceCalendarId == sourceCalendarId && it.targetCalendarId == targetCalendarId }
	}

	override suspend fun countForJob(sourceCalendarId: Long, targetCalendarId: Long): Int {
		return mappings.count { it.sourceCalendarId == sourceCalendarId && it.targetCalendarId == targetCalendarId }
	}

	override suspend fun countSyncedTargets(calendarId: Long): Int {
		return mappings.count { it.targetCalendarId == calendarId }
	}
}

private fun <T> runBlockingTest(block: suspend () -> T): T {
	return kotlinx.coroutines.runBlocking { block() }
}
