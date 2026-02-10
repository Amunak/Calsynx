package net.amunak.calscium.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncJobDao {
	@Query("SELECT * FROM sync_jobs ORDER BY id DESC")
	fun getAllFlow(): Flow<List<SyncJob>>

	@Query("SELECT * FROM sync_jobs ORDER BY id DESC")
	suspend fun getAll(): List<SyncJob>

	@Query("SELECT * FROM sync_jobs WHERE isActive = 1 ORDER BY id DESC")
	suspend fun getActive(): List<SyncJob>

	@Query("SELECT * FROM sync_jobs WHERE id = :id LIMIT 1")
	suspend fun getById(id: Long): SyncJob?

	@Upsert
	suspend fun upsert(job: SyncJob): Long

	@Delete
	suspend fun delete(job: SyncJob)
}
