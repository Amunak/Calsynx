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
import net.amunak.calsynx.data.sync.AvailabilityMode
import net.amunak.calsynx.data.sync.ReminderMode
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
	fun syncAppliesAvailabilityOverrides() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 17L
		val targetId = 27L

		insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Availability",
				startMillis = 1_000L,
				endMillis = 2_000L,
				availability = CalendarContract.Events.AVAILABILITY_BUSY
			)
		)

		syncer.sync(
			syncJob(
				sourceId,
				targetId,
				availabilityMode = AvailabilityMode.FORCE_FREE
			),
			SyncWindow(0L, 10_000L)
		)

		val target = queryEvents(resolver, targetId).first()
		assertEquals(CalendarContract.Events.AVAILABILITY_FREE, target.getAsInteger(CalendarContract.Events.AVAILABILITY))
	}

	@Test
	fun syncCopiesPrivacyAndColorWhenEnabled() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 18L
		val targetId = 28L

		insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Privacy",
				startMillis = 1_000L,
				endMillis = 2_000L,
				accessLevel = CalendarContract.Events.ACCESS_PRIVATE,
				eventColor = 0xFF3366CC.toInt()
			)
		)

		syncer.sync(
			syncJob(
				sourceId,
				targetId,
				copyPrivacy = true,
				copyEventColor = true
			),
			SyncWindow(0L, 10_000L)
		)

		val target = queryEvents(resolver, targetId).first()
		assertEquals(CalendarContract.Events.ACCESS_PRIVATE, target.getAsInteger(CalendarContract.Events.ACCESS_LEVEL))
		assertEquals(0xFF3366CC.toInt(), target.getAsInteger(CalendarContract.Events.EVENT_COLOR))
	}

	@Test
	fun syncCopiesAttendeesAndOrganizerWhenEnabled() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 19L
		val targetId = 29L

		val sourceEventId = insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Guests",
				startMillis = 1_000L,
				endMillis = 2_000L,
				organizer = "organizer@example.com",
				ownerAccount = "owner@example.com"
			)
		)
		insertAttendee(
			resolver,
			sourceEventId,
			email = "guest@example.com",
			name = "Guest",
			status = CalendarContract.Attendees.ATTENDEE_STATUS_INVITED
		)

		syncer.sync(
			syncJob(
				sourceId,
				targetId,
				copyAttendees = true,
				copyOrganizer = true
			),
			SyncWindow(0L, 10_000L)
		)

		val target = queryEvents(resolver, targetId).first()
		assertEquals("organizer@example.com", target.getAsString(CalendarContract.Events.ORGANIZER))
		assertEquals(null, target.getAsString(CalendarContract.Events.OWNER_ACCOUNT))

		val targetIdValue = target.getAsLong(CalendarContract.Events._ID)
		val attendees = queryAttendees(resolver, targetIdValue)
		assertEquals(1, attendees.size)
		assertEquals("guest@example.com", attendees.first().getAsString(CalendarContract.Attendees.ATTENDEE_EMAIL))
	}

	@Test
	fun syncAppliesCustomReminders() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 20L
		val targetId = 30L

		insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "All-day",
				startMillis = 1_000L,
				endMillis = 2_000L,
				allDay = true
			)
		)
		insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Timed",
				startMillis = 5_000L,
				endMillis = 6_000L,
				allDay = false
			)
		)

		syncer.sync(
			syncJob(
				sourceId,
				targetId,
				reminderMode = ReminderMode.CUSTOM,
				reminderAllDayMinutes = 2880,
				reminderTimedMinutes = 30
			),
			SyncWindow(0L, 10_000L)
		)

		val targets = queryEvents(resolver, targetId)
		val allDayTarget = targets.first { it.getAsString(CalendarContract.Events.TITLE) == "All-day" }
		val timedTarget = targets.first { it.getAsString(CalendarContract.Events.TITLE) == "Timed" }

		val allDayReminders = queryReminders(resolver, allDayTarget.getAsLong(CalendarContract.Events._ID))
		val timedReminders = queryReminders(resolver, timedTarget.getAsLong(CalendarContract.Events._ID))
		assertEquals(1, allDayReminders.size)
		assertEquals(2880L, allDayReminders.first().getAsLong(CalendarContract.Reminders.MINUTES))
		assertEquals(1, timedReminders.size)
		assertEquals(30L, timedReminders.first().getAsLong(CalendarContract.Reminders.MINUTES))
	}

	@Test
	fun syncSkipsDisabledReminderTypes() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 32L
		val targetId = 42L

		insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "All-day",
				startMillis = 1_000L,
				endMillis = 2_000L,
				allDay = true
			)
		)
		insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Timed",
				startMillis = 5_000L,
				endMillis = 6_000L,
				allDay = false
			)
		)

		syncer.sync(
			syncJob(
				sourceId,
				targetId,
				reminderMode = ReminderMode.CUSTOM,
				reminderAllDayEnabled = false,
				reminderTimedEnabled = true,
				reminderTimedMinutes = 45
			),
			SyncWindow(0L, 10_000L)
		)

		val targets = queryEvents(resolver, targetId)
		val allDayTarget = targets.first { it.getAsString(CalendarContract.Events.TITLE) == "All-day" }
		val timedTarget = targets.first { it.getAsString(CalendarContract.Events.TITLE) == "Timed" }

		val allDayReminders = queryReminders(resolver, allDayTarget.getAsLong(CalendarContract.Events._ID))
		val timedReminders = queryReminders(resolver, timedTarget.getAsLong(CalendarContract.Events._ID))
		assertEquals(0, allDayReminders.size)
		assertEquals(1, timedReminders.size)
		assertEquals(45L, timedReminders.first().getAsLong(CalendarContract.Reminders.MINUTES))
	}

	@Test
	fun syncDoesNotResyncRemindersWhenDisabled() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 33L
		val targetId = 43L

		val sourceEventId = insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Reminder",
				startMillis = 1_000L,
				endMillis = 2_000L
			)
		)
		insertReminder(resolver, sourceEventId, minutes = 15)

		syncer.sync(
			syncJob(sourceId, targetId, reminderMode = ReminderMode.COPY),
			SyncWindow(0L, 10_000L)
		)

		val targetEvent = queryEvents(resolver, targetId).first()
		val targetEventId = targetEvent.getAsLong(CalendarContract.Events._ID)
		resolver.delete(
			CalendarContract.Reminders.CONTENT_URI,
			"${CalendarContract.Reminders.EVENT_ID} = ?",
			arrayOf(targetEventId.toString())
		)
		insertReminder(resolver, targetEventId, minutes = 30)

		syncer.sync(
			syncJob(
				sourceId,
				targetId,
				reminderMode = ReminderMode.COPY,
				reminderResyncEnabled = false
			),
			SyncWindow(0L, 10_000L)
		)

		val reminders = queryReminders(resolver, targetEventId)
		assertEquals(1, reminders.size)
		assertEquals(30L, reminders.first().getAsLong(CalendarContract.Reminders.MINUTES))
	}

	@Test
	fun syncCopiesRemindersWhenEnabled() = runBlockingTest {
		val mappingDao = InMemoryEventMappingDao()
		val syncer = CalendarSyncer(resolver, mappingDao, eventsUri)
		val sourceId = 21L
		val targetId = 31L

		val sourceEventId = insertEvent(
			resolver,
			sourceId,
			eventValues(
				title = "Reminder copy",
				startMillis = 1_000L,
				endMillis = 2_000L
			)
		)
		insertReminder(resolver, sourceEventId, minutes = 15)

		syncer.sync(
			syncJob(sourceId, targetId, reminderMode = ReminderMode.COPY),
			SyncWindow(0L, 10_000L)
		)

		val target = queryEvents(resolver, targetId).first()
		val reminders = queryReminders(resolver, target.getAsLong(CalendarContract.Events._ID))
		assertEquals(1, reminders.size)
		assertEquals(15L, reminders.first().getAsLong(CalendarContract.Reminders.MINUTES))
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

	private fun syncJob(
		sourceId: Long,
		targetId: Long,
		availabilityMode: AvailabilityMode = AvailabilityMode.COPY,
		copyEventColor: Boolean = false,
		copyPrivacy: Boolean = true,
		copyAttendees: Boolean = false,
		copyOrganizer: Boolean = false,
		reminderMode: ReminderMode = ReminderMode.COPY,
		reminderAllDayMinutes: Int = 1440,
		reminderTimedMinutes: Int = 60,
		reminderAllDayEnabled: Boolean = true,
		reminderTimedEnabled: Boolean = true,
		reminderResyncEnabled: Boolean = true
	): SyncJob {
		return SyncJob(
			id = 1L,
			sourceCalendarId = sourceId,
			targetCalendarId = targetId,
			windowPastDays = 7,
			windowFutureDays = 7,
			availabilityMode = availabilityMode.value,
			copyEventColor = copyEventColor,
			copyPrivacy = copyPrivacy,
			copyAttendees = copyAttendees,
			copyOrganizer = copyOrganizer,
			reminderMode = reminderMode.value,
			reminderAllDayMinutes = reminderAllDayMinutes,
			reminderTimedMinutes = reminderTimedMinutes,
			reminderAllDayEnabled = reminderAllDayEnabled,
			reminderTimedEnabled = reminderTimedEnabled,
			reminderResyncEnabled = reminderResyncEnabled
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
			CalendarContract.Events.RDATE,
			CalendarContract.Events.AVAILABILITY,
			CalendarContract.Events.ACCESS_LEVEL,
			CalendarContract.Events.EVENT_COLOR,
			CalendarContract.Events.ORGANIZER,
			CalendarContract.Events.OWNER_ACCOUNT
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

	private fun insertReminder(
		resolver: android.content.ContentResolver,
		eventId: Long,
		minutes: Int,
		method: Int = CalendarContract.Reminders.METHOD_ALERT
	) {
		val values = ContentValues().apply {
			put(CalendarContract.Reminders.EVENT_ID, eventId)
			put(CalendarContract.Reminders.MINUTES, minutes)
			put(CalendarContract.Reminders.METHOD, method)
		}
		resolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
	}

	private fun queryReminders(
		resolver: android.content.ContentResolver,
		eventId: Long
	): List<ContentValues> {
		val projection = arrayOf(
			CalendarContract.Reminders.EVENT_ID,
			CalendarContract.Reminders.MINUTES,
			CalendarContract.Reminders.METHOD
		)
		val cursor = resolver.query(
			CalendarContract.Reminders.CONTENT_URI,
			projection,
			"${CalendarContract.Reminders.EVENT_ID} = ?",
			arrayOf(eventId.toString()),
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
						row.put(column, it.getLong(index))
					}
				}
				results.add(row)
			}
		}
		return results
	}

	private fun insertAttendee(
		resolver: android.content.ContentResolver,
		eventId: Long,
		email: String,
		name: String,
		status: Int
	) {
		val values = ContentValues().apply {
			put(CalendarContract.Attendees.EVENT_ID, eventId)
			put(CalendarContract.Attendees.ATTENDEE_EMAIL, email)
			put(CalendarContract.Attendees.ATTENDEE_NAME, name)
			put(CalendarContract.Attendees.ATTENDEE_STATUS, status)
			put(CalendarContract.Attendees.ATTENDEE_TYPE, CalendarContract.Attendees.TYPE_REQUIRED)
			put(CalendarContract.Attendees.ATTENDEE_RELATIONSHIP, CalendarContract.Attendees.RELATIONSHIP_ATTENDEE)
		}
		resolver.insert(CalendarContract.Attendees.CONTENT_URI, values)
	}

	private fun queryAttendees(
		resolver: android.content.ContentResolver,
		eventId: Long
	): List<ContentValues> {
		val projection = arrayOf(
			CalendarContract.Attendees.EVENT_ID,
			CalendarContract.Attendees.ATTENDEE_EMAIL,
			CalendarContract.Attendees.ATTENDEE_NAME,
			CalendarContract.Attendees.ATTENDEE_STATUS
		)
		val cursor = resolver.query(
			CalendarContract.Attendees.CONTENT_URI,
			projection,
			"${CalendarContract.Attendees.EVENT_ID} = ?",
			arrayOf(eventId.toString()),
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
						row.put(column, it.getString(index))
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
		availability: Int? = null,
		accessLevel: Int? = null,
		eventColor: Int? = null,
		organizer: String? = null,
		ownerAccount: String? = null,
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
			if (availability != null) {
				put(CalendarContract.Events.AVAILABILITY, availability)
			}
			if (accessLevel != null) {
				put(CalendarContract.Events.ACCESS_LEVEL, accessLevel)
			}
			if (eventColor != null) {
				put(CalendarContract.Events.EVENT_COLOR, eventColor)
			}
			if (organizer != null) {
				put(CalendarContract.Events.ORGANIZER, organizer)
			}
			if (ownerAccount != null) {
				put(CalendarContract.Events.OWNER_ACCOUNT, ownerAccount)
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
