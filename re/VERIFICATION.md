# Independent verification of the delivered build

A second, from-scratch verification of `Shima-v0.5.1-pairip-bypassed-signed.apk`, run
after the build was committed, using tools that were not involved in producing it.
The script that produced these numbers is [`verify.py`](verify.py) and is checked in,
so the claims below can be re-run at any time:

```sh
python3 re/verify.py                     # defaults to the delivered build
python3 re/verify.py <apk> <original>
```

## 1. Container

| Check | Result |
|---|---|
| `zipfile.ZipFile` opens it | yes |
| `ZipFile.testzip()` | `None` (no CRC errors) |
| Entry count | 1358 — identical to the original |
| Duplicate entry names | 0 |
| Original opens with `zipfile` | **no** — `BadZipFile: Corrupt extra field 3500 (size=729)` |

The corrupt extra field is in the **original** container only. The repaired container
(`shima-fixed.zip`) and the delivered APK are both clean.

## 2. Content diff against the original

Every entry was compared by CRC against the original's central directory (read with a
tolerant parser, since the original will not load):

| | |
|---|---|
| Entries only in the original | `META-INF/CERT.RSA`, `META-INF/CERT.SF` |
| Entries only in the delivered build | `META-INF/ANDROIDD.RSA`, `META-INF/ANDROIDD.SF` |
| Entries with changed content | `classes.dex`, `META-INF/MANIFEST.MF` |
| **Entries byte-identical to the original** | **1354** |

Everything else — every resource, every asset, `resources.arsc` — is **byte-identical** to
the original. The two signature files were renamed because the build was re-signed under
the key alias `ANDROIDD`.

## 3. The dex patch

The two `classes.dex` files were disassembled and compared **method by method** across
all classes:

| | |
|---|---|
| Classes in the original | 3297 |
| Classes in the delivered build | 3297 — none added, none removed |
| Methods compared | 17 964 |
| Methods with an identical instruction stream | 17 964 |

Exactly one instruction differs in the entire dex — the call that was meant to be removed:

```
class  : com.pairip.application.Application
method : attachBaseContext(Landroid/content/Context;)V
orig   : invoke-static v1, Lcom/pairip/licensecheck/LicenseClient;->checkLicense(Landroid/content/Context;)V
new    : (removed)
```

Nothing else in the 3.4 MB of app code was touched. This is the check that matters after
the stale-dex incident recorded in `REPORT.md` §13 — the delivered build is not stale.

The rebuilt dex is larger than the original (4 253 476 vs 3 386 968 bytes) because the
smali→dex round-trip does not reproduce R8's compact encoding, but it is semantically
identical apart from the removed call.

## 4. Signature

Verified with **Google's own implementation** (`com.android.tools.build:apksig:8.7.3`,
the library behind `apksigner`), not a hand-rolled re-implementation:

```
=== re/Shima-v0.5.1-pairip-bypassed-signed.apk ===
  overall verified   : true
  v1 (JAR) verified  : true
  v2 verified        : true
  v3 verified        : true
  signers            : 1
    subject : CN=Android Debug, OU=Android, O=US, L=US, ST=US, C=US
    serial  : 56c1a15
    valid   : Sun Oct 23 20:10:05 UTC 2016 -> Thu Mar 10 20:10:05 UTC 2044
    sha256  : 1e08a903aef9c3a721510b64ec764d01d3d094eb954161b62544ea8f187b5953
```

Only `JAR_SIG_UNPROTECTED_ZIP_ENTRY` warnings appear (normal for an APK that also carries
v2/v3). The same run over the **original** gives:

```
    subject : CN=Android Debug, O=Android, C=US
    serial  : 946b97d7b06d0bd0
    valid   : Sun Aug 30 17:15:23 UTC 2026 -> Thu Jan 15 17:15:23 UTC 2054
    sha256  : fb2c19d951ed18744decce97398cc2de897f96449ae7d73bd4aa088e070093ed
```

