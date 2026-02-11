# Calscium

Calscium is an Android app that copies events from one local calendar into another. It focuses on
simple, one-way synchronization (source → target) without any cloud services, accounts, or servers.

## What it does
- Creates sync jobs that copy events from a source calendar into a target calendar.
- Tracks the last sync time, counts, and any errors per job.
- Provides calendar management (create local calendars, rename, recolor, purge, delete).

## Permissions
Calscium needs `READ_CALENDAR` and `WRITE_CALENDAR` to list calendars and sync events.

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
