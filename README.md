# Shima

Unofficial, **modified** research build of [Shima](https://play.google.com/store/apps/details?id=com.piyush.island)
(`com.piyush.island`) by Piyush Mamidwar — with the PairIP licence/trial gate
removed so the app can be analysed without a Play Store licence.

> Shima and its rights belong to its author. This is not an official release.
> See [`re/README.md`](re/README.md) for the full disclosure.

## Releases

| Tag | Asset |
|---|---|
| [`v0.5.1-pairip-bypass`](../../releases/tag/v0.5.1-pairip-bypass) | `Shima-v0.5.1-pairip-bypassed-signed.apk` — v1/v2/v3 signed, zipaligned |

## What's in here

- `Shima v0.5.1_antisplit.apk` — the original uploaded build (debug-signed, malformed ZIP container, Pro-locked by the app's own cert allow-list)
- [`re/`](re/) — the bypassed build, the repaired container, and the analysis:
  - [`re/REPORT.md`](re/REPORT.md) — DRM & anti-tamper analysis, network audit, permission review, verification steps
  - [`re/BEHAVIOUR-ANALYSIS.md`](re/BEHAVIOUR-ANALYSIS.md) — actions, rules, notification parsing, storage, backup, abuse assessment
  - [`re/VERIFICATION.md`](re/VERIFICATION.md) — independent re-verification of the delivered build (container, content diff, dex patch, signatures, alignment)

Decompiled sources are not published (see `re/.gitignore`).

## Why

The app ships two independent anti-tamper layers — a PairIP licence wrapper and
its own APK-signing-certificate allow-list. Documenting both, and producing a
build that runs without a licence, is the point of this repository.
