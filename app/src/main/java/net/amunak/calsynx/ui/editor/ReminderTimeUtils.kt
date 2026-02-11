package net.amunak.calsynx.ui.editor

import android.content.res.Resources
import net.amunak.calsynx.R

internal fun computeAllDayReminderMinutes(daysBefore: Int, timeMinutes: Int): Int {
	val clampedDays = daysBefore.coerceAtLeast(0)
	val clampedTime = timeMinutes.coerceIn(0, 1439)
	return clampedDays * 1440 + (1440 - clampedTime)
}

internal fun deriveAllDayReminderDays(totalMinutes: Int): Int {
	if (totalMinutes <= 0) return 0
	return totalMinutes / 1440
}

internal fun deriveAllDayReminderTimeMinutes(totalMinutes: Int): Int {
	if (totalMinutes <= 0) return 0
	val remainder = totalMinutes % 1440
	return if (remainder == 0) 0 else 1440 - remainder
}

internal fun formatReminderOffset(resources: Resources, minutesBefore: Int): String {
	val clampedMinutes = minutesBefore.coerceAtLeast(0)
	val hours = clampedMinutes / 60
	val minutes = clampedMinutes % 60
	val durationLabel = when {
		minutes == 0 -> resources.getQuantityString(R.plurals.duration_hours, hours, hours)
		hours == 0 -> resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
		else -> resources.getString(
			R.string.format_hours_minutes,
			resources.getQuantityString(R.plurals.duration_hours, hours, hours),
			resources.getQuantityString(R.plurals.duration_minutes, minutes, minutes)
		)
	}
	return resources.getString(R.string.label_reminder_offset_preview, durationLabel)
}
