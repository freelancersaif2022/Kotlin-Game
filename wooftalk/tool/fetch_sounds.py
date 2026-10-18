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
import urllib.error
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
    "tiger": (["tiger roar"], 0.5, 4.0, 60, 6),
    "lion": (["lion roar"], 0.5, 5.0, 50, 6),
    "zebra": (["zebra"], 0.3, 3.0, 100, 6),
    "deer": (["deer"], 0.2, 3.0, 80, 8),
    "bird": (["bird"], 0.08, 1.6, 800, 8),
    "elephant": (["elephant"], 0.4, 3.5, 60, 6),
    "monkey": (["monkey"], 0.15, 3.0, 150, 8),
    "owl": (["owl"], 0.2, 2.5, 150, 8),
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


def lookup(titles):
    data = api({
        "action": "query",
        "titles": "|".join("File:" + t for t in titles),
        "prop": "imageinfo",
        "iiprop": "url|size|mime|extmetadata",
        "iiextmetadatafilter": "LicenseShortName|Artist|LicenseUrl",
    })
    out = []
    for p in data.get("query", {}).get("pages", []):
        ii = (p.get("imageinfo") or [{}])[0]
        meta = ii.get("extmetadata", {})
        out.append({
            "title": p["title"],
            "url": ii.get("url"),
            "page": ii.get("descriptionurl"),
            "size": ii.get("size", 0),
            "license": strip_html(meta.get("LicenseShortName", {}).get("value")),
            "license_url": meta.get("LicenseUrl", {}).get("value", ""),
            "artist": strip_html(meta.get("Artist", {}).get("value")) or "Unknown",
        })
    return out


def download(url, path):
    import time
    for attempt in range(6):
        try:
            req = urllib.request.Request(url, headers={"User-Agent": UA})
            with urllib.request.urlopen(req, timeout=180) as r, open(path, "wb") as fh:
                fh.write(r.read())
            time.sleep(2)
            return
        except urllib.error.HTTPError as e:
            if e.code != 429 or attempt == 5:
                raise
            wait = 10 * (attempt + 1)
            print(f"    429, waiting {wait}s")
            time.sleep(wait)


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


def loudest_windows(x, lo, hi, count=3):
    """Fallback for recordings that are one continuous call (no quiet gaps
    to measure noise from): a short file is used whole, a long one gives
    its loudest non-overlapping windows."""
    dur = len(x) / SR
    if dur < lo:
        return []
    if dur <= hi + 0.6:
        return [(0.0, 0, len(x))]
    win = int(SR * min(hi, 3.0))
    hop = SR // 10
    energies = []
    for a in range(0, len(x) - win, hop):
        e = sum(v * v for v in x[a:a + win:8])
        energies.append((e, a))
    energies.sort(reverse=True)
    out = []
    for e, a in energies:
        if all(abs(a - b) >= win for _, b, _ in out):
            out.append((10 * math.log10(e + 1e-12), a, a + win))
        if len(out) == count:
            break
    return out


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
    "tiger": ["tiger roar", "Panthera tigris", "tiger growl", "tiger chuff"],
    "zebra": ["zebra", "Equus quagga", "zebra call", "Equus burchellii"],
    "deer": ["deer call", "red deer roar", "Cervus elaphus", "deer bark", "fawn bleat", "Muntiacus", "roe deer"],
    "bird": ["bird song", "songbird", "Turdus merula", "Erithacus rubecula", "canary singing", "sparrow chirp"],
    "lion": ["lion roar", "Panthera leo", "lion growl"],
    "elephant": ["elephant trumpet", "Loxodonta", "Elephas maximus", "elephant call"],
    "monkey": ["monkey call", "gibbon", "macaque", "howler monkey", "Alouatta", "chimpanzee"],
    "owl": ["owl hoot", "Strix aluco", "Bubo bubo", "owl call"],
}


def list_candidates():
    import time
    for animal, queries in LIST_QUERIES.items():
        if animal in json.load(open(os.path.join(os.path.dirname(__file__), "sound_sources.json"))):
            continue
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
    sources = json.load(open(os.path.join(os.path.dirname(__file__), "sound_sources.json")))
    for animal in wanted:
        _, lo, hi, hp, max_calls = ANIMALS[animal]
        titles = sources.get(animal, [])
        cands = []
        for i in range(0, len(titles), 20):
            cands += lookup(titles[i:i + 20])
        print(f"\n== {animal}: {len(cands)} sources")
        per_file = max(2, -(-max_calls // max(1, len(cands))) + 1)
        picked = []  # (score, samples, source)
        for c in cands:
            if not c["url"]:
                print(f"  missing {c['title']}")
                continue
            if not OK_LICENSE.match(c["license"] or ""):
                print(f"  skip  [{c['license']}] {c['title']}")
                continue
            with tempfile.TemporaryDirectory() as tmpdir:
                path = os.path.join(tmpdir, "src" + os.path.splitext(c["url"])[1])
                try:
                    download(c["url"], path)
                    x = decode(path, hp)
                except Exception as e:  # noqa: BLE001
                    print(f"  fail  {c['title']}: {e}")
                    continue
            calls, snr = find_calls(x, lo, hi)
            if not calls:
                calls = loudest_windows(x, lo, hi)
                snr = 20 if calls else snr
            print(f"  ok    [{c['license']}] snr={snr:.0f}dB calls={len(calls)} {c['title']}")
            calls.sort(reverse=True)
            for loud, a, b in calls[:per_file]:
                pad = int(SR * 0.03)
                seg = x[max(0, a - pad): min(len(x), b + pad)]
                picked.append((loud + min(snr, 40) * 0.3, seg, c))
        # keep variety: best call of every file first, then the rest
        firsts, rest, seen_src = [], [], set()
        for item in sorted(picked, key=lambda t: -t[0]):
            (rest if item[2]["title"] in seen_src else firsts).append(item)
            seen_src.add(item[2]["title"])
        picked = firsts + rest
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
