# Calsynx

Calsynx is an Android app that copies events from one calendar into another, on your device.
It runs a simple one-way sync (source → target) and does not use any servers or cloud accounts.

## What it does
- One-way sync jobs between calendars.
- Tracks last sync time, counts, and errors per job.
- Basic calendar management (create, rename, recolor, purge, delete).

## Quick start
1. Grant calendar permissions when prompted.
2. Create a sync job: pick a source and target calendar.
3. Set a sync window (how far back and ahead to copy events).
4. Run a manual sync or let background sync run on its schedule.

Tip: use a dedicated target calendar rather than your main one.

### Sync behavior
- Source events are copied into the target calendar; target-only events are ignored.
- Sync windows control how far back/ahead events are synced.
- Synced events are updated or deleted to match the source.

### Sync options (per job)
- Availability can copy or force busy/free/tentative.
- Privacy, event color, organizer, and attendees are optional per job.
- Reminders can copy, disable, or use custom defaults, with optional re-syncing.

## Permissions
Calsynx needs `READ_CALENDAR` and `WRITE_CALENDAR` to list calendars and sync events.
It may request a battery optimization exemption to improve background sync reliability; the app still works without it.

## For developers

### Build (CLI)
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

### Tests (CLI)
Unit tests:
```bash
./gradlew --no-daemon test
```

Instrumented tests (requires a connected device or emulator):
```bash
./gradlew --no-daemon connectedAndroidTest
```
