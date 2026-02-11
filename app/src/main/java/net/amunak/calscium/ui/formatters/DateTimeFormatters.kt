package net.amunak.calscium.ui.formatters

import android.content.res.Resources
import net.amunak.calscium.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val lastSyncFormatter: DateTimeFormatter =
	DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun formatLastSync(resources: Resources, timestamp: Long?, isSyncing: Boolean): String {
	if (isSyncing) return resources.getString(R.string.last_sync_syncing)
	if (timestamp == null) return resources.getString(R.string.last_sync_never)
	val localDateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
	return resources.getString(
		R.string.last_sync_with_time,
		lastSyncFormatter.format(localDateTime)
	)
}

fun formatSyncCounts(
	resources: Resources,
	created: Int,
	updated: Int,
	deleted: Int,
	sourceCount: Int,
	targetCount: Int
): String {
	return resources.getString(
		R.string.sync_counts,
		created,
		updated,
		deleted,
		sourceCount,
		targetCount
	)
}

fun formatFrequency(resources: Resources, minutes: Int): String {
	return when {
		minutes < 60 -> resources.getQuantityString(R.plurals.frequency_minutes, minutes, minutes)
		minutes % 1440 == 0 -> {
			val days = minutes / 1440
			resources.getQuantityString(R.plurals.frequency_days, days, days)
		}
		minutes % 60 == 0 -> {
			val hours = minutes / 60
			resources.getQuantityString(R.plurals.frequency_hours, hours, hours)
		}
		else -> resources.getQuantityString(R.plurals.frequency_minutes, minutes, minutes)
	}
}
