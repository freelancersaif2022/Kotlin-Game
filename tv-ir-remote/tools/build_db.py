#!/usr/bin/env python3
"""Builds app/src/main/assets/irdb.txt from the Flipper-IRDB "TVs" folder.

Source database: https://github.com/Lucaslhm/Flipper-IRDB (CC0 1.0).

    git clone --depth 1 https://github.com/Lucaslhm/Flipper-IRDB /tmp/fdb
    python3 tools/build_db.py /tmp/fdb/TVs app/src/main/assets/irdb.txt

Output format (one record per line, tab separated):
    B <brand>
    S <label> <KEY>=<signal> <KEY>=<signal> ...
signal:
    <PROTO>:<address hex>:<command hex>       parsed protocol signal
    R<frequency>:<t1>.<t2>.<t3>...            raw mark/space timings in microseconds
"""
import os
import re
import sys
from collections import Counter, OrderedDict, defaultdict

PROTOS = {
    "NEC": "NEC", "NECext": "NECX", "Samsung32": "S32", "RC5": "RC5", "RC5X": "RC5X",
    "RC6": "RC6", "SIRC": "SIRC", "SIRC15": "SIRC15", "SIRC20": "SIRC20",
    "Kaseikyo": "KAS", "RCA": "RCA", "Pioneer": "PIO",
}

# canonical key -> accepted (normalized) Flipper names, in priority order
KEY_NAMES = OrderedDict([
    ("POWER", ["power", "on_off", "power_toggle", "pwr", "standby", "power_on_off", "tv_power"]),
    ("POWER_ON", ["power_on", "poweron", "on", "pwr_on", "discrete_on"]),
    ("POWER_OFF", ["power_off", "poweroff", "off", "pwr_off", "discrete_off"]),
    ("VOL_UP", ["vol_up", "vol+", "volume_up", "volup", "vol_plus", "volume+", "vol_upp"]),
    ("VOL_DOWN", ["vol_dn", "vol_down", "vol-", "vol_dwn", "volume_down", "voldown", "vol_minus", "volume-"]),
    ("MUTE", ["mute", "mute_toggle"]),
    ("CH_UP", ["ch_next", "ch_up", "ch+", "chan_up", "pr_up", "channel_up", "ch_plus", "p+", "prog_up"]),
    ("CH_DOWN", ["ch_prev", "ch_dn", "ch_down", "ch-", "chan_down", "pr_down", "channel_down", "ch_minus", "p-", "prog_down"]),
    ("UP", ["up", "up_arrow", "arrow_up", "cursor_up", "nav_up"]),
    ("DOWN", ["down", "dn", "dwn", "down_arrow", "arrow_down", "cursor_down", "nav_down"]),
    ("LEFT", ["left", "left_arrow", "arrow_left", "cursor_left", "nav_left"]),
    ("RIGHT", ["right", "right_arrow", "arrow_right", "cursor_right", "nav_right"]),
    ("OK", ["ok", "select", "enter", "ent", "center", "confirm"]),
    ("MENU", ["menu", "main_menu"]),
    ("HOME", ["home", "smart_hub", "smart", "smarthub", "home_menu"]),
    ("BACK", ["back", "return", "ret", "back_return"]),
    ("EXIT", ["exit", "close"]),
    ("INFO", ["info", "display", "i", "information"]),
    ("INPUT", ["source", "input", "sources", "input_source", "tv/av", "tv_av", "av", "input_tv", "inputs"]),
    ("SETTINGS", ["settings", "setup", "setting"]),
    ("OPTIONS", ["options", "option", "tools", "q_menu", "quick_menu", "quick", "t.opt", "opt"]),
    ("GUIDE", ["guide", "epg", "tv_guide"]),
    ("D1", ["1"]), ("D2", ["2"]), ("D3", ["3"]), ("D4", ["4"]), ("D5", ["5"]),
    ("D6", ["6"]), ("D7", ["7"]), ("D8", ["8"]), ("D9", ["9"]), ("D0", ["0"]),
    ("DASH", ["dash", "-", "_", "dot", ".", "ch_dash", "minus", "-/--"]),
    ("PRE_CH", ["pre_ch", "prev_ch", "last", "recall", "flashback", "flashbk", "jump", "pre-ch",
                "prev_channel", "q.view", "last_view", "swap", "return_ch"]),
    ("PLAY", ["play"]),
    ("PAUSE", ["pause", "still", "freeze"]),
    ("PLAY_PAUSE", ["play_pause", "play/pause", "play_pa", "playpause"]),
    ("STOP", ["stop"]),
    ("REWIND", ["rewind", "rew", "rwd", "rw", "fast_ba", "backward", "rev", "reverse", "fast_backward", "rr"]),
    ("FFWD", ["fast_forward", "ff", "fwd", "forward", "fast_fo", "fastforward", "fast_fwd", "ffw", "fw", "fast forward"]),
    ("PREV", ["prev", "previous", "skip_back", "skip_prev", "skip_backward"]),
    ("NEXT", ["next", "skip_fwd", "skip_forward", "skip", "skip_next"]),
    ("RECORD", ["record", "rec", "rec/*"]),
    ("RED", ["red", "a_red", "red_a", "a"]),
    ("GREEN", ["green", "b_green", "green_b", "b"]),
    ("YELLOW", ["yellow", "c_yellow", "yellow_c", "c"]),
    ("BLUE", ["blue", "d_blue", "blue_d", "d"]),
    ("NETFLIX", ["netflix"]),
    ("YOUTUBE", ["youtube"]),
    ("PRIME", ["prime_video", "prime", "amazon", "primevideo", "amazon_prime"]),
    ("DISNEY", ["disney+", "disney_plus", "disney", "disneyplus"]),
    ("APPS", ["apps", "app", "my_apps"]),
    ("SLEEP", ["sleep", "timer", "sleep_timer"]),
    ("SUBTITLE", ["subtitle", "cc", "subtitles", "caption", "captions", "sttl", "sub", "ad_subt", "ccd", "ad/subt."]),
    ("ASPECT", ["aspect", "p_size", "p.size", "pic_size", "size", "ratio", "wide", "zoom", "format", "aspect_ratio"]),
    ("TEXT", ["text", "txt", "teletext", "ttx", "ttx/mix", "ttx_mix"]),
    ("LIST", ["ch_list", "list", "chlist", "channel_list"]),
    ("FAV", ["fav", "favorite", "favorites", "favourite"]),
    ("AUDIO", ["audio", "mts", "sap", "mts_sap", "language", "lang"]),
    ("PICTURE", ["picture", "p_mode", "pmode", "picturemode", "picture_mode", "pic", "p.mode", "image", "psm"]),
    ("SOUND", ["s_mode", "smode", "sound", "s.mode", "sound_mode", "ssm"]),
])
NAME_TO_KEY = {}
NAME_PRIORITY = {}
for key, names in KEY_NAMES.items():
    for i, n in enumerate(names):
        NAME_TO_KEY.setdefault(n, key)
        NAME_PRIORITY.setdefault(n, i)


