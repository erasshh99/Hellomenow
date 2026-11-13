# Shima v0.5.1 — Reverse Engineering Report

**Artifact:** `Shima v0.5.1_antisplit.apk` (3,372,206 bytes)
**Package:** `com.piyush.island` · **Version:** 0.5.1 (versionCode 9) · **minSdk** 32 / **targetSdk** 36 / compileSdk 37
**Developer:** Piyush Mamidwar (`oto.music121@gmail.com`), GitHub `Piyush7890`
**What it is:** A "Dynamic Island"-style overlay app for Android (an iPhone Dynamic Island clone).
**Status of this build:** Repackaged (split-merged) and **re-signed with an Android Debug key**.

> **Headline findings**
> 1. The app is wrapped in **PairIP** DRM whose Application class is the manifest entry point — removing one `invoke-static` call makes it launch.
> 2. It has a **second, independent anti-tamper layer**: it hashes its own APK signing certificate and compares it against a hard-coded allow-list of the developer's certs. **The original "antisplit" APK already fails this check** — it is a permanently Pro-disabled build by design (§4b).
> 3. Its network surface is tiny: no developer server, no analytics, no ads, no developer-owned keys. The only outbound traffic is album-artwork fetches, a GitHub privacy-policy GET, and the Play-Billing library's own CCT telemetry to Google.

---

## 1. Toolchain used

| Tool | Version | Purpose |
|---|---|---|
| jadx | 1.5.2 | DEX → Java decompilation |
| apktool | 2.11.1 | APK decode, smali disassembly, dex re-assembly |
| androguard | 4.1.4 | Manifest/DEX/cert parsing, verification |
| uber-apk-signer | 1.3.0 | zipalign + v1/v2/v3 signing (bundles apksigner) |
| Android SDK | platform-tools, emulator, system-images;android-33 | Runtime verification |
| AVD `shima` | Android 13 (API 33) x86_64, KVM-accelerated | On-device verification |

---

## 2. Why the APK is called "antisplit"

The original Shima shipped as an Android App Bundle (split APKs). This file is a **merged universal APK**, which invalidates the Play upload signature, so whoever repackaged it **re-signed with a throwaway debug key**:

```
Subject: CN=Android Debug, O=Android, C=US
Serial:  946b97d7b06d0bd0
Valid:   2026-08-30 → 2054-01-15
SHA-256: fb2c19d951ed18744decce97398cc2de897f96449ae7d73bd4aa088e070093ed
```

(The same certificate is used for the v1, v2 and v3 signatures — verified with Google's
own `apksig` library; see [VERIFICATION.md](VERIFICATION.md) §4. The subject and serial
quoted in an earlier draft of this section were wrong and are corrected in §13.)

Consequences: it will **not** install over the Play version (signature mismatch), and it trips Play's own licence path (see §4).

**Bonus finding — the APK's ZIP container is malformed.** The central directory contains a corrupt extra field (`0x3500`, size 729), which breaks Python's `zipfile`, `apktool`, and `aapt`. I wrote a repairer that rebuilds the archive from the central directory with the bad extra fields stripped → `re/shima-fixed.zip` (1358 entries, `testzip()` clean). Everything downstream used the repaired container.

### 2.1 Cross-check against the live Play Store listing

The listing at `play.google.com/store/apps/details?id=com.piyush.island` is reachable and confirms the analysis baseline:

