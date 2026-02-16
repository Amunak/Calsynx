package net.amunak.calsynx.data.sync

import net.amunak.calsynx.data.EventMapping
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPlannerTest {
	@Test
	fun buildSyncPlan_handlesMissingTargetsCreatesAndOrphans() {
		val sources = listOf(
			sourceEvent(id = 1L),
			sourceEvent(id = 2L),
			sourceEvent(id = 3L)
		)
		val mappings = listOf(
			EventMapping(id = 10L, sourceEventId = 1L, targetEventId = 100L, sourceCalendarId = 7L, targetCalendarId = 8L),
			EventMapping(id = 11L, sourceEventId = 2L, targetEventId = 200L, sourceCalendarId = 7L, targetCalendarId = 8L),
			EventMapping(id = 12L, sourceEventId = 4L, targetEventId = 300L, sourceCalendarId = 7L, targetCalendarId = 8L),
			EventMapping(id = 13L, sourceEventId = 5L, targetEventId = 400L, sourceCalendarId = 7L, targetCalendarId = 8L)
		)
		val existingTargets = setOf(100L, 200L, 400L)

		val plan = buildSyncPlan(sources, mappings, existingTargets)

		assertEquals(listOf(12L), plan.missingMappingIds)
		assertEquals(listOf(3L), plan.createSources.map { it.id })
		assertEquals(listOf(100L, 200L), plan.updateTargets.map { it.first })
		assertEquals(listOf(400L), plan.orphanTargetIds)
		assertEquals(listOf(13L), plan.orphanMappingIds)
	}

	@Test
	fun buildSyncPlan_emptySourceMarksAllMappingsAsOrphans() {
		val mappings = listOf(
			EventMapping(id = 1L, sourceEventId = 10L, targetEventId = 100L, sourceCalendarId = 7L, targetCalendarId = 8L),
			EventMapping(id = 2L, sourceEventId = 20L, targetEventId = 200L, sourceCalendarId = 7L, targetCalendarId = 8L)
		)
		val plan = buildSyncPlan(emptyList(), mappings, setOf(100L, 200L))

		assertTrue(plan.createSources.isEmpty())
		assertTrue(plan.updateTargets.isEmpty())
		assertEquals(listOf(100L, 200L), plan.orphanTargetIds)
		assertEquals(listOf(1L, 2L), plan.orphanMappingIds)
	}

	@Test
	fun buildSyncPlan_missingTargetForExistingSourceCreatesNewTarget() {
		val sources = listOf(sourceEvent(id = 1L))
		val mappings = listOf(
			EventMapping(id = 5L, sourceEventId = 1L, targetEventId = 10L, sourceCalendarId = 7L, targetCalendarId = 8L)
		)

		val plan = buildSyncPlan(sources, mappings, existingTargetIds = emptySet())

		assertEquals(listOf(5L), plan.missingMappingIds)
		assertEquals(listOf(1L), plan.createSources.map { it.id })
		assertTrue(plan.updateTargets.isEmpty())
		assertTrue(plan.orphanTargetIds.isEmpty())
		assertTrue(plan.orphanMappingIds.isEmpty())
	}

	private fun sourceEvent(id: Long): SourceEvent {
		return SourceEvent(
			id = id,
			uid = "uid-$id",
			title = "Event $id",
			startMillis = 1_000L,
			endMillis = 2_000L,
			duration = null,
			allDay = false,
			timeZone = "UTC",
			endTimeZone = "UTC",
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
}
