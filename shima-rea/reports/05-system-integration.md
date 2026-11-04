# 05 · System integration

## 5.1 The chain of services

```
                       ┌──────────────────────────────┐
                       │   AccessibilityService        │
   user turns it on ──▶│  • draws the island           │
                       │  • runs the gestures          │
                       │  • watches the foreground app │
                       └───────────┬──────────────────┘
                                   │ starts / stops
                       ┌───────────▼──────────────────┐
                       │         AlertEngine           │
                       │  decides what the island shows│
                       └──┬─────┬─────┬─────┬─────┬───┘
                          │     │     │     │     │
        ┌─────────────────┘     │     │     │     └──────────────────┐
        ▼                       ▼     ▼     ▼                        ▼
┌───────────────┐  ┌──────────────┐ ┌────────┐ ┌──────────────┐ ┌──────────────┐
│ MediaController│  │ChargingCtrl  │ │Volume  │ │PrivacyCtrl   │ │SystemState   │
│ (MediaSession) │  │(BATTERY_     │ │(Settings│ │(CameraMgr +  │ │Controller    │
│                │  │ CHANGED)     │ │ observer│  │ AudioManager)│ │(broadcasts + │
└───────────────┘  └──────────────┘ └────────┘ └──────────────┘ │ polling)     │
                                                                  └──────────────┘
                       ┌──────────────────────────────┐
                       │  NotificationListenerService  │
                       │  • live cards (progress bars)  │
                       │  • the screenshot notification │
                       └──────────────────────────────┘
```

## 5.2 The accessibility service

| Aspect | Detail |
|--------|--------|
| Class | `com.piyush.island.overlay.IslandAccessibilityService` |
| Window type | `TYPE_ACCESSIBILITY_OVERLAY` |
| Events | `TYPE_WINDOW_STATE_CHANGED` (to know the foreground app) |
| Gestures | `canPerformGestures = true` |
| Window content | **not** retrieved (`canRetrieveWindowContent = false`) |
| Global actions | used for back, home, recents, shade, quick settings, lock and screenshot |

The foreground-app tracking is what makes "hide the island in these apps" and "hide the island in
full-screen apps" possible.

## 5.3 The notification listener

`com.piyush.island.content.IslandNotificationListener` is a `NotificationListenerService`. It
uses the notifications for two things:

1. **Live cards** — an on-going notification that carries `EXTRA_PROGRESS` and
   `EXTRA_PROGRESS_MAX` is a timer, a ride or a delivery. The listener turns it into a card with
   a progress bar and a button that opens the app.
2. **The screenshot notification** — the system's own "screenshot taken" notification becomes a
   card with share / copy / edit / delete buttons.

The listener also ignores its own notifications.

## 5.4 The media session

`MediaSessionManager.getActiveSessions(null)` is polled and watched with
`OnActiveSessionsChangedListener`. From the active session the app reads:

* `METADATA_KEY_TITLE`, `METADATA_KEY_ARTIST`, `METADATA_KEY_ALBUM_ARTIST`, `METADATA_KEY_ALBUM`
* `METADATA_KEY_ALBUM_ART` / `METADATA_KEY_ART` — the artwork bitmap
* the two lyrics keys `android.media.metadata.LYRICS` and `android.media.metadata.LYRIC`
* `PlaybackState.state` — whether it is playing

and drives the transport through `MediaController.TransportControls`
(`play()`, `pause()`, `skipToNext()`, `skipToPrevious()`).

## 5.5 The Quick Settings tile

`com.piyush.island.overlay.IslandTileService` is a `TileService` with
`TOGGLELE_TILE = true`. Clicking it flips `settings.enabled`, starts or stops the keep-alive
service and refreshes the tile state.

## 5.6 The keep-alive service

A foreground service with `foregroundServiceType = specialUse` and the subtype
`dynamic_island_overlay`. It exists because Android is allowed to kill a background process, and
an overlay that dies is an overlay that does not come back. The service posts a quiet
notification and keeps the process alive.

## 5.7 The watchdog job

A `JobService` that is scheduled periodically. It checks whether the accessibility service is
running and whether the island is supposed to be visible; if the service died, it asks the
system to start it again.

## 5.8 The boot receiver

`com.piyush.island.reliability.SystemEventsReceiver` listens for `BOOT_COMPLETED`,
`MY_PACKAGE_REPLACED`, `QUICKBOOT_POWERON` and the HTC variant, and puts the island back.

## 5.9 The automatic backup job

`com.piyush.island.backup.AutoBackupJob` writes the settings to a JSON file in the app's own
storage once a day, so a backup exists even if the user never asks for one.

## 5.10 The system states that have to be polled

Android does not announce every state with a broadcast. The original (and the rebuild) poll for:

| State | How it is read |
|-------|----------------|
| The next alarm | `AlarmManager.getNextAlarmClock()` |
| A screen recording | `MediaProjectionManager.isProjectionActive()` (a non-public method, called through reflection) |
| The microphone | `AudioManager.isMicrophoneActive()` (a non-public method, called through reflection) |
| The rotation lock | `Settings.System.ACCELEROMETER_ROTATION` |
| USB | the sticky `android.hardware.usb.action.USB_STATE` broadcast |

The reflection calls are wrapped so that a device which does not allow them simply reports
"no", instead of crashing.

## 5.11 The torch

`CameraManager.setTorchMode(cameraId, true)` — the flash is switched on through the camera
service. No camera permission is needed for the torch, and the rebuild does not ask for one.
