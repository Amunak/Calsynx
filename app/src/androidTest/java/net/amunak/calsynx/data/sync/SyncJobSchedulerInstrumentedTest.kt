package net.amunak.calsynx.data.sync

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import net.amunak.calsynx.data.SyncJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncJobSchedulerInstrumentedTest {
	private lateinit var workManager: WorkManager
	private lateinit var scheduler: SyncJobScheduler

	@Before
	fun setUp() {
		val context = InstrumentationRegistry.getInstrumentation().targetContext
		WorkManagerTestInitHelper.initializeTestWorkManager(
			context,
			Configuration.Builder()
				.setExecutor(SynchronousExecutor())
				.build()
		)
		workManager = WorkManager.getInstance(context)
		scheduler = SyncJobScheduler(context)
	}

	@Test
	fun scheduleEnqueuesPeriodicWork() {
		val job = SyncJob(id = 1L, sourceCalendarId = 1L, targetCalendarId = 2L, frequencyMinutes = 30)
		scheduler.schedule(job)

		val infos = workManager.getWorkInfosForUniqueWork("sync_job_1").get()
		assertEquals(1, infos.size)
		assertTrue(isEnqueuedOrRunning(infos.first().state))
	}

	@Test
	fun scheduleInactiveCancelsWork() {
		val job = SyncJob(id = 2L, sourceCalendarId = 1L, targetCalendarId = 2L, frequencyMinutes = 30)
		scheduler.schedule(job)
		scheduler.schedule(job.copy(isActive = false))

		val infos = workManager.getWorkInfosForUniqueWork("sync_job_2").get()
		assertTrue(infos.isEmpty() || infos.first().state == WorkInfo.State.CANCELLED)
	}

	@Test
	fun enqueueImmediateCreatesOneTimeWork() {
		val job = SyncJob(id = 3L, sourceCalendarId = 1L, targetCalendarId = 2L, frequencyMinutes = 30)
		scheduler.enqueueImmediate(job)

		val infos = workManager.getWorkInfosForUniqueWork("sync_job_immediate_3").get()
		assertEquals(1, infos.size)
		assertTrue(isEnqueuedOrRunning(infos.first().state))
	}

	private fun isEnqueuedOrRunning(state: WorkInfo.State): Boolean {
		return state == WorkInfo.State.ENQUEUED || state == WorkInfo.State.RUNNING
	}
}
