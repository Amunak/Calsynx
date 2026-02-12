package net.amunak.calsynx.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import net.amunak.calsynx.data.DatabaseProvider
import net.amunak.calsynx.data.repository.CalendarRepository
import net.amunak.calsynx.data.repository.SyncJobRepository
import net.amunak.calsynx.domain.RunManualSyncUseCase
import net.amunak.calsynx.domain.UpdateSyncErrorUseCase
import net.amunak.calsynx.domain.UpdateSyncStatsUseCase
import net.amunak.calsynx.ui.logs.SyncLogStore
import net.amunak.calsynx.ui.formatters.formatJobLabel

class CalendarSyncWorker(
	appContext: Context,
	params: WorkerParameters
) : CoroutineWorker(appContext, params) {
	override suspend fun doWork(): Result {
		val jobId = inputData.getLong(KEY_JOB_ID, INVALID_JOB_ID)
		if (jobId == INVALID_JOB_ID) {
			Log.w(TAG, "Missing job id for background sync")
			return Result.failure()
		}

		val database = DatabaseProvider.get(applicationContext)
		val jobRepository = SyncJobRepository(database.syncJobDao())
		val job = jobRepository.getById(jobId)
		if (job == null) {
			Log.w(TAG, "Background sync skipped: job $jobId not found")
			return Result.success()
		}
		if (!job.isActive) {
			Log.i(TAG, "Background sync skipped: job $jobId is inactive")
			return Result.success()
		}

		val syncer = CalendarSyncer(applicationContext.contentResolver, database.eventMappingDao())
		val updateStats = UpdateSyncStatsUseCase(jobRepository)
		val updateError = UpdateSyncErrorUseCase(jobRepository)
		val runner = RunManualSyncUseCase(syncer, updateStats)
		val logStore = SyncLogStore(applicationContext)
		val calendarRepository = CalendarRepository()
		val label = buildJobLabel(job, calendarRepository)

		return try {
			val result = runner.invoke(job)
			logStore.append(formatSyncSummary("Background sync", label, result))
			if (result.initialPairAttempted) {
				logStore.append(
					"Initial sync paired ${result.initialPairCount} existing target events for $label"
				)
			}
			logStore.append("Background sync completed for $label")
			Result.success()
		} catch (e: SecurityException) {
			Log.e(TAG, "Background sync permission denied for job $jobId", e)
			logStore.append("Background sync permission denied for $label")
			updateError(job, "Calendar permission denied")
			Result.success()
		} catch (e: RuntimeException) {
			Log.e(TAG, "Background sync failed for job $jobId", e)
			logStore.append("Background sync failed for $label: ${e.message ?: "unknown error"}")
			updateError(job, "Background sync failed")
			Result.retry()
		}
	}

	private fun buildJobLabel(job: net.amunak.calsynx.data.SyncJob, calendarRepository: CalendarRepository): String {
		return try {
			val calendars = calendarRepository.getCalendars(applicationContext, onlyVisible = false)
			formatJobLabel(job, calendars)
		} catch (e: RuntimeException) {
			"job ${job.id}"
		}
	}

	private fun formatSyncSummary(
		prefix: String,
		label: String,
		result: SyncResult
	): String {
		return "$prefix summary for $label: " +
			"created=${result.created}, updated=${result.updated}, deleted=${result.deleted}, " +
			"source=${result.sourceCount}, targets=${result.targetCount}, totalTargets=${result.targetTotalCount}"
	}

	companion object {
		const val KEY_JOB_ID = "sync_job_id"
		private const val INVALID_JOB_ID = -1L
		private const val TAG = "CalendarSyncWorker"
	}
}
