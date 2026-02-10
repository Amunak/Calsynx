package net.amunak.calscium.domain

import kotlinx.coroutines.flow.Flow
import net.amunak.calscium.data.SyncJob
import net.amunak.calscium.data.repository.SyncJobRepository

class ObserveSyncJobsUseCase(
	private val repository: SyncJobRepository
) {
	operator fun invoke(): Flow<List<SyncJob>> = repository.observeJobs()
}

class CreateSyncJobUseCase(
	private val repository: SyncJobRepository
) {
	suspend operator fun invoke(job: SyncJob): Long = repository.upsert(job)
}

class UpdateSyncJobUseCase(
	private val repository: SyncJobRepository
) {
	suspend operator fun invoke(job: SyncJob): Long = repository.upsert(job)
}

class DeleteSyncJobUseCase(
	private val repository: SyncJobRepository
) {
	suspend operator fun invoke(job: SyncJob) = repository.delete(job)
}

class UpdateLastSyncUseCase(
	private val repository: SyncJobRepository
) {
	suspend operator fun invoke(job: SyncJob, timestamp: Long): Long {
		return repository.upsert(job.copy(lastSyncTimestamp = timestamp))
	}
}
