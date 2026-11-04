# 09 · Recovered architecture

## 9.1 The original, as recovered

The original is an MVVM-style app with a single activity and a service locator:

```
com.piyush.island
├── App                        the Application class (the PairIP one is injected by Play)
├── MainActivity               the only activity; a bottom navigation with five tabs
│
├── overlay/
│   ├── IslandAccessibilityService   the overlay and the gestures
│   └── IslandTileService            the Quick Settings tile
├── content/
│   └── IslandNotificationListener   the media session and the live notifications
├── reliability/
│   ├── KeepAliveService        the foreground service
│   ├── WatchdogJob             the job that restarts the island
│   └── SystemEventsReceiver    the boot receiver
├── backup/
│   └── AutoBackupJob           the daily automatic backup
│
└── ui/                        94 classes over 25 areas
    ├── about/ apps/ behavior/ color/ deck/ donate/ expanded/ flashlight/
    ├── gesture/ install/ live/ lowbattery/ media/ onboarding/ pair/ pro/
    ├── reliability/ rules/ screenshot/ settings/ style/ tabs/ volume/
    └── widget/ widgets/
```

The `ui/` tree maps one-to-one onto the feature list: one package per area of the app, with the
fragment, the view model and the small custom views of that area together.

## 9.2 The rebuild, as written

The rebuild keeps the same shape but merges the areas that belong together:

```
com.piyush.island
├── MainActivity          the only activity and the navigation
├── ShimaApp              the Application class
│
├── core/
│   └── AppGraph          the service locator: settings, alert engine, island state
├── model/
│   ├── AlertType.kt      the seventeen alerts, their order and their colours
│   ├── AlertConfig.kt    what the user can change about one alert
│   ├── GestureAction.kt  the gestures and the actions they can be bound to
│   ├── Rule.kt           the rules and their conditions
│   ├── IslandStyle.kt    the island snapshot and the whole settings snapshot
│   └── Palette.kt        the seven palettes, light and dark
│
├── data/
│   ├── SettingsStore.kt  the one source of truth (SharedPreferences + a StateFlow)
│   ├── BackupManager.kt  the JSON export and import
│   ├── Changelog.kt      reads assets/changelog.json
│   └── AutoBackupJob.kt  the daily backup
│
├── island/
│   ├── IslandState.kt    the phases, the card, the state holder
│   ├── IslandView.kt     the custom View that draws the pill and the card
│   ├── IslandWindow.kt   the overlay window and its position
│   ├── IslandIcons.kt    the hand-drawn glyphs
│   └── CutoutInfo.kt     the cutout measurement
│
├── alerts/
│   ├── AlertEngine.kt    decides what the island shows
│   ├── MediaController.kt ChargingController.kt FlashlightController.kt
│   ├── VolumeController.kt PrivacyController.kt SystemStateController.kt
│
├── overlay/
│   └── IslandAccessibilityService.kt
├── content/
│   └── IslandNotificationListener.kt
├── gestures/
│   └── ActionExecutor.kt the action runner and the widget bridge
├── service/
│   ├── KeepAliveService.kt WatchdogJob.kt IslandTileService.kt SystemEventsReceiver.kt
│
└── ui/
    ├── island/ alerts/ gestures/ rules/ style/ more/ onboarding/ widget/
```

## 9.3 The data flow

```
   ┌──────────────┐   write    ┌───────────────┐   StateFlow   ┌──────────────┐
   │ the screens  │ ─────────▶ │ SettingsStore │ ────────────▶ │  the screens │
   └──────────────┘            └───────────────┘               └──────────────┘
          ▲                            ▲
          │ read                       │ read
          │                            │
   ┌──────┴───────┐            ┌───────┴────────┐
   │  AppGraph    │            │  AppGraph      │
   └──────────────┘            └────────────────┘

   ┌──────────────┐  card   ┌───────────────┐  state  ┌────────────────┐
   │ the alerts   │ ───────▶ │ AlertEngine   │ ──────▶ │ IslandState    │
   │ the listener │          └───────────────┘        └───────┬────────┘
   └──────────────┘                                            │
                                                               ▼
                                                    ┌────────────────┐
                                                    │ IslandWindow   │
                                                    │ + IslandView   │
                                                    └────────────────┘
```

One rule holds the whole app together: **the renderer never decides what to show, and the alert
controllers never draw anything.** A controller produces an `IslandCard`, the `AlertEngine`
decides whether that card is allowed to reach the island, the `IslandStateHolder` holds it, and
the `IslandView` draws it.

## 9.4 The state

```kotlin
data class ShimaSettings(
    val enabled: Boolean = true,
    val keepAlive: Boolean = false,
    val autoBackup: Boolean = false,
    val style: IslandStyle = IslandStyle(),
    val alerts: Map<AlertType, AlertConfig> = ...,
    val gestures: GestureBindings = GestureBindings(),
    val rules: List<Rule> = emptyList(),
    val decks: Map<String, List<String>> = ...,
    val appRow: List<String> = emptyList(),
    val proUnlocked: Boolean = false,
    val onboardingDone: Boolean = false,
)
```

Everything is immutable. A change produces a new snapshot, the snapshot is written to disk and
the `StateFlow` pushes it to every screen that is listening. There is no other way to change the
state, so there is no way for two screens to disagree about it.

## 9.5 The thread model

| Thread | What happens there |
|--------|--------------------|
| Main | the island view, the alert engine, the gesture handling |
| Background (the `JobService` workers) | the automatic backup |
| Wherever the platform calls | the accessibility events and the notification callbacks — both are delivered on the main thread by the framework |

The `AlertEngine` owns a `CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)`, so a
controller that throws cannot take the others down.

## 9.6 The island state machine

```
       a card arrives
 HIDDEN ─────────────▶ COLLAPSED
   ▲                       │
   │  a timeout, a long     │ a tap, an auto-open, a rule
   │  press, a tap outside  ▼
   └──────────────────── EXPANDED
```

`IslandStateHolder` owns the transitions; `IslandView` only reads the state and draws it. The
companion pill is a second card in the same snapshot, so it follows the same rules.

## 9.7 Why the rebuild uses XML views and not Compose

The original is not a Compose app: there is no Compose artefact in its 103 dependencies and its
189 layouts are all XML. The rebuild follows the same choice, which has three practical
advantages:

* the island is a single custom `View` that draws on a `Canvas` — Compose would only get in the
  way of the exact pill shape, the clipping and the animation;
* the minimum SDK of 28 is supported without the extra work that Compose needs for old versions;
* the build stays small and the project opens quickly in Android Studio.

## 9.8 The dependency graph of the rebuild

```
                     ┌──────────────────────────────┐
                     │       MainActivity            │
                     │  MainActivity + the fragments │
                     └───────────────┬──────────────┘
                                     │ reads
                     ┌───────────────▼──────────────┐
                     │         core.AppGraph          │
                     └───┬────────────┬───────────┬───┘
                         │            │           │
              ┌──────────▼───┐  ┌─────▼─────┐  ┌──▼──────────────────┐
              │ SettingsStore│  │AlertEngine│  │   IslandStateHolder │
              └──────┬───────┘  └─────┬─────┘  └──────────┬──────────┘
                     │                │                     │
                     │        ┌───────┴────────┐            │
                     │        │ the controllers│            │
                     │        └────────────────┘            │
                     │                                     │
              ┌──────▼─────────────────────────────────────▼──────┐
              │        overlay.IslandAccessibilityService         │
              │   the window, the view, the gestures, the rules   │
              └──────────────────────────────────────────────────┘
```
