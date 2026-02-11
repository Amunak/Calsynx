package net.amunak.calsynx.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
	tableName = "event_mappings",
	indices = [
		Index(
			value = ["sourceEventId", "sourceCalendarId", "targetCalendarId"],
			unique = true
		)
	]
)
data class EventMapping(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0L,
	val sourceEventId: Long,
	val targetEventId: Long,
	val sourceCalendarId: Long,
	val targetCalendarId: Long
)
