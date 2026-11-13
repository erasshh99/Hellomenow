# Shima v0.5.1 — Deep Behavioural Analysis

*Companion document to `REPORT.md`. Deeper map of what the app does, derived from a systematic sweep of all 3297 decompiled classes.*

Paths: `APP/` = `com/piyush/island/`, `DEF/` = `defpackage/`

---

## 1. Component map

| Component | Class | Role |
|---|---|---|
| Bootstrap | `APP/App.java:47` | Warms SharedPreferences; per-app locale (API 33+) |
| Overlay + gestures | `APP/overlay/IslandAccessibilityService.java` + `DEF/C0181.java` | Draws the island via `WindowManager` type **2032 = `TYPE_ACCESSIBILITY_OVERLAY`** |
| Notification source | `APP/content/IslandNotificationListener.java` | `NotificationListenerService` |
| Persistence | `DEF/C1953a.java`, `C1349.java`, `C2254c.java`, `C2857i.java`, `C2614e.java` | Per-domain SharedPreferences stores |
| Licensing | `DEF/C0750.java`, `C2603e.java`, `C2709e.java`, `C0078.java`, `C2605e.java` | Play Billing + **APK-integrity check** |
| Reliability | `APP/reliability/*` | Foreground keep-alive + 15-min watchdog job + boot receiver |

---

## 2. Accessibility service

### 2.1 Events consumed — only two

```java
// APP/overlay/IslandAccessibilityService.java:125-151
int eventType = accessibilityEvent.getEventType();
if (eventType != 32) {                      // TYPE_WINDOW_STATE_CHANGED
    if (eventType != 4194304) return;       // TYPE_WINDOWS_CHANGED
}
int windowId = accessibilityEvent.getWindowId();
CharSequence packageName = accessibilityEvent.getPackageName();
c1233.f11938a.put(Integer.valueOf(windowId), packageName.toString());
```

**Purpose: foreground-app detection only** (`Map<windowId, packageName>` in `DEF/C1233.java:33`).
Resolution path `DEF/C1233.java:285-296` reads **only** `root.getPackageName()`.
**Absent:** `findAccessibilityNodeInfosBy*`, `getText()`, `getContentDescription()`, node `performAction()`.

### 2.2 Global actions executed (7)

All in `DEF/C0441.java`:

| Line | Call | Action |
|---|---|---|
| 632 | `performGlobalAction(5)` | Quick Settings |
| 655 | `performGlobalAction(1)` | Back |
| 661 | `performGlobalAction(2)` | Home |
| 667 | `performGlobalAction(8)` | Lock Screen |
| 703 | `performGlobalAction(9)` | Take Screenshot |
| 722 | `performGlobalAction(4)` | Notification shade |
| 792 | `performGlobalAction(3)` | Recents |

**No `dispatchGesture()`, no `injectInputEvent()`** anywhere.

### 2.3 Gestures are detected on the app's own overlay

```java
// DEF/C0158.java:230-234
this.f7612a = 40.0f * f;   // swipe distance
this.f7613a = 400.0f * f;  // fling distance
this.f7614a = f * 25.0f;   // fling velocity
```
Kinds (`DEF/EnumC1345.java:52-62`): `tap`, `dtap`, `hold`, `swl`, `swr`, `swd`.

### 2.4 Action registry — 38 actions / 7 categories

`DEF/C1347.java:20-95`; categories `DEF/EnumC1802a.java:44-50`:
```
playpause next prev seekfwd seekback volup voldn mute
search chooseapp approw tasker appshortcut translate chatgptvoice claudevoice
back home recents lock sysset islandset shot shade qs
dnd ringer torch torchup torchdn wifi bt rotate saver brightup brightdn
pillswap widget none
```
Concrete effects (`DEF/C0441.java`):
- **Media** — `transportControls.skipToNext/Previous` (:682,:694), `seekTo(±10000)` (:888-930)
- **Volume** — `audioManager.adjustStreamVolume(3, ±1)` (:1068-1077)
- **Brightness** — writes `Settings.System` screen_brightness (:988-1010)
- **Rotation** — toggles `"accelerometer_rotation"` (:576-591)
- **Torch** — `CameraManager.setTorchMode` on `HandlerThread("island-torch-gesture")` (:727-741,:1013-1050)
- **DND** — `NotificationManager.setInterruptionFilter(...)` (:637-649)
- **Ringer** — cycles `setRingerMode` (:551-570)
- **Third-party** — `Uri.parse("https://chat.com/c?mode=voice").setPackage("com.openai.chatgpt")` (:619); `Intent(ACTION_ASSIST).setClassName("com.anthropic.claude", …)` (:535)
- **Tasker** — broadcast `net.dinglisch.android.tasker.ACTION_TASK` (:600-613)

