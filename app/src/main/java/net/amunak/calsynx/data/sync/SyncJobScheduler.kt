package net.amunak.calsynx.data.sync

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import net.amunak.calsynx.data.SyncJob
import java.util.concurrent.TimeUnit

class SyncJobScheduler(private val context: Context) {
	private val workManager = WorkManager.getInstance(context)

	fun schedule(job: SyncJob) {
		if (!job.isActive) {
			cancel(job.id)
			return
		}

		val intervalMinutes = job.frequencyMinutes.coerceAtLeast(MIN_INTERVAL_MINUTES)
		// Allow WorkManager to batch within a flex window to save power.
		val flexMinutes = (intervalMinutes / 3).coerceIn(MIN_FLEX_MINUTES, intervalMinutes)
		val data = Data.Builder()
			.putLong(CalendarSyncWorker.KEY_JOB_ID, job.id)
			.build()
		val request = PeriodicWorkRequestBuilder<CalendarSyncWorker>(
			intervalMinutes.toLong(),
			TimeUnit.MINUTES,
			flexMinutes.toLong(),
			TimeUnit.MINUTES
		)
			.setInputData(data)
			.setConstraints(Constraints.NONE)
			.addTag(jobTag(job.id))
			.build()

		workManager.enqueueUniquePeriodicWork(
			uniqueWorkName(job.id),
			ExistingPeriodicWorkPolicy.UPDATE,
			request
		)
	}

	fun enqueueImmediate(job: SyncJob) {
		if (!job.isActive) return
		val data = Data.Builder()
			.putLong(CalendarSyncWorker.KEY_JOB_ID, job.id)
			.build()
		val request = OneTimeWorkRequestBuilder<CalendarSyncWorker>()
			.setInputData(data)
			.setConstraints(Constraints.NONE)
			.addTag(jobTag(job.id))
			.build()
		workManager.enqueueUniqueWork(
			immediateWorkName(job.id),
			ExistingWorkPolicy.REPLACE,
			request
		)
	}

	fun cancel(jobId: Long) {
		workManager.cancelUniqueWork(uniqueWorkName(jobId))
	}

	fun scheduleAll(jobs: List<SyncJob>) {
		val activeIds = HashSet<Long>()
		jobs.forEach { job ->
			if (job.isActive) {
				activeIds.add(job.id)
				schedule(job)
			}
		}
		jobs.filter { !it.isActive }.forEach { cancel(it.id) }
		if (activeIds.isEmpty()) {
			Log.i(TAG, "No active sync jobs to schedule")
		}
	}

	private fun uniqueWorkName(jobId: Long) = "sync_job_$jobId"

	private fun immediateWorkName(jobId: Long) = "sync_job_immediate_$jobId"

	private fun jobTag(jobId: Long) = "sync_job_tag_$jobId"

	companion object {
		private const val TAG = "SyncJobScheduler"
		private const val MIN_INTERVAL_MINUTES = 15
		private const val MIN_FLEX_MINUTES = 5
	}
}
