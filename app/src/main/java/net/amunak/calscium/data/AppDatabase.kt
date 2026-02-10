package net.amunak.calscium.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
	entities = [SyncJob::class, EventMapping::class],
	version = 2,
	exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
	abstract fun syncJobDao(): SyncJobDao
	abstract fun eventMappingDao(): EventMappingDao
}