### 2.5 Overlay window — no `SYSTEM_ALERT_WINDOW` needed

```java
// DEF/C0181.java:378
new WindowManager.LayoutParams(0, 0, 2032, 776, -2);
```
`2032 = TYPE_ACCESSIBILITY_OVERLAY`; `776 = FLAG_LAYOUT_IN_SCREEN|FLAG_LAYOUT_NO_LIMITS|FLAG_NOT_FOCUSABLE`.

---

## 3. Notification listener

### 3.1 Card catalogue (~22 flows)

`DEF/AbstractC1915a.java:14-140`:

| Group | Card ids |
|---|---|
| `power` | `chg1 chg0 batfull batlow saver` |
| `network` | `wifi1 wifi0 air1 air0 hot1 hot0 vpn1 vpn0 nosig` |
| `devices` | `bt1 bt0 usb1 usb0 usbmode` |
| `sound` | `vol ring dnd alarm flash bright rot` |
| `capture` | `shot rec miccam loc` |
| `apps` | `app1 app0` |
| special | `m_media` (MediaSession), `m_live` (nav/call/timer/progress), `m_notif` (queue/reply/split/otp) |

### 3.2 Fields read

```java
// DEF/C0324.java:213-222
Bundle bundle = notification.extras;
int i = notification.flags;
boolean z3 = (262144 & i) != 0;                    // FLAG_ONGOING_EVENT
String str3 = notification.category;
int i2 = bundle.getInt("android.callType", 0);
boolean z5 = bundle.getBoolean("android.showChronometer");
boolean z6 = bundle.getInt("android.progressMax", 0) > 0
          || bundle.getBoolean("android.progressIndeterminate");
```
Also `android.chronometerCountDown`, `android.metrics[].value` (countdown types 1/4/5/6 → `DEF/C0324.java:449-497`), `android.metrics.criticalIndex`, `android.shortCriticalText`, `android.template`, `notification.when`.

- **Media** — `DEF/C0167.java:60-160`: `MediaMetadata` keys `MEDIA_ID/TITLE/ARTIST/ALBUM/DURATION/ALBUM_ART_URI/ART_URI/DISPLAY_ICON_URI`; `PlaybackState` 3/6/8/9/10/11. **Lyrics come from the session's own metadata JSON** (`"lyric"/"rawLyric"`, TTML-parsed `DEF/AbstractC2959i.java`) — never fetched from a network.
- **Battery/charging** — from broadcast receivers, not notifications (`DEF/C1909a.java`)
- **Text content** — walks `notification.contentView/bigContentView/headsUpContentView` RemoteViews for TextViews (`DEF/C3014i.java:117-134`); plus `notification.tickerText` (`DEF/C0386.m3814a`)

### 3.3 Special-cased packages

```java
// DEF/C0324.java:38-44
RIDE:     me.lyft.android, com.olacabs.customer, com.rapido.passenger,
          ee.mtakso.client, com.didiglobal.passenger, com.blablacar.android, com.cabify.rider
DELIVERY: in.swiggy.android, com.application.zomato, com.dd.doordash, com.ubercab.eats,
          com.instacart.client, com.grofers.customerapp, com.zeptoconsumerapp,
          com.deliveroo.orderapp, com.grubhub.android, com.global.foodpanda.android,
          com.talabat, com.glovo, com.postmates.android
// DEF/C1001.java:16-19
SCREENREC: screenrec, screencapture, com.miui.screenrecorder,
          com.coloros.screenrecorder, com.oplus.screenrecorder,
          com.samsung.android.app.smartcapture
```
Purpose: promote ride-hailing / food-delivery / screen-recorder notifications to "live" island cards even without a progress bar (`DEF/C0324.java:274`, `:293-297`).

### 3.4 Notification "reply"
Surfaces the posting app's own `Notification.Action` + `RemoteInput` (`DEF/C2160c.java:87-108`) and fires the notification's **own** `PendingIntent` (`DEF/C0319.java:310`). The app does not forge replies.

---

## 4. Network — complete inventory (incl. obfuscated endpoints)

Only **6 files** in the tree import `java.net`. **No OkHttp / Retrofit / Volley / Socket / WebSocket.**

### 4.1 Google Client Telemetry (CCT) — Play Billing library

Endpoints are string-interleaved. Decoder `DEF/AbstractC2730i.java:105-118` alternates chars of two strings; source `DEF/C0654.java:30-35`:

```java
f9923a = AbstractC2730i.m2646a("hts/frbslgiggolai.o/0clgbthfra=snpoo","tp:/ieaeogn.ogepscmvc/o/ac?omtjo_rt3");
AbstractC2730i.m2646a("hts/frbslgigp.ogepscmv/ieo/eaybtho","tp:/ieaeogn-agolai.o/1frlglgc/aclg");
AbstractC2730i.m2646a("AzSCki82AwsLzKd5O8zo","IayckHiZRO1EFl1aGoK");
```

