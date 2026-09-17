# Verification — September 16, 2026

- Debug APK built with JDK17, SDK36, Gradle8.13.
- 10 JVM tests passed, zero failures. Validation, unit conversion, timestamps, manual metadata, retry identity, stale acknowledgements, deletion routing and stale-edit guards covered.
- Android lint: zero errors, six warnings (target SDK35, two programmatic custom-view constructors, three optional KTX suggestions).
- APK signature verified with apksigner (v2).
- Independent code review found stale edit/deletion and draft-restoration issues; corrected with expected revision guards, deletion draft cleanup and persisted draft revision.
- Pixel installation succeeded for an intermediate build. Isolated SQLite device test demonstrated the original stale-edit bug after confirming save/reopen, edit revision, stale-ACK and tombstone behavior. Wireless ADB then stalled after installation reported success; the corrected full device test has not completed. Do not label final APK physically verified.
- User reports the app works; Cronometer still plots both unit magnitudes at the same timestamp. No stored values changed to compensate. User is checking import settings.
- No synthetic test readings written to Health Connect. Device test uses a separate temporary database. The new package does not modify the old Garmin bridge.

Still to check with final APK: native screen layout and interaction, permission grant, one intentional reading export, edit/delete propagation and background retry. Health Connect uses canonical mmol/L; Cronometer graph/import behavior is external to this app.
