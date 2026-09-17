# Glucose Log

A small, personal Android glucose journal. Enter on your phone, save locally, and export to Health Connect for apps such as Cronometer. No account, Garmin connection, advertising, analytics, or server.

## Use

Open **Glucose Log** (distinct from **Glucose Log Bridge**). The entry starts at 100 mg/dL. Tap the unit button to convert the draft to mmol/L. Choose a measurement date/time or leave it at Now, optionally add a meal label and notes, then press the keyboard’s **Done** button or hardware **Enter**. This saves and syncs immediately. There is no separate Save button. Optional time, meal, and notes are under **+ Time, meal & notes**; set these before pressing Done.

On first use, tap **Connect Health Connect** and allow blood glucose write access. The connection button disappears once connected; a small status line remains. A toast names the reading only after Health Connect accepts its saved revision. New readings and edits sync after saving and on opening the app; pending changes also retry periodically when Android permits. Local logging works without permission. If sync is unavailable, the status says changes are waiting; Settings offers Retry pending sync. History marks each reading sent or waiting. A successful export does not guarantee immediate Cronometer import. In Cronometer, enable its Health Connect blood-glucose import and backfill if needed.

History contains a seven-day scatter graph and all local entries. Settings controls the history display unit and default unit for new forms; the current draft retains its own unit. Edit preserves the reading identity. Delete asks for confirmation and queues removal of this app's Health Connect record. Cronometer may retain an already imported copy.

Notes remain local. Health Connect receives glucose, timestamp/offset, manual-entry metadata and supported meal relation. Android internally represents glucose in mmol/L even when entered in mg/dL. 100 mg/dL = 5.5556 mmol/L; this does not alter the measurement. No A1c estimate or treatment recommendation.

## Privacy and data ownership

The private SQLite database stores the journal and pending sync changes. The app requests only WRITE_BLOOD_GLUCOSE and RECEIVE_BOOT_COMPLETED (to preserve periodic retries). It has no Internet permission and does not read other apps' records. App backup and device transfer are excluded. Uninstalling or clearing storage loses local readings/notes and pending changes; exported Health Connect records may remain. Keep the app installed while changes are waiting. The older Garmin bridge and its Health Connect records are untouched.

## Build

Requirements: JDK 17, Android SDK platform 36, build tools, and Gradle 8.13 (wrapper included). Set JAVA_HOME and ANDROID_HOME for your installation, then:

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. A delivery copy is `dist/GlucoseLog.apk`. Package `dev.glucoselog.phone`, Android 9+ (Health Connect availability depends on Android/provider). Target SDK 35. Signed with the local Android debug key for personal installation, not a Play Store release.

Install using `adb install --no-streaming -r dist/GlucoseLog.apk` on a paired device, or copy the APK to the phone and open it with Android's package installer. Preserve the signing key for future updates.

## Verification

`testDebugUnitTest` covers value validation, mg/dL and mmol/L, time bounds, retry identity, stale acknowledgements, deletion routing, and actual Health Connect record metadata/conversions.

The custom `DeviceChecks` instrumentation uses an isolated temporary database and never writes synthetic data to Health Connect. It checks save/reopen durability, revision handling, deletion, stale edit rejection, and main-screen startup. Build `assembleDebugAndroidTest`, install the test APK, then:

```sh
adb shell am instrument -w dev.glucoselog.phone.test/dev.glucoselog.phone.DeviceChecks
```

Expect `result=PASS: ...` and `INSTRUMENTATION_CODE: -1`; Android's command exit status alone does not establish success. Run with the phone unlocked. This is a development test that opens and closes the app; run before entering a draft. Remove the test-only package afterwards with `adb uninstall dev.glucoselog.phone.test`.

Physical acceptance: allow Health Connect access, enter a real reading, verify its value/time in Health Connect and Cronometer. Test editing and deleting a disposable test reading only if intentionally desired. Background retries are opportunistic, not an exact 15-minute promise. This app cannot control Cronometer's unit rendering or import timing.
