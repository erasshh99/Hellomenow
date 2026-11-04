# 08 · Security review

The findings below are about the **original** app and about the **rebuild**, and every finding
that applies to the rebuild was fixed.

## 8.1 Summary

| # | Finding | Severity | Original | Rebuild |
|---|---------|----------|----------|---------|
| 1 | The Pro entitlement is stored next to the ordinary settings | medium | ✅ kept apart (`island_pro.xml`, excluded from backup) | ✅ fixed — separate file, excluded from backup, not in the export |
| 2 | Six permissions are declared but never used | low | present | removed |
| 3 | The accessibility service asks for window-content access | low | yes | yes — but only the window size is read, never the content |
| 4 | The accessibility overlay can read the user's screen area | by design | yes | yes — this is what the app is |
| 5 | Three non-public platform methods are called | low | yes | yes — each one wrapped, each one read-only |
| 6 | No backup of the purchase, no offline entitlement bypass | info | ✅ | ✅ |

## 8.2 Finding 1 — the Pro entitlement must not travel

**Original.** The Pro flag lives in its own `SharedPreferences` file, `island_pro.xml`, and both
backup rule files exclude it:

```xml
<full-backup-content>
    <exclude domain="sharedpref" path="island_pro.xml" />
</full-backup-content>
```

```xml
<data-extraction-rules>
    <cloud-backup><exclude domain="sharedpref" path="island_pro.xml" /></cloud-backup>
    <device-transfer><exclude domain="sharedpref" path="island_pro.xml" /></device-transfer>
</data-extraction-rules>
```

**Rebuild.** The first version of the rebuild kept the flag inside the one settings file, which
would have let the Android cloud backup carry the entitlement to another device — a free Pro
unlock for anyone restoring a backup. This was changed during the rebuild:

* the flag now lives in its own file, `island_pro` (`SettingsStore.PRO_PREFS_NAME`),
* both backup rule files exclude it,
* the JSON export written by the **Backup → Export** button does not contain it either,
* the import ignores any `proUnlocked` key that a hand-edited file might contain.

The entitlement itself still comes only from the Play Billing result; the stored flag is a cache
of that result, and it is re-checked with `queryPurchasesAsync` whenever the Pro screen is opened.

## 8.3 Finding 2 — permissions that are not used

The original declares location, bluetooth, storage and secure-settings permissions that its own
code never uses; they arrive through `play-services-location` and the legacy storage paths.

The rebuild declares only what it uses. The table in
[02 · Manifest analysis](02-manifest-analysis.md) lists every permission side by side.

This matters for the user in a concrete way: the Play Store shows the permission list on the
listing page, and a user who is careful about permissions sees exactly what the app needs.

## 8.4 Finding 3 — the accessibility service

An accessibility service is a very powerful permission: it can see the windows of other apps and
act on the screen. It is also the only way an app can draw an overlay that sits above every other
app, which is exactly what a Dynamic Island has to do.

The service configuration of both versions:

| Setting | Value | Consequence |
|---------|-------|-------------|
| `accessibilityEventTypes` | `typeWindowStateChanged\|typeWindowsChanged` | the service is told when another app comes to the front — nothing more |
| `accessibilityFeedbackType` | `feedbackGeneric` | no spoken feedback |
| `accessibilityFlags` | `flagDefault\|flagRetrieveInteractiveWindows` | the window bounds are available, so a full-screen app can be recognised |
| `canRetrieveWindowContent` | `true` | the *content* could be read — the rebuild reads only the window size |
| `canPerformGestures` | `true` | the island can run the gestures the user bound to it |
| `notificationTimeout` | `0` | the service is not throttled |

The rebuild reads exactly one thing out of the window: its bounds, to decide whether the app in
the foreground is full screen. It never reads a node, a label or a text.

The service description that the user sees in the accessibility settings says so in plain words.

## 8.5 Exported components

| Component | Exported | Assessment |
|-----------|----------|------------|
| `MainActivity` | yes, with `LAUNCHER` | correct, it is the app's front door |
| `IslandAccessibilityService` | yes, guarded by `BIND_ACCESSIBILITY_SERVICE` | correct: only the system can bind it |
| `IslandTileService` | yes, guarded by `BIND_QUICK_SETTINGS_TILE` | correct: only the system can bind it |
| `IslandNotificationListener` | no | correct, it is bound by the system through the listener permission |
| `KeepAliveService`, `WatchdogJob`, `AutoBackupJob` | no | correct |
| `SystemEventsReceiver` | yes | correct, it has to receive the boot broadcast. It only starts services, it does not act on the intent |
| `DeleteRequestActivity` | no | correct |
| `com.pairip.licensecheck.LicenseActivity` | yes | injected by Play App Signing, not app code |

## 8.6 Input handling

| Input | Where it comes from | What happens |
|-------|--------------------|--------------|
| The JSON backup file | the user picks a file | every field is read with `optX` and falls back to the current value; a malformed file is rejected and nothing is changed |
| The `uri` extra of `DeleteRequestActivity` | the app itself | it is parsed with `Uri.parse` inside a `runCatching` and it is only ever handed to `MediaStore.createDeleteRequest` |
| The package list of an alert filter | the installed apps | stored as plain package names |
| The gesture bindings | the user | stored as `app:<package>`, `widget:<id>`, `system:<id>` or `tasker:<package>:<task>`; the executor only ever acts on these four shapes |
| The notification text | other apps | rendered as text on the card, never executed |

There is no `WebView` in the app, so there is no JavaScript bridge and no remote content. There
is no `SQLite` and no database, so there is no injection surface in the storage layer.

## 8.7 The billing flow

The Pro purchase uses the standard Play Billing flow:

1. `BillingClient.newBuilder(...).enablePendingPurchases(PendingPurchasesParams...)` — the
   pending-purchases API that Google requires since 2023.
2. `queryProductDetailsAsync` for the product `shima_pro`.
3. `launchBillingFlow` with the `ProductDetails` the store returned.
4. The result arrives in the `PurchasesUpdatedListener`; a purchase that is `PURCHASED` and not
   yet acknowledged is acknowledged with `acknowledgePurchase`, and only then is the entitlement
   stored.
5. `queryPurchasesAsync` restores the entitlement if the user reinstalls the app.

The price that is shown on the button is the `formattedPrice` the store returned — the app never
makes up a price.

There is no way to unlock Pro without a purchase: there is no server that could grant it, no
receipt check that could be spoofed, and no offline fallback that would hand out the entitlement
on its own.

## 8.8 What the app cannot do

* It cannot read the content of another app's screen (it reads window sizes only).
* It cannot send anything anywhere (there is no server and no analytics).
* It cannot switch the camera or the microphone on; it only notices that another app is using
  them.
* It cannot delete a photo without the system's own confirmation sheet.
* It cannot take a screenshot on its own; the screenshot gesture asks the system to do it.

## 8.9 Privacy summary

| Question | Answer |
|----------|--------|
| Is there an account? | No |
| Is there an analytics SDK? | No |
| Is there an advertising identifier? | No |
| Is there a crash reporter? | No |
| Is there a server? | No |
| What leaves the phone? | Only the Google Play billing and update traffic, which the Play libraries handle |
| What is stored? | The configuration, in the app's own sandbox |
| Can the configuration be backed up? | Yes, except the Pro entitlement |