def norm(name):
    n = name.strip().lower().replace(" ", "_")
    return n


def parse_hex_le(s):
    b = [int(x, 16) for x in s.split()]
    v = 0
    for i, x in enumerate(b):
        v |= x << (8 * i)
    return v


def trim_raw(timings):
    """Keep the first frame plus up to two repeats, at most ~400 ms in total."""
    if not timings:
        return []
    if len(timings) % 2 == 1:
        timings = timings + [20000]
    frames, cur = [], []
    for i in range(0, len(timings), 2):
        cur += timings[i:i + 2]
        if timings[i + 1] > 8000:
            frames.append(cur)
            cur = []
    if cur:
        frames.append(cur)
    out, total = [], 0
    for f in frames[:3]:
        d = sum(f)
        if out and total + d > 400000:
            break
        out += f
        total += d
    return out


def parse_file(path):
    text = open(path, encoding="utf-8", errors="replace").read()
    text = text.replace("type: parsedFiletype", "type: parsed\nFiletype")
    signals = []
    cur = {}
    for line in text.splitlines():
        line = line.strip()
        if line.startswith("#"):
            if cur:
                signals.append(cur)
            cur = {}
            continue
        if ":" not in line:
            continue
        k, v = line.split(":", 1)
        k, v = k.strip(), v.strip()
        if k == "name" and "name" in cur:
            signals.append(cur)
            cur = {}
        cur[k] = v
    if cur:
        signals.append(cur)

    keys = {}
    prio = {}
    for s in signals:
        if "name" not in s:
            continue
        n = norm(s["name"])
        key = NAME_TO_KEY.get(n)
        if key is None:
            continue
        sig = None
        if s.get("type") == "parsed" and s.get("protocol") in PROTOS:
            try:
                addr = parse_hex_le(s["address"])
                cmd = parse_hex_le(s["command"])
            except (KeyError, ValueError):
                continue
            sig = "%s:%X:%X" % (PROTOS[s["protocol"]], addr, cmd)
        elif s.get("type") == "raw" and "data" in s:
            try:
                freq = int(float(s.get("frequency", "38000")))
                t = [int(x) for x in s["data"].split()]
            except ValueError:
                continue
            t = trim_raw(t)
            if len(t) < 4:
                continue
            sig = "R%d:%s" % (freq, ".".join(str(x) for x in t))
        if sig is None:
            continue
        p = NAME_PRIORITY[n]
        if key not in keys or p < prio[key]:
            keys[key] = sig
            prio[key] = p
    return keys


