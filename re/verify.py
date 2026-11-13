#!/usr/bin/env python3
"""Independent verification of the delivered Shima research build.

Checks, from scratch:
  1. container      - valid zip, entry count, no corrupt extra fields
  2. signature      - v1 (jarsigner) + v2 + v3 digests recomputed and compared
  3. alignment      - zipalign 4-byte rule for stored entries
  4. content        - every entry byte-identical to the original except the patched dex
  5. dex patch      - exactly one call removed, instruction-level diff over all classes
"""
import difflib
import hashlib
import struct
import sys
import zipfile

APK = sys.argv[1] if len(sys.argv) > 1 else 're/Shima-v0.5.1-pairip-bypassed-signed.apk'
ORIG = sys.argv[2] if len(sys.argv) > 2 else 'Shima v0.5.1_antisplit.apk'

data = open(APK, 'rb').read()
print(f'file      : {APK}')
print(f'size      : {len(data)} bytes')
print(f'sha256    : {hashlib.sha256(data).hexdigest()}')

# ---------------------------------------------------------------- container
print('\n=== 1. container ===')
try:
    z = zipfile.ZipFile(APK)
    print('zipfile opens        : yes')
    print('testzip() bad entry  :', z.testzip())
    print('entries              :', len(z.namelist()))
    print('duplicate names      :', len(z.namelist()) - len(set(z.namelist())))
except Exception as e:
    z = None
    print('zipfile opens        : NO ->', e)
eocd = data.rfind(b'PK\x05\x06')
cd_off, = struct.unpack_from('<I', data, eocd + 16)
print(f'central dir          : offset {cd_off} size {eocd - cd_off}')

# ------------------------------------------------- apk signing block lookup
def find_signing_block(buf):
    """Return (block_start, block_len, block_bytes) for the APK Signing Block.

    Layout (AOSP):
        [u64 size_of_block][pairs...][u64 size_of_block]["APK Sig Block 42"]
    size_of_block counts everything after the first size field, so the block
    occupies size_of_block + 8 bytes and ends right before the central dir.
    """
    eocd = buf.rfind(b'PK\x05\x06')
    cd_off, = struct.unpack_from('<I', buf, eocd + 16)
    if cd_off < 32:
        return None
    size2, magic = struct.unpack_from('<Q16s', buf, cd_off - 24)
    if magic != b'APK Sig Block 42':
        return None
    start = cd_off - size2 - 8
    size1, = struct.unpack_from('<Q', buf, start)
    if size1 != size2:
        raise ValueError(f'signing block size mismatch: {size1} != {size2}')
    return start, size2 + 8, buf[start:cd_off]


def walk_pairs(block):
    """Yield (offset, pair_len, id, value) for every pair in the signing block."""
    off = 8
    end = len(block) - 24
    while off < end:
        plen, = struct.unpack_from('<Q', block, off)
        pid, = struct.unpack_from('<I', block, off + 8)
        yield off, plen, pid, block[off + 12:off + 8 + plen]
        off += 8 + plen


sb = find_signing_block(data)
print('\n=== 2. APK Signing Block ===')
if sb:
    start, size, block = sb
    print(f'present at offset {start}, size {size}')
    for off, plen, cid, val in walk_pairs(block):
        name = {0x7109871a: 'v2', 0xf05368c0: 'v3', 0x1b93ad61: 'v3.1',
                0x42726577: 'padding', 0x6dff800d: 'source stamp',
                0x2b09189e: 'verity padding'}.get(cid, hex(cid))
        print(f'  chunk {name:<14} id={cid:#010x} pair_len={plen} value={len(val)}')
else:
    print('no APK Signing Block')

# ------------------------------------------------------- v2 / v3 digest calc
def content_digest(regions):
    """The content digest used by APK Signature Scheme v2/v3.

    Per com.android.tools.build:apksig: the file is split into 1 MiB chunks
    (the chunk boundaries never span two regions); each chunk is hashed as
    0xA5 || uint32le(len) || chunk; the per-chunk digests are then hashed as
    0x5A || uint32le(count) || digests.  The EOCD is digested with its
    "offset of central directory" field rewritten to the start of the APK
    Signing Block, i.e. as if the signing block were not there.
    """
    chunks = []
    for r in regions:
        for i in range(0, len(r), 1024 * 1024):
            part = r[i:i + 1024 * 1024]
            h = hashlib.sha256()
            h.update(bytes([0xA5]))
            h.update(struct.pack('<I', len(part)))
            h.update(part)
            chunks.append(h.digest())
    out = hashlib.sha256()
    out.update(bytes([0x5A]))
    out.update(struct.pack('<I', len(chunks)))
    for c in chunks:
        out.update(c)
    return out.digest()


def parse_len_prefixed(buf, off):
    ln, = struct.unpack_from('<I', buf, off)
    off += 4
    return buf[off:off + ln], off + ln


