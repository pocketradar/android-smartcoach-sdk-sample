# SmartCoach Android SDK Sample App

This repository contains a sample app demonstrating how to integrate and use the [Pocket Radar SmartCoach SDK](https://github.com/pocketradar/smartcoach-android-sdk) in an Android application.

> **This SDK is currently in beta.** Please do not publish apps using the beta SDK to Google Play.

This sample uses SDK version **0.2.0-beta.1**.

## Requirements

- Android Studio with Android Gradle Plugin 9.0 support, and JDK 17 or later
- An Android 10 (API 29) or later **physical device** for live scanning and measuring — the app
  builds and runs on the emulator, but Bluetooth is unavailable there
- A valid Pocket Radar API key

## What This Sample Shows

The app opens on a single screen (`HomeScreen` / `HomeViewModel`) demonstrating the SDK's
canonical integration pattern:

- `SmartCoach.configure()` called once in `Application.onCreate()` (`SampleApplication`)
- The Bluetooth runtime permission request before the first scan (`BluetoothPermissions`)
- One collector of `SmartCoach.sessionStateStream()` in the ViewModel — all UI state derives from the
  stream
- Scanning (including auto-connect to the last paired radar), connecting, live speed streaming,
  and disconnecting, with the wait-for-`connected` sequencing the SDK requires
- Radar settings: measurement unit, speed range, and sensitivity
- Error surfacing via the `disconnected(error)` state and thrown `SmartCoachError`s, with a hint for the
  errors you can fix yourself (API key, entitlements, permissions)

## Getting an API Key

API keys are issued only to approved Pocket Radar partners. If you don't have one, contact:

**[partners@pocketradar.com](mailto:partners@pocketradar.com)**

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/pocketradar/android-smartcoach-sdk-sample
cd android-smartcoach-sdk-sample
```

### 2. Open the project

Open the folder in Android Studio and let Gradle sync. The SDK resolves automatically from
the Pocket Radar Maven repository declared in `settings.gradle`.

### 3. Add your API key

The project ships without an API key — you must add your own before building. In
`app/src/main/AndroidManifest.xml` replace `YOUR_API_KEY_HERE` with your key:

```xml
<meta-data
    android:name="com.pocketradar.smartcoach.API_KEY"
    android:value="YOUR_API_KEY_HERE" />
```

> **Never commit your API key to source control.**

Your key is issued for one application ID. The sample's is `com.pocketradar.smartcoach.sample`;
unless your key was issued for that, change `applicationId` in `app/build.gradle` to the
application ID your key was issued for. Otherwise the first scan fails with
`invalidEntitlements`. The first scan also needs network access, to fetch your entitlements.

### 4. Build and run

- Select your device
- Press **Run** (`⌃R`), or run `./gradlew :app:installDebug`
- Grant the Bluetooth permission when asked, then put your radar in pairing mode and tap
  **Scan**

## Documentation

API reference for this SDK version:
**[pocketradar.github.io/smartcoach-android-sdk/docs/0.2.0-beta.1](https://pocketradar.github.io/smartcoach-android-sdk/docs/0.2.0-beta.1/)**

## SDK Repository

The SmartCoach SDK is available at:

**[https://github.com/pocketradar/smartcoach-android-sdk](https://github.com/pocketradar/smartcoach-android-sdk)**

For SDK installation instructions, API reference, and integration guides see the SDK repository. It also ships **agent skills** — recipes that let an AI coding assistant (Claude Code today) integrate SDK capabilities into your own app; see the SDK repository's "AI-Assisted Integration" section.

## Feedback & Reporting Issues

Please use [GitHub Issues](https://github.com/pocketradar/android-smartcoach-sdk-sample/issues) to report bugs or issues with the sample app. For SDK-specific issues please open an issue in the [SDK repository](https://github.com/pocketradar/smartcoach-android-sdk/issues).

For urgent support contact [partners@pocketradar.com](mailto:partners@pocketradar.com).