def device_id(keys):
    c = Counter()
    for sig in keys.values():
        if not sig.startswith("R"):
            proto, addr, _ = sig.split(":")
            c[(proto, addr)] += 1
    return c.most_common(1)[0][0] if c else None


def label_for(fname, brand):
    base = os.path.splitext(fname)[0]
    b = brand.replace(" ", "_")
    if base.lower().startswith(b.lower()):
        base = base[len(b):]
    base = base.strip("_- ").replace("_", " ")
    return base or "Standard"


def main():
    src, dst = sys.argv[1], sys.argv[2]
    brands = []
    for brand in sorted(os.listdir(src), key=str.lower):
        bdir = os.path.join(src, brand)
        if not os.path.isdir(bdir):
            continue
        files = []
        for root, _, names in os.walk(bdir):
            for n in sorted(names):
                if n.endswith(".ir"):
                    files.append((n, parse_file(os.path.join(root, n))))
        # Group files that drive the same device code map (protocol + address), then pick each
        # key's signal by majority vote so a single mislabelled capture cannot win.
        groups = OrderedDict()
        for fname, keys in files:
            if not keys:
                continue
            gid = device_id(keys) or ("RAW", keys.get("POWER") or keys.get("POWER_ON") or fname)
            groups.setdefault(gid, []).append((label_for(fname, brand), keys))
        sets = []  # [labels, keys, file count]
        for gid, members in groups.items():
            votes = defaultdict(Counter)
            for _, keys in members:
                for k, v in keys.items():
                    votes[k][v] += 1
            base = {k: c.most_common(1)[0][0] for k, c in votes.items()}
            labels = [m[0] for m in members]
            sets.append([labels, base, len(members)])
            # alternative POWER codes seen for this device become their own code sets
            for alt, cnt in votes.get("POWER", Counter()).most_common()[1:]:
                variant = dict(base)
                variant["POWER"] = alt
                alt_labels = [m[0] for m in members if m[1].get("POWER") == alt]
                sets.append([alt_labels, variant, cnt])
        sets = [s for s in sets if "POWER" in s[1] or "POWER_ON" in s[1] or len(s[1]) >= 5]
        sets.sort(key=lambda s: (-s[2], -len(s[1])))
        if sets:
            brands.append((brand.replace("_", " "), sets))

    key_order = list(KEY_NAMES.keys())
    total_sets = 0
    with open(dst, "w", encoding="utf-8") as out:
        out.write("# Generated by tools/build_db.py from Flipper-IRDB (CC0 1.0). Do not edit.\n")
        for brand, sets in brands:
            out.write("B\t%s\n" % brand)
            for labels, keys, _ in sets:
                label = ", ".join(OrderedDict.fromkeys(labels))
                if len(label) > 60:
                    label = label[:57] + "..."
                body = " ".join("%s=%s" % (k, keys[k]) for k in key_order if k in keys)
                out.write("S\t%s\t%s\n" % (label, body))
                total_sets += 1
    print("brands=%d sets=%d bytes=%d" % (len(brands), total_sets, os.path.getsize(dst)))


if __name__ == "__main__":
    main()
