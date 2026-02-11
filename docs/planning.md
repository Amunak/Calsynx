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
- [ ] Reduce repeated calendar/event queries when building calendar management stats.
- [x] Centralize job label formatting to avoid duplicate logic in ViewModel/Worker/Log export.
- [x] Add targeted tests for update field behavior.
- [x] Add targeted tests for sync plan behavior.
- [x] Add tests to cover recurring event window inclusion.

## Decisions
- Keep calendar management as a separate utility screen (not embedded in sync job dialogs).
- Use an expanded Material palette for calendar colors with compact rows for scanning.
- Keep sync one-way (source → target) and local-only (no server).
- Schedule background sync with WorkManager periodic work using flex windows to let Android optimize.
- Align UI copy with background scheduling now that WorkManager is in place.
- Allow hidden calendars in selections; mark them with a visibility-off icon in selection/management lists.

## Rejected
- Move “Sync now” into an overflow-only action.
- Introduce a full DI framework for now; stick to lightweight constructors/providers.