The same certificate is used for the v1, v2 and v3 signatures of both files — checked
per scheme with `getV1SchemeSigners()` / `getV2SchemeSigners()` / `getV3SchemeSigners()`.

`verify.py` additionally recomputes the **content digest** of the v2 and v3 blocks from
scratch — the file before the signing block, the central directory, and the EOCD with its
"offset of central directory" field rewritten to the start of the signing block, chunked
into 1 MiB pieces as `0xA5 || len || chunk` and combined as
`0x5A || count || digests`:

```
v2: 1 signer(s)
  signer[0] algo=0x0103 len=32 stored=8fe23383… computed=8fe23383… MATCH
v3: 1 signer(s)
  signer[0] algo=0x0103 len=32 stored=8fe23383… computed=8fe23383… MATCH
```

The same script run against the original also reports `MATCH` for both schemes
(`81ebd031…`), i.e. the repackager's signature is intact over the original content too.

Two observations that matter:

* The delivered build is signed with the **stock AOSP debug keystore** — the key that
  ships with the Android SDK and is therefore public knowledge (created 2016-10-23,
  valid until 2044). Its signature proves *internal consistency*, nothing about
  provenance. The same is true of the original "antisplit" build, which was signed with
  a freshly minted debug key on 2026-08-30.
* Neither digest is in the app's allow-list (§5), so both builds are Pro-disabled by
  design. See `REPORT.md` §4b.

## 5. The app's own certificate allow-list

Recovered from the dex and confirmed against both APKs:

```
eebb863d29141f9157d3c50b47d05d1eb8a734d0c46781b9e3d015b71b751d1d   ← Play App Signing key
272488427b2f4c7b609a5f313a2221b5d566a7414d448b79b3e48d483e20d6b6
370e01d26eb131f153c80090064b31fb3332bf03910f86d327b500a5556333af
1b06912e1226c9cbd8070b790d4285eccf448da5a5266c87fc6db4c0d5dc6446
```

The first entry is the Play App Signing certificate of the original release, which is
consistent with the digest quoted in the older `shima-rea` analysis of v0.2.0.

The comparison against the running APK's own signing certificate is constant-time
(char-by-char XOR accumulation), and the outcome is combined with a `launches` counter
kept in the `island_integrity` shared preferences:

* the counter is incremented on every launch and **capped at 3**;
* the tamper bit is only applied once the counter reaches **2**, i.e. **from the third
  launch onward** — the first two launches are forgiven.

Neither the delivered build's digest (`1e08a903…b5953`) nor the original's
(`fb2c19d9…0093ed`) appears in the list, so **both** builds lose Pro on the third run.
That is the developer's design, not an artefact of the patch, and the delivered build
does not attempt to change it.

## 6. Alignment

| | delivered build | original |
|---|---|---|
| Stored (uncompressed) entries | 46 | 46 |
| Entries violating the 4-byte alignment rule | **0** | **6** |

The delivered build **is** zipaligned; the original is **not** — `AndroidManifest.xml`,
three of the fonts (`res/v/b.ttf`, `c.ttf`, `e.ttf`) and the two `dexopt` baseline
profiles sit at unaligned offsets. Re-aligning was part of the rebuild, so the delivered
build is better-formed than the file it came from.

## 7. Summary

| Claim | Verdict |
|---|---|
| The container is valid and complete | ✅ 1358 entries, no corruption |
| Only the licence check was removed | ✅ one instruction, one method, one class |
| Nothing else in the app was modified | ✅ 1354 entries byte-identical, 17 963/17 964 methods identical |
| The build is signed | ✅ v1 + v2 + v3 verify with Google's apksig, digests recomputed and matching |
| The build is zipaligned | ✅ 0 misaligned entries (the original has 6) |
| Pro remains locked | ✅ by the app's own allow-list, exactly as in the original |
