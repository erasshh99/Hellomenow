# 04 · Dynamic Island analysis

This is the heart of the app, so it gets the most detail.

## 4.1 Why an accessibility service

Android gives an ordinary app no way to draw over other apps. There are exactly three ways to
get an overlay:

| Technique | Usable? |
|-----------|---------|
| `TYPE_APPLICATION_OVERLAY` | needs the user to grant "display over other apps" — a second permission, and the Play Store rejects apps that only use it for a floating widget |
| `TYPE_SYSTEM_ALERT_WINDOW` | the same problem |
| **`TYPE_ACCESSIBILITY_OVERLAY`** | the overlay that belongs to an accessibility service — granted by the accessibility settings, which the Play listing describes as the one permission Shima needs |

The original uses the third one. `AccessibilityService` can attach a window with
`TYPE_ACCESSIBILITY_OVERLAY`, and that window is drawn on top of everything, including the
status bar and the cutout area.

The original's own description in the Play listing confirms this: the app asks for the
accessibility service so it can draw its island above the status bar.

## 4.2 The window

```java
WindowManager.LayoutParams params = new WindowManager.LayoutParams(
        width, height,
        TYPE_ACCESSIBILITY_OVERLAY,
        FLAG_NOT_FOCUSABLE | FLAG_LAYOUT_NO_LIMITS | FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT);
params.gravity = Gravity.TOP | Gravity.START;
```

| Flag | Why |
|------|-----|
| `FLAG_NOT_FOCUSABLE` | the island must never steal a key press from the app underneath |
| `FLAG_LAYOUT_NO_LIMITS` | the island may be drawn over the cutout and over the status bar |
| `FLAG_LAYOUT_IN_SCREEN` | the window is laid out against the whole screen, not the app area |
| `PixelFormat.TRANSLUCENT` | the pill has rounded corners and transparency |

The window is positioned with `gravity = TOP | START` and an `x` / `y` offset that is computed
from the cutout, so the island lines up with the camera hole on every device.

## 4.3 The cutout

Android 9 introduced `DisplayCutout`:

```java
DisplayCutout cutout = display.getCutout();
int top = cutout.getSafeInsetTop();       // the gap above the cutout
int left = cutout.getSafeInsetLeft();
int right = cutout.getSafeInsetRight();
int bottom = cutout.getSafeInsetBottom();
```

The original measures these **per edge and per orientation** and stores them in the settings, so
the island can be:

* centred on the cutout,
* pushed to the side the camera is on,
* moved by a user offset in dp.

On a foldable, the inner screen has its own cutout, so the measurement is taken for the display
the island is currently on.

The rebuild keeps this in `island/CutoutInfo.kt`, which measures the cutout of the display the
activity is on and hands it to the overlay through `IslandAccessibilityService.updateCutout()`.

## 4.4 The drawing

The island is **not** a stack of views with a background drawable. It is one custom `View` that
draws everything on a `Canvas`:

| Part | How it is drawn |
|------|-----------------|
| The pill | a `Path` with the chosen corner radius, filled with the background colour |
| The outline | the same path, stroked with the outline width and colour |
| The shadow | a `setShadowLayer` on the paint |
| The icon | a hand-drawn glyph (`IslandIcons`), so the island keeps its own weight |
| The title and the subtitle | `Paint.drawText` with the Google Sans Flex font |
| The progress bar | a rounded rectangle, background plus fill |
| The artwork | the bitmap from the media session, clipped to a circle or a rounded rectangle |
| The buttons | rounded rectangles with the glyph of the action in the middle |

The original draws the glyphs itself as well: `ui/widget/` contains a set of small custom views
that paint the icons on a `Canvas`, so the island does not depend on an icon font.

The rebuild does the same in `island/IslandView.kt` and `island/IslandIcons.kt`.

## 4.5 The state machine

```
            ┌──────────────┐
            │    HIDDEN    │   no card, the window is not visible
            └──────┬───────┘
                   │ a card arrives
                   ▼
            ┌──────────────┐
            │  COLLAPSED   │   the pill, the icon and one line
            └──────┬───────┘
                   │ tap / auto-open / a rule
                   ▼
            ┌──────────────┐
            │   EXPANDED   │   the full card with the artwork and the buttons
            └──────┬───────┘
                   │ tap outside / a timeout / long press
                   ▼
             (HIDDEN)
```

The rebuild keeps the same three phases in `island/IslandState.kt`
(`HIDDEN`, `COLLAPSED`, `EXPANDED`) and the same holder object, so the renderer and the alert
engine stay apart: the engine writes into the holder, the overlay reads from it.

## 4.6 The companion pill

A second, smaller window next to the island. It keeps showing the **previous** card for a short
while after a new card arrives, so a burst of notifications does not make the island flicker.
It is a Pro feature and is switched off by default.

## 4.7 The gestures on the island

The island view handles its own touch events and classifies them itself:

| Gesture | How it is recognised |
|---------|---------------------|
| Tap | the pointer moved less than the touch slop |
| Long press | the pointer stayed down for the long-press timeout |
| Swipe | the pointer moved more than the touch slop; the axis with the larger delta decides the direction |

The classification happens in the view, the *meaning* is decided by the accessibility service,
which looks up the binding for the surface (collapsed or expanded) and then the rules. A rule
that matches the current state of the phone wins over the plain binding.

The rebuild reproduces this in `island/IslandView.kt` (recognition) and
`overlay/IslandAccessibilityService.kt` (meaning).

## 4.8 Why the original uses the accessibility service for the gestures

Because the island is an accessibility overlay, its touches never reach the app underneath — so
the island can read the gestures itself. That is also why the service description says it does
not read the content of the screen: `canRetrieveWindowContent` is `false`, and the service only
asks for window-state events, which it uses to know which app is in the foreground so the island
can step aside for the apps the user picked.
