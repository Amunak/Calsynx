# Calsynx

Calsynx is an Android app that copies (synchronizes) events on-device from one calendar into another.
It does a simple one-way synchronization, (source → target). You need to provide the calendars:
it does not use any cloud services, accounts, or servers. These can be either local calendars
or calendars added by other apps / device accounts.

A typical use case would be syncing your work calendar (Outlook, Google Calendar, etc.) with your personal calendar.

Note that while we try to take care to not delete events in the target calendar,
you should still probably use a separate calendar for the sync outside of your main calendar.

## What it does
- Creates sync jobs that copy events from a source calendar into a target calendar.
- Tracks the last sync time, counts, and any errors per job.
- Provides convenient calendar management (create local calendars, rename, recolor, purge, delete).

### Sync behavior (one-way copy)
- Source events are copied into the target calendar; target-only events are ignored.
  - If you want to clean the target calendar, you can purge it in the calendar management screen with the "Purge" option.
- A calendar can be used either as a source or a target across all jobs to avoid circular sync; multiple jobs may target the same calendar.
- Sync windows are configurable (including an "all events" option) to control how far back and ahead events are synced.
- If a source event was already synced before, its target event is updated to match the source.
- If a saved target event no longer exists, the mapping is dropped and the source is re-synced.
- If a source event disappears, the previously synced target event is deleted.
- Only events inside the configured sync window are kept in sync; mappings outside the window are removed.

## Permissions
Calsynx needs `READ_CALENDAR` and `WRITE_CALENDAR` to list calendars and sync events.

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
