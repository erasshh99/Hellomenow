# 10 · Feature parity matrix

Every feature of the original and where it lives in the rebuild.

Legend: ✅ implemented · ➕ added by the rebuild · ⚠️ different on purpose · ❌ not implemented

## 10.1 The island

| Feature | Original | Rebuild | Where |
|---------|----------|---------|-------|
| Overlay over other apps | accessibility overlay | ✅ same | `overlay/IslandAccessibilityService.kt` |
| Collapsed pill / expanded card | ✅ | ✅ | `island/IslandState.kt` |
| Hand-drawn canvas rendering | ✅ | ✅ | `island/IslandView.kt`, `island/IslandIcons.kt` |
| Cutout measurement per edge and orientation | ✅ | ✅ | `island/CutoutInfo.kt` |
| Width, height, corner radius, shape | ✅ | ✅ | `model/IslandStyle.kt` |
| Horizontal / vertical offset, fit-to-cutout | ✅ | ✅ | `island/IslandWindow.kt` |
| Transparency, outline, shadow (both states) | ✅ | ✅ | `island/IslandView.kt` |
| Animations (spring, fade, slide) | ✅ | ✅ | `island/IslandView.kt` |
| Seven palettes, light and dark | ✅ | ✅ | `model/Palette.kt` |
| Material You | ✅ | ✅ | `model/Palette.kt` |
| Custom background and foreground | ✅ | ✅ | `model/IslandStyle.kt` |
| Companion pill | ✅ | ✅ | `island/IslandState.kt` |
| Hide in full-screen apps | ✅ | ✅ | `overlay/IslandAccessibilityService.kt` |
| Hide in chosen apps | ✅ | ✅ | `overlay/IslandAccessibilityService.kt` |
| Show on the lock screen | ✅ | ✅ | `model/IslandStyle.kt` |
| Live preview in the settings | ✅ | ✅ | `ui/island/IslandPreviewView.kt` |
| Google Sans Flex font | shipped in the APK | ✅ **reused from the APK** | `res/font/` |

## 10.2 The alerts

| Alert | Original | Rebuild | Where |
|-------|----------|---------|-------|
| Media (artwork, track, artist, lyrics, transport) | ✅ | ✅ | `alerts/MediaController.kt` |
| Live cards from on-going notifications | ✅ | ✅ | `content/IslandNotificationListener.kt` |
| Charging (AC / USB / wireless) | ✅ | ✅ | `alerts/ChargingController.kt` |
| Torch | ✅ | ✅ | `alerts/FlashlightController.kt` |
| Volume, ringer, brightness | ✅ | ✅ | `alerts/VolumeController.kt` |
| Microphone and camera | ✅ | ✅ | `alerts/PrivacyController.kt` |
| Low battery | ✅ | ✅ | `alerts/ChargingController.kt` |
| Battery full | ✅ | ✅ | `alerts/ChargingController.kt` |
| Battery saver | ✅ | ✅ | `alerts/SystemStateController.kt` |
| No signal | ✅ | ✅ | `alerts/SystemStateController.kt` |
| USB | ✅ | ✅ | `alerts/SystemStateController.kt` |
| Do Not Disturb | ✅ | ✅ | `alerts/SystemStateController.kt` |
| Alarm | ✅ | ✅ | `alerts/SystemStateController.kt` |
| Rotation lock | ✅ | ✅ | `alerts/SystemStateController.kt` |
| Screen recording | ✅ | ✅ | `alerts/SystemStateController.kt` |
| Screenshot (share, copy, edit, delete) | ✅ | ✅ | `content/IslandNotificationListener.kt` + `overlay/IslandAccessibilityService.kt` |
| Uninstalled app | ✅ | ✅ | `alerts/SystemStateController.kt` |
| Per-alert: on/off, duration, icon, colour, animation | ✅ | ✅ | `data/SettingsStore.kt`, `ui/alerts/AlertDetailFragment.kt` |
| Per-alert: allowed-apps filter | ✅ | ✅ | `ui/alerts/AlertDetailFragment.kt` |
| Per-alert: card parts | ✅ | ✅ | `ui/more/CardsFragment.kt` |

## 10.3 The gestures and the rules