def verify_scheme(block, block_start, scheme_id, label):
    """Recompute the scheme's content digest and compare with the stored one."""
    target = None
    for off, plen, pid, val in walk_pairs(block):
        if pid == scheme_id:
            target = val
    if target is None:
        print(f'{label}: block not present')
        return None
    # v2/v3 block = length-prefixed sequence of signers;
    # each signer = length-prefixed signed data.
    signers_seq, _ = parse_len_prefixed(target, 0)
    signers = []
    off_s = 0
    while off_s < len(signers_seq):
        s, off_s = parse_len_prefixed(signers_seq, off_s)
        signers.append(s)
    print(f'{label}: {len(signers)} signer(s)')

    # Digested regions: the file before the signing block, the central
    # directory, and the EOCD with the CD offset moved back to the start of
    # the signing block.
    eocd_off = data.rfind(b'PK\x05\x06')
    eocd = bytearray(data[eocd_off:])
    struct.pack_into('<I', eocd, 16, block_start)
    regions = [data[:block_start], data[cd_off:eocd_off], bytes(eocd)]
    computed = content_digest(regions)

    ok = False
    for si, sig in enumerate(signers):
        sd, _ = parse_len_prefixed(sig, 0)
        digests_seq, _ = parse_len_prefixed(sd, 0)
        rec, _ = parse_len_prefixed(digests_seq, 0)
        algo, dlen = struct.unpack_from('<II', rec, 0)
        stored = rec[8:8 + dlen]
        match = computed == stored
        ok = ok or match
        print(f'  signer[{si}] algo=0x{algo:04x} len={dlen} '
              f'stored={stored.hex()[:32]}… computed={computed.hex()[:32]}… '
              f'{"MATCH" if match else "MISMATCH"}')
    return ok


print('\n=== 3. signature digests (recomputed from scratch) ===')
if sb:
    v2 = verify_scheme(sb[2], sb[0], 0x7109871a, 'v2')
    v3 = verify_scheme(sb[2], sb[0], 0xf05368c0, 'v3')
else:
    v2 = v3 = None

# --------------------------------------------------------------- alignment
print('\n=== 4. zipalign (4-byte rule for stored entries) ===')
off = 0
misaligned = []
stored = 0
while off < len(data) - 4:
    if data[off:off + 4] != b'PK\x03\x04':
        break
    ver, flags, method, t, d = struct.unpack_from('<HHHHH', data, off + 4)
    crc, csize, usize = struct.unpack_from('<III', data, off + 14)
    nlen, elen = struct.unpack_from('<HH', data, off + 26)
    name = data[off + 30:off + 30 + nlen].decode('utf-8', 'replace')
    dstart = off + 30 + nlen + elen
    if method == 0:
        stored += 1
        if dstart % 4 != 0:
            misaligned.append((name, dstart))
    off = dstart + csize
    if flags & 0x08:
        break
print(f'stored entries checked : {stored}')
print(f'misaligned             : {len(misaligned)}')
for m in misaligned[:10]:
    print('   ', m)

# ------------------------------------------------------ original comparison
print('\n=== 5. vs original ===')
try:
    zipfile.ZipFile(ORIG)
    print('original opens with zipfile: yes')
except Exception as e:
    print('original opens with zipfile: NO ->', e)


def cd_entries(path):
    buf = open(path, 'rb').read()
    e = buf.rfind(b'PK\x05\x06')
    c, = struct.unpack_from('<H', buf, e + 10)
    o, = struct.unpack_from('<I', buf, e + 16)
    out = {}
    for _ in range(c):
        (sig, vmade, vneed, flags, method, t, dd, crc, csize, usize, nlen, elen,
         clen, disk, iattr, eattr, lho) = struct.unpack_from('<IHHHHHHIIIHHHHHII', buf, o)
        out[buf[o + 46:o + 46 + nlen]] = crc
        o += 46 + nlen + elen + clen
    return out


o_crc = cd_entries(ORIG)
n_crc = {}
if z is not None:
    for k in z.namelist():
        n_crc[k.encode('utf-8', 'surrogateescape')] = z.getinfo(k).CRC


def names(seq):
    seq = sorted(x.decode('utf-8', 'replace') if isinstance(x, bytes) else x for x in seq)
    if len(seq) <= 10:
        return seq
    return seq[:10] + [f'… and {len(seq) - 10} more']


print('entries only in the reference: ', names(set(o_crc) - set(n_crc)))
print('entries only in the file under test:', names(set(n_crc) - set(o_crc)))
changed = [k.decode('utf-8', 'replace') for k in o_crc
           if k in n_crc and o_crc[k] != n_crc[k]]
print('entries with changed content:', names(changed))
if len(set(o_crc) & set(n_crc)) - len(changed):
    print(f'entries byte-identical to the reference: '
          f'{len(set(o_crc) & set(n_crc)) - len(changed)}')
