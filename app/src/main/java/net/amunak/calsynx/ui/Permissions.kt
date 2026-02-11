package net.amunak.calsynx.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

fun hasCalendarPermissions(context: Context): Boolean {
	val readGranted = ContextCompat.checkSelfPermission(
		context,
		Manifest.permission.READ_CALENDAR
	) == PackageManager.PERMISSION_GRANTED
	val writeGranted = ContextCompat.checkSelfPermission(
		context,
		Manifest.permission.WRITE_CALENDAR
	) == PackageManager.PERMISSION_GRANTED
	return readGranted && writeGranted
}