| Feature | Original | Rebuild | Where |
|---------|----------|---------|-------|
| Tap, long press, four swipes | ✅ | ✅ | `island/IslandView.kt` |
| Separate bindings for the collapsed and the expanded surface | ✅ | ✅ | `model/GestureAction.kt` |
| Open an app | ✅ | ✅ | `gestures/ActionExecutor.kt` |
| Open a widget | ✅ | ✅ | `gestures/ActionExecutor.kt` |
| System actions (13 of them) | ✅ | ✅ | `gestures/ActionExecutor.kt` |
| Tasker task | ✅ | ✅ | `gestures/ActionExecutor.kt` |
| Rules with conditions | ✅ | ✅ | `model/Rule.kt`, `ui/rules/RuleEditorFragment.kt` |
| Rule conditions: charging, media, DND, screen, lock, battery | ✅ | ✅ | `model/Rule.kt` |
| "Require unlock" per rule | ✅ | ✅ | `model/Rule.kt` |
| Rules win over the plain binding | ✅ | ✅ | `overlay/IslandAccessibilityService.kt` |

## 10.4 The app and the system integration

| Feature | Original | Rebuild | Where |
|---------|----------|---------|-------|
| App row, up to ten apps | ✅ | ✅ | `data/SettingsStore.kt`, `ui/more/AppRowFragment.kt` |
| Quick Settings tile | ✅ | ✅ | `service/IslandTileService.kt` |
| Keep-alive foreground service | ✅ | ✅ | `service/KeepAliveService.kt` |
| Watchdog job | ✅ | ✅ | `service/WatchdogJob.kt` |
| Boot receiver | ✅ | ✅ | `service/SystemEventsReceiver.kt` |
| Automatic daily backup | ✅ | ✅ | `data/AutoBackupJob.kt` |
| Reliability screen | ✅ | ✅ | `ui/more/ReliabilityFragment.kt` |
| Onboarding | ✅ | ✅ | `ui/onboarding/OnboardingFragment.kt` |

## 10.5 The rest

| Feature | Original | Rebuild | Where |
|---------|----------|---------|-------|
| JSON backup and restore | ✅ | ✅ | `data/BackupManager.kt`, `ui/more/BackupFragment.kt` |
| Pro through Google Play Billing | ✅ | ✅ | `ui/more/ProFragment.kt` |
| In-app update | ✅ | ✅ | `ui/more/AboutFragment.kt` |
| "What's new" from the bundled changelog | ✅ | ✅ | `data/Changelog.kt` |
| Licence list | ✅ | ✅ | `assets/licenses.json` |
| Privacy text | ✅ | ✅ **reused from the APK** | `assets/privacy.md` |
| The Google Sans Flex fonts | ✅ | ✅ **reused from the APK** | `res/font/` |
| The launcher icon artwork | ✅ | ✅ **reused from the APK** | `res/drawable/`, `res/mipmap-anydpi-v26/` |
| The Quick Settings tile icon | ✅ | ✅ **reused from the APK** | `res/drawable/ic_tile_island.xml` |
| The release notes for 0.1.0 and 0.2.0 | ✅ | ✅ **reused from the APK** | `assets/changelog.json` |

## 10.6 The differences, and why

| Difference | Reason |
|------------|--------|
| The rebuild asks for **six fewer permissions** | the original declares location, bluetooth, storage and secure-settings permissions that its own code never uses; they come from libraries. The rebuild only declares what it uses. |
| The rebuild declares the Pro entitlement in its own file | the original already does this (`island_pro.xml`); the first version of the rebuild did not, and it was corrected. |
| The rebuild does not ship `play-services-location`, `review` or the navigation component | the rebuild does not use them. The navigation is a plain fragment transaction. |
| The rebuild is Kotlin 2.1.10 / AGP 8.9.1 / Material 1.12, the original is Kotlin 2.3.21 / Material 1.14 | the rebuild sticks to versions that are known to work together and that support API 28. |
| The rebuild targets SDK 35, the original targets 36 | SDK 36 was not available in the build environment; nothing in the app needs it. |
| The rebuild uses `AccessibilityService.GLOBAL_ACTION_*` where the original dispatches gestures | `GestureDescription.Builder` is not part of the public SDK, so a gesture has to be built through reflection. The global actions give the same result with public API. |
| The rebuild draws its own glyphs | the original does the same, but the exact shapes could not be recovered from the obfuscated code; the rebuild has its own set of 18 hand-drawn glyphs. |

## 10.7 The features that could not be recovered

| Feature | Why |
|---------|-----|
| The exact colour values of the seven original palettes | the original's palette code is obfuscated; the rebuild uses its own seven palettes with the same names and roles |
| The exact animations of the original | the original uses the `res/anim` and `res/animator` resources, which are obfuscated; the rebuild has spring, fade and slide |
| The exact layout of the original's 189 screens | the original's layouts are R8-renamed and the design is not recoverable from the code; the rebuild has 13 layouts that cover the same functions |
| The original's 0.4.0 features that were never in 0.2.0 | the 0.4.0 binary could not be downloaded; the delta was recovered from the Play listing and the changelog |
