package net.amunak.calsynx.domain

import kotlinx.coroutines.flow.Flow
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.data.EventMappingDao
import net.amunak.calsynx.data.repository.SyncJobRepository
import net.amunak.calsynx.data.sync.CalendarSyncer
import net.amunak.calsynx.data.sync.SyncResult

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
	private val repository: SyncJobRepository,
	private val mappingDao: EventMappingDao
) {
	suspend operator fun invoke(job: SyncJob) {
		mappingDao.deleteByJob(job.sourceCalendarId, job.targetCalendarId)
		repository.delete(job)
	}
}

class UpdateLastSyncUseCase(
	private val repository: SyncJobRepository
) {
	suspend operator fun invoke(job: SyncJob, timestamp: Long): Long {
		return repository.upsert(job.copy(lastSyncTimestamp = timestamp))
	}
}

class UpdateSyncStatsUseCase(
	private val repository: SyncJobRepository
) {
	suspend operator fun invoke(job: SyncJob, result: SyncResult): Long {
		val unpaired = (result.targetTotalCount - result.targetCount).coerceAtLeast(0)
		return repository.upsert(
			job.copy(
				lastSyncTimestamp = System.currentTimeMillis(),
				lastSyncCreated = result.created,
				lastSyncUpdated = result.updated,
				lastSyncDeleted = result.deleted,
				lastSyncSourceCount = result.sourceCount,
				lastSyncTargetCount = result.targetCount,
				lastSyncUnpairedTargetCount = unpaired,
				lastSyncError = null
			)
		)
	}
}

class UpdateSyncErrorUseCase(
	private val repository: SyncJobRepository
) {
	suspend operator fun invoke(job: SyncJob, message: String): Long {
		return repository.upsert(job.copy(lastSyncError = message))
	}
}

class RunManualSyncUseCase(
	private val syncer: CalendarSyncer,
	private val updateSyncStats: UpdateSyncStatsUseCase
) {
	suspend operator fun invoke(job: SyncJob): SyncResult {
		val result = syncer.sync(job)
		updateSyncStats(job, result)
		return result
	}
}
