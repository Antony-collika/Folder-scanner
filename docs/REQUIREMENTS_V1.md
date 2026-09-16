# Folder Tree Snapshot — Requirements v1.0

## Product goal
Offline, read-only Android application that snapshots a user-selected SAF folder tree and exports Markdown/JSON representations and incremental diffs.

## Decisions for v1
- Storage access uses `ACTION_OPEN_DOCUMENT_TREE` and persisted read permission only.
- Maximum roots: 20.
- First scan is a full scan. Later scans reuse unchanged snapshot subtrees when SAF document ID, type and modified timestamp are unchanged; the provider is still enumerated to detect additions/removals/moves.
- Latest snapshot plus previous snapshot are retained only as needed to calculate the next diff; no unlimited history.
- Diff identity is SAF document ID. Path changes are classified as rename/move; metadata changes are modified.
- Default Markdown model: D. Default order: folders first, then case-insensitive A–Z.
- JSON `version` is the string `"1.0"`; `scanned_at` and entry timestamps use ISO-8601 UTC offsets.
- Scan runs in a foreground service and exposes cancellation from its notification.
- Unreadable child folders are skipped and scanning continues. A root access failure aborts the scan.
- SAF write permission is not requested because the app is non-destructive.
- English localization is optional for v1; Vietnamese is the default UI language.

## Acceptance criteria
1. User can select up to 20 roots through SAF.
2. Persisted read permission is requested and validated; revoked roots are reported/reselectable.
3. New root scan shows an estimate and confirmation before starting.
4. Scan works off the main thread, reports progress and supports cancellation.
5. Scanner captures name, type, relative path, SAF document ID, modified time, size, MIME and extension where available.
6. Hidden/system-junk filters and max depth are applied from Settings.
7. Tree UI supports expand/collapse, search, configured sort, file detail and path copy.
8. Markdown A/B/C/D are selectable; metadata can be included.
9. JSON follows the v1 schema and uses null for folder size/file children.
10. Rescan creates a new snapshot and compares it with the previous snapshot.
11. Diff supports added, removed, modified, renamed and moved categories.
12. Diff is visible and exportable as Markdown and JSON.
13. Markdown can be copied and shared with Android `ACTION_SEND`.
14. Cache can be inspected/deleted without touching source files.
15. App requests no broad storage permission and performs no network access.
16. CI must build `assembleDebug` and upload `app-debug.apk` as an artifact.

## Explicit non-goals
Editing, deleting or moving source files; file contents/EXIF/hash scanning; cloud sync; multi-user; Play Store publishing; regex/extension filters; importing JSON; storage charts; cleanup suggestions.
