# Entry polish

User approved keyboard Done or hardware Enter to save and auto-sync. No per-keystroke saves and no pause-triggered saving. Remove the redundant Save button and hide the Sync action while connected. Keep an error/permission recovery action and manual retry in Settings. A receipt toast names the value/unit only after the exact saved revision is acknowledged, with local pending status on failures. A new save during an active sync requests another pass. Preserve SQLite IDs, history, settings and Health Connect permissions through an in-place upgrade (version 0.2.0).

Visual direction: white background, ink headings and teal accents; large number on a pale cyan entry area; optional time/meal/notes collapsed; clear two-tab navigation with selected state and Settings in header. Keep accessibility labels, scrolling and system/keyboard insets. Notes are still local. No changes to clinical values, conversions or Cronometer imports.

Tests: accepted revision notice, no false/duplicate/stale-edit notices; isolated instrumented form validates partial typing does not save, Done submits once, in-progress guard and invalid value rejection. JVM tests/build/lint run locally; physical checks require connected Pixel.
