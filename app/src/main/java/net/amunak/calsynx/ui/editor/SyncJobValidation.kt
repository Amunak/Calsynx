package net.amunak.calsynx.ui.editor

import net.amunak.calsynx.R
import net.amunak.calsynx.data.SyncJob

fun validateSyncJobSelection(
	sourceId: Long?,
	targetId: Long?,
	jobs: List<SyncJob>,
	currentJobId: Long?
): Int? {
	if (sourceId == null || targetId == null) return R.string.message_validation_select_both
	if (sourceId == targetId) return R.string.message_validation_source_target_same
	if (jobs.any { it.targetCalendarId == sourceId && it.id != currentJobId }) {
		return R.string.message_validation_source_is_target
	}
	if (jobs.any { it.sourceCalendarId == targetId && it.id != currentJobId }) {
		return R.string.message_validation_target_is_source
	}
	if (jobs.any {
			it.sourceCalendarId == sourceId &&
				it.targetCalendarId == targetId &&
				it.id != currentJobId
		}) {
		return R.string.message_validation_duplicate_job
	}
	return null
}
