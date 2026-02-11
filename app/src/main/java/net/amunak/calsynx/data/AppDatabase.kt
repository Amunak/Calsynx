package net.amunak.calsynx.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
	entities = [SyncJob::class, EventMapping::class],
	version = 6,
	exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
	abstract fun syncJobDao(): SyncJobDao
	abstract fun eventMappingDao(): EventMappingDao
}
