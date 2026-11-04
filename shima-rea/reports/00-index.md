# Shima — Dynamic Island · Analysis & Rebuild Report

Reverse engineering and clean-room rebuild of **Shima: Dynamic Island**
(`com.piyush.island`), done with **Apktool 3.0.3**, **JADX** and manual REA-style inspection,
followed by a from-scratch reimplementation in Kotlin as an Android Studio project.

| | |
|---|---|
| Package | `com.piyush.island` |
| Application ID | `com.piyush.island` |
| Version analysed | **0.2.0** (versionCode 5) — the only binary that could be obtained |
| Version rebuilt | **0.4.0** (versionCode 5) — the current Play Store version |
| minSdk / targetSdk | 28 / 36 (original) · 28 / 35 (rebuild) |
| Developer | Piyush (`oto.music121@gmail.com` in the About screen) |
| License | Proprietary, sold through Google Play (one-off Pro purchase) |
| Privacy | No account, no ads, no analytics, no server (`assets/privacy.md`) |

## The reports

| # | Report | What is in it |
|---|--------|---------------|
| 01 | [Application identification](01-application-identification.md) | Where the APK came from, hashes, certificate, packaging |
| 02 | [Manifest analysis](02-manifest-analysis.md) | Every permission, component, feature and query, and why |
| 03 | [Feature inventory](03-feature-inventory.md) | The complete list of what the app can do |
| 04 | [Dynamic Island analysis](04-dynamic-island-analysis.md) | How the overlay, the pill and the card are built and drawn |
| 05 | [System integration](05-system-integration.md) | Accessibility service, notification listener, tile, keep-alive, boot |
| 06 | [Static analysis](06-static-analysis.md) | Obfuscation, reflection, dynamic loading, native code, network use |
| 07 | [Storage and data](07-storage-and-data.md) | The settings store, the backup format, what leaves the phone |
| 08 | [Security review](08-security-review.md) | Exported components, permission hygiene, billing, privacy findings |
| 09 | [Recovered architecture](09-recovered-architecture.md) | Package map, data flow, the state machine, the diagrams |
| 10 | [Feature parity matrix](10-feature-parity-matrix.md) | Original feature → rebuild implementation, line by line |
| 11 | [Rebuild and verification](11-rebuild-verification.md) | How the project was built and how it was checked against the original |
| 12 | [Limitations and open questions](12-limitations.md) | What could not be recovered and why |

## The short version

Shima is a **system-overlay utility**. It cannot draw over other apps with the ordinary Android
APIs, so it uses an **accessibility service** with `TYPE_ACCESSIBILITY_OVERLAY` to put a
pill-shaped view in the camera cutout of the screen. The pill is collapsed by default and expands
into a card that shows whatever is currently interesting: the track that is playing, the timer that
is running, that the phone is charging, that the microphone is on, that the battery is nearly
empty, and so on.

Everything is local. There is no server, no account and no analytics; the only network traffic
the original can generate is the Google Play billing and in-app-update plumbing.

The rebuild in `shima-android/` is a **stock Gradle project**: open the folder in Android Studio,
sync, press Run. See its [README](../shima-android/README.md) for the details.
