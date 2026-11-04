# Shima — Dynamic Island (rebuild)

A clean-room rebuild of **Shima: Dynamic Island** (`com.piyush.island`), written from scratch in
Kotlin as a normal Android Studio / Gradle project. It draws a Dynamic-Island-style pill in the
cutout of your screen and puts the things that are happening on your phone into it: music, calls,
timers, charging, the torch, the microphone, the camera and more.

Nothing about the purchase is checked outside Google Play, there is no account, no ads, no
analytics and no server: everything the island shows is read on the phone and used there.

---

## Opening it in Android Studio

This is a stock Gradle project — nothing is generated, there is no unusual step.

1. **File → Open…** and pick the `shima-android` folder (the one that contains `settings.gradle.kts`).
   Do *not* use *Import Project*; *Open* is enough.
2. Android Studio will offer to use the bundled **Gradle Wrapper** (Gradle 8.13). Accept it.
   If the IDE asks for a **Gradle JDK**, use **JDK 17 or newer** (the build is tested on JDK 21).
3. If the SDK location is missing, set it in **Settings → Languages & Frameworks → Android SDK**,
   or create a `local.properties` file in the project root:

   ```properties
   sdk.dir=/path/to/Android/sdk
   ```

4. Wait for **Gradle Sync** to finish, then press **Run ▶** on the `app` configuration.

The minimum SDK is **28 (Android 9)**, the target/compile SDK is **35 (Android 15)**.

### Building from the command line

```bash
./gradlew :app:assembleDebug      # debug APK  -> app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:assembleRelease    # release APK (needs the signing keys, see below)
./gradlew :app:testDebugUnitTest  # unit tests
```

### Signing a release build

`app/build.gradle.kts` reads the signing material from the environment, so no key ever ends up
in the repository:

| Variable                | Meaning                       |
|-------------------------|-------------------------------|
| `SHIMA_KEYSTORE_PATH`   | path to the `.jks` file       |
| `SHIMA_STORE_PASSWORD`  | store password                |
| `SHIMA_KEY_ALIAS`       | key alias                     |
| `SHIMA_KEY_PASSWORD`    | key password                  |

```bash
export SHIMA_KEYSTORE_PATH=/path/to/shima.jks
export SHIMA_STORE_PASSWORD=…
export SHIMA_KEY_ALIAS=shima
export SHIMA_KEY_PASSWORD=…
./gradlew :app:assembleRelease
```

Without those variables the release build is unsigned but still produces an installable artifact
once you sign it with `apksigner`.

---

## What the app does

| Area | What you get |
|------|--------------|
| **Island** | A pill that sits in the camera cutout, expands into a card, follows the cutout on every edge, orientation and foldable screen, seven palettes, Material You, custom colours, transparency, outline, shadow, three animations and a companion pill next to it. |
| **Alerts** | Media (artwork, track, artist, lyrics, transport buttons), live updates from on-going notifications (timers, rides, deliveries), charging, torch, volume/brightness, microphone and camera, low battery, battery full, battery saver, no signal, USB, Do Not Disturb, alarms, rotation lock, screen recording, screenshots and uninstalled apps. Every alert has its own duration, icon, colour, animation, close behaviour and app filter. |
| **Gestures** | Tap, long press and the four swipes on both the collapsed pill and the open card. Each one can launch an app, open a widget, run a system action (back, home, recents, shade, quick settings, lock, screenshot, torch, rotation lock, DND, media transport) or run a Tasker task. |
| **Rules** | One gesture can do different things depending on the state of the phone (charging, locked, music playing, screen on/off, and so on). |
| **App row** | Up to ten apps that live under the island and can be reached with a gesture. |
| **Cards** | Every card can be assembled part by part: title, subtitle, progress, artwork, lyrics and buttons. |
| **Backup** | The whole setup as a readable JSON file that can be edited by hand and restored on another phone, plus an automatic daily backup. |
| **Pro** | A one-off purchase through Google Play that unlocks the extra rules, swipe gestures, the companion pill and the backup tools. |
| **Reliability** | A keep-alive foreground service, a watchdog job, a boot receiver and a page that tells you exactly what is missing when the island refuses to come back. |

### Permissions and what they are for

| Permission | Why |
|------------|-----|
| `ACCESS_NOTIFICATION_POLICY` | Read and switch Do Not Disturb. |
| `WRITE_SETTINGS` | Read the rotation lock and change it when you ask for it. |
| `POST_NOTIFICATIONS` | The keep-alive notification. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the island alive while you use the phone. |
| `RECEIVE_BOOT_COMPLETED` | Bring the island back after a restart. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Ask to be exempt from the battery saver, which otherwise kills the overlay. |
| `BILLING`, `CHECK_LICENSE`, `INTERNET`, `ACCESS_NETWORK_STATE` | The Play purchase and the in-app update. |
| `net.dinglisch.android.tasker.PERMISSION_RUN_TASKS` | Only used when you pick a Tasker task. |

The accessibility service is the only way the island can be drawn over other apps; the service
description in the app explains this. The notification listener reads the media session and
on-going notifications so the island can show them.

---

## Layout of the project

```
shima-android/
├── settings.gradle.kts          # the module list and the repositories
├── build.gradle.kts             # the plugins, applied in the app module
├── gradle/libs.versions.toml    # every dependency in one place
├── gradle/wrapper/              # Gradle 8.13 wrapper
└── app/
    ├── build.gradle.kts          # the app module: sdk levels, build types, signing
    └── src/main/
        ├── AndroidManifest.xml
        ├── assets/               # changelog.json, licenses.json, privacy.md
        ├── java/com/piyush/island/
        │   ├── MainActivity.kt   # the host activity and the navigation
        │   ├── ShimaApp.kt       # the application class
        │   ├── core/             # the service locator (AppGraph)
        │   ├── model/            # the data classes: alerts, rules, style, palette
        │   ├── data/             # settings store, backup, changelog, auto backup job
        │   ├── island/           # the island view, the overlay window, the icons
        │   ├── alerts/           # one controller per kind of alert
        │   ├── overlay/          # the accessibility service
        │   ├── content/          # the notification listener
        │   ├── gestures/         # the action executor
        │   ├── service/          # keep alive, watchdog, quick settings tile, boot receiver
        │   └── ui/               # the screens (island, alerts, gestures, rules, style, more)
        └── res/                  # layouts, drawables, the Google Sans Flex font, strings
```

The whole app is built around one immutable `ShimaSettings` snapshot that lives in
`data/SettingsStore.kt`. Every screen reads that snapshot and every change writes a new one, so
there is a single place where the app's state is defined and one place where it is stored.
