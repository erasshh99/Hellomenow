# Shima — pairip-bypassed build (unofficial)

> **This is an unofficial, modified build of a third-party app.**
> Shima (`com.piyush.island`) is by **Piyush Mamidwar** (`oto.music121@gmail.com`)
> and is normally distributed through Google Play:
> <https://play.google.com/store/apps/details?id=com.piyush.island>
> All rights in the original app belong to its author. This repository contains
> a build that has been modified for **security research only**.

## What this is

A `v0.5.1` build of Shima with its **PairIP licence/trial gate removed** so the app
can be analysed on a device without a Play Store licence.

| | |
|---|---|
| Package | `com.piyush.island` |
| Version | 0.5.1 (versionCode 9) |
| min / target SDK | 32 / 36 |
| Release asset | `Shima-v0.5.1-pairip-bypassed-signed.apk` |

## Exactly what was changed

1. **ZIP container repaired.** The original APK's central directory contains a
   corrupt extra field (`0x3500`), which breaks `zipfile`, `apktool` and `aapt`.
   The archive was rebuilt with the bad extra fields stripped — all 1358 entries
   preserved. (`shima-fixed.zip` in this directory is that repaired container.)
2. **PairIP licence check removed.** One call was deleted from
   `com.pairip.application.Application.attachBaseContext`:
   ```smali
   invoke-static {p1}, Lcom/pairip/licensecheck/LicenseClient;->checkLicense(Landroid/content/Context;)V
   ```
   The manifest `android:name` still points at `com.pairip.application.Application`,
   which now simply extends the app's real `com.piyush.island.App`.
3. **Re-signed** with a throwaway debug key (the original was already
   debug-signed by whoever repackaged it, so the Play upload signature was
   invalid anyway). Signed v1 + v2 + v3, zipaligned.

Everything else — all app code, resources, assets, and the remaining 3297 classes —
is unchanged.

## What was deliberately *not* changed

**The Pro tier stays locked.** The app carries a second, independent anti-tamper
layer: it hashes its own APK signing certificate and compares it against a
hard-coded allow-list of the developer's certs. Because this build is not signed
with one of those keys, the app records the mismatch in `island_integrity` and
**disables Pro from the third launch onward**. That is the same behaviour as the
original `Shima v0.5.1_antisplit.apk`, which already fails that check.

See `REPORT.md` §4b for the mechanism and the on-device verification.

## Documentation

| File | Contents |
|---|---|
| [`REPORT.md`](REPORT.md) | Main report: DRM/anti-tamper analysis, network audit, permission review, build & verification steps, corrections log |
| [`BEHAVIOUR-ANALYSIS.md`](BEHAVIOUR-ANALYSIS.md) | Deep behavioural map: actions, rules & triggers, notification parsing, storage, backup, abuse assessment |

`re/decompiled/` (decompiled sources) is intentionally **not** published — see
`re/.gitignore`.

## Verification performed

Built, signed and tested on a KVM-accelerated **Android 13 / API 33** emulator:

```
adb install                          -> Success
am start com.piyush.island/.MainActivity
                                     -> "Displayed com.piyush.island/.MainActivity"
logcat | grep LicenseClient         -> 0 lines
logcat | grep "FATAL EXCEPTION"     -> 0
```

## Installing

```sh
adb install Shima-v0.5.1-pairip-bypassed-signed.apk
```

Requires API 32+. The app asks for an accessibility service and a notification
listener to draw the island overlay — grant those in Settings for it to do
anything.

## Original author

Shima is the work of **Piyush Mamidwar**. If you own the app, this repository
is yours to take down — and you are very welcome to.
