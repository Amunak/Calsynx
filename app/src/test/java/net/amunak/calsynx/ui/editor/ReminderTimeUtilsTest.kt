package net.amunak.calsynx.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderTimeUtilsTest {
	@Test
	fun computeAllDayReminderMinutesUsesPreviousDayTime() {
		val minutes = computeAllDayReminderMinutes(daysBefore = 0, timeMinutes = 20 * 60)
		assertEquals(240, minutes)
	}

	@Test
	fun computeAllDayReminderMinutesAddsDaysAndTimeOffset() {
		val minutes = computeAllDayReminderMinutes(daysBefore = 1, timeMinutes = 18 * 60)
		assertEquals(1800, minutes)
	}

	@Test
	fun deriveAllDayReminderValuesRoundTrip() {
		val originalMinutes = computeAllDayReminderMinutes(daysBefore = 2, timeMinutes = 21 * 60)
		val days = deriveAllDayReminderDays(originalMinutes)
		val timeMinutes = deriveAllDayReminderTimeMinutes(originalMinutes)
		assertEquals(2, days)
		assertEquals(21 * 60, timeMinutes)
	}
}
