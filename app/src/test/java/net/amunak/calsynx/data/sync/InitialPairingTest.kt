package net.amunak.calsynx.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class InitialPairingTest {
	@Test
	fun pairExistingEvents_matchesTitleDateAndAllDay() {
		val zone = ZoneId.of("UTC")
		val source = listOf(
			sourceEvent(
				id = 1L,
				title = "Standup",
				dateTime = LocalDateTime.of(2026, 2, 10, 0, 0),
				allDay = true,
				zone = zone
			)
		)
		val targets = listOf(
			targetEvent(
				id = 10L,
				title = "Standup",
				dateTime = LocalDateTime.of(2026, 2, 10, 9, 0),
				allDay = false,
				zone = zone
			),
			targetEvent(
				id = 11L,
				title = "Standup",
				dateTime = LocalDateTime.of(2026, 2, 10, 0, 0),
				allDay = true,
				zone = zone
			)
		)

		val pairs = pairExistingEventsByTitleAndDate(source, targets, zone)

		assertEquals(listOf(1L to 11L), pairs)
	}

	@Test
	fun pairExistingEvents_pairsInOrderUntilOneSideEnds() {
		val zone = ZoneId.of("UTC")
		val sources = listOf(
			sourceEvent(
				id = 1L,
				title = "Review",
				dateTime = LocalDateTime.of(2026, 2, 11, 8, 0),
				allDay = false,
				zone = zone
			),
			sourceEvent(
				id = 2L,
				title = "Review",
				dateTime = LocalDateTime.of(2026, 2, 11, 9, 0),
				allDay = false,
				zone = zone
			)
		)
		val targets = listOf(
			targetEvent(
				id = 10L,
				title = "Review",
				dateTime = LocalDateTime.of(2026, 2, 11, 7, 0),
				allDay = false,
				zone = zone
			),
			targetEvent(
				id = 11L,
				title = "Review",
				dateTime = LocalDateTime.of(2026, 2, 11, 10, 0),
				allDay = false,
				zone = zone
			),
			targetEvent(
				id = 12L,
				title = "Review",
				dateTime = LocalDateTime.of(2026, 2, 11, 11, 0),
				allDay = false,
				zone = zone
			)
		)

		val pairs = pairExistingEventsByTitleAndDate(sources, targets, zone)

		assertEquals(listOf(1L to 10L, 2L to 11L), pairs)
	}

	@Test
	fun pairExistingEvents_handlesFewerTargetsThanSources() {
		val zone = ZoneId.of("UTC")
		val sources = listOf(
			sourceEvent(
				id = 1L,
				title = "Planning",
				dateTime = LocalDateTime.of(2026, 2, 12, 9, 0),
				allDay = false,
				zone = zone
			),
			sourceEvent(
				id = 2L,
				title = "Planning",
				dateTime = LocalDateTime.of(2026, 2, 12, 10, 0),
				allDay = false,
				zone = zone
			),
			sourceEvent(
				id = 3L,
				title = "Planning",
				dateTime = LocalDateTime.of(2026, 2, 12, 11, 0),
				allDay = false,
				zone = zone
			)
		)
		val targets = listOf(
			targetEvent(
				id = 10L,
				title = "Planning",
				dateTime = LocalDateTime.of(2026, 2, 12, 8, 30),
				allDay = false,
				zone = zone
			)
		)

		val pairs = pairExistingEventsByTitleAndDate(sources, targets, zone)

		assertEquals(listOf(1L to 10L), pairs)
	}

	@Test
	fun pairExistingEvents_returnsEmptyWhenNoTargets() {
		val zone = ZoneId.of("UTC")
		val sources = listOf(
			sourceEvent(
				id = 1L,
				title = "Kickoff",
				dateTime = LocalDateTime.of(2026, 2, 13, 9, 0),
				allDay = false,
				zone = zone
			)
		)

		val pairs = pairExistingEventsByTitleAndDate(sources, emptyList(), zone)

		assertEquals(emptyList<Pair<Long, Long>>(), pairs)
	}

	@Test
	fun pairExistingEvents_ignoresBlankTitles() {
		val zone = ZoneId.of("UTC")
		val sources = listOf(
			sourceEvent(
				id = 1L,
				title = "  ",
				dateTime = LocalDateTime.of(2026, 3, 1, 8, 0),
				allDay = false,
				zone = zone
			)
		)
		val targets = listOf(
			targetEvent(
				id = 10L,
				title = "  ",
				dateTime = LocalDateTime.of(2026, 3, 1, 8, 0),
				allDay = false,
				zone = zone
			)
		)

		val pairs = pairExistingEventsByTitleAndDate(sources, targets, zone)

		assertEquals(emptyList<Pair<Long, Long>>(), pairs)
	}

	@Test
	fun pairExistingEvents_matchesMixedTitlesAndDates() {
		val zone = ZoneId.of("UTC")
		val sources = listOf(
			sourceEvent(
				id = 1L,
				title = "Design",
				dateTime = LocalDateTime.of(2026, 2, 14, 9, 0),
				allDay = false,
				zone = zone
			),
			sourceEvent(
				id = 2L,
				title = "Design",
				dateTime = LocalDateTime.of(2026, 2, 15, 9, 0),
				allDay = false,
				zone = zone
			),
			sourceEvent(
				id = 3L,
				title = "Holiday",
				dateTime = LocalDateTime.of(2026, 2, 15, 0, 0),
				allDay = true,
				zone = zone
			)
		)
		val targets = listOf(
			targetEvent(
				id = 10L,
				title = "Design",
				dateTime = LocalDateTime.of(2026, 2, 14, 10, 0),
				allDay = false,
				zone = zone
			),
			targetEvent(
				id = 11L,
				title = "Holiday",
				dateTime = LocalDateTime.of(2026, 2, 15, 12, 0),
				allDay = false,
				zone = zone
			),
			targetEvent(
				id = 12L,
				title = "Holiday",
				dateTime = LocalDateTime.of(2026, 2, 15, 0, 0),
				allDay = true,
				zone = zone
			)
		)

		val pairs = pairExistingEventsByTitleAndDate(sources, targets, zone)

		assertEquals(listOf(1L to 10L, 3L to 12L), pairs)
	}

	@Test
	fun excludeMappedTargets_filtersMappedIds() {
		val targets = listOf(
			TargetEvent(id = 1L, title = "A", startMillis = 1L, allDay = false),
			TargetEvent(id = 2L, title = "B", startMillis = 2L, allDay = false),
			TargetEvent(id = 3L, title = "C", startMillis = 3L, allDay = true)
		)

		val filtered = excludeMappedTargets(targets, setOf(2L, 3L))

		assertEquals(listOf(targets[0]), filtered)
	}

	private fun sourceEvent(
		id: Long,
		title: String,
		dateTime: LocalDateTime,
		allDay: Boolean,
		zone: ZoneId
	): SourceEvent {
		return SourceEvent(
			id = id,
			title = title,
			startMillis = dateTime.atZone(zone).toInstant().toEpochMilli(),
			endMillis = null,
			duration = null,
			allDay = allDay,
			timeZone = zone.id,
			endTimeZone = zone.id,
			rrule = null,
			exdate = null,
			exrule = null,
			rdate = null,
			originalId = null,
			originalInstanceTime = null,
			originalAllDay = null,
			status = null,
			location = null,
			description = null,
			availability = null,
			accessLevel = null,
			eventColor = null,
			organizer = null,
			ownerAccount = null
		)
	}

	private fun targetEvent(
		id: Long,
		title: String,
		dateTime: LocalDateTime,
		allDay: Boolean,
		zone: ZoneId
	): TargetEvent {
		return TargetEvent(
			id = id,
			title = title,
			startMillis = dateTime.atZone(zone).toInstant().toEpochMilli(),
			allDay = allDay
		)
	}
}