| Decoded | Role |
|---|---|
| `https://firebaselogging.googleapis.com/v0cc/log/batch?format=json_proto3` | **active** CCT endpoint |
| `https://firebaselogging-pa.googleapis.com/v1/firelog/legacy/batchlog` | legacy constant, unused |
| `AIzaSyCckkiH8i2ZARwOs1LEZFKld15aOG8ozKo` | Google API key — **decode result discarded (dead code)** but recoverable from the DEX |

Transport — `DEF/C1728a.java:107-194`:
```java
httpURLConnection.setRequestMethod("POST");
httpURLConnection.setRequestProperty("User-Agent", "datatransport/3.1.8 android/");
httpURLConnection.setRequestProperty("Content-Encoding", "gzip");
httpURLConnection.setRequestProperty("Content-Type", "application/json");
if (str != null) httpURLConnection.setRequestProperty("X-Goog-Api-Key", str);
```

Auto-attached device metadata — `DEF/C0710.java:100-151`:
```java
c2036aM910a("model", Build.MODEL);   c2036aM910a("hardware", Build.HARDWARE);
c2036aM910a("device", Build.DEVICE); c2036aM910a("product", Build.PRODUCT);
c2036aM910a("os-uild", Build.ID);    c2036aM910a("manufacturer", Build.MANUFACTURER);
c2036aM910a("fingerprint", Build.FINGERPRINT);
map.put("tz-offset", …); map.put("net-type", …); map.put("mobile-subtype", …);
c2036aM910a("country", …); c2036aM910a("locale", …);
c2036aM910a("mcc_mnc", ((TelephonyManager) …).getSimOperator());
```
Event source `PLAY_BILLING_LIBRARY` (`DEF/C0441.java:948`), Play Billing **8.3.0** (`DEF/C0600.java:139`). Queued in SQLite `com.google.android.datatransport.events` (`DEF/C1130.java:34`).

> **Never** notification text, clipboard, location, or user content — only device/build/network/locale + billing state, to Google.

### 4.2 Privacy-policy fetch
`DEF/C1373.java:72` — plain `GET`, 5 s timeouts, 64 KB cap, no body, no user data.

### 4.3 Album-art download (arbitrary host, GET)
`DEF/AbstractC2241c.java:781-790` — 2.5 s timeouts, 1 MB cap. Called **only** from `DEF/C1819a.java:267` (`http`) and `:294` (`https`) for `ALBUM_ART_URI`/`ART_URI`/`DISPLAY_ICON_URI`. URL supplied by the media app; no app data sent.

---

## 5. Pro vs free

### 5.1 Product ID
```java
// DEF/C2605e.java:60-68
c0781.f10455a = "pro_unlock";
c0781.f10456a = "inapp";
```
**`pro_unlock` / `inapp` is the only SKU in the app.**

### 5.2 Purchase verification
- Query owned items `DEF/C2709e.java:103` → `m2608a()` (`CallableC1590`, `"inapp"`, 30 s timeout)
- Filter `DEF/C0078.java:146` → `purchase.m980a().contains("pro_unlock")`
- Verify `DEF/C2709e.java:118+` `m2609a(Purchase)`: requires `purchase.m981a() == 1` (PURCHASED), Base64-decodes the signature, **RSA-verifies** it against a hard-coded modulus/exponent (`AbstractC1540.m5308a`, `C2662e.f5724a/f5725a`), then checks `packageName`, `productIds.contains("pro_unlock")`, `purchaseState`
- Persist `purchase` + `signature` into SharedPreferences `island_pro`
- **AndroidKeyStore HMAC seal** of the licence (`DEF/C2603e.java:56-70`):
```java
Mac mac = Mac.getInstance("HmacSHA256");
KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
Key key = keyStore.getKey("island.pro.seal", null);
KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256", "AndroidKeyStore");
keyGenerator.init(new KeyGenParameterSpec.Builder("island.pro.seal", 4).build());
mac.update(str.getBytes(charset)); mac.update((byte) 0); mac.update(c0781.f10456a.getBytes(charset));
return Base64.getEncoder().encodeToString(mac.doFinal());
```
stored under `"seal"`; removed on revocation (`DEF/C0078.java:170-172`).

### 5.3 Licence status codes — `DEF/C0750.java:59-60`
```java
this.f10355a = ((string2 != null && (string = sharedPreferences.getString("signature", null)) != null)
 ? new C0781(string2, string) : null) != null ? 0 : 4096;
```
**Pro == status 0.** Others: `512` signature mismatch · `1024` unparsable JSON · `2048` wrong product · `8192` no valid purchase (`C0078.java:174`) · `32768` not owned.

