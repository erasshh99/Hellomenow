# 11 · Rebuild and verification

## 11.1 What was built

| | |
|---|---|
| Location | `shima-android/` |
| Type | A stock Gradle / Android Studio project |
| Language | Kotlin |
| UI | Android Views (XML) + custom `Canvas` drawing |
| Source | 51 Kotlin files, 6 382 lines |
| Resources | 13 layouts, 19 drawables, 310 strings, 4 fonts |
| Assets | `changelog.json`, `licenses.json`, `privacy.md` |
| Application ID | `com.piyush.island` |
| versionName / versionCode | `0.4.0` / `5` |
| minSdk / targetSdk / compileSdk | 28 / 35 / 35 |
| AGP / Kotlin / Gradle | 8.9.1 / 2.1.10 / 8.13 |

The project opens with **File → Open** in Android Studio — no import step, no code generation, no
extra tooling. The README next to it has the details.

## 11.2 How it was built

```bash
cd shima-android
export JAVA_HOME=/usr/local/sdkman/candidates/java/21.0.13.0+2-ms
export ANDROID_HOME=/home/codespace/android-sdk
./gradlew :app:assembleDebug
```

Result: **BUILD SUCCESSFUL**, and the artefact is written to

```
app/build/outputs/apk/debug/app-debug.apk
```

The build was run from a clean tree (`./gradlew clean :app:assembleDebug`) to make sure that
nothing was carried over from an earlier attempt.

## 11.3 The verification against the original

The rebuilt APK was decoded again with the same tools that were used on the original, and the two
were compared.

### Components

| Original | Rebuild | ✓ |
|----------|---------|---|
| `com.piyush.island.MainActivity` | `com.piyush.island.MainActivity` | ✅ |
| `com.piyush.island.ui.screenshot.DeleteRequestActivity` | `com.piyush.island.ui.screenshot.DeleteRequestActivity` | ✅ |
| `com.piyush.island.overlay.IslandAccessibilityService` | `com.piyush.island.overlay.IslandAccessibilityService` | ✅ |
| `com.piyush.island.overlay.IslandTileService` | `com.piyush.island.service.IslandTileService` | ✅ |
| `com.piyush.island.content.IslandNotificationListener` | `com.piyush.island.content.IslandNotificationListener` | ✅ |
| `com.piyush.island.reliability.KeepAliveService` | `com.piyush.island.service.KeepAliveService` | ✅ |
| `com.piyush.island.reliability.WatchdogJob` | `com.piyush.island.service.WatchdogJob` | ✅ |
| `com.piyush.island.reliability.SystemEventsReceiver` | `com.piyush.island.service.SystemEventsReceiver` | ✅ |
| `com.piyush.island.backup.AutoBackupJob` | `com.piyush.island.data.AutoBackupJob` | ✅ |
| `com.pairip.licensecheck.LicenseActivity` | — | injected by Google, not app code |

Every component of the original is present in the rebuild, under the same name or the same
package, and the library components (Play billing, Play core, AndroidX startup, the profile
installer, the datatransport scheduler) come out of the same libraries.

### Permissions

The rebuild declares **every permission the original uses**, and drops the six that the original
declares but never uses. The full table is in
[02 · Manifest analysis](02-manifest-analysis.md).

### The decompiled class tree

| | Original | Rebuild |
|---|----------|---------|
| Classes in `com.piyush.island` | 104 | 105 |
| Layouts | 189 | 13 |
| Own code | Kotlin, R8-obfuscated | Kotlin, readable |

The rebuilt APK was decompiled with JADX again; the tree comes out as
`alerts/`, `content/`, `core/`, `data/`, `gestures/`, `island/`, `model/`, `overlay/`, `service/`
and `ui/`, with the same class names as the source.

## 11.4 What was checked by hand

| Check | How |
|-------|-----|
| Every alert type has an implementation | each of the seventeen `AlertType` values was traced to the controller that raises it |
| Every gesture action has an implementation | each of the thirteen `SystemActionKind` values was traced to `ActionExecutor` |
| Every component in the manifest exists in the code | `DeleteRequestActivity` was found missing during this check and was written |
| The accessibility overlay is a real overlay | `TYPE_ACCESSIBILITY_OVERLAY`, `FLAG_NOT_FOCUSABLE`, `FLAG_LAYOUT_NO_LIMITS` |
| The gestures work without the hidden SDK | the global actions are used, so nothing depends on `GestureDescription.Builder` |
| The Pro entitlement cannot travel | separate preference file, excluded from both backup rule files, absent from the JSON export |
| The build is reproducible | a clean build from an empty `build/` directory succeeds |

## 11.5 Known gaps in the verification

There is **no device or emulator in this environment**, so the app could not be installed and
watched running. Everything that could be checked statically was checked; the following still
needs a real phone:

* that the island lines up with the cutout on every device,
* that the accessibility service is granted without a restart on every Android version,
* that the keep-alive service survives the battery saver on every OEM,
* that the live cards read correctly on the notification formats of the common timer apps.

The code paths for all four are the same ones the original uses, and every call is wrapped so
that a device that behaves differently degrades to "the feature is off" instead of crashing.
