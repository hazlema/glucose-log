# Phone Logger Implementation Plan

> For agentic workers: execute using superpowers:executing-plans, with tests and review before delivery.

**Goal:** An installable standalone manual glucose logger with durable Health Connect sync.
**Architecture:** Kotlin native UI, SQLite outbox, serialized Health Connect writes and JobScheduler retry.
**Tech Stack:** Kotlin 2.1.20, AGP 8.9.2, SDK36, Health Connect 1.1.0.
**Spec:** ../specs/2026-09-16-phone-logger-design.md

## Global constraints
Package dev.glucoselog.phone; minSDK28; target35; no Garmin, Internet, accounts or telemetry; preserve old bridge app/data.

## 1. Validated reading model and sync semantics
- Write CoreTest.kt: assert 100 mg/dL maps to 100; comma decimal 5,6 maps to 5.6; reject zero, nonfinite, out-of-range and future timestamps. Test failed write stays pending, retry uses identity/version, tombstones delete, stale acknowledgement cannot discard a later edit.
- Run testDebugUnitTest and observe missing implementation failures.
- Implement Entry(id,value,unit,time,offset,context,note,revision,deleted,synced), GlucoseUnit.parse, Entry.validate; SyncStore.pending/acknowledge, HealthSink.write/delete, SyncEngine.run returns pending count.
- Run tests and inspect failures before UI work.

## 2. Durable storage and Health Connect boundary
- Implement EntryStore SQLite table, transaction save/edit/delete with increasing revisions, conditional exact-version acknowledgement and persisted tombstones.
- Implement HealthRecords.record using BloodGlucose factories and Metadata.manualEntry; verify units, original offset/time, revision, manual status and context in tests.
- Implement HealthSync with application-wide Mutex and HealthSink, permission check and write/delete own client IDs. Implement retry JobService and scheduling.
- Run tests; verify SQLite SQL version guards and transaction paths.

## 3. Native logger and history
- MainActivity controls permission launcher, Log/History navigation, form drafts restored across recreation, numeric keyboard, current/backdated time, meal picker, notes, save and edit.
- EntryForm owns validated draft and native date/time dialogs. ChartView plots last-seven-day points in selected display unit with no inferred lines.
- History provides edit and confirmed deletion, per-entry sync status. Show local save separately from provider success. Disable duplicate save actions during DB mutation.
- Add privacy activity, manifest, icon, backups excluded. Build assembleDebug and lintDebug. Fix errors, inspect warnings.

## 4. Delivery
- Review data loss, duplicate imports, revoked permission, sync/edit races and time/unit handling. Run final unit tests and lint; verify APK package and signature.
- Write README with build/install instructions and physical acceptance checklist; copy reviewed project and APK to requested folder, initialize git on codex/phone-logger.
- If paired phone available, install distinct package using adb. Never fabricate patient records to test the real Health Connect database. Report remaining physical checks honestly.
