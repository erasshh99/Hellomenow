# 06 · Static analysis

## 6.1 Obfuscation and packing

| Check | Result |
|-------|--------|
| R8 / ProGuard | ✅ the app code is renamed (`AbstractC0781`, `C0780`, `C1880a`, `C1895a` …) |
| Debug info | stripped (`--no-debug-info` has no effect) |
| String encryption | none observed — the string table is plain in `resources.arsc` |
| Native code | **none**: the APK has no `lib/` directory at all |
| Dynamic dex loading | **none**: no `DexClassLoader`, no `PathClassLoader`, no `Class.forName` in the app code |
| Anti-debug / anti-tamper | **none** in the app code |
| Root detection | none found |
| Emulator detection | none found |
| Play App Signing "pair" | the `com.pairip.licensecheck` classes are injected by Google, not by the developer |

The obfuscation is the normal R8 output of a release build. The app has no packer and no
self-protecting code: every behaviour can be read from the dex.

## 6.2 The dependency inventory

The app ships its own licence list, `assets/licenses.json`, with **103 artefacts**:

| Group | Notable artefacts |
|-------|-------------------|
| `androidx.*` | activity 1.8.1, appcompat 1.8.0, constraintlayout 2.2.2, core-ktx 1.19.0, fragment 1.9.0, lifecycle 2.11.0, navigation-fragment 2.9.8, recyclerview 1.4.0, viewbinding 9.1.1 |
| `com.google.android.material` | material 1.14.0 |
| `com.android.billingclient` | billing 8.3.0 |
| `com.google.android.play` | app-update 2.1.0, core-common 2.0.4, review 2.0.2 |
| `com.google.android.gms` | play-services-base 18.5.0, **play-services-location 19.0.0**, play-services-places-placereport 17.0.0, play-services-tasks 18.2.0 |
| `com.google.android.datatransport` | transport-runtime 3.1.8 |
| `com.google.android.flexbox` | flexbox 3.0.0 |
| `com.google.firebase` | firebase-encoders 17.0.0 (transitive) |
| `org.jetbrains.kotlin` | kotlin-stdlib 2.3.21 |
| `org.jetbrains.kotlinx` | kotlinx-coroutines 1.9.0, kotlinx-serialization 1.7.3 |

**No Jetpack Compose.** The whole UI is Android Views with XML layouts and custom `Canvas`
drawing, which is confirmed by the absence of any Compose artefact and by the 189 XML layouts.

`play-services-location` is the reason the original declares the location permissions; the app
code never calls a location API.

## 6.3 Network surface

| Endpoint | Where | Why |
|----------|-------|-----|
| `https://play.google.com/store/apps/details?id=` | the code | the "rate the app" button |

Everything else that can talk to the network comes from the libraries:

| Library | Traffic |
|---------|---------|
| Play Billing | talks to Google Play over the Play Services IPC — the app itself never makes an HTTPS request |
| Play in-app update | same |
| Play Services / datatransport | the telemetry that the Play libraries ship with; the app code does not use it |

**There is no backend.** The app has no API of its own, no account, no analytics and no ads. The
privacy policy in `assets/privacy.md` says the same thing in plain words.

## 6.4 Storage and persistence

| Store | What |
|-------|------|
| `SharedPreferences` (one file per settings section) | the whole configuration |
| `SharedPreferences` `accessibility_notice` | whether the user has seen the accessibility-service notice |
| `files/` | the automatic backup JSON |
| MediaStore | only when the user asks to delete a screenshot |

There is no database, no Room, no SQLite, no cache directory and no external storage.

## 6.5 The bundled assets

| Asset | Content |
|-------|---------|
| `changelog.json` | the release notes for every version, read by the About screen |
| `licenses.json` | the 103 dependencies with their licence and their source |
| `privacy.md` | the privacy policy, shown in the app |
| `dexopt/` | the ART profile / dex metadata Play generates for the release |

## 6.6 The fonts

`res/font/` holds **Google Sans Flex** in four weights (300, 500, 600, 750) plus the family
XML. The font is the one Google uses for the system UI, and it is shipped with the app so the
island looks the same on every device.

The rebuild reuses these four font files and the family XML verbatim — the font is
[SIL Open Font License 1.1](https://openfontlicense.org) licensed, which allows redistribution
as long as the licence travels with it, which it does in `assets/licenses.json`.

## 6.7 The icons

The launcher icon is an **adaptive icon**: a gradient background (`grad_launcher_a…c`), a
foreground (`grad_fore_a…e`) and a monochrome layer for the themed icons. The Quick Settings tile
uses `ic_tile_island`.

The rebuild reuses the launcher artwork and renames the files so that the names describe them
instead of the obfuscated originals.

## 6.8 Reflections and hidden APIs

The app reaches for three non-public platform members, each of them wrapped in a `try`:

| Call | Why |
|------|-----|
| `AudioManager.isMicrophoneActive()` | there is no public way to ask whether the microphone is in use |
| `MediaProjectionManager.isProjectionActive()` | there is no public way to ask whether a screen recording is running |
| `PowerManager.goToSleep(boolean)` | the public `GLOBAL_ACTION_LOCK_SCREEN` is not allowed on every device |

None of these are a security problem: they read a state, they do not change one, and when the
system refuses the call the app falls back to "no".

## 6.9 The rebuild's dependency list

The rebuild uses fewer libraries, because it does not need the navigation component, the
serialisation runtime, the review library or the Play Services:

| Group | Version in the rebuild |
|-------|------------------------|
| AGP | 8.9.1 |
| Kotlin | 2.1.10 |
| Gradle | 8.13 |
| compileSdk / targetSdk / minSdk | 35 / 35 / 28 |
| `androidx.core:core-ktx` | 1.15.0 |
| `androidx.appcompat:appcompat` | 1.7.0 |
| `com.google.android.material:material` | 1.12.0 |
| `androidx.constraintlayout` | 2.2.0 |
| `androidx.recyclerview` | 1.4.0 |
| `androidx.fragment:fragment-ktx` | 1.8.5 |
| `androidx.lifecycle` | 2.8.7 |
| `androidx.activity:activity-ktx` | 1.9.3 |
| `kotlinx-coroutines-android` | 1.9.0 |
| `com.google.android.flexbox:flexbox` | 3.0.0 |
| `com.android.billingclient:billing-ktx` | 7.1.1 |
| `com.google.android.play:app-update-ktx` | 2.1.0 |

The version numbers are pinned lower than the original's on purpose: the rebuild only uses
APIs that have been stable for a long time, and a lower Material version keeps the look and feel
identical on Android 9 as well.