| Play listing | Value | Matches the binary? |
|---|---|---|
| Current version | **0.5.1**, versionCode **9** | ✅ the manifest |
| "Updated on" | **Nov 4, 2026** | ✅ `assets/changelog.json` → `"version": "0.5.1", "date": "2026-11-04"` |
| Developer | **Piyush M.** / Piyush Ganesh Mamidwar, `oto.music121@gmail.com` | ✅ the address in the About screen |
| Data safety | "No data collected", "No data shared with third parties" | ✅ consistent with §10 — with the CCT nuance noted there |
| "What's new" | "Timers now show on Samsung phones, and from Google's Clock before Android 17" | ✅ same bullet as the bundled changelog ("Android 16 and earlier") |
| Version history on the listing | 0.5.1, 0.4.0, 0.2.0 | ✅ (0.3.0 is in the app's own changelog, not on the page) |

The identical dates are the useful part: the analysed binary is the **current public Play
release**, not a private or pre-release build, so the behavioural map in this report
describes what users actually get.

What could **not** be done from here is the differential analysis against the *genuine
Play-signed* build: every APK mirror reachable from this environment is behind Cloudflare
or DNS-blocked (`apkpure`, `apkgk` → 403; `apkcombo` → NXDOMAIN; `apkmirror` → 403), and
Play itself only serves the APK to an authenticated account. The Play App Signing
certificate that the app's own allow-list expects (`eebb863d…751d1d`, §4b) is therefore
still verified only indirectly — it is the same digest the v0.2.0 XAPK carried, per the
older `shima-rea` analysis of that build.

---

## 3. Structure

- 1358 entries · single `classes.dex` (3,386,968 B) · **no `lib/*.so`** (pure Kotlin/Java)
- 3297 classes total → 105 app classes, 36 PairIP classes, the rest AndroidX + Play Services
- 15,589 unique string constants
- **R8-obfuscated core** (`defpackage/AbstractC####`, `C####`) but **`com.piyush.island.ui.*` left in the clear** (likely kept by R8 rules for layout/XML references)
- **Resource names obfuscated** (`a`, `b`, `c`…) — resource-name shrinking. This later blocks a full apktool rebuild (§6.2)
- Bundled assets: `privacy.md` (effective 2026-10-18), `changelog.json`, `licenses.json`, `dexopt/baseline.prof`

### Manifest components (7 activities, 8 services, 3 receivers)

| Component | Role |
|---|---|
| `com.piyush.island.MainActivity` | launcher / settings UI |
| `overlay.IslandAccessibilityService` | `BIND_ACCESSIBILITY_SERVICE` — draws the island + executes gestures |
| `content.IslandNotificationListener` | `BIND_NOTIFICATION_LISTENER_SERVICE` — reads notifications |
| `overlay.IslandTileService` | Quick Settings tile |
| `reliability.KeepAliveService` | foreground service, `specialUse` subtype `dynamic_island_overlay` |
| `reliability.WatchdogJob` / `SystemEventsReceiver` | self-heal; reacts to BOOT_COMPLETED / MY_PACKAGE_REPLACED / QUICKBOOT |
| `backup.AutoBackupJob` | scheduled settings backup |
| `ui.screenshot.DeleteRequestActivity` | `MediaStore.createDeleteRequest` for screenshot cleanup |
| `com.pairip.licensecheck.LicenseActivity` | PairIP paywall / error dialog |

---

## 4. The DRM: PairIP (the real blocker)

The manifest's Application class is **not** the app's:

```xml
<application android:name="com.pairip.application.Application" ...>
```

`com.pairip.application.Application extends com.piyush.island.App` and injects the licence check:

```java
protected void attachBaseContext(Context context) {
    LicenseClient.checkLicense(context);
    super.attachBaseContext(context);
}
```

`com.pairip.licensecheck.LicenseClient` (1142 lines of decompiled logic):

| Element | Detail |
|---|---|
| `licensePubKey` | RSA-2048 public key, base64 X509, embedded in the DEX (`MIIBIjANBgkqhkiG9w0...`) |
| `packageName` | hardcoded `com.piyush.island` |
| `performLocalInstallerCheck()` | requires `getInstallSourceInfo().getInstallingPackageName() == "com.android.vending"`. **Sideload ⇒ "Local install check failed due to wrong installer."** |
| fallback | binds `com.android.vending.licensing.ILicensingService`, transact **2** = `checkLicenseV2` |
| response validation | `LicenseResponseHelper.validateResponse` — splits a JWS, asserts `alg == RS256`, verifies with `SHA256withRSA` + the embedded public key, and asserts `packageName` matches |
| response codes | `0` = licensed · `2` = paywall (PendingIntent) · `3` = invalid package name |
| retry policy | 3 retries @ 2 s → `handleError()` → `startErrorDialogActivity()` + `System.exit(0)` after 30 s (`scheduleAppShutdown`) |
| states | `CHECK_REQUIRED → LOCAL_CHECK_OK → FULL_CHECK_OK / LOCAL_CHECK_REPORTED / REPEATED_CHECK_REQUIRED` |
| extra | `TrialClient`/`TrialStatus` (custom trial), `LicenseContentProvider` (a **second, dead** entry point — the provider is *not* registered in the manifest, confirmed on-device) |

Note the licence is validated **server-side by the Play Store** against a real purchase — so the debug-signed rebuild cannot pass it legitimately even with the right key.

---

## 4b. A SECOND, INDEPENDENT anti-tamper layer (the app's own)

**This is the more interesting gate, and it changes the story about the "antisplit" build.**

On top of PairIP, the app **verifies its own APK signing certificate**. Six call sites inline the same logic: `defpackage/C0169.java:186+`, `C0175.java:230+`, `C0473.java:236+`, `C1953a.java:654+`, `C1349.java:672+`, `C1908a.java:971+`.

**Mechanism:**
1. Parse the **APK Signing Block** (v2/v3) of its own installed APK to extract the signing-cert SHA-256 digests — `defpackage/AbstractC0140.java:1859` → `m3465a()` (scans for the `APK Sig Block 42` magic `f7531a`).
2. Compare against **two hard-coded allow-lists**, constant-time (length check, then XOR-accumulate over all chars):
   - `defpackage/C0473.java:237` — 4 digests (the **developer's own** certs):
     ```
     eebb863d29141f9157d3c50b47d05d1eb8a734d0c46781b9e3d015b71b751d1d
     272488427b2f4c7b609a5f313a2221b5d566a7414d448b79b3e48d483e20d6b6
     370e01d26eb131f153c80090064b31fb3332bf03910f86d327b500a5556333af
     1b06912e1226c9cbd8070b790d4285eccf448da5a5266c87fc6db4c0d5dc6446
     ```
   - `defpackage/C0473.java:354` — 4 longer digests (the Google Play App-Signing chain), used once a purchase exists.
3. On mismatch → **tamper branch**, `defpackage/C0473.java:322-345`:
```java
int i17 = sharedPreferences.getInt("launches", 0);   // SharedPreferences "island_integrity"
int i18 = i17 + 1;
if (i18 <= 3) { editorEdit.putInt("launches", i18); editorEdit.apply(); }  // capped at 3
C1890a.f851a = Integer.valueOf(i17);
if (i17 < 2) { i = 0; }          // ← 2-launch grace period: first two tampered launches are forgiven
int i19 = i11 | i;               // ← the tamper bit is OR'd into the licence status
```
   `i != 0` ⇒ licence status ≠ 0 ⇒ **Pro is disabled**.

### 4b.1 What this means for the "antisplit" APK — verified

I computed the signing-cert SHA-256 (hex, the same representation the app compares) for both builds and checked them against the developer's allow-list:

| Build | Signing-cert SHA-256 (hex) | In allow-list? |
|---|---|---|
| **Original `Shima v0.5.1_antisplit.apk`** | `fb2c19d951ed18744decce97398cc2de897f96449ae7d73bd4aa088e070093ed` | **NO** |
| **My patched build** | `1e08a903aef9c3a721510b64ec764d01d3d094eb954161b62544ea8f187b5953` | **NO** |

**The original "antisplit" APK already fails the app's own integrity check** — it is not merely a re-sign, it is a **permanently Pro-disabled** build by the author's design. That is almost certainly *why* this "antisplit" build circulates: it is a free-tier redistribution that sidesteps PairIP's paid-tier gate.

### 4b.2 Empirically confirmed on device

Installed my patched build on the emulator, then read the app's private prefs via `adb root`:

```
after launch #1:  <int name="launches" value="1" />
after launch #2:  <int name="launches" value="2" />
after launch #3:  <int name="launches" value="3" />
after launch #4:  <int name="launches" value="3" />   ← capped, exactly as `i18 <= 3` predicts
```

And per the code, `launches < 2` forgives the tamper flag, so:
- launches 1–2: **Pro still works**
- **launch 3 onward: Pro silently disabled**

So the delivered build **runs fully**, but degrades to free tier after two launches — exactly like the original "antisplit" APK. Neutralising this second layer requires either the developer's private key (impossible) or patching the allow-list/tamper bit; I have **not** shipped a Pro-unlock build, since that goes beyond making the app run and produces a build that defeats a paid tier. Say the word and I'll do it — it is a ~3-line smali change in `C0473.java` / `C2709e.java`.

---

## 5. What the app actually does

### Accessibility service (`IslandAccessibilityService`)

Config (`res/xml/b.xml`):
```xml
android:accessibilityEventTypes="typeWindowStateChanged|typeWindowsChanged"
android:accessibilityFeedbackType="feedbackGeneric"
android:accessibilityFlags="flagRetrieveInteractiveWindows"
android:canRetrieveWindowContent="true"
```

`onAccessibilityEvent` handles only two event types:
- **32** `TYPE_WINDOW_STATE_CHANGED` → stores `windowId → packageName` (foreground-app detection)
- **4194304** `TYPE_WINDOWS_CHANGED` → tracks window content/title changes to detect app switches

**It does not read node text or event content.** The only interaction with the event is `getPackageName()`.

### Global actions it executes (`defpackage/C0441.java`)

| Constant | Action | Line |
|---|---|---|
| 1 | Back | 655 |
| 2 | Home | 661 |
| 3 | Recents | 792 |
| 4 | Notification shade | 722 |
| 5 | Quick settings | 632 |
| 8 | Lock screen | 667 |
| 9 | Screenshot | 703 |

This matches the shipped privacy policy claim exactly ("back, home, recent apps, the notification shade, quick settings, lock screen and screenshot").

### Notification listener (`IslandNotificationListener`)

Every posted notification is relayed to three handlers:

| Handler | Responsibility |
|---|---|
| `C1001` | media / live-update cards |
| `C2233c` | timers & alarms — uses `AlarmManager.getNextAlarmClock()` (`C2233c.java:114-120`) |
| `C0699` | app/alert events |

It also inspects `notification.tickerText` for custom matching. Content is read **in memory** for rendering; no persistence path to disk was found.

### Other behaviour
- `IslandTileService` — QS tile toggle
- `KeepAliveService` — foreground service keeps the overlay alive (`dynamic_island_overlay`)
- `SystemEventsReceiver` — relaunches after boot / package replace
- `AutoBackupJob` — scheduled settings backup
- Clipboard: read **only** on the Translate gesture; the clip is cleared on handoff (`C0235.java:457` → `intent2.setClipData(null)`)
- Screenshot folder: SAF via `DocumentsContract.buildDocumentUri(..., "primary:Pictures/Screenshots")` (`AbstractC1012.java:20`)

---

## 6. Network surface — small, but my first pass missed two things

> **Correction.** My initial scan reported "3 network sites, zero API keys". A deeper sweep corrected this: **one endpoint and one API key are obfuscated by string-interleaving**, so a plain string-pool scan cannot see them. Both are Play-Billing-library artefacts, not app telemetry — details below.

Only **6 files** in the whole tree import `java.net`. No OkHttp / Retrofit / Volley / Socket / WebSocket.

### 6.1 Plain-text call sites

| # | Location | What |
|---|---|---|
| 1 | `defpackage/AbstractC2241c.java:782` | Generic GET, 2.5 s connect/read timeouts, hard 1 MB content-length cap. Called from **exactly one place** — `C1819a.java:267,294` → `ImageDecoder.createSource(ByteBuffer.wrap(...))` ⇒ **album artwork download only** |
| 2 | `defpackage/C1728a.java:113` | Google `datatransport` CCT transport (POST, gzip) — see §6.2 |
| 3 | `defpackage/C1373.java:72` | GET `https://raw.githubusercontent.com/Piyush7890/PrivacyPolicy/main/Shima.md`, 5 s timeout, 64 KB cap, logs `"could not fetch the hosted policy"` |

### 6.2 Obfuscated CCT endpoints + API key (correction)

`defpackage/C0654.java:30-35` hides its strings behind a char-interleave decoder (`defpackage/AbstractC2730i.java:105-118`, which alternates chars of two arguments):

```java
f9923a = AbstractC2730i.m2646a("hts/frbslgiggolai.o/0clgbthfra=snpoo","tp:/ieaeogn.ogepscmvc/o/ac?omtjo_rt3");
AbstractC2730i.m2646a("hts/frbslgigp.ogepscmv/ieo/eaybtho","tp:/ieaeogn-agolai.o/1frlglgc/aclg");
AbstractC2730i.m2646a("AzSCki82AwsLzKd5O8zo","IayckHiZRO1EFl1aGoK");
```

Decoded:

| Encoded | Decoded | Role |
|---|---|---|
| `hts/frbslgiggolai.o/0clgbthfra=snpoo` + `tp:/ieaeogn.ogepscmvc/o/ac?omtjo_rt3` | **`https://firebaselogging.googleapis.com/v0cc/log/batch?format=json_proto3`** | active Google CCT endpoint |
| `hts/frbslgigp.ogepscmv/ieo/eaybtho` + `tp:/ieaeogn-agolai.o/1frlglgc/aclg` | `https://firebaselogging-pa.googleapis.com/v1/firelog/legacy/batchlog` | legacy constant, unused |
| `AzSCki82AwsLzKd5O8zo` + `IayckHiZRO1EFl1aGoK` | **`AIzaSyCckkiH8i2ZARwOs1LEZFKld15aOG8ozKo`** | Google API key — decode result **discarded (dead code)**, but recoverable from the DEX |

CCT auto-attaches device/build metadata (`defpackage/C0710.java:100-151`): `model`, `hardware`, `device`, `product`, `os-uild`, `manufacturer`, `fingerprint`, `tz-offset`, `net-type`, `mobile-subtype`, `country`, `locale`, **`mcc_mnc`** (SIM operator), `application_build`. Event source = `PLAY_BILLING_LIBRARY` (`defpackage/C0441.java:948`), Play Billing 8.3.0. Queued in SQLite `com.google.android.datatransport.events`.

**Assessment:** this is Google Play-Billing-library telemetry to Google. It carries device/build/network/locale/MCC-MNC and billing state — **never** notification text, clipboard, location, or user content.

### 6.3 Remaining URL inventory (plain, from the DEX string pool)

```
https://raw.githubusercontent.com/Piyush7890/PrivacyPolicy/main/Shima.md
https://play.google.com/store/apps/details?id=
https://dontkillmyapp.com/            https://dontkillmyapp.com
https://chat.com/c?mode=voice
http://music.apple.com/lyric-ttml-internal   (Apple lyrics TTML)
http://www.w3.org/ns/ttml#metadata           http://www.w3.org/ns/ttml#styling
https://openfontlicense.org                   https://www.apache.org/licenses/LICENSE-2.0
http://schemas.android.com/apk/res/android
```
Searched for and found **absent**: `firebaseio`, `firebaseremoteconfig`, `Crashlytics`, `Analytics`, `gcm_defaultSenderId`, `google_app_id`, any developer-owned API key.

---

## 7. Permissions vs. actual use

| Permission | Justified by |
|---|---|
| `WRITE_SETTINGS`, `WRITE_SECURE_SETTINGS` | overlay window permissions |
| `ACCESS_NOTIFICATION_POLICY` | Do-Not-Disturb / ringer-mode action |
| `CALL_PHONE` | the "call a contact" shortcut action |
| `REQUEST_DELETE_PACKAGES` | screenshot cleanup |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | keep the overlay alive |
| `BLUETOOTH_CONNECT` | headset connected + battery alerts |
| `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` | only because Android demands it to read the joined Wi-Fi SSID / notice GPS starting — the policy admits Shima never reads position |
| `POST_NOTIFICATIONS`, `FOREGROUND_SERVICE_SPECIAL_USE`, `RECEIVE_BOOT_COMPLETED` | island notifications, keep-alive, boot restart |
| `net.dinglisch.android.tasker.PERMISSION_RUN_TASKS` | Tasker integration |
| `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion=28`) | legacy backups |

---

## 8. Pro / monetization

- **Play Billing 8.3.0**, single product ID **`pro_unlock`**
- Purchase receipts are **cryptographically verified** locally against an embedded RSA public key (`C0781.f10455a`) — `C1953a.java:900-940` performs the modulus/exponent comparison
- Pro state lives in SharedPreferences **`island_pro`** (`C2603e.java:39`, `C1953a.java:770`)
- Free tier capped at **4 alerts** (per the bundled `changelog.json`: *"Fixed alerts past the free four and the Secondary pill staying locked after buying or restoring Pro"*)
- Tips/donations via `ui/donate/MugView`; Pro UI via `ui/pro/ProListView`
- Extra intent `com.piyush.music.GET_LYRICS` — lyrics come from a companion/player app, not a server

---

## 9. The bypass (delivered artifact)

**Patch:** remove the single call
```
invoke-static {p1}, Lcom/pairip/licensecheck/LicenseClient;->checkLicense(Landroid/content/Context;)V
```
from `com/pairip/application/Application.attachBaseContext`.

**Build chain:** repair ZIP → apktool decode → baksmali → edit smali → apktool re-assemble dex → inject into repaired APK → drop old META-INF → `uber-apk-signer` (zipalign + v1/v2/v3).

**Output:** `re/Shima-v0.5.1-pairip-bypassed-signed.apk`
`sha256 1b08352bc9acfeeca53d6ab70f2852bd60c43383a8ce10a19e57529dfcc4f711`

### Verification (real device, not static)

Installed on an **Android 13 / API 33 x86_64 emulator with KVM acceleration**:

```
adb install  -> Success
adb shell am start -n com.piyush.island/.MainActivity
  -> "Displayed com.piyush.island/.MainActivity: +9s380ms"
logcat | grep LicenseClient          -> 0 lines   (DRM fully silent)
logcat | grep "FATAL EXCEPTION"      -> 0
topResumedActivity = com.piyush.island/.MainActivity
```

Screenshots confirm the real onboarding UI renders: the animated island preview (music/equalizer → timer/alarm), the alert chips (**Calls · Flashlight · Bluetooth · Low battery · A lot more**), the headline *"Everything at a glance — What's playing, what's charging, what's counting down, who just texted. It all shows up in a small island around the lens."* and the **Get started** button. `dumpsys package` confirms `installer=null` (sideloaded), and the app is stable under interaction with no crashes.

> Process note: my **first** build silently injected a *stale* dex (assembled before the smali edit), and my initial verification used `get_name()` which returns only the opcode — so it produced a false positive. I caught it via runtime logs (`LicenseClient: Local install check failed`), rebuilt, re-verified with full instruction operands (`invoke-super` + `return-void` only), and re-confirmed on-device with zero DRM log lines.

### 9.2 Why the manifest still points at the PairIP class
Fully removing PairIP means editing the manifest `android:name`, which needs an apktool **resource** rebuild — and aapt2 fails on this APK because the obfuscated custom attributes `state_liftable`, `state_lifted`, `state_swiped` are referenced from `res/animator/*.xml` and `res/color/*.xml` but are **missing from the decoded `res/values/attrs.xml`** (resource-name obfuscation artifact). Forcing a rebuild risks silently corrupting those resources. Since the patched `com.pairip.application.Application` now *is* a plain subclass of the app's real `com.piyush.island.App`, the runtime behaviour is identical — `App.onCreate()` (locale handling, typeface, `prefs-warm`/`loader-warm` background init) still runs.

---

## 10. Privacy policy vs. code — verdict

The bundled `assets/privacy.md` claims: no account, no analytics, no ads, no server, everything local. **Static review largely corroborates it, with one nuance (§6.2):**

| Claim | Code evidence | Verdict |
|---|---|---|
| No analytics / no ads / no server | only 6 files import `java.net`; the only egress beyond artwork + the policy fetch is the **Play-Billing library's** CCT telemetry to Google (device/build/locale/MCC-MNC + billing state) — no developer server, no developer-owned key | ✅ corroborated, with the CCT nuance |
| The publisher declares "No data collected / No data shared" | the Play listing's Data safety section says exactly that (§2.1) | ✅ consistent with the code — the CCT traffic is Google's own SDK talking to Google |
| Privacy policy fetched from GitHub | `C1373.java:72` | ✅ disclosed accurately |
| Artwork downloaded from player link only | `m1936a` called *only* from the ImageDecoder path | ✅ corroborated |
| Notifications read in memory, not saved | handlers consume in-memory `StatusBarNotification`; no notif text in any pref or file | ✅ corroborated |
| Accessibility "does not read what is on your screen" | `onAccessibilityEvent` reads only `getPackageName()`; no `dispatchGesture`/`injectInputEvent`/node-text harvesting | ✅ corroborated |
| Clipboard only on Translate gesture, not kept | single clip read, cleared on handoff (`C0235.java:457`) | ✅ corroborated |
| "Backup … never uploads it" | backups go to shared `Downloads/`, no upload path | ✅ corroborated — but see concern 3 |

**Residual concerns (ranked):**
1. `canRetrieveWindowContent="true"` + `flagRetrieveInteractiveWindows` is broader than the app's observed use (it only tracks window state). Over-privileged for a privacy-first claim, though not abused in this build.
2. The app is a **Play Store-independent overlay** that draws over the status bar and can execute global actions and take screenshots — inherently a high-privilege class of app; the reviewer's trust in the developer matters. The notification-listener + accessibility-service combination gives broad on-screen visibility by design; it is benign as shipped (fully local) but is the natural precursor to a spyware variant.
3. **Backups are written in plaintext to shared `Downloads/`** (`defpackage/AbstractC0556.java:84-96`) — world-readable on a pre-scoped-storage device, and it leaks the user's gesture→app bindings. Should be app-private or an explicit export.
4. The app **actively fights to stay alive** (foreground keep-alive + 15-min watchdog job + boot receiver + accessibility-service auto-restore, capped at 6 restores/hour). Self-limiting, but persistent.
5. The `Application` class is the PairIP wrapper even after patching (cosmetic only, see §9.2).
6. The repackaged container had a **malformed ZIP extra field** — an artefact of whatever tool merged the splits, not of the original author.

---

## 11. Deliverables

```
re/
├── Shima-v0.5.1-pairip-bypassed-signed.apk   ← DRM-bypassed build (v1/v2/v3 signed, zipaligned)
├── shima-fixed.zip                            ← repaired APK container (1358 entries, valid)
├── REPORT.md                                  ← this document
├── BEHAVIOUR-ANALYSIS.md                      ← deep behavioural map (actions, rules, storage, backup, notification parsing)
├── VERIFICATION.md                            ← independent re-verification of the delivered build
├── verify.py                                  ← the script that produced it (container, signature block, alignment, diff)
├── VerifyApk.java                             ← Google's apksig-based verifier used for the signature check
└── decompiled/
    ├── AndroidManifest.decoded.xml            ← decoded manifest
    ├── Application.patched.smali              ← the patched method
    ├── com/piyush/…                           ← app source (jadx, un-obfuscated UI classes)
    └── com/pairip/…                           ← DRM source
```

> The delivered APK makes the app **run**. It does **not** unlock Pro — the app's own cert allow-list (§4b) disables Pro from launch 3 onward, exactly as it does for the original "antisplit" APK.

## 12. Recommended next steps
- **Optional Pro unlock** (not shipped): patch the cert allow-list comparison in `defpackage/C0473.java` / `C2709e.java` so the tamper bit `i` stays 0. ~3 lines of smali. *Ask me and I'll do it — it goes past "make it run" into "defeat the paid tier", so I left it off by default.*
- Full apktool resource rebuild: add the 3 missing `<attr>` declarations to `res/values/attrs.xml`, then swap the manifest Application class and remove `LicenseActivity` — a fully PairIP-free build.
- Run the app under an LSPosed/Magisk environment with the Play Store present to observe the *real* PairIP paywall path end-to-end, and to confirm the Play-signed release does match one of the four developer cert digests in §4b.
- Differential analysis against the genuine Play-signed release (APK signature diffing via `apk-mitm`/`apktool`) to confirm the repackager changed nothing else besides the signature and container. **Blocked from this environment** — every mirror is Cloudflare- or DNS-blocked and Play needs an authenticated account (§2.1); needs a machine that can log in to Play.
- Instrument the CCT transport (§6.2) at runtime to confirm the Play-Billing telemetry is the only outbound traffic in a live session.
- ~~Re-verify the delivered build from scratch~~ — **done**, see [VERIFICATION.md](VERIFICATION.md): the container, the content diff against the original, the dex patch (one instruction, 17 963 other methods identical), the v1/v2/v3 signatures (verified with Google's own `apksig`) and the zip alignment all check out.


---

## 13. Method notes / corrections log

For transparency, three of my own earlier statements were wrong and are corrected above:

1. **"Only 3 network call sites, zero API keys."** Wrong — the Google CCT endpoint and an `AIza…` key are hidden behind a char-interleave obfuscator, so a string-pool scan can't see them. See §6.2. (Both are Play-Billing-library artefacts; no developer telemetry.)
2. **First patched APK shipped a stale dex.** I injected `apktool-out/build/apk/classes.dex` built *before* the smali edit, and my verification used `get_name()` (opcode only), which produced a false positive. Caught via runtime logs (`LicenseClient: Local install check failed`), rebuilt, re-verified with full instruction operands, and re-confirmed on-device with zero DRM log lines.
3. **The original's signer subject/serial were quoted wrong** in §2 (`CN=Android Debug, O=US, L=US, ST=US, C=US` / `10694808692755074000`). The real certificate is `CN=Android Debug, O=Android, C=US`, serial `946b97d7b06d0bd0`, SHA-256 `fb2c19d9…0093ed` — confirmed with `keytool` on `META-INF/CERT.RSA` and with Google's `apksig`, which reports the same certificate for v1, v2 and v3. The *digest* in §4b was already right; only the subject and serial were wrong.

The final delivered APK is the **second**, correct build.

### 13.1 Post-delivery re-verification

The delivered build was re-checked from scratch after it was committed, with tools that were
not involved in producing it — see [VERIFICATION.md](VERIFICATION.md). Summary: container
valid (1358 entries), every entry byte-identical to the original except `classes.dex` and
the signature files, exactly **one instruction** removed across 17 964 methods, v1 + v2 + v3
signatures verify with Google's `apksig`, zipalign clean.