### 5.4 Pro-gated features

| Feature | Gate | Location |
|---|---|---|
| Swipe gestures `swl`/`swr`/`swd` | `EnumC1345.f12524a`; `if ((z \|\| (c1346.f12532a && !str.equals("widget"))) && !this.f751a.m4436a())` | `DEF/EnumC1345.java:58-62`, `C1865a.java:561` |
| Actions `search`, `approw`, `appshortcut`, `pillswap` (+`widget` exempt) | `C1346.f12532a` pro flag, default `false` | `DEF/C1346.java:47`, `C1347.java:52-95` |
| **Max 4 alert cards** (free) | `return (c1916a == null \|\| !c1916a.f927a) && m771a(map) >= 4;` | `DEF/C1953a.java:118-129` |
| Backup (auto + manual) | `if (!c0552M3962a.f9344a \|\| !C0750.f10349a.m5085a(this).m4436a()) return false;` | `APP/backup/AutoBackupJob.java:46`; `DEF/C0473.java:236`,`:532` |
| Companion pill | `if (((C0750) companionFragment.f2685a.getValue()).m4436a())` | `DEF/C1728a.java:216` |
| Home-screen widget | `if (c1461M5147a.f13000a && !c1461M5147a.f12998a.m4436a())` | `DEF/C1453.java:571`,`:616`,`:661` |
| Alert-detail enable-card | `if (C0750.f10349a.m5085a(…).m4436a())` | `DEF/C1926a.java:119` |

**Free:** tap / double-tap / long-press; all non-pro actions (media, volume, brightness, torch, DND, ringer, rotation, global nav, system settings, translate, app launch, tasker); up to 4 alert cards; `app0`/`app1`.

### 5.5 APK-integrity / licence verification (inlined in 6 places)
`DEF/C0169.java:186+`, `C0175.java:230+`, `C0473.java:236+`, `C1953a.java:654+`, `C1349.java:672+`, `C1908a.java:971+`

