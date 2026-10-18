#!/usr/bin/env python3
"""Downloads real, freely licensed animal recordings from Wikimedia Commons
and cuts them into short single calls (one bark, one meow, ...).

Runs in CI (needs internet + ffmpeg). Output:
  assets/sounds/<animal>/<n>.wav   16-bit mono 22050 Hz
  assets/sounds/CREDITS.md         author + license of every source file

Usage: python3 tool/fetch_sounds.py [animal ...]
"""
import array
import json
import math
import os
import re
import subprocess
import sys
import tempfile
import urllib.parse
import urllib.request

API = "https://commons.wikimedia.org/w/api.php"
UA = "WoofTalkSoundFetcher/1.0 (https://github.com/freelancersaif2022/Kotlin-Game)"
SR = 22050
OUT = os.path.join(os.path.dirname(__file__), "..", "assets", "sounds")
OK_LICENSE = re.compile(r"^(public domain|pd.*|cc0.*|cc[- ]by[- ][0-9.]+.*|cc[- ]by-sa[- ][0-9.]+.*)$", re.I)

# id: (search queries, min call sec, max call sec, highpass Hz, max calls)
ANIMALS = {
    "dog": (["dog barking", "dog bark"], 0.10, 0.70, 120, 8),
    "puppy": (["puppy barking", "puppy yelping", "puppy"], 0.07, 0.70, 200, 8),
    "cat": (["cat meow", "meowing cat", "cat meowing"], 0.25, 1.60, 150, 8),
    "wolf": (["wolf howling", "wolf howl"], 0.80, 5.00, 100, 6),
    "duck": (["duck quack", "mallard quack", "duck quacking"], 0.08, 0.70, 150, 8),
    "cow": (["cow moo", "cow mooing", "cattle mooing"], 0.50, 3.00, 60, 6),
    "rooster": (["rooster crowing", "rooster crow", "cock crowing"], 0.80, 4.00, 150, 6),
    "sheep": (["sheep bleating", "sheep baa", "sheep"], 0.30, 2.00, 120, 8),
    "pig": (["pig grunting", "pig oink", "pig squeal"], 0.10, 1.20, 80, 8),
    "horse": (["horse neighing", "horse whinny", "horse neigh"], 0.60, 3.00, 100, 6),
    "goat": (["goat bleating", "goat"], 0.25, 1.80, 120, 8),
    "frog": (["frog croaking", "frog call", "frog"], 0.06, 1.20, 100, 8),
}


def api(params):
    params = dict(params, format="json", formatversion="2")
    url = API + "?" + urllib.parse.urlencode(params)
    req = urllib.request.Request(url, headers={"User-Agent": UA})
    with urllib.request.urlopen(req, timeout=60) as r:
        return json.load(r)


def strip_html(s):
    return re.sub(r"<[^>]+>", "", s or "").strip()


def search(query, limit=25):
    data = api({
        "action": "query",
        "generator": "search",
        "gsrsearch": f"{query} filetype:audio",
        "gsrnamespace": 6,
        "gsrlimit": limit,
        "prop": "imageinfo",
        "iiprop": "url|size|mime|extmetadata",
        "iiextmetadatafilter": "LicenseShortName|Artist|LicenseUrl",
    })
    out = []
    for p in data.get("query", {}).get("pages", []):
        ii = (p.get("imageinfo") or [{}])[0]
        meta = ii.get("extmetadata", {})
        lic = strip_html(meta.get("LicenseShortName", {}).get("value"))
        out.append({
            "title": p["title"],
            "url": ii.get("url"),
            "page": ii.get("descriptionurl"),
            "size": ii.get("size", 0),
            "license": lic,
            "license_url": meta.get("LicenseUrl", {}).get("value", ""),
            "artist": strip_html(meta.get("Artist", {}).get("value")) or "Unknown",
        })
    return out


