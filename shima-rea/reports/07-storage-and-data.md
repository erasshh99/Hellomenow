# 07 · Storage and data

## 7.1 Where the settings live

The original keeps its configuration in **`SharedPreferences`**, one file per section. The
decompiled `App` class writes and reads each section by its own file name, which keeps the
writes small and means a corrupted section cannot take the whole configuration with it.

The rebuild uses the same idea in one place, `data/SettingsStore.kt`:

| Piece | Where it goes |
|-------|---------------|
| `ShimaSettings` | one JSON object in `SharedPreferences` under the key `settings` |
| The alert configuration | the `alerts` section of the same object |
| The gesture bindings | the `gestures` section |
| The rules | the `rules` section |
| The card layouts | the `decks` section |
| The island style | the `style` section |

One immutable snapshot at a time:

```kotlin
data class ShimaSettings(
    val enabled: Boolean,
    val keepAlive: Boolean,
    val autoBackup: Boolean,
    val style: IslandStyle,
    val alerts: Map<AlertType, AlertConfig>,
    val gestures: GestureBindings,
    val rules: List<Rule>,
    val decks: Map<String, List<String>>,
    val appRow: List<String>,
    val proUnlocked: Boolean,
    val onboardingDone: Boolean,
)
```

Every screen collects this snapshot from a `StateFlow`, and every change writes a new snapshot.
There is exactly one place where the state of the app is defined, which is what makes the
behaviour easy to follow and easy to test.

## 7.2 The backup format

The export is a **plain JSON document** — readable, editable by hand and easy to move between
phones. The rebuild writes the same shape:

```json
{
  "version": 1,
  "exportedAt": 1762200000000,
  "settings": {
    "enabled": true,
    "keepAlive": false,
    "autoBackup": false,
    "style": { "widthDp": 220, "heightDp": 36, "palette": "ink", "animation": "spring", "...": "..." },
    "alerts": {
      "media": { "enabled": true, "icon": "music", "color": -13919681, "duration": 8000, "packages": [] },
      "charging": { "...": "..." }
    },
    "gestures": { "collapsed": { "tap": "app:com.spotify.music" }, "expanded": { "...": "..." } },
    "rules": [ { "id": "…", "gesture": "tap", "conditions": ["charging"], "action": "…", "requireUnlock": true } ],
    "decks": { "media": ["title", "subtitle", "artwork", "progress", "actions"] },
    "appRow": ["com.spotify.music", "com.android.chrome"]
  }
}
```

The import is defensive: every field falls back to its default, a broken file never crashes the
app, and an import that cannot be read leaves the current configuration untouched.

The Pro flag is **not** part of the backup. The entitlement comes from Google Play and is
re-checked with the Play Billing library; a backup file cannot unlock it. This is deliberate:
copying the Pro state through a file would be a way to hand the entitlement around for free.

## 7.3 The automatic backup

`AutoBackupJob` is a `JobService` that runs once a day and writes the same JSON into the app's
own `files/` directory. It never leaves the sandbox, and it is switched off by default.

## 7.4 What the app reads from the system

| Source | What is read |
|--------|--------------|
| `MediaSessionManager` | the metadata of the media that is playing |
| `NotificationListenerService` | the title, the text and the progress of the on-going notifications |
| `BatteryManager` (broadcast) | the level, the scale and the plug type |
| `Settings.System` | the rotation lock, the brightness |
| `AudioManager` | the volume, the ringer mode |
| `CameraManager` | the availability of the camera, the torch |
| `PowerManager` | the battery saver mode, whether the screen is on |
| `AlarmManager` | the next alarm |
| `TelephonyManager` | whether there is a signal |
| `PackageManager` | the list of installed apps, the launch intents, the widget providers |

## 7.5 What leaves the phone

**Nothing from the app itself.** The only network traffic that can happen is the traffic of the
Play Billing and the Play in-app-update libraries, which talk to Google Play over the Play
Services IPC.

There is:

* no account,
* no analytics,
* no advertising identifier,
* no crash reporting,
* no server of any kind.

## 7.6 The privacy policy

`assets/privacy.md` is shipped inside the app and shown in the About screen. It says the same
thing: the app reads the screen state and the notifications to draw the island, keeps everything
on the device, and sends nothing anywhere.

## 7.7 Android backup

The manifest sets `android:allowBackup="true"` together with
`@xml/backup_rules` and `@xml/data_extraction_rules`, so the configuration is included in the
normal Android cloud backup and in the device-to-device transfer. The rebuild ships the same two
files.
