package net.amunak.calsynx.ui.formatters

import net.amunak.calsynx.calendar.CalendarInfo
import net.amunak.calsynx.data.SyncJob
import net.amunak.calsynx.ui.components.sanitizeCalendarName

data class JobCalendarNames(
	val sourceName: String,
	val targetName: String
)

fun resolveJobCalendarNames(
	job: SyncJob,
	calendarById: Map<Long, CalendarInfo>
): JobCalendarNames {
	val sourceName = sanitizeCalendarName(
		calendarById[job.sourceCalendarId]?.displayName
			?: "Unknown (${job.sourceCalendarId})"
	)
	val targetName = sanitizeCalendarName(
		calendarById[job.targetCalendarId]?.displayName
			?: "Unknown (${job.targetCalendarId})"
	)
	return JobCalendarNames(sourceName, targetName)
}

fun resolveJobCalendarNames(
	job: SyncJob,
	calendars: List<CalendarInfo>
): JobCalendarNames {
	return resolveJobCalendarNames(job, calendars.associateBy { it.id })
}

fun formatJobLabel(
	job: SyncJob,
	calendarById: Map<Long, CalendarInfo>
): String {
	val names = resolveJobCalendarNames(job, calendarById)
	return "job ${job.id} (${names.sourceName} → ${names.targetName})"
}

fun formatJobLabel(
	job: SyncJob,
	calendars: List<CalendarInfo>
): String {
	return formatJobLabel(job, calendars.associateBy { it.id })
}
