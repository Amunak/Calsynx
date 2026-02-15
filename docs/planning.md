# Planning

## Plan & Progress
- [x] Manual one-way sync between calendars.
- [x] Sync job UI with status, last run info, and manual sync.
- [x] Calendar management (list, create, rename, recolor, purge, delete).
- [x] Distinct UI states for paused and missing-calendar jobs.
- [x] Background scheduling with WorkManager.
- [ ] Optional periodic sync cadence configuration for active jobs.
- [ ] Export or backup of sync job configurations.

## Refactor & Cleanup Plan (Proposed)
- [x] Fix sync updates to clear stale `DTEND`/`DURATION` when the source field switches.
- [x] Update UI copy that still claims "manual sync only" now that WorkManager scheduling exists.
- [x] Decide how to treat hidden calendars in job creation/listing (show all vs. visible-only).
- [x] Move sync job create/edit into a dedicated screen with sections.
- [x] Reduce repeated calendar/event queries when building calendar management stats.
- [x] Centralize job label formatting to avoid duplicate logic in ViewModel/Worker/Log export.
- [x] Add targeted tests for update field behavior.
- [x] Add targeted tests for sync plan behavior.
- [x] Add tests to cover recurring event window inclusion.
- [x] Add per-job copy options (availability, privacy, color, organizer, attendees, reminders).
- [x] Add reminder re-sync toggle and battery optimization warning/log export.
- [x] Consolidate sync job validation between editor UI and ViewModel.
- [x] Remove or re-route unused SyncJobViewModel create/update helpers.
- [x] Decide how toggling copy options should handle previously copied fields (clear vs. preserve).
- [x] Use string resources for background sync errors to keep copy consistent.
- [x] Centralize event count queries (avoid duplicate logic in repository/syncer).
- [x] Update README to be user-first and move technical details later.

## Decisions
- Keep calendar management as a separate utility screen (not embedded in sync job dialogs).
- Use an expanded Material palette for calendar colors with compact rows for scanning.
- Keep sync one-way (source → target) and local-only (no server).
- Schedule background sync with WorkManager periodic work using flex windows to let Android optimize.
- Align UI copy with background scheduling now that WorkManager is in place.
- Allow hidden calendars in selections; mark them with a visibility-off icon in selection/management lists.
- Move sync job create/edit to a dedicated activity with sectioned layout.
- Expose per-job copy controls with safe defaults; attendees remain opt-in with warnings.
- Hide advanced copy/reminder settings behind an "Advanced" toggle in the editor.
- Do not write `OWNER_ACCOUNT` to target events; only copy organizer when enabled.
- Add per-type reminder toggles and all-day time-of-day selection under advanced reminders.
- All-day reminder timing uses “days before + time of day on the prior day” (e.g., 0 days at 8 PM = 4 hours before).
- Expose a reminder re-sync toggle so users can keep custom target reminders.
- Warn about battery optimizations in the job list and log exemption status in exports.
- Match calendar management scroll indicator padding to the job list layout and surface additional input sources in list rows.
- Align list content, scroll indicators, and FABs with navigation bar insets; keep pause button visible on slightly narrower cards.
- Extracted shared nav bar padding and screen surface helpers to keep layout code consistent.
- Add an optional initial sync pairing that matches existing target events by title/date (all-day aware).
- Exclude already-mapped target events from initial pairing and add an optional cleanup to delete unmapped targets in the sync window.
- Log per-sync stats (created/updated/deleted/targets) for manual and background runs.
- Order sync jobs by creation time and provide a manual re-pair action from the job overflow menu.
- Require confirmation before clearing logs.
- Document battery optimization exemption in README permissions.
- Consolidate sync job validation in a shared helper used by the editor UI and ViewModel.
- Disabling copy options clears organizer/privacy/color values and removes attendees on sync updates.
- Background sync errors reuse existing string resources for consistency.
- Calendar management stats use batched event counts and precomputed job mappings.
- Remap recurring exceptions to the target master event and attempt to repair missing mappings before creating new targets.
- Add calendar export/import with raw event fields for debugging.

## Rejected
- Move “Sync now” into an overflow-only action.
- Introduce a full DI framework for now; stick to lightweight constructors/providers.
- Preserve previously copied organizer/privacy/color/attendees when copy options are disabled.
- Skip recurring exceptions entirely to avoid duplicate rendering.

## Open Questions
- None.
