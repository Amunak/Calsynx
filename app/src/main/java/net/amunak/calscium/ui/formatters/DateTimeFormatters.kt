package net.amunak.calscium.ui.formatters

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val lastSyncFormatter: DateTimeFormatter =
	DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun formatLastSync(timestamp: Long?, isSyncing: Boolean): String {
	if (isSyncing) return "Last sync: syncing..."
	if (timestamp == null) return "Last sync: never"
	val localDateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
	return "Last sync: " + lastSyncFormatter.format(localDateTime)
}

fun formatSyncCounts(
	created: Int,
	updated: Int,
	deleted: Int,
	sourceCount: Int,
	targetCount: Int
): String {
	return "Created $created, Updated $updated, Deleted $deleted · Source $sourceCount, Target $targetCount"
}

fun formatFrequency(minutes: Int): String {
	return when {
		minutes < 60 -> "Every ${minutes} min"
		minutes % 1440 == 0 -> "Every ${minutes / 1440} day(s)"
		minutes % 60 == 0 -> "Every ${minutes / 60} hour(s)"
		else -> "Every ${minutes} min"
	}
}
