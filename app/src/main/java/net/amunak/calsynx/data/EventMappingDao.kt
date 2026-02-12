package net.amunak.calsynx.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface EventMappingDao {
	@Query(
		"""
		SELECT * FROM event_mappings
		WHERE sourceCalendarId = :sourceCalendarId
		  AND targetCalendarId = :targetCalendarId
		"""
	)
	suspend fun getForJob(
		sourceCalendarId: Long,
		targetCalendarId: Long
	): List<EventMapping>

	@Upsert
	suspend fun upsert(mapping: EventMapping): Long

	@Query("DELETE FROM event_mappings WHERE id IN (:ids)")
	suspend fun deleteByIds(ids: List<Long>)

	@Query(
		"""
		DELETE FROM event_mappings
		WHERE sourceCalendarId = :sourceCalendarId
		  AND targetCalendarId = :targetCalendarId
		"""
	)
	suspend fun deleteByJob(sourceCalendarId: Long, targetCalendarId: Long)

	@Query(
		"""
		SELECT COUNT(*) FROM event_mappings
		WHERE sourceCalendarId = :sourceCalendarId
		  AND targetCalendarId = :targetCalendarId
		"""
	)
	suspend fun countForJob(sourceCalendarId: Long, targetCalendarId: Long): Int

	@Query("SELECT COUNT(*) FROM event_mappings WHERE targetCalendarId = :calendarId")
	suspend fun countSyncedTargets(calendarId: Long): Int

	@Query(
		"""
		SELECT targetEventId FROM event_mappings
		WHERE targetCalendarId = :calendarId
		"""
	)
	suspend fun getTargetEventIdsForCalendar(calendarId: Long): List<Long>
}
