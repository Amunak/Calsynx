# Calsynx

Calsynx is an Android app that copies (synchronizes) events on-device from one calendar into another.
It does a simple one-way synchronization, (source → target). You need to provide the calendars:
it does not use any cloud services, accounts, or servers. These can be either local calendars
or calendars added by other apps / device accounts.

A typical use case would be syncing your work calendar (Outlook, Google Calendar, etc.) with your personal calendar.

Note that while we try to take care to not delete events in the target calendar,
you should still probably use a separate calendar for the sync outside of your main calendar.

## What it does
- Creates one-way sync jobs between calendars.
- Tracks last sync time, counts, and errors per job.
- Includes basic calendar management (create, rename, recolor, purge, delete).

### Sync behavior (one-way copy)
- Source events are copied into the target calendar; target-only events are ignored.
- Calendars can be used as source or target to avoid circular sync.
- Sync windows control how far back/ahead events are synced.
- Synced events are updated or deleted to match the source.

### Sync options (per job)
- Availability can copy or force busy/free/tentative.
- Privacy, event color, organizer, and attendees are optional per job.
- Reminders can copy, disable, or use custom defaults, with optional re-syncing.

## Permissions
Calsynx needs `READ_CALENDAR` and `WRITE_CALENDAR` to list calendars and sync events.
It may also request a battery optimization exemption to improve background sync reliability; the app still works without it.

## Build (CLI)
Prerequisites:
- JDK 17+
- Android SDK with platform tools installed
- `ANDROID_HOME` or `ANDROID_SDK_ROOT` configured

Debug build:
```bash
./gradlew --no-daemon :app:assembleDebug
```

Release build (unsigned):
```bash
./gradlew --no-daemon :app:assembleRelease
```

## Tests (CLI)
Unit tests:
```bash
./gradlew --no-daemon test
```

Instrumented tests (requires a connected device or emulator):
```bash
./gradlew --no-daemon connectedAndroidTest
```
