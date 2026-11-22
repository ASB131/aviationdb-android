# AviationDB for Android

Native Kotlin app for [AviationDB](https://aviationdb.uk): live overhead tracking, personal sightings, and aircraft/airline lookup via the public API.

## Requirements

- Android 8.0+ (API 26)
- AviationDB account with an **API key** and active **tracking** subscription for Live / Sightings / Account test
- API keys are created on the website under **Account**

## Build locally

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