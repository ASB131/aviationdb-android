# AviationDB for Android

Native Kotlin app for [AviationDB](https://aviationdb.uk): live overhead tracking, personal sightings, and aircraft/airline lookup via the public API.

## Requirements

- Android 8.0+ (API 26)
- AviationDB account (**email + password**, same as the website)
- Active **tracking** subscription for Live and Sightings

## Build the APK

**Use GitHub Actions** (recommended). Pushing to `main` runs **Android CI** and uploads a debug APK artifact. Do not run Gradle on the production VPS — Android builds need several GB of RAM and can take the server down.

To build on a dev PC with Android Studio / SDK installed:

```bash
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

## CI

GitHub Actions runs on push to `main` and uploads a debug APK artifact from the workflow **Android CI**.

## Project layout

- `app/src/main/java/uk/aviationdb/android/` — application code (Compose UI, Retrofit client)
- API base URL: `https://aviationdb.uk/api/v1/` (see `app/build.gradle.kts` `BuildConfig`)

## Repo

Develop on your machine or VPS, push to GitHub; builds run on GitHub-hosted runners (no Android SDK required on the server).