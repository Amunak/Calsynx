package net.amunak.calscium.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
	entities = [SyncJob::class],
	version = 1,
	exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
	abstract fun syncJobDao(): SyncJobDao
}
