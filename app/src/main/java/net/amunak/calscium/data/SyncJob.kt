package net.amunak.calscium.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_jobs")
data class SyncJob(
	@PrimaryKey(autoGenerate = true)
	val id: Long = 0L,
	val sourceCalendarId: Long,
	val targetCalendarId: Long,
	val lastSyncTimestamp: Long? = null,
	val lastSyncCreated: Int = 0,
	val lastSyncUpdated: Int = 0,
	val lastSyncDeleted: Int = 0,
	val lastSyncSourceCount: Int = 0,
	val lastSyncTargetCount: Int = 0,
	val lastSyncError: String? = null,
	val isActive: Boolean = true
)
