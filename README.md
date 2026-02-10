# Calscium - Local Calendar Sync

Calscium is an Android application that keeps one local on-device calendar in sync with another.
The scope is intentional: one-way, source -> target, with no cloud service or accounts.

## High-Level Implementation

### 1. Data Model & Storage
*   **SyncJob**: Represents a synchronization task.
    *   `id`: Unique identifier.
    *   `sourceCalendarId`: ID from `CalendarContract.Calendars`.
    *   `targetCalendarId`: ID from `CalendarContract.Calendars`.
    *   `lastSyncTimestamp`: Last successful run, used for incremental sync.
    *   `isActive`: Enable/disable a job.
*   **Room Database**: Persist `SyncJob` configurations locally.

### 2. Calendar Integration (CalendarProvider)
*   **Calendar Discovery**: Query `CalendarContract.Calendars` to show sources/targets.
*   **Event Syncing (One-Way: Source -> Target)**:
    *   Query `CalendarContract.Events` (or `CalendarContract.Instances` for a time window)
        scoped to `sourceCalendarId` and a sync window.
    *   **Identifier Mapping**: Use a custom metadata field (e.g. `SYNC_DATA1`) to store
        the source event ID on target events. This allows reliable updates/deletes.
    *   **Updates**: Upsert target events by matching the stored source ID.
    *   **Deletions**: If a source event is deleted, delete the mapped target event.
    *   **Properties**: Copy core fields (title, start/end, all-day, time zone, rrule,
        location, description) while avoiding attendee/organizer changes.
    *   **Sync Window**: Forward-looking by default; optionally include a short past window
        to catch recent edits.
    *   **Manual Sync**: Triggered per job from the UI while background scheduling is pending.

### 3. Background Processing (WorkManager)
*   **SyncWorker**: Periodic worker that runs active `SyncJob`s.
*   **Schedule**: A few times a day; can add manual "Sync now" in UI.

### 4. User Interface (Jetpack Compose)
*   **Main Screen**: List `SyncJob`s with status and last sync time.
*   **Edit/Create Job Screen**:
    *   Source calendar selector.
    *   Target calendar selector.
    *   Toggle active and delete option.
    *   Manual "Sync now" trigger per job.

### 5. Permissions
*   Request `READ_CALENDAR` and `WRITE_CALENDAR` at runtime before syncing.

## Roadmap
- [x] Setup permissions in Manifest.
- [x] Setup Room database for Sync Jobs.
- [x] Implement Calendar Provider helper to list calendars.
- [x] Build UI for listing and adding Sync Jobs.
- [x] Implement the core Sync Logic (manual sync).
- [ ] Integrate WorkManager for background execution.
