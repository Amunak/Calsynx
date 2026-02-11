# Planning

## Plan & Progress
- [x] Manual one-way sync between calendars.
- [x] Sync job UI with status, last run info, and manual sync.
- [x] Calendar management (list, create, rename, recolor, purge, delete).
- [x] Distinct UI states for paused and missing-calendar jobs.
- [ ] Background scheduling with WorkManager.
- [ ] Optional periodic sync cadence configuration for active jobs.
- [ ] Export or backup of sync job configurations.

## Decisions
- Keep calendar management as a separate utility screen (not embedded in sync job dialogs).
- Use an expanded Material palette for calendar colors with compact rows for scanning.
- Keep sync one-way (source → target) and local-only (no server).

## Rejected
- Move “Sync now” into an overflow-only action.
