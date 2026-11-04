# 01 · Application identification

## 1.1 The binary

| | |
|---|---|
| File | `com.piyush.island.apk` (base split of an XAPK) |
| Size | 3 353 413 bytes (3.20 MiB) |
| SHA-256 | `138387c9e7d0d8c9f23b4ddb2aaaed1ee49c56bed4726d06e4e1d6c3b5080723` |
| Package | `com.piyush.island` |
| versionName / versionCode | `0.2.0` / `5` |
| Format | APK, one dex (`classes.dex`), no native libraries |
| Signing | APK Signature Scheme v3, **Google Play App Signing** (`CN=Android, O=Google Inc.`) |

The certificate digest of the original:

```
Signer #1 certificate SHA-256 digest: eebb863d29141f9157d3c50b47d05d1eb8a734d0c46781b9e3d015b71b751d1d
```

The app is published through Google Play and therefore carries the Play App Signing key, not the
developer's own upload key. A rebuilt APK can consequently never share the original's signature;
the rebuild is signed with the standard Android debug key until a release key is supplied.

## 1.2 Where the binary came from

The version that the Play Store lists today is **0.4.0**. That binary could not be fetched from
inside this environment:

| Source | Result |
|--------|--------|
| APKPure XAPK | ✅ v0.2.0 (versionCode 5) — this is the binary that was analysed |
| APKMirror | 403 (Cloudflare) |
| APK-Pure API | only exposes v0.2.0 |
| Aurora / APKCombo mirrors | DNS or Cloudflare blocked |

Everything that distinguishes **0.4.0** from **0.2.0** was therefore recovered from the Play
Store listing and from the app's own bundled `assets/changelog.json`, and re-implemented in the
rebuild. See [12 · Limitations](12-limitations.md).

The XAPK also contained a configuration split (`config.xxhdpi.apk`) that only carried the
Play-store-generated density resources; it has no code and was not needed for the analysis.

## 1.3 The packaging

```
com.piyush.island.xapk
├── com.piyush.island.apk          3.2 MB   base (code + resources + assets)
└── config.xxhdpi.apk                     density configuration split
```

## 1.4 Identity markers inside the app

| Marker | Where | Value |
|--------|-------|-------|
| Application class | manifest `android:name` | `com.pairip.licensecheck.PairIP` — injected by the Play App Signing "pair" library, not app code |
| Application id | manifest | `com.piyush.island` |
| Launch activity | manifest | `com.piyush.island.MainActivity` |
| Package + version | `BuildConfig` | `com.piyush.island` / `0.2.0` |
| Support address | About screen | `oto.music121@gmail.com` |
| Font | `res/font/` | Google Sans Flex (300 / 500 / 600 / 750) |

## 1.5 The tool chain used

| Tool | Version | Use |
|------|---------|-----|
| Apktool | 3.0.3 | manifest decoding, resource decoding, asset extraction |
| JADX | 1.5.x | dex → Java decompilation (3 055 files; 104 classes in `com.piyush.island`) |
| aapt2 / apksigner | build-tools 35.0.0 | identity, signatures, resource inspection |
| keytool | JDK 21 | certificate inspection |

## 1.6 The original at a glance

| | |
|---|---|
| Code | Kotlin, compiled with R8, one dex, no native code |
| UI | Android **Views** (XML + custom `Canvas` drawing), **not** Jetpack Compose |
| Architecture | MVVM-ish: one `ViewModel` per screen, a repository over `SharedPreferences`, a single `ServiceLocator` |
| Dependencies | 103 artefacts (see [06 · Static analysis](06-static-analysis.md)) |
| Resources | 189 layouts, `res/anim`, `res/animator`, `res/navigation`, `res/font`, `res/color-night` |
| Assets | `changelog.json`, `licenses.json`, `privacy.md`, `dexopt/` |
