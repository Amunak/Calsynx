package net.amunak.calsynx.data.repository

import kotlinx.coroutines.flow.Flow
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.data.SyncJobDao

class SyncJobRepository(
	private val dao: SyncJobDao
) {
	fun observeJobs(): Flow<List<SyncJob>> = dao.getAllFlow()

	suspend fun getAll(): List<SyncJob> = dao.getAll()

	suspend fun getActive(): List<SyncJob> = dao.getActive()

	suspend fun getById(id: Long): SyncJob? = dao.getById(id)

	suspend fun upsert(job: SyncJob): Long = dao.upsert(job)

	suspend fun delete(job: SyncJob) = dao.delete(job)
}
