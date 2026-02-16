# Development Guidelines

## Architecture & Code Style
- Compose-first UI with Material 3 design patterns
- Room database for sync job storage and event mappings
- WorkManager for background sync scheduling
- Keep UI components focused and separated; add previews for composables
- Centralize shared logic (validation, formatting, queries)

## Key Design Decisions
- **Sync model**: One-way (source → target), local-only, no cloud/servers
- **Calendar visibility**: Hidden calendars are selectable; show visibility-off icon in lists
- **Copy options**: Per-job controls for availability, privacy, color, organizer, attendees, reminders
- **Advanced settings**: Hide advanced copy/reminder options behind toggle by default
- **Recurring events**: Remap exceptions to target master events; repair missing mappings before creating new targets
- **Target event cleanup**: When copy options are disabled, clear organizer/privacy/color and remove attendees on sync updates
- **Background sync**: WorkManager periodic tasks with flex windows; battery optimization warnings shown as top-of-list card
- **Initial pairing**: Optional title/date matching for existing target events; exclude already-mapped targets
## Testing
- Run unit tests (`./gradlew test`) for logic verification
- Avoid instrumented tests unless high risk of breakage (they wipe app storage)
- Verify builds before commits when making structural changes
