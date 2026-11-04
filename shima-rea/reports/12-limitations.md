# 12 · Limitations and open questions

## 12.1 The 0.4.0 binary could not be obtained

The Play Store lists **0.4.0** as the current version. The binary that could be analysed is
**0.2.0**:

| Source | Outcome |
|--------|---------|
| APKPure XAPK | ✅ 0.2.0 — the binary that was analysed |
| APKMirror | 403, the request is behind Cloudflare |
| The APK-Pure API | only exposes 0.2.0 |
| Aurora and other mirrors | the host names do not resolve or are blocked |

So the analysis describes 0.2.0, and the rebuild declares 0.4.0 with the 0.3.0 and 0.4.0
features that were recovered from two places:

1. the Play Store listing of the app, which describes the newer features in prose;
2. `assets/changelog.json` of the original, which is the app's own release-notes file — the
   rebuild extends it with the 0.3.0 and 0.4.0 entries.

**What this means in practice:** the 0.2.0 code was read line by line, and the 0.4.0 behaviour
was reconstructed from the published description. If 0.4.0 contains a change that is neither in
the listing nor in the changelog, the rebuild does not have it.

## 12.2 What could not be recovered from the obfuscated code

| Item | Why |
|------|-----|
| The exact palette colours | the palette code is R8-renamed and the colour values are built at runtime from an obfuscated table |
| The exact animations | the original ships `res/anim` and `res/animator` resources whose names are obfuscated; the timings and interpolators could not be mapped back to the screens |
| The exact layouts | 189 obfuscated XML layouts; the design cannot be recovered from the names |
| The original's icon artwork for the island | the glyphs are painted in code that is renamed; the rebuild has its own set of 18 hand-drawn glyphs |
| The exact copy of every screen | the string table was recovered (the resolved strings are in the analysis), but the original's wording was rewritten in the rebuild to read naturally |

## 12.3 The things that were deliberately left out

| Thing | Why |
|-------|-----|
| Any kind of purchase bypass | The Pro entitlement comes from Google Play and from nothing else. There is no offline unlock, no receipt check that could be spoofed and no server that could grant it. |
| The Play App Signing pair library | it is injected by Google into the Play build; it is not part of the app |
| `play-services-location`, `review`, the navigation component | the rebuild does not need them; dropping them removes six unused permissions |
| The `datatransport` telemetry | it comes with the Play libraries; the rebuild does not use the Play libraries that need it |

## 12.4 Things that could be checked better on a device

* the cutout calibration on a device with a hole in the corner and on a foldable,
* the behaviour of the keep-alive service on the aggressive battery managers of Samsung and Xiaomi,
* the exact look of the island on a screen with a very different aspect ratio,
* how quickly the live cards appear for the different timer apps,
* whether the in-app update flow works with a build that is signed with a different key than the
  one in the Play Store (it will not: Play only offers an update to the build it knows).

## 12.5 Open questions about the original

| Question | What is known |
|----------|---------------|
| Does the original show the lyrics of every player? | it reads two different lyrics metadata keys, so the support depends on the player |
| Is the companion pill a separate window or a second pill in the same window? | the code shows a second, smaller surface next to the island |
| Does the original's "hide in full-screen apps" use the window bounds? | it asks for `canRetrieveWindowContent`, so it has to be doing something of the kind; the rebuild compares the window bounds with the display bounds |
| What is the exact list of the Pro features? | the listing names more alerts, more rules, the swipe gestures, the companion and the backup tools; the rebuild gates the same five groups |

## 12.6 How to continue the work

1. **Get the 0.4.0 binary.** Download it from a device with Play Services, or from a machine
   that can reach APKMirror, and drop it in. Then:
   ```bash
   apktool d -f -o apktool_040 com.piyush.island.apk
   jadx -d jadx_040 com.piyush.island.apk
   ```
   and diff the manifest and the decompiled tree against the 0.2.0 analysis.
2. **Check the app on a device.** Install the debug APK, turn on the accessibility service, and
   walk through the onboarding.
3. **Compare the look.** The palette, the animations and the glyphs are the parts where the
   rebuild is an interpretation rather than a copy; the settings screen of the original is the
   reference.
4. **Sign the release.** Fill in the `SHIMA_*` environment variables and run
   `./gradlew :app:assembleRelease`.
