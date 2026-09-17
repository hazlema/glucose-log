# Glucose Log

A simple Android blood-glucose journal that syncs with Health Connect. Built for quick manual logging, with no account, ads, analytics, or server.

## Features

- Log in mg/dL or mmol/L; new entries start at 100 mg/dL.
- Save with the keyboard's Done/Enter key, or dismiss the keyboard after making changes.
- Optional measurement time, meal label, and local-only notes.
- History, a seven-day graph, and editing/deletion.
- Automatic Health Connect sync, durable retries, and a toast confirming successful export.

## Getting started

1. Install the APK and open **Glucose Log**.
2. Tap **Connect Health Connect** and allow blood glucose write access.
3. Enter a reading, optionally expand **Time, meal & notes**, then finish entry to save.

**New reading** always opens a fresh form. **History** shows saved entries; editing has a **Save changes** button. Untouched defaults are not saved when the keyboard closes, and invalid entries remain in the form for correction.

For Cronometer, enable its Health Connect blood-glucose import. Cronometer controls import timing and may require a backfill. Health Connect can display mmol/L even when you enter mg/dL; the values are converted correctly.

## Build and install

Requires JDK 17 and Android SDK platform 36. Set `JAVA_HOME` and `ANDROID_HOME` (or set `sdk.dir` in `local.properties`). The included wrapper uses Gradle 8.13.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Android 9+; Health Connect availability depends on Android version and provider support. Package: `dev.glucoselog.phone`. Tested on a Pixel 7. APKs and signing keys are not committed; preserve your signing key to install future updates over the same app.

## Your data

Readings and notes live in a private database on your phone. Only glucose, measurement time, and supported meal labels are exported to Health Connect. Notes stay local. The app has no Internet permission and does not read other apps' health records.

Sync runs after saving, when the app opens, and periodically when Android permits. Failed changes stay queued. Deletions also sync, though other apps may retain imported copies.

Local backup is disabled. Uninstalling or clearing app storage removes local history, notes, and pending changes; previously exported Health Connect records may remain.

This is a personal logging app, not a glucose sensor or treatment advisor. It does not estimate A1c.

## Testing

The JVM suite covers validation, unit conversion, timestamps, retry behavior, edit versions, keyboard dismissal, and sync confirmations. On-device checks use a separate temporary database and do not write synthetic readings to Health Connect. See [VALIDATION.md](VALIDATION.md) for verification details.