1. Read the app's **own** APK signing-cert SHA-256 digests by parsing the **APK Signing Block**: `AbstractC0140.m3472a(applicationInfo.sourceDir)` (`DEF/AbstractC0140.java:1859-1879` → `m3465a` parses the v2/v3 signing block, `f7531a` is the APK Sig Block 42 magic).
2. Compare against **two hard-coded allow-lists**, constant-time (length check, then XOR-accumulate):
   - `DEF/C0473.java:237` — 4 digests (the *developer's* own certs):
     ```
     eebb863d29141f9157d3c50b47d05d1eb8a734d0c46781b9e3d015b71b751d1d
     272488427b2f4c7b609a5f313a2221b5d566a7414d448b79b3e48d483e20d6b6
     370e01d26eb131f153c80090064b31fb3332bf03910f86d327b500a5556333af
     1b06912e1226c9cbd8070b790d4285eccf448da5a5266c87fc6db4c0d5dc6446
     ```
   - `DEF/C0473.java:354` — 4 longer digests (Google Play App Signing chain), used once a purchase exists.
3. On mismatch → **tamper branch**:
```java
// DEF/C0473.java:322-345
int i17 = sharedPreferences.getInt("launches", 0);   // "island_integrity"
int i18 = i17 + 1;
if (i18 <= 3) { editorEdit.putInt("launches", i18); editorEdit.apply(); }
C1890a.f851a = Integer.valueOf(i17);
if (i17 < 2) { i = 0; }          // ← 2-launch grace period
int i19 = i11 | i;               // ← tamper bit OR'd into licence status
```
   - `i != 0` ⇒ status ≠ 0 ⇒ **Pro disabled**
   - The counter is **capped at 3** (`i18 <= 3`)
   - The first **2** launches are forgiven (`i17 < 2` resets `i` to 0) — a deliberate grace window so a legitimate re-sign on a dev machine isn't punished immediately
4. Purchase JSON is RSA-verified (SHA-1 + PKCS#1 padding, `DEF/C0473.java:398-510`).

### 5.6 Upsell views are pure artwork
`APP/ui/pro/ProListView.java:31-37` is a `NestedScrollView` that swallows touches behind a paywall; `MugView` (999 lines) and `ProStageView` (943 lines) are animated artwork. **The real purchase CTA is `DEF/C2605e.java:60-68`.**

---

## 6. Rules, triggers, conditions

- **Rule model** — `DEF/C0924.java:16-33`: `Rule(id: UUID, on: Boolean, conditions: List<Condition>, action: String, app: String?, task: String?, widget: ComponentName?, row: List, shortcut: C2745i?)`
- **Stored per gesture** in `island_gestures` — `DEF/EnumC1345.java:86-113` (`"<gesture>.app"`, `".row"`, `".rules"`, `".shortcut"`, `".task"`), read/written `DEF/C1349.java:165`,`:286`,`:305`,`:479`,`:506-508`,`:540`
- **20 trigger conditions** — `DEF/EnumC2161c.java:44-83`: `pill`(on/off) `music`(playing/notplaying) `player` `headphones` `ringer`(sound/vibrate/silent) `call` `dnd` `charging` `battery` `saver` `torch` `locked` `orientation`(upright/sideways) `rotate` `wifi` `btdevice` `airplane` `app` `home` `fullscreen`
- `Condition(id, value)` = `DEF/C2158c.java`; editor `APP/ui/rules/RuleEditorFragment.java` (apps picker `:287-293`; warns on `LOCKED` + `"on"` `:296-329`)
- Action picker `APP/ui/gesture/ActionPickerFragment.java` — 8 category pages; pro-gated actions without a licence write `"pendingPro"` into the draft and show the upsell.

---

## 7. Clipboard / Translate

Trigger = user performs a gesture whose action id is `"translate"`:

```java
// DEF/C0158.java:294-309
if (str3.equals("translate")) {
    if (!c0181.f7817a && (c0162 = c0181.f7793a) != null
        && AbstractC2093c.m1579a((IslandAccessibilityService) c0181.f7816a.f13675a,
                                 new Intent("android.intent.action.TRANSLATE"))) {
        c0181.f7817a = true;
        c0181.m3527a();
        c0162.postDelayed(c0181.f7854a, 1000L);   // 1-second cancel window
        C0173 c0173 = new C0173(c0162, c0181, z3, view);
        if (c0162.hasWindowFocus()) c0173.mo340a();
        else { /* defer until the island window regains focus */ }
    }
}
```
```java
// DEF/C0173.java:62-63, 77-89
ClipboardManager clipboardManager = (ClipboardManager) islandAccessibilityService.getSystemService(ClipboardManager.class);
c0897 = clipboardManager != null ? clipboardManager.getPrimaryClip() : null;
...
if (text != null && AbstractC2093c.m1606a(islandAccessibilityService,
      Intent.createChooser(new Intent("android.intent.action.TRANSLATE")
          .putExtra("android.intent.extra.TEXT", text), null)) && z) {
    view.performHapticFeedback(6);
}
```
Reads **only** `ClipData.getItemAt(0).getText()`, **only after an explicit user gesture**, hands it to the OS `ACTION_TRANSLATE` chooser. The 1 s delay + focus deferral stop the island stealing focus before the clipboard is captured. **No network transmission by Shima.**

Other clipboard use is benign: write-only copy in the Reliability screen (`DEF/ViewOnClickListenerC2004a.java:67-70`), copy of a `content://` URI on screenshot delete (`DEF/C0388.java:64-72`), `setClipData(null)` when launching a shortcut (`DEF/C0235.java:457`).

---

## 8. Screenshot feature

**There is NO screen capture.** No `MediaProjection`, `ImageReader`, `VirtualDisplay`, or `createScreenCaptureIntent` anywhere.

**(a) `shot` gesture action** → `performGlobalAction(9)` (`DEF/C0441.java:702-704`) — the standard Android a11y screenshot action; the OS takes the shot.

**(b) `shot` card** → MediaStore observer over `Pictures/Screenshots`:
```java
// DEF/AbstractC1012.java:20
public static final Uri f11206a = DocumentsContract.buildDocumentUri(
    "com.android.externalstorage.documents", "primary:" + Environment.DIRECTORY_PICTURES + "/Screenshots");
```
- Permission = **SAF persisted read grant**; `m4664a` finds it via `getPersistedUriPermissions()` (`DEF/AbstractC1012.java:146-165`); `m4661a` verifies the tree is named `Screenshots` (`:50-87`)
- Observer `DEF/C2714e.java:405` → `registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, c1014)`
- Query `DEF/C1015.java:38-56` selects `"_id","date_added","_display_name","relative_path"/"_data","width","height","_size"` + a freshness flag (`(now/1000) - date_added > 10`)
- Thumbnail via `ContentResolver.loadThumbnail(uri, size, null)` / `openInputStream` (`DEF/AbstractC0140.java:1887-1900`)

**Deletion** — `APP/ui/screenshot/DeleteRequestActivity.java:40-58` (transparent trampoline):
```java
if (i >= 30) {
    startIntentSenderForResult(MediaStore.createDeleteRequest(getContentResolver(),
        Collections.singletonList(ContentUris.withAppendedId(
            MediaStore.Images.Media.getContentUri(MediaStore.getVolumeName(uriWithAppendedId)),
            ContentUris.parseId(uriWithAppendedId)))).getIntentSender(), 1, null, 0, 0, 0);
} else { contentResolver.delete(uriWithAppendedId, null, null); }
```
→ **user-consented** delete (API 30+ shows the system consent dialog).

---

## 9. Local storage

### 9.1 SharedPreferences (all app-private)

| Pref file | Contents | Source |
|---|---|---|
| `island_pro` | **`purchase`**, **`signature`**, `seal` (HMAC-SHA256 / AndroidKeyStore) | `DEF/C2603e.java:39`, `C2709e.java` |
| `island_gestures` | per-gesture rules/targets/shortcuts, `feel.haptic`, `feel.unlock` | `DEF/C1349.java:105,165,286,305` |
| `island_decks` | `<id>.name`, `<id>.cards`, ordering | `DEF/C2254c.java:50` |
| `island_alerts` | enabled cards, `<card>.x.<mode>.<opt>`, `expanded_groups`, `apps.list`, `apps.mode`, `app0`/`app1` | `DEF/C1953a.java:60` |
| `island_style_px` | `shape`, `width_px`, `height_px`, `x`, `rot`, `morph_*`, `corner_percent`, `shadow*` | `DEF/C2857i.java:58` |
| `island_overlay` | `enabled`, `companion_pill`, `companion_progress_ring`, `companion_ring_style`, `companion_separation`, `companion_side_right`, `hide_empty`, `hide_full_screen`, `hide_lock_screen`, `hidden_apps` | `DEF/C2614e.java:90` |
| `appearance` | `palette`, `dynamic`, `mode`, `custom_seed`, `custom_seeds` | `DEF/C0486.java:95` |
| `custom_colors` | custom colour list | `DEF/C1777a.java` case 7 |
| `island_backup` | `sections`, `auto`, `frequency`, `last_backup_at` | `DEF/C0486.java:50,90,235-239` |
| `island_integrity` | **`launches`** (tamper counter, ≤3) | `DEF/C0473.java:326`, `C0169.java:280` |
| `island_reliability` | `keep_alive`, `hide_from_recents` | `DEF/C0878.java:33` |
| `island_review` | Play review prompt state | `DEF/C0906.java:50` |
| `island_recent_apps` | `packages` (comma list of recently used packages) | `DEF/C1025.java:323` |
| `island_update` | `offered_version`, `offered_at` | `DEF/C0481.java:71` |
| `island_widget_ids` | home-screen widget ids | `DEF/C1468.java:59` |
| `island_onboarding` | onboarding flags | `DEF/C2603e.java:42` |
| `accessibility_notice` | `accessibility_ever_connected` | `APP/overlay/IslandAccessibilityService.java:203-210` |
| `changelog` | `lastSeenVersion`, `sensuCardClosed` | `DEF/C0725.java:154` |
| `com.google.android.gms.signin` | GMS account cache (library) | `DEF/C2839i.java:25` |

**Only sensitive item:** the Play purchase token + RSA signature in `island_pro`, protected by the AndroidKeyStore HMAC seal. No notification text, clipboard, location, or credentials are persisted.

### 9.2 Files & databases

| Path | Content | Source |
|---|---|---|
| `noBackupFilesDir/reliability.log` | TSV reliability events, trimmed to 12 KB | `DEF/C0877.java:38-72` |
| `noBackupFilesDir/reliability.properties` | `a11y.wanted`, `a11y.restoreWindow`, `a11y.restoreCount` (max 6/h), `a11y.writtenAt`, `a11y.dropLogged`, `listener.forcedAt` | `DEF/C0875.java`, `C0874.java`, `C1777a.java` |
| `noBackupFilesDir/app_snapshots/<pkg>.json` | `{"label","version","source","play","bytes","category","adaptive","sourceDir"}` for installed media apps | `DEF/C0469.java:213` |
| `noBackupFilesDir/app_snapshots/<pkg>.icon` | WEBP/JPEG icon | `DEF/C0469.java:188-212` |
| `Downloads/<app name>/*.islbak` | **backup JSON** | `DEF/AbstractC0556.java:84-101` |
| SQLite `com.google.android.datatransport.events` | queued Google CCT events (only DB in the app) | `DEF/C1130.java:34` |

---

## 10. Backup feature

### 10.1 Content (`DEF/C0473.java:533-647`)
```java
JSONObject jSONObject3 = new JSONObject();
for (EnumC0558 enumC0558 : set) {                       // selected sections
    JSONObject jSONObject4 = new JSONObject();
    for (C2684e c2684e2 : enumC0558.f9374a) {           // one entry per SharedPreferences file
        String str15 = c2684e2.f5762a;                  // e.g. "island_gestures"
        SharedPreferences sharedPreferences3 = …getSharedPreferences(str15, 0);
        JSONObject jSONObject5 = new JSONObject();
        for (Map.Entry<String,?> entry : sharedPreferences3.getAll().entrySet()) {
            if (((Boolean) c2684e2.f5763a.mo341a(entry.getKey())).booleanValue())
                jSONObject5.put(entry.getKey(), new JSONObject().put("t", type).put("v", value)); // b/i/l/f/s/ss
        }
        jSONObject4.put(str15, jSONObject5);
    }
    jSONObject3.put(enumC0558.f9370a, jSONObject4);
}
String string3 = new JSONObject().put("format", 1).put("createdAt", System.currentTimeMillis())
    .put("appVersion", str).put("sections", jSONObject3).toString(2);
```

### 10.2 Section → prefs mapping (`DEF/EnumC0558.java:44-83`)

| Section | Preference file(s) | Filter |
|---|---|---|
| `themes` | `appearance`, `island_alerts` | `island_alerts` excludes `custom_colors` |
| `island` | `island_style_px` | all |
| `gestures` | `island_gestures`, `island_decks` | all |
| `triggers` | `island_alerts` | excludes `custom_colors` |
| `app` | `island_overlay` | all |

> **`island_pro` (purchase + signature) is in NO section → the licence is never exported.**

### 10.3 Destination — plaintext in shared Downloads
```java
// DEF/AbstractC0556.java:84-96
File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                      context.getString(R.string.k4));
if (!file.isDirectory() && !file.mkdirs()) { … }
File file2 = new File(file, str);
FileOutputStream fileOutputStream = new FileOutputStream(file2);
AbstractC1540.m5313a(fileOutputStream, str2, charset);
fileOutputStream.close();
MediaScannerConnection.scanFile(context, new String[]{file2.getPath()}, new String[]{"application/octet-stream"}, null);
```
Filename `<appname>-auto-<yyyyMMdd-HHmm>.islbak` (auto) / `<appname>-backup-…` (manual) — `DEF/C0473.java:619`, format `DEF/AbstractC0556.java:30`. API 29+ uses MediaStore Downloads (`m4062a`, `:102-127`, `mime_type = application/octet-stream`).

### 10.4 Scheduling / pruning / restore
- `APP/backup/AutoBackupJob.java:23` `Duration.ofHours(2)`; `:44-51` requires `auto && Pro`
- Frequency `daily`/`weekly`/`monthly`, keep 7/5/3 (`DEF/EnumC0557.java:38-44`)
- Prune after auto-backup: `AbstractC0556.m4059a` (filesystem) / `m4060a` (MediaStore delete) — `DEF/C0473.java:633-644`
- Restore: `DEF/C1943a.java:126-205` (SAF read, `format` must be 1) → `DEF/C0554.java:80-140` writes back with type coercion

---

## 11. Reliability subsystem

| File | Behaviour |
|---|---|
| `APP/reliability/KeepAliveService.java:59-111` | Foreground service (id 2, channel `"keep_alive"`, low importance); `START_STICKY`/`START_NOT_STICKY`; 2 actions: open `MainActivity` (extra `com.piyush.island.extra.OPEN_RELIABILITY`), open channel settings |
| `APP/reliability/WatchdogJob.java:36-39` | `JobService` → `DEF/C2714e.java:731-750` → start keep-alive, reschedule watchdog, restore a11y service; `jobFinished(params, false)` |
| `APP/reliability/SystemEventsReceiver.java:18` | `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `QUICKBOOT_POWERON`, `com.htc.intent.action.QUICKBOOT_POWERON` |
| `DEF/C1437.java:23-39` | Schedules `WatchdogJob` id 6780: `.setPeriodic(900000L, 300000L).setPersisted(true)` — only if the island is enabled, else cancels 6780 |
| `DEF/C0877.java:38-72` | Appends to `reliability.log` (12 KB cap) |
| `DEF/C1777a.java` cases 0-3 | `a11y.restoreCount` capped at **6 per hour**; `listener.forcedAt` 10-min cooldown |

---

## 12. Abuse / exfiltration assessment

### 12.1 Accessibility-service abuse — legitimate, non-abusive
Exactly three uses: (1) hosting the island overlay via `TYPE_ACCESSIBILITY_OVERLAY` (the standard technique avoiding `SYSTEM_ALERT_WINDOW`); (2) foreground-app detection for per-app rules (`getPackageName()` only); (3) 7 user-visible `performGlobalAction()` calls bound to explicit user gestures.
**Absent:** `dispatchGesture()`, `injectInputEvent()`, node-text harvesting, node `performAction()`, background clipboard polling, keystroke capture.

### 12.2 Keylogging — none
No `KeyEvent` capture in the service, no `InputConnection` interception, no `onKeyEvent` override. The only `KeyEvent` code (`DEF/C1093.java`, `C0441.java:104-121`) is ordinary TextView span handling in the app's own UI.

### 12.3 Screen scraping — none
No `MediaProjection` / `ImageReader` / `VirtualDisplay` / `createScreenCaptureIntent`.

### 12.4 Data exfiltration — no user content leaves the device
Egress is only: (a) Google CCT billing telemetry (device/build/network/locale + billing state) from the bundled Play Billing Library; (b) a `GET` of the public policy markdown from GitHub raw; (c) album-art `GET`s to the URL supplied by the media app. Notification text, clipboard text, location, installed-app lists, gestures, rules and the licence never leave the device.

### 12.5 Residual risks
1. **Notification-listener + accessibility-service combo** gives broad on-screen visibility by design. Benign as shipped (fully local), but it is the natural precursor to a spyware variant — any future network egress would flip this.
2. **Plaintext backup in shared `Downloads/`** is world-readable and leaks the user's gesture→app bindings. Recommendation: app-private storage or explicit user export.
3. **Dead Google API key literal** `AIzaSyCckkiH8i2ZARwOs1LEZFKld15aOG8ozKo` is recoverable from `DEF/C0654.java:34` (Google's Play-Billing key, currently unreferenced).
4. **`performGlobalAction(9)`** needs no extra permission on API 30+; combined with the MediaStore observer it is harmless here, but adding `MEDIA_PROJECTION` would be a step change.
5. `island_integrity` launch counter + `a11y.restoreCount` (max 6/h) show the app deliberately self-limits service-restart thrash — but it does actively try to keep its accessibility service alive.

---

## 13. File → responsibility index

| Path | Responsibility |
|---|---|
| `APP/App.java` | Bootstrap, per-app locale, pref warm-up |
| `APP/MainActivity.java` | Single-Activity nav host, tabs |
| `APP/overlay/IslandAccessibilityService.java` | A11y service: window→package tracking, overlay lifecycle |
| `APP/overlay/IslandTileService.java` | QS tile → toggle island |
| `APP/content/IslandNotificationListener.java` | Notification listener; builds all card flows |
| `APP/reliability/*` | Keep-alive, watchdog, boot receiver |
| `APP/backup/AutoBackupJob.java` | Scheduled auto-backup (Pro-gated) |
| `APP/ui/pro/ProListView.java`, `ui/donate/MugView.java`, `ui/pro/ProStageView.java` | Pure upsell/donation artwork |
| `APP/ui/rules/RuleEditorFragment.java` | Rule editor UI |
| `APP/ui/gesture/ActionPickerFragment.java` | Action picker UI (8 pages, pro-upsell) |
| `APP/ui/screenshot/DeleteRequestActivity.java` | Consented MediaStore delete trampoline |
| `DEF/C0441.java` | **Gesture-action dispatcher** (all `performGlobalAction` + device controls) |
| `DEF/C1347.java` / `C1346.java` | Action registry + model (pro / permission / repeatEveryMs) |
| `DEF/C0158.java` | Gesture recognition + translate trigger |
| `DEF/C0181.java` | Island overlay window manager (type 2032) |
| `DEF/C1233.java` | Foreground-app detection (windowId → package) |
| `DEF/C0324.java` / `C1001.java` | Notification classification + special-cased packages |
| `DEF/C0167.java` | MediaSession metadata / playback |
| `DEF/C0473.java` / `AbstractC0556.java` / `C1943a.java` / `C0554.java` | Backup write → Downloads / read / restore / prune |
| `DEF/C0750.java` / `C2603e.java` / `C2709e.java` / `C0078.java` / `C2605e.java` | Licence state, key seal, RSA purchase verification, product `pro_unlock`, **cert allow-lists** |
| `DEF/C0710.java` / `C1728a.java` / `C0654.java` / `C1247.java` | Google CCT transport + obfuscated endpoints + API key |
| `DEF/C1373.java` | Privacy-policy fetch (GitHub raw) |
| `DEF/AbstractC2241c.java:781` | Generic byte download (album art only) |
| `DEF/C0173.java` | Clipboard read → `ACTION_TRANSLATE` |
| `DEF/C0469.java` | `app_snapshots` cache of installed media apps |
| `DEF/C0877.java` / `C0878.java` / `C1437.java` / `C1777a.java` | Reliability counters / log / service restore |
| `DEF/EnumC1345.java` / `EnumC2161c.java` / `EnumC1802a.java` / `EnumC1949a.java` / `EnumC0558.java` | Gesture kinds, trigger conditions, action categories, permission types, backup sections |
