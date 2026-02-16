# Planning & Roadmap

## Completed Features
- Manual and background sync (WorkManager) between calendars
- Sync job management with status, logs, and statistics
- Calendar management (create, rename, recolor, purge, delete, export/import)
- Per-job copy options (availability, privacy, color, organizer, attendees, reminders)
- Initial pairing for existing target events with optional cleanup
- Recurring event exception handling with mapping repair
- Battery optimization warnings and exemption logging

## Decisions Log
- Default missing event time zones to system default for sync writes.
- Preserve or generate event UID_2445 on sync to stabilize cloud de-duplication.

## Future Enhancements
- [ ] Configurable periodic sync cadence per job
- [ ] Sync job configuration backup/restore
- [ ] More granular sync schedules (specific times, days)
- [ ] Event filtering rules (by title, description, location, etc.)