def decode(path, highpass):
    cmd = ["ffmpeg", "-v", "error", "-i", path, "-t", "90", "-ac", "1", "-ar", str(SR),
           "-af", f"highpass=f={highpass},lowpass=f=10000", "-f", "s16le", "-"]
    raw = subprocess.run(cmd, capture_output=True, timeout=120).stdout
    a = array.array("h")
    a.frombytes(raw[: len(raw) // 2 * 2])
    return [x / 32768.0 for x in a]


def envelope(x, hop):
    env = []
    for i in range(0, len(x) - hop, hop):
        s = sum(v * v for v in x[i:i + hop]) / hop
        env.append(10 * math.log10(s + 1e-12))
    return env


def pct(vals, q):
    s = sorted(vals)
    return s[min(len(s) - 1, int(len(s) * q))]


def find_calls(x, lo, hi):
    hop = SR // 100  # 10 ms
    env = envelope(x, hop)
    if len(env) < 20:
        return [], 0
    floor, peak = pct(env, 0.2), pct(env, 0.99)
    snr = peak - floor
    if snr < 14:
        return [], snr
    thr = floor + 0.42 * snr
    segs, start = [], None
    for i, e in enumerate(env):
        if e > thr and start is None:
            start = i
        elif e <= thr and start is not None:
            segs.append([start, i])
            start = None
    if start is not None:
        segs.append([start, len(env)])
    gap = 6 if hi < 1 else 12
    merged = []
    for s in segs:
        if merged and s[0] - merged[-1][1] <= gap:
            merged[-1][1] = s[1]
        else:
            merged.append(s)
    calls = []
    for a, b in merged:
        # extend to where the sound fades into the floor
        while a > 0 and env[a - 1] > floor + 0.2 * snr:
            a -= 1
        while b < len(env) and env[b] > floor + 0.2 * snr:
            b += 1
        dur = (b - a) / 100
        if lo <= dur <= hi:
            loud = max(env[a:b]) - floor
            calls.append((loud, a * hop, b * hop))
    return calls, snr


def write_wav(path, samples):
    peak = max(abs(v) for v in samples) or 1
    g = 0.9 / peak
    n = len(samples)
    fade = min(int(SR * 0.012), n // 4)
    data = array.array("h")
    for i, v in enumerate(samples):
        f = 1.0
        if i < fade:
            f = i / fade
        elif i > n - fade:
            f = (n - i) / fade
        data.append(int(max(-1, min(1, v * g * f)) * 32767))
    with open(path, "wb") as fh:
        size = len(data) * 2
        import struct
        fh.write(b"RIFF" + struct.pack("<I", 36 + size) + b"WAVEfmt ")
        fh.write(struct.pack("<IHHIIHH", 16, 1, 1, SR, SR * 2, 2, 16))
        fh.write(b"data" + struct.pack("<I", size))
        fh.write(data.tobytes())


LIST_QUERIES = {
    "dog": ["dog barking", "dog bark", "Canis familiaris bark", "barking dog", "dog woof", "puppy barking", "puppy yelp", "puppy whimper"],
    "cat": ["cat meow", "meowing", "Felis catus", "kitten meow"],
    "wolf": ["wolf howl", "wolves howling", "Canis lupus howl"],
    "duck": ["duck quack", "Anas platyrhynchos", "mallard", "duck quacking"],
    "cow": ["cow moo", "mooing", "Bos taurus", "cattle"],
    "rooster": ["rooster crowing", "Gallus gallus domesticus", "cock crow"],
    "sheep": ["sheep bleating", "Ovis aries", "lamb bleating"],
    "pig": ["pig grunt", "Sus scrofa domesticus", "pigs oinking", "piglet"],
    "horse": ["horse neigh", "whinny", "Equus caballus"],
    "goat": ["goat bleat", "Capra hircus", "goats bleating", "kid goat"],
    "frog": ["frog croak", "bullfrog", "Lithobates", "Hyla", "Rana temporaria", "tree frog call"],
}


def list_candidates():
    import time
    for animal, queries in LIST_QUERIES.items():
        seen = set()
        print(f"\n##### {animal}")
        for q in queries:
            try:
                res = search(q, 40)
            except Exception as e:  # noqa: BLE001
                print(f"  search failed {q}: {e}")
                time.sleep(5)
                continue
            for c in res:
                if c["title"] in seen:
                    continue
                seen.add(c["title"])
                ok = "OK " if OK_LICENSE.match(c["license"] or "") else "NO "
                print(f"  {ok}{c['size']//1000:>6}KB [{c['license']}] {c['title']}")
            time.sleep(1.5)


def main():
    if sys.argv[1:2] == ["--list"]:
        return list_candidates()
    wanted = sys.argv[1:] or list(ANIMALS)
    os.makedirs(OUT, exist_ok=True)
    credits_path = os.path.join(OUT, "credits.json")
    credits = json.load(open(credits_path)) if os.path.exists(credits_path) else {}
    for animal in wanted:
        queries, lo, hi, hp, max_calls = ANIMALS[animal]
        seen, cands = set(), []
        for q in queries:
            try:
                for c in search(q):
                    if c["title"] in seen or not c["url"]:
                        continue
                    seen.add(c["title"])
                    cands.append(c)
            except Exception as e:  # noqa: BLE001
                print(f"  search failed for {q}: {e}")
        key = queries[0].split()[0]
        cands.sort(key=lambda c: key not in c["title"].lower())
        cands = [c for c in cands if OK_LICENSE.match(c["license"] or "")][:14]
        print(f"\n== {animal}: {len(cands)} usable candidates")
        picked = []  # (score, samples, source)
        for c in cands:
            ok = bool(OK_LICENSE.match(c["license"] or ""))
            name_ok = any(w in c["title"].lower() for w in queries[0].split()[:1])
            if not ok or c["size"] > 25_000_000:
                print(f"  skip  [{c['license']}] {c['title']}")
                continue
            with tempfile.NamedTemporaryFile(suffix=os.path.splitext(c["url"])[1]) as tmp:
                try:
                    req = urllib.request.Request(c["url"], headers={"User-Agent": UA})
                    with urllib.request.urlopen(req, timeout=120) as r:
                        tmp.write(r.read())
                    tmp.flush()
                    x = decode(tmp.name, hp)
                except Exception as e:  # noqa: BLE001
                    print(f"  fail  {c['title']}: {e}")
                    continue
            calls, snr = find_calls(x, lo, hi)
            print(f"  ok    [{c['license']}] snr={snr:.0f}dB calls={len(calls)} name_match={name_ok} {c['title']}")
            calls.sort(reverse=True)
            for loud, a, b in calls[:3]:
                pad = int(SR * 0.03)
                seg = x[max(0, a - pad): min(len(x), b + pad)]
                score = loud + (6 if name_ok else 0) + min(snr, 40) * 0.3
                picked.append((score, seg, c))
        picked.sort(key=lambda t: -t[0])
        picked = picked[:max_calls]
        adir = os.path.join(OUT, animal)
        os.makedirs(adir, exist_ok=True)
        for f in os.listdir(adir):
            os.remove(os.path.join(adir, f))
        credits[animal] = []
        for i, (score, seg, c) in enumerate(picked):
            write_wav(os.path.join(adir, f"{i}.wav"), seg)
            credits[animal].append({
                "file": f"{animal}/{i}.wav", "source": c["page"], "title": c["title"],
                "author": c["artist"], "license": c["license"], "license_url": c["license_url"],
                "seconds": round(len(seg) / SR, 2), "score": round(score, 1),
            })
            print(f"  -> {animal}/{i}.wav {len(seg)/SR:.2f}s score={score:.0f} from {c['title']}")
        if not picked:
            print(f"  !! no usable calls for {animal}")
    json.dump(credits, open(credits_path, "w"), indent=1)
    with open(os.path.join(OUT, "CREDITS.md"), "w") as fh:
        fh.write("# Animal sound credits\n\nAll sounds come from Wikimedia Commons and were "
                 "trimmed into short clips.\n\n")
        for animal, items in sorted(credits.items()):
            fh.write(f"## {animal}\n\n")
            for it in items:
                fh.write(f"- `{it['file']}`: [{it['title']}]({it['source']}) by "
                         f"{it['author']}, {it['license']}\n")
            fh.write("\n")


if __name__ == "__main__":
    main()
