package net.amunak.calsynx.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_jobs")
data class SyncJob(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0L,
	val sourceCalendarId: Long,
	val targetCalendarId: Long,
	val windowPastDays: Int = 7,
	val windowFutureDays: Int = 90,
	val syncAllEvents: Boolean = false,
	val frequencyMinutes: Int = 240,
	val availabilityMode: Int = net.amunak.calsynx.data.sync.AvailabilityMode.COPY.value,
	val copyEventColor: Boolean = false,
	val copyPrivacy: Boolean = true,
	val copyAttendees: Boolean = false,
	val copyOrganizer: Boolean = false,
	val reminderMode: Int = net.amunak.calsynx.data.sync.ReminderMode.COPY.value,
	val reminderAllDayMinutes: Int = 1440,
	val reminderTimedMinutes: Int = 60,
	val reminderAllDayEnabled: Boolean = true,
	val reminderTimedEnabled: Boolean = true,
	val reminderResyncEnabled: Boolean = true,
	val pairExistingOnFirstSync: Boolean = false,
	val lastSyncTimestamp: Long? = null,
	val lastSyncCreated: Int = 0,
	val lastSyncUpdated: Int = 0,
	val lastSyncDeleted: Int = 0,
	val lastSyncSourceCount: Int = 0,
	val lastSyncTargetCount: Int = 0,
	val lastSyncUnpairedTargetCount: Int = 0,
	val lastSyncError: String? = null,
	val isActive: Boolean = true
)
