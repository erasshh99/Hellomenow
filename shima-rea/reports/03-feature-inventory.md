# 03 · Feature inventory

Everything the app can do, recovered from the decompiled code, the resolved string table and the
Play Store description.

## 3.1 The island itself

| Feature | Notes |
|---------|-------|
| Pill in the camera cutout | drawn as an accessibility overlay, `TYPE_ACCESSIBILITY_OVERLAY` |
| Collapsed and expanded states | the pill grows into a card; the same view draws both |
| Cut-out calibration | the cutout is measured per edge (top / bottom / left / right), per orientation and per display, including the inner screen of a foldable |
| Size, corner radius, shape | capsule, circle, rounded rectangle and squircle |
| Position | horizontal and vertical offset in dp, plus "fit to the cutout" |
| Transparency | separate values for the collapsed and the expanded card |
| Outline | separate width and colour for both states |
| Shadow | separate radius and colour for both states |
| Animations | spring, fade and slide |
| Palettes | seven built-in palettes, each with a light and a dark variant |
| Material You | the palette can follow the wallpaper colours |
| Custom colours | a custom background and a custom foreground colour |
| Companion pill | a small second pill next to the island that keeps the previous card alive |
| Hide in full-screen apps | optional |
| Hide in chosen apps | a list of packages |
| Show on the lock screen | optional |
| Live preview | the island is previewed inside the settings screen, exactly as it will look |

## 3.2 Alerts

Every alert can be switched on and off on its own, and each one has its own duration, icon,
colour, animation, close behaviour, auto-open behaviour, mini-alert behaviour and an
**allow-list of apps** that are allowed to raise it.

| Alert | What it shows |
|-------|---------------|
| **Media** | the artwork, the track, the artist, the album, the lyrics (when the player publishes them) and the transport buttons |
| **Live** | on-going notifications with a progress bar: timers, rides, deliveries, downloads |
| **Charging** | the battery level and whether the phone is on AC, USB or wireless |
| **Torch** | that the flash was switched on or off |
| **Volume** | the volume, the ringer and the brightness, with a progress bar |
| **Privacy** | that another app is using the microphone or the camera |
| **Low battery** | the level, once it falls below the threshold |
| **Battery full** | that the phone has finished charging |
| **Battery saver** | that the battery saver was switched on |
| **No signal** | that the network went away |
| **USB** | that a cable was plugged in |
| **Do Not Disturb** | that DND was switched on |
| **Alarm** | the time of the next alarm |
| **Rotation lock** | that the rotation is locked |
| **Screen recording** | that a screen recording or projection is running |
| **Screenshot** | that a screenshot was taken, with share / copy / edit / delete buttons |
| **Uninstalled app** | that an app was removed |

The **cards** tab lets the user pick, for every alert, which parts of the card are shown:
title, subtitle, progress bar, artwork, lyrics and buttons.

## 3.3 Gestures

| Gesture | Notes |
|---------|-------|
| Tap | on the collapsed pill and on the open card |
| Long press | on the collapsed pill and on the open card |
| Swipe up / down / left / right | on the collapsed pill and on the open card |

Each gesture can be bound to an action:

| Action | Notes |
|--------|-------|
| Open an app | any installed app |
| Open a widget | any installed home-screen widget; Shima opens the widget's own app |
| System action | back, home, recents, notification shade, quick settings, lock screen, screenshot, torch, rotation lock, DND, media previous / next / play-pause |
| Tasker task | any task of an installed Tasker |

A tap with no binding opens and closes the card. A long press with no binding clears the island.

## 3.4 Rules

A **rule** makes one gesture do different things depending on the state of the phone. The
conditions that can be used are:

* the phone is charging
* music is playing
* Do Not Disturb is on
* the screen is on / off
* the phone is locked / unlocked
* the battery is low

A rule can also demand that the phone is unlocked before the action runs.

## 3.5 The app row

Up to **ten** apps can be pinned under the island and reached with a gesture.

## 3.6 Settings and reliability

* switch the island on and off
* a Quick Settings tile that does the same
* a keep-alive foreground service
* a watchdog job that restarts the island if it dies
* a boot receiver that brings it back after a restart
* a battery-optimisation exemption request
* a page that tells the user exactly which permission is missing

## 3.7 Backup

* export the whole configuration to a JSON file
* import it again, on this phone or on another one
* an automatic daily backup in the app's own storage

## 3.8 Pro

A one-off purchase through Google Play that unlocks:

* more alerts
* more rules
* the swipe gestures
* the companion pill
* the backup tools

## 3.9 About

* the version and the build
* an in-app update check through the Play in-app-update library
* "what's new", read from the bundled `assets/changelog.json`
* the privacy summary: no account, no ads, no analytics, everything on the device
* the licences of the 103 dependencies, read from `assets/licenses.json`

## 3.10 Onboarding

The first run explains what the island is, walks the user through granting the accessibility
service and the notification listener, and shows a live preview of the island.
