package net.amunak.calscium.ui.formatters

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val lastSyncFormatter: DateTimeFormatter =
	DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

fun formatLastSync(timestamp: Long?): String {
	if (timestamp == null) return "Last sync: never"
	val localDateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault())
	return "Last sync: " + lastSyncFormatter.format(localDateTime)
}
