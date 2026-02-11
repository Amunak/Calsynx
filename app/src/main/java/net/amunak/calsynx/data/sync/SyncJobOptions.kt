package net.amunak.calsynx.data.sync

import android.provider.CalendarContract

enum class AvailabilityMode(val value: Int) {
	COPY(0),
	FORCE_BUSY(1),
	FORCE_FREE(2),
	FORCE_TENTATIVE(3);

	companion object {
		fun from(value: Int): AvailabilityMode {
			return entries.firstOrNull { it.value == value } ?: COPY
		}

		fun toAvailabilityValue(mode: AvailabilityMode): Int? {
			return when (mode) {
				COPY -> null
				FORCE_BUSY -> CalendarContract.Events.AVAILABILITY_BUSY
				FORCE_FREE -> CalendarContract.Events.AVAILABILITY_FREE
				FORCE_TENTATIVE -> CalendarContract.Events.AVAILABILITY_TENTATIVE
			}
		}
	}
}

enum class ReminderMode(val value: Int) {
	COPY(0),
	NONE(1),
	CUSTOM(2);

	companion object {
		fun from(value: Int): ReminderMode {
			return entries.firstOrNull { it.value == value } ?: COPY
		}
	}
}
