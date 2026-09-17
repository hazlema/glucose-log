# Verification — Glucose Log 0.3.3

- Graph uses a minimum 60 mg/dL span (equivalent in mmol/L), expands for wider data, and keeps its lower bound nonnegative. Points remain unconnected; this is a display scale, not a clinical target range.
- 20 JVM tests pass, including narrow/single-point, wide-range, lower-bound, and unit-equivalence scaling cases. Android lint has zero errors.
- Installed in place on Pixel 7 running Android 17. Existing isolated on-device regression checks pass; no synthetic Health Connect readings written. Test helper removed.
- Entry and history screenshots captured on the phone and visually inspected. Entry hint now reads “Close the keyboard to save.”

# Verification — Glucose Log 0.3.2

Bottom navigation now clears the transient edit/draft and opens the chosen page. New reading always opens a fresh entry. Installed in place on Pixel. 16 JVM tests pass; lint zero errors. Phone regression verifies New reading removes edit controls and restores the default entry value. Prior phone checks pass. No synthetic Health Connect records written; test helper removed.

# Verification — Glucose Log 0.3.1

Edit-only Save changes button added above Cancel edit. Installed on Pixel. 16 JVM tests pass; lint zero errors. Isolated phone test confirms notes-only edits preserve ID, measurement time and expected revision, and repeated Save taps submit once. All previous device regressions pass. No synthetic Health Connect records written. Test helper removed.

# Verification — Glucose Log 0.3.0

- Installed in place on the Pixel; app readings and permissions retained.
- 16 JVM tests pass; Android lint has zero errors.
- On-device isolated tests pass for storage/version/deletion, Done, hardware Enter, invalid input and duplicate guards. Added form-level visible/hidden event tests for changed, untouched, invalid and restored drafts, and Done followed by keyboard dismissal. These drive the form's keyboard visibility callback; they do not inject fake readings into Health Connect or automate a real IME animation.
- MainActivity observes AndroidX WindowInsetsCompat IME visibility using Android's documented API. Startup, background, window-focus loss and already-submitting states do not auto-save. Existing glucose baseline survives draft restoration.
- Code review found no important issues in the bounded change.
- Last saved value/time remains visible independently of Health Connect status and survives Activity recreation. The accepted-revision Health Connect toast is unchanged.
- Test-only package removed after checks. No synthetic Health Connect data written.

# Verification — Glucose Log 0.2.0

- Built and installed in place on the paired Pixel. Existing app database and Health Connect permission preserved; no uninstall of the actual app.
- 13 JVM tests pass: parsing, units, record metadata, time validation, retry identity, stale revisions, deletion routing, exact-revision success notices, no duplicate/false notices, confirmation restoration state.
- Android lint: zero errors. Remaining warnings concern target SDK35, programmatic view constructors and optional KTX conveniences.
- Isolated on-device tests pass: durable save/reopen, versioned edits, stale acknowledgements, deletion, rejection of stale edits, native entry startup, no per-keystroke saves, Done, physical Enter, repeated-submission guard, invalid input.
- The physical Enter test first failed, then passed after explicit key handling and immediate submission locking. Both key-down and key-up are consumed; key repeats do not submit.
- The rendered entry screen was captured from the Pixel and visually inspected for fit and legibility. Optional details are collapsed and healthy sync shows only a status line.
- Test helper removed after checks. No synthetic records written to Health Connect; isolated database removed by the test. Existing real readings may sync normally when the app opens.
- Code review addressed pending confirmation loss on rotation by persisting the expected reading ID/value/unit/revision in saved state. Toasts appear only after the exact revision is acknowledged.

User flow: enter a reading, optionally set time/meal/notes before completion, then Done or Enter. Saves locally and automatically syncs. Failed sync leaves the reading queued and a visible recovery action. Final receipt names the reading after Health Connect acceptance. No modification to Cronometer data or glucose-unit conversions.
